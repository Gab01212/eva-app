package dev.inacap.miniinvernadero.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class Role {
    ADMINISTRADOR,
    OPERADOR,
    OBSERVADOR
}

enum class DeviceMode {
    TELEFONO_1_HOST,      // Servidor / Hub Invernadero
    TELEFONO_2_CLIENTE    // Cliente Remoto / Monitor
}

data class UserAccount(
    val username: String = "admin",
    val password: String = "123456",
    val fullName: String = "Administrador del Sistema",
    val email: String = "admin@inacap.cl",
    val role: Role = Role.ADMINISTRADOR,
    val isLoggedIn: Boolean = false
)

data class SensorData(
    val temperaturaC: Float = 24.5f,
    val humedadAirePct: Float = 62.0f,
    val humedadSustratoPct: Float = 45.0f,
    val nivelLuzPct: Float = 78.0f,
    val nivelEstanquePct: Float = 85.0f,
    val lastUpdated: String = getCurrentTimeString()
) {
    val estadoSustrato: String
        get() = when {
            humedadSustratoPct < 30f -> "Requiere Riego"
            humedadSustratoPct > 80f -> "Exceso de Agua"
            else -> "Óptimo"
        }

    val estadoLuz: String
        get() = if (nivelLuzPct >= 50f) "Alta" else "Baja"

    val estadoEstanque: String
        get() = when {
            nivelEstanquePct < 15f -> "Crítico (Vacío)"
            nivelEstanquePct < 35f -> "Bajo"
            else -> "Normal"
        }
}

data class ActuatorState(
    val bombaAguaActiva: Boolean = false,
    val techoAbierto: Boolean = false,
    val ledAlertaActivo: Boolean = false,
    val zumbadorActivo: Boolean = false,
    val modoAutomatico: Boolean = true
)

enum class AlertSeverity {
    CRITICA,
    ADVERTENCIA,
    INFO
}

data class InvernaderoAlert(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String = getCurrentTimeString(),
    val titulo: String,
    val mensaje: String,
    val severidad: AlertSeverity = AlertSeverity.INFO,
    val atendida: Boolean = false
)

data class InterDeviceMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderDevice: String,
    val timestamp: String = getCurrentTimeString(),
    val content: String,
    val isSystemAlert: Boolean = false
)

data class InvernaderoState(
    val sensorData: SensorData = SensorData(),
    val actuatorState: ActuatorState = ActuatorState(),
    val alerts: List<InvernaderoAlert> = emptyList(),
    val messages: List<InterDeviceMessage> = emptyList(),
    val hostIp: String = "Desconectado",
    val isConnected: Boolean = false,
    val connectedClientsCount: Int = 0,
    val lastSyncTime: String = getCurrentTimeString()
)

fun getCurrentTimeString(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}
