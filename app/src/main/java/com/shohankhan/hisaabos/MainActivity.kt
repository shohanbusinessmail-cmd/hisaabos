package com.shohankhan.hisaabos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.shohankhan.hisaabos.di.LocalAppContainer
import com.shohankhan.hisaabos.ui.app.HisaabRoot
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.HisaabTheme

/**
 * Single activity. All screens are Compose destinations inside one NavHost so
 * the session, the lock state and the loaded container live exactly as long as
 * the user does.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        val container = (application as HisaabApp).container
        setContent {
            HisaabTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxSize().background(AppBackground),
                    ) {
                        HisaabRoot(container)
                    }
                }
            }
        }
    }
}
