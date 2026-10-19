package dev.inacap.miniinvernadero.network

import com.google.gson.Gson
import dev.inacap.miniinvernadero.data.model.InvernaderoState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

data class SocketPacket(
    val type: String, // "SYNC_STATE", "COMMAND_TOGGLE_PUMP", "COMMAND_TOGGLE_ROOF", "COMMAND_TOGGLE_AUTO", "CHAT_MESSAGE"
    val stateJson: String? = null,
    val sender: String? = null,
    val messageContent: String? = null
)

class SocketManager {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var serverSocket: ServerSocket? = null
    private var udpDiscoverySocket: DatagramSocket? = null
    private var clientSocket: Socket? = null
    private var clientWriter: PrintWriter? = null

    private val connectedClients = ConcurrentHashMap<String, Socket>()

    private val _connectionStatus = MutableStateFlow("Sin conexión activa")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()

    private val _discoveredHostIp = MutableStateFlow<String?>(null)
    val discoveredHostIp: StateFlow<String?> = _discoveredHostIp.asStateFlow()

    private var serverJob: Job? = null
    private var clientJob: Job? = null
    private var udpJob: Job? = null

    // Callbacks to ViewModel
    var onStateReceivedFromHost: ((InvernaderoState) -> Unit)? = null
    var onCommandReceivedFromClient: ((SocketPacket) -> Unit)? = null
    var onHostDiscoveredAuto: ((String) -> Unit)? = null

    fun startServer(port: Int = 8888, localIp: String) {
        stopAll()

        // 1. Start TCP Server for data & commands
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                _isConnected.value = true
                _connectionStatus.value = "Teléfono 1 (Servidor Activo en Wi-Fi)"

                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    val clientIp = socket.inetAddress.hostAddress ?: "Cliente"
                    connectedClients[clientIp] = socket
                    _connectedClientsCount.value = connectedClients.size
                    _connectionStatus.value = "Teléfono 1: Teléfono 2 conectado ($clientIp)"

                    launch {
                        handleClientInput(socket, clientIp)
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    _connectionStatus.value = "Error Servidor: ${e.message}"
                    _isConnected.value = false
                }
            }
        }

        // 2. Start UDP Auto-Discovery Responder (Teléfono 1 listens for Teléfono 2 pings)
        udpJob = scope.launch {
            try {
                val socket = DatagramSocket(8889)
                udpDiscoverySocket = socket
                val buffer = ByteArray(1024)

                while (isActive && !socket.isClosed) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length).trim()

                    if (message == "DISCOVER_MINIINVERNADERO") {
                        val responseMessage = "MINIINVERNADERO_HOST_IP:$localIp"
                        val responseData = responseMessage.toByteArray()
                        val responsePacket = DatagramPacket(
                            responseData,
                            responseData.size,
                            packet.address,
                            packet.port
                        )
                        socket.send(responsePacket)
                    }
                }
            } catch (e: Exception) {
                // UDP socket closed
            }
        }
    }

    fun startAutoDiscovery(senderName: String = "Teléfono 2") {
        _connectionStatus.value = "Buscando Teléfono 1 en la red Wi-Fi..."
        scope.launch {
            try {
                val udpSocket = DatagramSocket()
                udpSocket.broadcast = true
                val requestMessage = "DISCOVER_MINIINVERNADERO"
                val requestData = requestMessage.toByteArray()

                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(requestData, requestData.size, broadcastAddr, 8889)

                // Send 3 discovery pings
                repeat(3) {
                    udpSocket.send(packet)
                    delay(300)
                }

                udpSocket.soTimeout = 2000
                val receiveBuffer = ByteArray(1024)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

                udpSocket.receive(receivePacket)
                val response = String(receivePacket.data, 0, receivePacket.length).trim()

                if (response.startsWith("MINIINVERNADERO_HOST_IP:")) {
                    val hostIp = response.substringAfter("MINIINVERNADERO_HOST_IP:")
                    _discoveredHostIp.value = hostIp
                    _connectionStatus.value = "¡Teléfono 1 encontrado en $hostIp!"

                    withContext(Dispatchers.Main) {
                        onHostDiscoveredAuto?.invoke(hostIp)
                    }
                    connectToHost(hostIp = hostIp, senderName = senderName)
                }
                udpSocket.close()
            } catch (e: Exception) {
                if (!_isConnected.value) {
                    _connectionStatus.value = "Búsqueda automática finalizada. Ingrese IP manualmente si es necesario."
                }
            }
        }
    }

    private suspend fun handleClientInput(socket: Socket, clientIp: String) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            while (scope.isActive && !socket.isClosed) {
                val line = reader.readLine() ?: break
                val packet = gson.fromJson(line, SocketPacket::class.java)
                withContext(Dispatchers.Main) {
                    onCommandReceivedFromClient?.invoke(packet)
                }
            }
        } catch (e: Exception) {
            // Client disconnected
        } finally {
            connectedClients.remove(clientIp)
            _connectedClientsCount.value = connectedClients.size
            if (connectedClients.isEmpty()) {
                _connectionStatus.value = "Teléfono 1 (Esperando Teléfono 2 en Wi-Fi)..."
            }
        }
    }

    fun broadcastStateToClients(state: InvernaderoState) {
        if (connectedClients.isEmpty()) return
        scope.launch {
            val packet = SocketPacket(
                type = "SYNC_STATE",
                stateJson = gson.toJson(state)
            )
            val jsonPayload = gson.toJson(packet)

            val iterator = connectedClients.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                try {
                    val writer = PrintWriter(entry.value.getOutputStream(), true)
                    writer.println(jsonPayload)
                } catch (e: Exception) {
                    iterator.remove()
                    _connectedClientsCount.value = connectedClients.size
                }
            }
        }
    }

    fun connectToHost(hostIp: String, port: Int = 8888, senderName: String = "Teléfono 2") {
        clientJob?.cancel()
        clientJob = scope.launch {
            try {
                _connectionStatus.value = "Conectando a Teléfono 1 ($hostIp)..."
                val socket = Socket(hostIp, port)
                clientSocket = socket
                clientWriter = PrintWriter(socket.getOutputStream(), true)
                _isConnected.value = true
                _connectionStatus.value = "¡Conectado automáticamente a Teléfono 1 ($hostIp)!"

                // Announce connection
                sendCommandToHost(SocketPacket(type = "CHAT_MESSAGE", sender = senderName, messageContent = "Teléfono 2 se ha conectado automáticamente vía Wi-Fi"))

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (isActive && !socket.isClosed) {
                    val line = reader.readLine() ?: break
                    val packet = gson.fromJson(line, SocketPacket::class.java)
                    if (packet.type == "SYNC_STATE" && packet.stateJson != null) {
                        val state = gson.fromJson(packet.stateJson, InvernaderoState::class.java)
                        withContext(Dispatchers.Main) {
                            onStateReceivedFromHost?.invoke(state)
                        }
                    } else if (packet.type == "CHAT_MESSAGE") {
                        withContext(Dispatchers.Main) {
                            onCommandReceivedFromClient?.invoke(packet)
                        }
                    }
                }
            } catch (e: Exception) {
                _isConnected.value = false
                _connectionStatus.value = "Error al conectar con Teléfono 1: ${e.message}"
            }
        }
    }

    fun sendCommandToHost(packet: SocketPacket) {
        scope.launch {
            try {
                clientWriter?.println(gson.toJson(packet))
            } catch (e: Exception) {
                _connectionStatus.value = "Error al enviar comando: ${e.message}"
            }
        }
    }

    fun stopAll() {
        serverJob?.cancel()
        clientJob?.cancel()
        udpJob?.cancel()
        try {
            serverSocket?.close()
            udpDiscoverySocket?.close()
            clientSocket?.close()
            connectedClients.values.forEach { it.close() }
            connectedClients.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isConnected.value = false
        _connectedClientsCount.value = 0
        _connectionStatus.value = "Conexión Detenida"
    }
}
