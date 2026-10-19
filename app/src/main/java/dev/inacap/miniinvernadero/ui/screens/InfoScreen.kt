package dev.inacap.miniinvernadero.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InfoScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F0))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.2f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Yard,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Miniinvernadero Inteligente",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Informe Técnico & Arquitectura IoT",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Authors Card
        InfoSectionCard(
            title = "Autores & Asignatura",
            icon = Icons.Default.People,
            iconColor = Color(0xFF2E7D32)
        ) {
            InfoRow("Estudiantes:", "Jorge Cáceres Fernández y Gabriel Urra Ortiz")
            InfoRow("Carrera:", "Ingeniería en Ciberseguridad")
            InfoRow("Asignatura:", "Aplicaciones Móviles para IoT (TI3042)")
            InfoRow("Profesor:", "David Eduardo Méndez Jeldres")
            InfoRow("Fecha:", "19/10/2026")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware Architecture Card
        InfoSectionCard(
            title = "Hardware & Microcontroladores",
            icon = Icons.Default.Memory,
            iconColor = Color(0xFF1565C0)
        ) {
            InfoRow("Micro Principal:", "Arduino UNO R4 WiFi (Gateway & Comms)")
            InfoRow("Micro Secundario:", "Arduino UNO R3 (Distribución de carga)")
            InfoRow("Sensor Clima:", "DHT11 (Temperatura °C y Humedad Aire %)")
            InfoRow("Sensor Sustrato:", "Sensores resistivos de humedad de suelo")
            InfoRow("Sensor Luz:", "Fotorresistencia LDR (Iluminación Lux)")
            InfoRow("Sensor Nivel Agua:", "HC-SR04 Ultrasónico (Nivel de estanque %)")
            InfoRow("Actuador Riego:", "Mini bomba de agua (MOSFET SH-M221)")
            InfoRow("Actuador Techo:", "Servomotores SG90 (Ventilación aire)")
            InfoRow("Alertas Locales:", "LEDs estado y Zumbador piezoeléctrico")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Communication Strategy Card
        InfoSectionCard(
            title = "Comunicación Wi-Fi & Sockets",
            icon = Icons.Default.Router,
            iconColor = Color(0xFFE65100)
        ) {
            Text(
                text = "Se utiliza conexión Wi-Fi mediante arquitectura TCP Socket / Cliente-Servidor entre dos dispositivos Android:",
                fontSize = 12.sp,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(6.dp))
            InfoBullet("Teléfono 1 (Servidor/Hub): Recibe telemetría IoT y retransmite datos a teléfonos cliente.")
            InfoBullet("Teléfono 2 (Cliente Remoto): Se conecta mediante IP local Wi-Fi para monitorear y enviar comandos a distancia.")
            InfoBullet("Baja latencia y comunicación bidireccional directa entre teléfonos.")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security ISO 27400 Card
        InfoSectionCard(
            title = "Seguridad ISO 27400 & OT Security",
            icon = Icons.Default.Security,
            iconColor = Color(0xFFD32F2F)
        ) {
            InfoBullet("Confidencialidad: Acceso restringido por pantalla de login con credenciales de usuario.")
            InfoBullet("Cifrado Perimetral: Tráfico perimetral protegido con cifrado WPA2/WPA3.")
            InfoBullet("Disponibilidad (Tolerancia a Fallos): Si falla la red Wi-Fi o se apaga la app móvil, los rutinas críticas de riego y ventilación se ejecutan localmente en los Arduino R4/R3 garantizando la supervivencia de las plantas.")
        }
    }
}

@Composable
fun InfoSectionCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconColor.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1B5E20)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Color.DarkGray
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color.Black
        )
    }
}

@Composable
fun InfoBullet(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "• ",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color.DarkGray
        )
    }
}
