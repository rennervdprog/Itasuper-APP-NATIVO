package com.example.ui.legal

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.LegalChange
import com.example.data.model.LegalDocumentLinks
import com.example.data.model.PendingLegalChanges
import com.example.data.model.UserSession
import com.example.ui.theme.ItaSuperPrimary

@Composable
fun LegalConsentGate(
    session: UserSession,
    viewModel: LegalConsentViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val pending = uiState.pending ?: return
    if (!uiState.requiresAcceptance) return

    BackHandler(enabled = true) { }
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 24.dp)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 1.dp,
            shadowElevation = 10.dp
        ) {
            Column {
                LegalConsentHeader(pending)
                HorizontalDivider(color = Color(0xFFEEEEEE))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Veja abaixo somente o que mudou desde o seu último aceite.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF686868))
                    )
                    LegalRequirementNotice()

                    if (pending.needsTerms) {
                        LegalChangesCard(
                            title = "Termos de Uso",
                            version = pending.currentTermsVersion,
                            tint = ItaSuperPrimary,
                            changes = pending.termsChanges
                        )
                    }
                    if (pending.needsPrivacy) {
                        LegalChangesCard(
                            title = "Política de Privacidade",
                            version = pending.currentPrivacyVersion,
                            tint = Color(0xFF11845B),
                            changes = pending.privacyChanges
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LegalDocumentLink(
                            label = "Ler Termos",
                            icon = Icons.Default.Description,
                            url = LegalDocumentLinks.TERMS_URL,
                            modifier = Modifier.weight(1f)
                        )
                        LegalDocumentLink(
                            label = "Ler Política",
                            icon = Icons.Default.GppGood,
                            url = LegalDocumentLinks.PRIVACY_URL,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pending.needsTerms) {
                        LegalAcceptanceCheckbox(
                            checked = uiState.acceptedTerms,
                            onCheckedChange = viewModel::setTermsAccepted,
                            prefix = "Li e aceito os ",
                            link = "Termos de Uso",
                            suffix = " versão ${pending.currentTermsVersion}.",
                            linkUrl = LegalDocumentLinks.TERMS_URL,
                            accent = ItaSuperPrimary
                        )
                    }
                    if (pending.needsPrivacy) {
                        LegalAcceptanceCheckbox(
                            checked = uiState.acceptedPrivacy,
                            onCheckedChange = viewModel::setPrivacyAccepted,
                            prefix = "Li e aceito a ",
                            link = "Política de Privacidade",
                            suffix = " versão ${pending.currentPrivacyVersion}.",
                            linkUrl = LegalDocumentLinks.PRIVACY_URL,
                            accent = Color(0xFF11845B)
                        )
                    }

                    uiState.errorMessage?.let {
                        Text(
                            text = it,
                            color = Color(0xFFB3261E),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = { viewModel.accept(session) },
                        enabled = uiState.canAccept,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                    ) {
                        if (uiState.isAccepting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(19.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(9.dp))
                            Text("Registrando...")
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Aceitar e continuar", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Text(
                        text = "Consentimento registrado com data e hora.",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF737373), fontSize = 10.sp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun LegalConsentHeader(pending: PendingLegalChanges) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ItaSuperPrimary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = ItaSuperPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Termos atualizados", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold))
            Text(
                "Termos v${pending.currentTermsVersion} · Privacidade v${pending.currentPrivacyVersion}",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF686868))
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFFFFF0E6)
        ) {
            Text(
                "Obrigatório",
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall.copy(color = ItaSuperPrimary, fontWeight = FontWeight.ExtraBold)
            )
        }
    }
}

@Composable
private fun LegalRequirementNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF6E9))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.GppGood, contentDescription = null, tint = Color(0xFF9A6200), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Text(
            "É necessário aceitar as novas versões antes de continuar usando o ItaSuper.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF715000))
        )
    }
}

@Composable
private fun LegalChangesCard(
    title: String,
    version: String,
    tint: Color,
    changes: List<LegalChange>
) {
    var expanded by remember(title, version) { mutableIntStateOf(0) }
    val isExpanded = expanded == 1
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.055f))
            .clickable { expanded = if (isExpanded) 0 else 1 }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Description, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(color = tint, fontWeight = FontWeight.Bold))
            Text(
                "v$version · ${changes.size} mudança${if (changes.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF686868))
            )
            Spacer(Modifier.width(4.dp))
            Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = Color(0xFF737373), modifier = Modifier.size(18.dp))
        }
        if (isExpanded) {
            Spacer(Modifier.height(12.dp))
            if (changes.isEmpty()) {
                Text("Sem detalhes registrados.", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF686868)))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    changes.forEach { change ->
                        Column {
                            Text(
                                "${change.changeType.uppercase()} · ${change.section}",
                                style = MaterialTheme.typography.labelSmall.copy(color = tint, fontWeight = FontWeight.ExtraBold)
                            )
                            Text(change.summary, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF4D4D4D)))
                            change.legalBasis?.takeIf { it.isNotBlank() }?.let { basis ->
                                Text(basis, style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF737373)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalDocumentLink(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF7F7F7))
            .clickable {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = ItaSuperPrimary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF4D4D4D)))
    }
}

@Composable
private fun LegalAcceptanceCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    prefix: String,
    link: String,
    suffix: String,
    linkUrl: String,
    accent: Color
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = accent),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = prefix + link + suffix,
            modifier = Modifier.padding(top = 3.dp),
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF5C5C5C))
        )
        // O rótulo completo alterna o checkbox. O atalho abaixo preserva a leitura do documento oficial.
        Text(
            text = "Ler",
            modifier = Modifier
                .padding(top = 3.dp, start = 6.dp)
                .clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))) } },
            style = MaterialTheme.typography.bodySmall.copy(color = accent, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
        )
    }
}
