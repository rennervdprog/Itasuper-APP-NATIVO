package com.example.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperTextPrimary
import com.example.ui.theme.ItaSuperTextSecondary
import kotlinx.coroutines.delay

private const val LOCATION_ONBOARDING_PREFS = "itasuper_location_onboarding"

/**
 * Persiste a conclusão por usuário e dispositivo. O onboarding é exibido após o login
 * apenas quando o usuário ainda não tomou uma decisão sobre a localização.
 */
object LocationOnboardingPreferences {
    fun shouldShow(context: Context, userId: String): Boolean {
        if (userId.isBlank()) return false
        return !context
            .getSharedPreferences(LOCATION_ONBOARDING_PREFS, Context.MODE_PRIVATE)
            .getBoolean("completed_$userId", false)
    }

    fun markCompleted(context: Context, userId: String) {
        if (userId.isBlank()) return
        context
            .getSharedPreferences(LOCATION_ONBOARDING_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("completed_$userId", true)
            .apply()
    }
}

private enum class LocationOnboardingStep {
    Explanation,
    Done
}

/**
 * Tela cheia exibida depois do login. Nesta primeira etapa o fluxo pede somente
 * a localização; notificações e push permanecem fora deste onboarding.
 */
@Composable
fun LocationPermissionOnboarding(
    userId: String,
    onFinished: (locationGranted: Boolean) -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(LocationOnboardingStep.Explanation) }
    var locationGranted by remember { mutableStateOf(PermissionUtils.hasLocationPermission(context)) }

    fun complete(granted: Boolean) {
        locationGranted = granted
        LocationOnboardingPreferences.markCompleted(context, userId)
        step = LocationOnboardingStep.Done
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            PermissionUtils.hasLocationPermission(context)
        complete(granted)
    }

    LaunchedEffect(step) {
        if (step == LocationOnboardingStep.Done) {
            delay(900)
            onFinished(locationGranted)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("location_permission_onboarding"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BEM-VINDO",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = ItaSuperTextPrimary.copy(alpha = 0.8f)
                )
            )

            Spacer(modifier = Modifier.height(56.dp))

            if (step == LocationOnboardingStep.Explanation) {
                LocationIllustration()

                Spacer(modifier = Modifier.height(44.dp))

                Text(
                    text = "Permitir localização",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = ItaSuperTextPrimary,
                        fontSize = 27.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Para descobrir lojas que entregam na sua região e calcular a entrega com mais precisão.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = ItaSuperTextSecondary,
                        lineHeight = 23.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 340.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = { complete(false) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("location_onboarding_skip"),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = "Pular por enquanto",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (PermissionUtils.hasLocationPermission(context)) {
                            complete(true)
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("location_onboarding_allow"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                ) {
                    Text(
                        text = "Permitir localização",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(74.dp))

                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0x1A22C55E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(62.dp)
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = "Tudo pronto!",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = ItaSuperTextPrimary,
                        fontSize = 27.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (locationGranted) {
                        "Vamos encontrar as lojas que atendem sua região."
                    } else {
                        "Você pode informar seu endereço ou permitir a localização quando quiser."
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = ItaSuperTextSecondary,
                        lineHeight = 23.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 340.dp)
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LocationIllustration() {
    Box(
        modifier = Modifier
            .size(244.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(ItaSuperPrimary.copy(alpha = 0.16f), Color.White)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(154.dp)
                .clip(CircleShape)
                .background(ItaSuperPrimary.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = ItaSuperPrimary,
                modifier = Modifier.size(82.dp)
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp)) {
                Text(
                    text = "Lojas perto de você",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ItaSuperTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Entrega e taxa calculadas para sua região",
                    style = MaterialTheme.typography.bodySmall.copy(color = ItaSuperTextSecondary)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.Storefront,
            contentDescription = null,
            tint = ItaSuperPrimary.copy(alpha = 0.58f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(27.dp)
                .size(28.dp)
        )
    }
}
