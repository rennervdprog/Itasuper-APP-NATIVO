package com.example.ui.permissions

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notifications.PushNotificationManager
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperTextPrimary
import com.example.ui.theme.ItaSuperTextSecondary

private const val NOTIFICATION_ONBOARDING_PREFS = "itasuper_notification_onboarding"

object NotificationOnboardingPreferences {
    fun shouldShow(context: Context, userId: String): Boolean {
        if (userId.isBlank()) return false
        return !context.getSharedPreferences(NOTIFICATION_ONBOARDING_PREFS, Context.MODE_PRIVATE)
            .getBoolean("completed_$userId", false)
    }

    fun markCompleted(context: Context, userId: String) {
        if (userId.isBlank()) return
        context.getSharedPreferences(NOTIFICATION_ONBOARDING_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("completed_$userId", true)
            .apply()
    }
}

/** Mostra o pedido de notificações somente após contextualizar as atualizações de pedido. */
@Composable
fun NotificationPermissionOnboarding(
    userId: String,
    onFinished: (granted: Boolean) -> Unit
) {
    val context = LocalContext.current
    var isRequesting by remember { mutableStateOf(false) }

    fun complete(granted: Boolean) {
        NotificationOnboardingPreferences.markCompleted(context, userId)
        if (granted) PushNotificationManager.registerCurrentDevice(context)
        onFinished(granted)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> complete(granted || PushNotificationManager.hasNotificationPermission(context)) }

    Surface(
        modifier = Modifier.fillMaxSize().testTag("notification_permission_onboarding"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(38.dp))
                    .background(Brush.verticalGradient(listOf(ItaSuperPrimary.copy(alpha = 0.18f), Color.White))),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(82.dp).clip(CircleShape).background(ItaSuperPrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = ItaSuperPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(38.dp))
            Text(
                text = "Acompanhe seu pedido",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 27.sp,
                    color = ItaSuperTextPrimary
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Receba avisos quando a loja aceitar, preparar e enviar seu pedido para entrega.",
                style = MaterialTheme.typography.bodyLarge.copy(color = ItaSuperTextSecondary, lineHeight = 23.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 340.dp)
            )
            Spacer(modifier = Modifier.height(46.dp))
            OutlinedButton(
                onClick = { complete(false) },
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("notification_onboarding_skip"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Pular por enquanto", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                enabled = !isRequesting,
                onClick = {
                    isRequesting = true
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || PushNotificationManager.hasNotificationPermission(context)) {
                        complete(true)
                    } else {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(58.dp).testTag("notification_onboarding_allow"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
            ) {
                Text(
                    text = "Permitir notificações",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
