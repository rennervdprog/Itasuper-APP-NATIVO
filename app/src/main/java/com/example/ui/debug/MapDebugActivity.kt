// TODO(DEBUG): REMOVER ANTES DE PUBLICAR - activity temporária para debug do mapa
package com.example.ui.debug

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.ui.theme.ItaSuperTheme

class MapDebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ItaSuperTheme {
                MapDebugScreen(onBack = { finish() })
            }
        }
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, MapDebugActivity::class.java))
        }
    }
}
