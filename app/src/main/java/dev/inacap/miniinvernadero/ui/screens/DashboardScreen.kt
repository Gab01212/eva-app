package dev.inacap.miniinvernadero.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.inacap.miniinvernadero.data.model.DeviceMode
import dev.inacap.miniinvernadero.data.model.InvernaderoState
import dev.inacap.miniinvernadero.data.model.UserAccount

@Composable
fun DashboardScreen(
    user: UserAccount,
    deviceMode: DeviceMode,
    state: InvernaderoState,
    connectionStatus: String,
    onTogglePump: () -> Unit,
    onToggleRoof: () -> Unit,
    onToggleAuto: () -> Unit,
    onSimulateWatering: () -> Unit,
    onRefillWaterTank: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F0))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Device Mode & Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (deviceMode == DeviceMode.TELEFONO_1_HOST) Color(0xFF1B5E20) else Color(0xFF0D47A1)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (deviceMode == DeviceMode.TELEFONO_1_HOST) "Teléfono 1 (Hub Invernadero)" else "Teléfono 2 (Cliente Remoto)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (state.isConnected || deviceMode == DeviceMode.TELEFONO_1_HOST) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                shape = CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (deviceMode == DeviceMode.TELEFONO_1_HOST) "Servidor" else if (state.isConnected) "Conectado" else "Buscando",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = connectionStatus,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dashboard de Cultivo",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            )
            Text(
                text = "Última sync: ${state.sensorData.lastUpdated}",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2x2 Sensor Cards Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SensorCard(
                modifier = Modifier.weight(1f),
                title = "Temperatura",
                value = "${state.sensorData.temperaturaC}°C",
                subtitle = "DHT11 Aire",
                statusText = if (state.sensorData.temperaturaC > 30) "Calor Elevado" else "Normal",
                statusColor = if (state.sensorData.temperaturaC > 30) Color(0xFFD32F2F) else Color(0xFF388E3C),
                icon = Icons.Default.Thermostat,
                cardColor = Color(0xFFFFF3E0)
            )

            SensorCard(
                modifier = Modifier.weight(1f),
                title = "Humedad Aire",
                value = "${state.sensorData.humedadAirePct}%",
                subtitle = "DHT11 Aire",
                statusText = "Ambiente",
                statusColor = Color(0xFF1976D2),
                icon = Icons.Default.Air,
                cardColor = Color(0xE1E3F2FD)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SensorCard(
                modifier = Modifier.weight(1f),
                title = "Humedad Sustrato",
                value = "${state.sensorData.humedadSustratoPct}%",
                subtitle = "Sensor Resistivo",
                statusText = state.sensorData.estadoSustrato,
                statusColor = when (state.sensorData.estadoSustrato) {
                    "Requiere Riego" -> Color(0xFFE65100)
                    "Exceso de Agua" -> Color(0xFFC2185B)
                    else -> Color(0xFF2E7D32)
                },
                icon = Icons.Default.WaterDrop,
                cardColor = Color(0xFFE8F5E9)
            )

            SensorCard(
                modifier = Modifier.weight(1f),
                title = "Nivel de Luz",
                value = state.sensorData.estadoLuz,
                subtitle = "LDR Fotorresistencia",
                statusText = "${state.sensorData.nivelLuzPct}% Lux",
                statusColor = Color(0xFFF57F17),
                icon = Icons.Default.WbSunny,
                cardColor = Color(0xFFFFFDE7)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Water Tank Card (HC-SR04 Ultrasonic)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE1F5FE), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Opacity,
                                contentDescription = null,
                                tint = Color(0xFF0288D1)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Estanque de Agua (HC-SR04)",
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "Nivel ultrasónico: ${state.sensorData.nivelEstanquePct}%",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Text(
                        text = state.sensorData.estadoEstanque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (state.sensorData.nivelEstanquePct < 20) Color.Red else Color(0xFF0288D1)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val animatedProgress by animateFloatAsState(targetValue = state.sensorData.nivelEstanquePct / 100f, label = "waterProgress")
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (state.sensorData.nivelEstanquePct < 20) Color.Red else Color(0xFF0288D1),
                    trackColor = Color(0xFFE0E0E0)
                )

                if (state.sensorData.nivelEstanquePct < 20) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRefillWaterTank,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rellenar Estanque de Agua")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Actuator Controls Section
        Text(
            text = "Control de Actuadores IoT",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Auto Mode Toggle
                ActuatorRow(
                    title = "Modo Automático",
                    subtitle = "Arduino regula riego y ventilación",
                    icon = Icons.Default.Autorenew,
                    isChecked = state.actuatorState.modoAutomatico,
                    onCheckedChange = { onToggleAuto() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pump Toggle (MOSFET)
                ActuatorRow(
                    title = "Mini Bomba de Agua (Riego)",
                    subtitle = "Módulo MOSFET SH-M221",
                    icon = Icons.Default.WaterDrop,
                    isChecked = state.actuatorState.bombaAguaActiva,
                    onCheckedChange = { onTogglePump() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Roof Servo Toggle
                ActuatorRow(
                    title = "Ventilación Techo (Servos)",
                    subtitle = "Servomotores SG90",
                    icon = Icons.Default.Window,
                    isChecked = state.actuatorState.techoAbierto,
                    onCheckedChange = { onToggleRoof() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manual Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSimulateWatering,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simular Riego")
            }

            OutlinedButton(
                onClick = onRefillWaterTank,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF0288D1))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Llenar Agua")
            }
        }
    }
}

@Composable
fun SensorCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    statusText: String,
    statusColor: Color,
    icon: ImageVector,
    cardColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun ActuatorRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (isChecked) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isChecked) Color(0xFF2E7D32) else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2E7D32))
        )
    }
}
