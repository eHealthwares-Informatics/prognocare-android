package com.ehealthwares.prognocare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.ehealthwares.prognocare.designsystem.theme.AppearanceMode
import com.ehealthwares.prognocare.designsystem.theme.PrognoCareTheme
import com.ehealthwares.prognocare.designsystem.theme.ThemeViewModel
import com.ehealthwares.prognocare.feature.auth.FacebookAuthBridge
import com.ehealthwares.prognocare.navigation.PrognoCareNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeSettings by themeViewModel.themeSettings.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            
            PrognoCareTheme(
                themeSettings = themeSettings,
                isSystemDark = isSystemDark
            ) {
                PrognoCareNavGraph()
            }
        }
    }

    /** Facebook Login still resolves through the classic callback manager; the
     * bridge routes every activity result to the SDK and one-shot handler. */
    @Deprecated("Required by the Facebook SDK callback path")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (FacebookAuthBridge.onActivityResult(requestCode, resultCode, data)) return
        super.onActivityResult(requestCode, resultCode, data)
    }
}
