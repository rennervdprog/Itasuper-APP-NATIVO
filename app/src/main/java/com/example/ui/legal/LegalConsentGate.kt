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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
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
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun LegalConsentGate(
    session: UserSession,
    viewModel: LegalConsentViewModel,
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val pending = uiState.pending ?: return
    if (!uiState.requiresAcceptance) return

    // Em aviso prévio (modo notice) os Termos §6.3 garantem 30 dias corridos em que nada
    // muda para o usuário: bloquear a tela nesse período faria o app contradizer o contrato.
    // Só o modo binding, com a vigência já em curso, prende o usuário aqui.
    val isBlocking = uiState.isBlocking
    // O Dialog consome o botão voltar por conta própria; o BackHandler abaixo só existe
    // para o caso de o diálogo ainda não ter o foco da janela.
    BackHandler(enabled = true) { if (!isBlocking) viewModel.dismissNotice() }
    Dialog(
        onDismissRequest = { if (!isBlocking) viewModel.dismissNotice() },
        properties = DialogProperties(
            dismissOnBackPress = !isBlocking,
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
                LegalConsentHeader(
                    pending = pending,
                    isBlocking = isBlocking,
                    onClose = viewModel::dismissNotice
                )
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
                    if (isBlocking) {
                        LegalRequirementNotice()
                    } else {
                        LegalAdvanceNotice(pending)
                    }

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
                            Text(
                                // Aceitar antes da vigência é permitido e vale como aceite.
                                if (isBlocking) "Aceitar e continuar" else "Aceitar desde já",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    if (!isBlocking) {
                        OutlinedButton(
                            onClick = viewModel::dismissNotice,
                            enabled = uiState.canDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Ver depois", fontWeight = FontWeight.Bold, color = Color(0xFF4D4D4D))
                        }
                    }

                    // Consentimento sob bloqueio total e sem saída não é consentimento livre
                    // (LGPD art. 8º, §4º). A recusa aponta para exclusão de conta e suporte.
                    TextButton(
                        onClick = { viewModel.setRefusalPanelVisible(!uiState.showRefusalPanel) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Não concordo com as mudanças",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color(0xFF737373),
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    }
                    if (uiState.showRefusalPanel) {
                        LegalRefusalPanel(onOpenProfile = onNavigateToProfile)
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
private fun LegalConsentHeader(
    pending: PendingLegalChanges,
    isBlocking: Boolean,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 10.dp, top = 16.dp, bottom = 16.dp),
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
            Text(
                if (isBlocking) "Termos atualizados" else "Aviso de mudança nos Termos",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(
                "Termos v${pending.currentTermsVersion} · Privacidade v${pending.currentPrivacyVersion}",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF686868))
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = if (isBlocking) Color(0xFFFFF0E6) else Color(0xFFE8F1FF)
        ) {
            Text(
                if (isBlocking) "Obrigatório" else "Aviso prévio",
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isBlocking) ItaSuperPrimary else Color(0xFF1B5FB8),
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }
        if (!isBlocking) {
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Fechar aviso", tint = Color(0xFF737373), modifier = Modifier.size(18.dp))
            }
        } else {
            Spacer(Modifier.width(10.dp))
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

/**
 * Aviso prévio: mostra a data de vigência e quantos dias faltam. Enquanto este período
 * corre, nada muda para o usuário — o app só informa.
 */
@Composable
private fun LegalAdvanceNotice(pending: PendingLegalChanges) {
    val formattedDate = remember(pending.effectiveDate) { formatLegalEffectiveDate(pending.effectiveDate) }
    val days = pending.daysUntilEffective?.coerceAtLeast(0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEAF2FF))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF1B5FB8), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Column {
            Text(
                buildString {
                    append("Estas mudanças passam a valer")
                    if (formattedDate != null) append(" em $formattedDate") else append(" em breve")
                    append(".")
                },
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF123E78), fontWeight = FontWeight.Bold)
            )
            Text(
                when {
                    days == null -> "Até lá nada muda para você e não é preciso fazer nada agora."
                    days <= 0 -> "Até a virada nada muda para você e não é preciso fazer nada agora."
                    days == 1 -> "Falta 1 dia. Até lá nada muda para você e não é preciso fazer nada agora."
                    else -> "Faltam $days dias. Até lá nada muda para você e não é preciso fazer nada agora."
                },
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF2C5C96))
            )
        }
    }
}

/**
 * Caminho de recusa. Não há como "recusar e seguir usando" — o que existe é sair:
 * exportar/excluir a conta no perfil, ou falar com o suporte.
 */
@Composable
private fun LegalRefusalPanel(onOpenProfile: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF7F7F7))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Você não precisa concordar. Se preferir não seguir com as novas versões, pode pedir seus dados ou encerrar sua conta a qualquer momento.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF4D4D4D))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable(onClick = onOpenProfile),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.GppGood, contentDescription = null, tint = ItaSuperPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Meus dados e excluir conta", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF4D4D4D)))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LegalDocumentLinks.SUPPORT_WHATSAPP_URL)))
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = ItaSuperPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Falar com o suporte", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF4D4D4D)))
        }
    }
}

/** O backend manda ISO-8601 com fuso; a tela mostra só a data. */
private fun formatLegalEffectiveDate(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val datePart = value.take(10)
    val parsed = runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(datePart)
    }.getOrNull() ?: return null
    return SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(parsed)
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
