package dev.inacap.miniinvernadero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.inacap.miniinvernadero.ui.navigation.MainNavGraph
import dev.inacap.miniinvernadero.ui.theme.MiniInvernaderoTheme
import dev.inacap.miniinvernadero.ui.viewmodel.InvernaderoViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: InvernaderoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiniInvernaderoTheme {
                MainNavGraph(viewModel = viewModel)
            }
        }
    }
}
