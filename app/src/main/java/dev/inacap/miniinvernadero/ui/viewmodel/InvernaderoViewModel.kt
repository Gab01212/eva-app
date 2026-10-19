package dev.inacap.miniinvernadero.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.inacap.miniinvernadero.data.model.ActuatorState
import dev.inacap.miniinvernadero.data.model.AlertSeverity
import dev.inacap.miniinvernadero.data.model.DeviceMode
import dev.inacap.miniinvernadero.data.model.InterDeviceMessage
import dev.inacap.miniinvernadero.data.model.InvernaderoAlert
import dev.inacap.miniinvernadero.data.model.InvernaderoState
import dev.inacap.miniinvernadero.data.model.Role
import dev.inacap.miniinvernadero.data.model.SensorData
import dev.inacap.miniinvernadero.data.model.UserAccount
import dev.inacap.miniinvernadero.data.model.getCurrentTimeString
import dev.inacap.miniinvernadero.network.NetworkUtils
import dev.inacap.miniinvernadero.network.SocketManager
import dev.inacap.miniinvernadero.network.SocketPacket
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class InvernaderoViewModel(application: Application) : AndroidViewModel(application) {

    private val socketManager = SocketManager()
    private val prefs = application.getSharedPreferences("miniinvernadero_user_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Registered accounts list persisted in SharedPreferences
    private val _registeredUsers = MutableStateFlow<MutableList<UserAccount>>(mutableListOf())

    // Auth & Device Mode State
    private val _currentUser = MutableStateFlow(UserAccount(isLoggedIn = false))
    val currentUser: StateFlow<UserAccount> = _currentUser.asStateFlow()

    private val _deviceMode = MutableStateFlow(DeviceMode.TELEFONO_1_HOST)
    val deviceMode: StateFlow<DeviceMode> = _deviceMode.asStateFlow()

    // Main Greenhouse State
    private val _state = MutableStateFlow(InvernaderoState())
    val state: StateFlow<InvernaderoState> = _state.asStateFlow()

    val connectionStatus = socketManager.connectionStatus
    val isConnected = socketManager.isConnected
    val connectedClientsCount = socketManager.connectedClientsCount

    private val _localIp = MutableStateFlow("127.0.0.1")
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private var isSimulating = true

    init {
        // Load saved users from disk
        _registeredUsers.value = loadSavedUsers()

        // Load local IP
        val ip = NetworkUtils.getLocalIpAddress()
        _localIp.value = ip
        _state.value = _state.value.copy(hostIp = ip)

        // Socket Callbacks
        socketManager.onStateReceivedFromHost = { updatedStateFromHost ->
            if (_deviceMode.value == DeviceMode.TELEFONO_2_CLIENTE) {
                _state.value = updatedStateFromHost.copy(
                    hostIp = _state.value.hostIp,
                    isConnected = true,
                    lastSyncTime = getCurrentTimeString()
                )
            }
        }

        socketManager.onCommandReceivedFromClient = { packet ->
            handleSocketPacket(packet)
        }

        // Start initial alerts
        addAlert("Sistema Iniciado", "Nodo Arduino UNO R4 WiFi & R3 conectados vía Wi-Fi local", AlertSeverity.INFO)
        addAlert("Seguridad ISO 27400", "Entorno perimetral protegido con cifrado WPA2/WPA3", AlertSeverity.INFO)

        // Start Sensor Simulation loop
        startSimulationLoop()
    }

    private fun loadSavedUsers(): MutableList<UserAccount> {
        val json = prefs.getString("registered_users_json", null)
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<UserAccount>>() {}.type
                val savedList: List<UserAccount> = gson.fromJson(json, type)
                if (savedList.isNotEmpty()) {
                    return savedList.toMutableList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Default accounts on first launch
        val defaults = mutableListOf(
            UserAccount("admin", "123456", "Administrador Principal", "admin@inacap.cl", Role.ADMINISTRADOR),
            UserAccount("jorge", "123456", "Jorge Cáceres", "jorge.caceres@inacap.cl", Role.ADMINISTRADOR),
            UserAccount("gabriel", "123456", "Gabriel Urra", "gabriel.urra@inacap.cl", Role.ADMINISTRADOR)
        )
        saveUsersToDisk(defaults)
        return defaults
    }

    private fun saveUsersToDisk(list: List<UserAccount>) {
        try {
            val json = gson.toJson(list)
            prefs.edit().putString("registered_users_json", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loginUser(usernameInput: String, passwordInput: String, mode: DeviceMode): String? {
        val cleanUser = usernameInput.trim()
        val user = _registeredUsers.value.find { 
            it.username.equals(cleanUser, ignoreCase = true) 
        }

        if (user == null) {
            return "Usuario '$cleanUser' no encontrado. Por favor regístrese primero."
        }

        if (user.password != passwordInput.trim()) {
            return "Contraseña incorrecta. Intente nuevamente."
        }

        _currentUser.value = user.copy(isLoggedIn = true)
        setDeviceMode(mode)
        addAlert("Inicio de Sesión", "Usuario '${user.username}' (${user.role.name}) ha iniciado sesión", AlertSeverity.INFO)
        return null
    }

    fun registerUser(
        usernameInput: String,
        passwordInput: String,
        fullNameInput: String,
        emailInput: String,
        roleInput: Role,
        modeInput: DeviceMode
    ): String? {
        val cleanUsername = usernameInput.trim()
        val cleanPassword = passwordInput.trim()
        if (cleanUsername.isBlank() || cleanPassword.isBlank()) {
            return "Por favor complete usuario y contraseña."
        }

        val existing = _registeredUsers.value.find { 
            it.username.equals(cleanUsername, ignoreCase = true) 
        }
        if (existing != null) {
            return "El nombre de usuario '$cleanUsername' ya está registrado."
        }

        val newUser = UserAccount(
            username = cleanUsername,
            password = cleanPassword,
            fullName = fullNameInput.ifBlank { cleanUsername },
            email = emailInput.ifBlank { "$cleanUsername@miniinvernadero.cl" },
            role = roleInput,
            isLoggedIn = true
        )

        val updatedList = _registeredUsers.value
        updatedList.add(newUser)
        _registeredUsers.value = updatedList
        saveUsersToDisk(updatedList)

        _currentUser.value = newUser
        setDeviceMode(modeInput)
        addAlert("Usuario Registrado", "Nuevo usuario '$cleanUsername' creado e iniciado sesión", AlertSeverity.INFO)
        return null
    }

    fun logout() {
        _currentUser.value = UserAccount(isLoggedIn = false)
        socketManager.stopAll()
    }

    fun setDeviceMode(mode: DeviceMode) {
        _deviceMode.value = mode
        if (mode == DeviceMode.TELEFONO_1_HOST) {
            socketManager.startServer(port = 8888, localIp = _localIp.value)
        } else {
            socketManager.stopAll()
            startAutoDiscovery()
        }
    }

    fun startAutoDiscovery() {
        val deviceName = if (_currentUser.value.isLoggedIn) "Teléfono 2 (${_currentUser.value.username})" else "Teléfono 2 Remoto"
        socketManager.startAutoDiscovery(senderName = deviceName)
    }

    fun connectToPhone1(targetIp: String) {
        val deviceName = if (_currentUser.value.isLoggedIn) "Teléfono 2 (${_currentUser.value.username})" else "Teléfono 2 Remoto"
        socketManager.connectToHost(hostIp = targetIp.trim(), port = 8888, senderName = deviceName)
    }

    fun disconnectSockets() {
        socketManager.stopAll()
    }

    fun toggleBombaAgua() {
        val current = _state.value.actuatorState.bombaAguaActiva
        updateActuators(_state.value.actuatorState.copy(bombaAguaActiva = !current))

        val sender = getDeviceLabel()
        val actionText = if (!current) "activó" else "desactivó"
        addMessage(sender, "Se $actionText manualmente la Mini Bomba de Agua")

        if (_deviceMode.value == DeviceMode.TELEFONO_2_CLIENTE) {
            socketManager.sendCommandToHost(SocketPacket(type = "COMMAND_TOGGLE_PUMP"))
        }
    }

    fun toggleTechoVentilacion() {
        val current = _state.value.actuatorState.techoAbierto
        updateActuators(_state.value.actuatorState.copy(techoAbierto = !current))

        val sender = getDeviceLabel()
        val actionText = if (!current) "abrió" else "cerró"
        addMessage(sender, "Se $actionText el techo con Servomotores SG90")

        if (_deviceMode.value == DeviceMode.TELEFONO_2_CLIENTE) {
            socketManager.sendCommandToHost(SocketPacket(type = "COMMAND_TOGGLE_ROOF"))
        }
    }

    fun toggleModoAutomatico() {
        val current = _state.value.actuatorState.modoAutomatico
        updateActuators(_state.value.actuatorState.copy(modoAutomatico = !current))

        val sender = getDeviceLabel()
        val modeText = if (!current) "AUTOMÁTICO" else "MANUAL"
        addMessage(sender, "Cambiado modo de control a $modeText")

        if (_deviceMode.value == DeviceMode.TELEFONO_2_CLIENTE) {
            socketManager.sendCommandToHost(SocketPacket(type = "COMMAND_TOGGLE_AUTO"))
        }
    }

    fun sendMessageBetweenPhones(content: String) {
        if (content.isBlank()) return
        val sender = getDeviceLabel()
        addMessage(sender, content)

        if (_deviceMode.value == DeviceMode.TELEFONO_2_CLIENTE) {
            socketManager.sendCommandToHost(
                SocketPacket(
                    type = "CHAT_MESSAGE",
                    sender = sender,
                    messageContent = content
                )
            )
        }
    }

    private fun handleSocketPacket(packet: SocketPacket) {
        when (packet.type) {
            "COMMAND_TOGGLE_PUMP" -> {
                val current = _state.value.actuatorState.bombaAguaActiva
                updateActuators(_state.value.actuatorState.copy(bombaAguaActiva = !current))
                addMessage("Teléfono 2 Remoto", "Comando recibido: Toggle Bomba de Agua")
            }
            "COMMAND_TOGGLE_ROOF" -> {
                val current = _state.value.actuatorState.techoAbierto
                updateActuators(_state.value.actuatorState.copy(techoAbierto = !current))
                addMessage("Teléfono 2 Remoto", "Comando recibido: Toggle Techo Servo")
            }
            "COMMAND_TOGGLE_AUTO" -> {
                val current = _state.value.actuatorState.modoAutomatico
                updateActuators(_state.value.actuatorState.copy(modoAutomatico = !current))
                addMessage("Teléfono 2 Remoto", "Comando recibido: Toggle Modo Auto")
            }
            "CHAT_MESSAGE" -> {
                val sender = packet.sender ?: "Teléfono Remoto"
                val text = packet.messageContent ?: ""
                if (text.isNotBlank()) {
                    addMessage(sender, text)
                }
            }
        }
    }

    private fun updateActuators(newActuators: ActuatorState) {
        _state.value = _state.value.copy(
            actuatorState = newActuators,
            lastSyncTime = getCurrentTimeString()
        )
        syncStateWithClients()
    }

    fun triggerWateringSimulation() {
        val currentSustrato = _state.value.sensorData.humedadSustratoPct
        val currentEstanque = _state.value.sensorData.nivelEstanquePct

        val newSustrato = (currentSustrato + 20f).coerceAtMost(95f)
        val newEstanque = (currentEstanque - 8f).coerceAtLeast(5f)

        _state.value = _state.value.copy(
            sensorData = _state.value.sensorData.copy(
                humedadSustratoPct = newSustrato,
                nivelEstanquePct = newEstanque,
                lastUpdated = getCurrentTimeString()
            ),
            actuatorState = _state.value.actuatorState.copy(bombaAguaActiva = true)
        )
        addAlert("Riego de Cultivo", "Bomba de agua activada. Humedad de sustrato subió a ${newSustrato.toInt()}%", AlertSeverity.INFO)
        syncStateWithClients()

        viewModelScope.launch {
            delay(4000)
            _state.value = _state.value.copy(
                actuatorState = _state.value.actuatorState.copy(bombaAguaActiva = false)
            )
            syncStateWithClients()
        }
    }

    fun refillWaterTank() {
        _state.value = _state.value.copy(
            sensorData = _state.value.sensorData.copy(
                nivelEstanquePct = 100f,
                lastUpdated = getCurrentTimeString()
            ),
            actuatorState = _state.value.actuatorState.copy(zumbadorActivo = false, ledAlertaActivo = false)
        )
        addAlert("Estanque Rellenado", "Nivel del estanque restablecido al 100% mediante sensor ultrasónico HC-SR04", AlertSeverity.INFO)
        syncStateWithClients()
    }

    private fun addAlert(titulo: String, mensaje: String, severidad: AlertSeverity) {
        val newAlert = InvernaderoAlert(
            titulo = titulo,
            mensaje = mensaje,
            severidad = severidad
        )
        val updatedList = listOf(newAlert) + _state.value.alerts.take(19)
        _state.value = _state.value.copy(alerts = updatedList)
        syncStateWithClients()
    }

    private fun addMessage(sender: String, content: String) {
        val msg = InterDeviceMessage(
            senderDevice = sender,
            content = content
        )
        val updatedMsgs = _state.value.messages + msg
        _state.value = _state.value.copy(messages = updatedMsgs.takeLast(30))
        syncStateWithClients()
    }

    private fun syncStateWithClients() {
        if (_deviceMode.value == DeviceMode.TELEFONO_1_HOST) {
            socketManager.broadcastStateToClients(_state.value)
        }
    }

    private fun startSimulationLoop() {
        viewModelScope.launch {
            while (isSimulating) {
                delay(3000)

                // Only Host Phone simulates sensor input variations and auto rules
                if (_deviceMode.value == DeviceMode.TELEFONO_1_HOST) {
                    val prevSensors = _state.value.sensorData
                    val tempVariation = (Random.nextFloat() * 0.6f) - 0.3f
                    val newTemp = (prevSensors.temperaturaC + tempVariation).coerceIn(15f, 38f)

                    val humVariation = (Random.nextFloat() * 1.0f) - 0.5f
                    val newAirHum = (prevSensors.humedadAirePct + humVariation).coerceIn(30f, 95f)

                    val soilDryRate = if (_state.value.actuatorState.bombaAguaActiva) -1.5f else 0.2f
                    val newSoilHum = (prevSensors.humedadSustratoPct - soilDryRate).coerceIn(10f, 95f)

                    val newTankLevel = if (_state.value.actuatorState.bombaAguaActiva) {
                        (prevSensors.nivelEstanquePct - 0.4f).coerceAtLeast(0f)
                    } else prevSensors.nivelEstanquePct

                    val updatedSensors = prevSensors.copy(
                        temperaturaC = (newTemp * 10).toInt() / 10f,
                        humedadAirePct = (newAirHum * 10).toInt() / 10f,
                        humedadSustratoPct = (newSoilHum * 10).toInt() / 10f,
                        nivelEstanquePct = (newTankLevel * 10).toInt() / 10f,
                        lastUpdated = getCurrentTimeString()
                    )

                    var currentActuators = _state.value.actuatorState

                    // Automatic IoT control logic
                    if (currentActuators.modoAutomatico) {
                        // High Temp -> Open Roof Servo
                        if (updatedSensors.temperaturaC > 30.0f && !currentActuators.techoAbierto) {
                            currentActuators = currentActuators.copy(techoAbierto = true)
                            addAlert("Calor Crítico", "Temp > 30°C. Servomotores SG90 abrieron techo automáticamente.", AlertSeverity.ADVERTENCIA)
                        } else if (updatedSensors.temperaturaC < 24.0f && currentActuators.techoAbierto) {
                            currentActuators = currentActuators.copy(techoAbierto = false)
                            addAlert("Temperatura Normal", "Techo cerrado automáticamente para mantener microclima.", AlertSeverity.INFO)
                        }

                        // Low Soil Moisture -> Turn on water pump
                        if (updatedSensors.humedadSustratoPct < 25.0f && !currentActuators.bombaAguaActiva && updatedSensors.nivelEstanquePct > 10f) {
                            currentActuators = currentActuators.copy(bombaAguaActiva = true)
                            addAlert("Riego Automático", "Humedad de sustrato < 25%. Mini bomba de agua activada.", AlertSeverity.ADVERTENCIA)
                        } else if (updatedSensors.humedadSustratoPct > 65.0f && currentActuators.bombaAguaActiva) {
                            currentActuators = currentActuators.copy(bombaAguaActiva = false)
                            addAlert("Riego Completado", "Humedad de sustrato óptima alcanzada. Bomba apagada.", AlertSeverity.INFO)
                        }

                        // Water tank level critical -> Buzzer & LED alert
                        if (updatedSensors.nivelEstanquePct < 15.0f && !currentActuators.zumbadorActivo) {
                            currentActuators = currentActuators.copy(zumbadorActivo = true, ledAlertaActivo = true, bombaAguaActiva = false)
                            addAlert("Estanque Vacío", "Nivel de agua < 15% (HC-SR04). Zumbador y LED Rojo activados.", AlertSeverity.CRITICA)
                        }
                    }

                    _state.value = _state.value.copy(
                        sensorData = updatedSensors,
                        actuatorState = currentActuators,
                        lastSyncTime = getCurrentTimeString()
                    )

                    syncStateWithClients()
                }
            }
        }
    }

    private fun getDeviceLabel(): String {
        return if (_deviceMode.value == DeviceMode.TELEFONO_1_HOST) "Teléfono 1 (Hub)" else "Teléfono 2 (Remoto)"
    }
}
