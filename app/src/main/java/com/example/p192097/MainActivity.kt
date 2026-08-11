package com.example.p192097

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.p192097.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(
                    // safeDrawingPadding() 让内容自动避开状态栏/导航栏/刘海等系统绘制区域，
                    // 同时保留沉浸式全屏的背景效果（系统栏依然透明，只是内容不会被压住）。
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = Color(0xFF101014)
                ) {
                    CalculatorScreen()
                }
            }
        }
    }
}
