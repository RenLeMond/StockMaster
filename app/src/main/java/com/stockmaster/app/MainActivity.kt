package com.stockmaster.app

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.stockmaster.app.ui.AppRoot
import com.stockmaster.app.ui.MainViewModel
import com.stockmaster.app.ui.theme.StockMasterTheme
import com.stockmaster.app.util.ScannerGun

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockMasterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    AppRoot(viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ScannerGun.onScanned = null
        com.stockmaster.app.util.BeepPlayer.release()
    }

    /** 外接扫码枪按键分发。 */
    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return if (ScannerGun.handleKeyEvent(event)) true else super.dispatchKeyEvent(event)
    }
}