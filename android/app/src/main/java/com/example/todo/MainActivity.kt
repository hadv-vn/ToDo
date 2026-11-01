package com.example.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.todo.presentation.navigation.NavGraph
import com.example.todo.presentation.theme.ToDoTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity gốc duy nhất của app (single-activity, toàn bộ UI bằng Jetpack Compose).
 * @AndroidEntryPoint để Hilt có thể inject vào ViewModel/field của Activity này (Phase 1+).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToDoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavGraph()
                }
            }
        }
    }
}
