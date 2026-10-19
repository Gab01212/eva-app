package dev.inacap.miniinvernadero.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.inacap.miniinvernadero.ui.screens.AlertasScreen
import dev.inacap.miniinvernadero.ui.screens.DashboardScreen
import dev.inacap.miniinvernadero.ui.screens.InfoScreen
import dev.inacap.miniinvernadero.ui.screens.InterconexionScreen
import dev.inacap.miniinvernadero.ui.screens.LoginScreen
import dev.inacap.miniinvernadero.ui.viewmodel.InvernaderoViewModel

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Dashboard : BottomNavItem("dashboard", "Dashboard", Icons.Default.Yard)
    data object Interconexion : BottomNavItem("interconexion", "2 Teléfonos", Icons.Default.PhoneAndroid)
    data object Alertas : BottomNavItem("alertas", "Alertas", Icons.Default.Notifications)
    data object Info : BottomNavItem("info", "Info IoT", Icons.Default.Info)
}

@Composable
fun MainNavGraph(
    viewModel: InvernaderoViewModel
) {
    val navController = rememberNavController()
    val currentUser by viewModel.currentUser.collectAsState()
    val deviceMode by viewModel.deviceMode.collectAsState()
    val state by viewModel.state.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val connectedClientsCount by viewModel.connectedClientsCount.collectAsState()
    val localIp by viewModel.localIp.collectAsState()

    if (!currentUser.isLoggedIn) {
        LoginScreen(
            onLoginUser = { username, password, mode ->
                viewModel.loginUser(username, password, mode)
            },
            onRegisterUser = { username, password, fullName, email, role, mode ->
                viewModel.registerUser(username, password, fullName, email, role, mode)
            }
        )
    } else {
        val navItems = listOf(
            BottomNavItem.Dashboard,
            BottomNavItem.Interconexion,
            BottomNavItem.Alertas,
            BottomNavItem.Info
        )

        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBar(
                    containerColor = Color.White
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (item == BottomNavItem.Alertas && state.alerts.isNotEmpty()) {
                                    BadgedBox(
                                        badge = { Badge { Text("${state.alerts.size}") } }
                                    ) {
                                        Icon(item.icon, contentDescription = item.title)
                                    }
                                } else if (item == BottomNavItem.Interconexion && isConnected) {
                                    BadgedBox(
                                        badge = { Badge { Text("OK") } }
                                    ) {
                                        Icon(item.icon, contentDescription = item.title)
                                    }
                                } else {
                                    Icon(item.icon, contentDescription = item.title)
                                }
                            },
                            label = { Text(item.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF2E7D32),
                                selectedTextColor = Color(0xFF2E7D32),
                                indicatorColor = Color(0xFFE8F5E9)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(BottomNavItem.Dashboard.route) {
                    DashboardScreen(
                        user = currentUser,
                        deviceMode = deviceMode,
                        state = state,
                        connectionStatus = connectionStatus,
                        onTogglePump = { viewModel.toggleBombaAgua() },
                        onToggleRoof = { viewModel.toggleTechoVentilacion() },
                        onToggleAuto = { viewModel.toggleModoAutomatico() },
                        onSimulateWatering = { viewModel.triggerWateringSimulation() },
                        onRefillWaterTank = { viewModel.refillWaterTank() }
                    )
                }

                composable(BottomNavItem.Interconexion.route) {
                    InterconexionScreen(
                        deviceMode = deviceMode,
                        localIp = localIp,
                        state = state,
                        connectionStatus = connectionStatus,
                        isConnected = isConnected,
                        connectedClientsCount = connectedClientsCount,
                        onConnectToHost = { targetIp -> viewModel.connectToPhone1(targetIp) },
                        onAutoDiscover = { viewModel.startAutoDiscovery() },
                        onDisconnect = { viewModel.disconnectSockets() },
                        onSendMessage = { content -> viewModel.sendMessageBetweenPhones(content) },
                        onTogglePump = { viewModel.toggleBombaAgua() },
                        onToggleRoof = { viewModel.toggleTechoVentilacion() }
                    )
                }

                composable(BottomNavItem.Alertas.route) {
                    AlertasScreen(
                        alerts = state.alerts
                    )
                }

                composable(BottomNavItem.Info.route) {
                    InfoScreen()
                }
            }
        }
    }
}
