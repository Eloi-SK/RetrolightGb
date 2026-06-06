package com.eloi.retrolightgb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.eloi.retrolightgb.ui.mobile.GameBoyMobileBox

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        SaveManager.init(applicationContext)
        super.onCreate(savedInstanceState)

        setContent {
            GameBoyMobileBox()
        }
    }
}