package com.burrow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.burrow.app.ui.BurrowRoot
import com.burrow.app.ui.theme.Burrow
import com.burrow.app.ui.theme.BurrowTheme
import com.burrow.app.viewmodel.BurrowViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BurrowApp()
        }
    }
}

@Composable
private fun BurrowApp() {
    BurrowTheme {
        Surface(color = Burrow.Neutral200, modifier = Modifier.fillMaxSize()) {
            val viewModel: BurrowViewModel = viewModel()
            BurrowRoot(viewModel)
        }
    }
}
