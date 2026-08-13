package com.example.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserSession
import com.example.data.repository.UserSessionRepository
import com.example.ui.navigation.ItaSuperBottomNavBar
import com.example.ui.theme.ItaSuperBorder
import com.example.ui.theme.ItaSuperHighlightBg
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperSuccess
import com.example.ui.theme.ItaSuperTextPrimary
import com.example.ui.theme.ItaSuperTextSecondary

@Composable
fun ProfileScreen(
    onNavigateToRoute: (String) -> Unit,
    onLogout: () -> Unit
) {
    val session by UserSessionRepository.userSession.collectAsState()
    val displayName = session.name.ifBlank { session.email.substringBefore("@").ifBlank { "Cliente ItaSuper" } }
    val initials = displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "CI" }

    val profileComplete = listOf(
        session.name.isNotBlank(),
        session.cpfCnpj.isNotBlank(),
        session.addressStreet.isNotBlank() && session.addressNumber.isNotBlank() && session.addressNeighborhood.isNotBlank(),
        session.whatsapp.isNotBlank()
    )
    val completed = profileComplete.count { it }
    val progress = completed / profileComplete.size.toFloat()

    Scaffold(
        bottomBar = {
            ItaSuperBottomNavBar(
                currentRoute = "perfil",
                onNavigateToRoute = onNavigateToRoute
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ProfileHero(
                    name = displayName,
                    email = session.email,
                    initials = initials
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (completed < profileComplete.size) {
                        CompletionCard(completed, profileComplete.size, progress, onNavigateToRoute)
                    }
                    Text(
                        text = "Acesso rápido",
                        style = MaterialTheme.typography.labelMedium,
                        color = ItaSuperTextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    QuickActionGrid(
                        onOrders = { onNavigateToRoute("pedidos") },
                        onSupport = { onNavigateToRoute("home") }
                    )
                    Text(
                        text = "Conta",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = ItaSuperTextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    AccountCard(session = session, onNavigateToRoute = onNavigateToRoute)
                    LogoutCard(onLogout = onLogout)
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileHero(name: String, email: String, initials: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(ItaSuperPrimary, Color(0xFFE95D00))
                )
            )
            .padding(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 38.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = email.ifBlank { "Conta ItaSuper" },
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "CLIENTE",
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.20f))
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
private fun CompletionCard(completed: Int, total: Int, progress: Float, onNavigateToRoute: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaSuperPrimary.copy(alpha = 0.25f)),
        onClick = { onNavigateToRoute("home") }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = ItaSuperPrimary, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("Complete seu cadastro", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Text("$completed/$total", color = ItaSuperPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = ItaSuperPrimary,
                trackColor = ItaSuperHighlightBg
            )
            Text(
                text = "Complete seus dados e endereço para uma entrega mais rápida.",
                modifier = Modifier.padding(top = 9.dp),
                style = MaterialTheme.typography.bodySmall,
                color = ItaSuperTextSecondary
            )
        }
    }
}

@Composable
private fun QuickActionGrid(onOrders: () -> Unit, onSupport: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        QuickAction(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.ShoppingBag,
            title = "Meus pedidos",
            subtitle = "Acompanhe",
            onClick = onOrders
        )
        QuickAction(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.PhoneInTalk,
            title = "Suporte",
            subtitle = "Precisa de ajuda?",
            onClick = onSupport
        )
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(106.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaSuperBorder),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(ItaSuperHighlightBg, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ItaSuperPrimary, modifier = Modifier.size(19.dp))
            }
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = ItaSuperTextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AccountCard(session: UserSession, onNavigateToRoute: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ItaSuperBorder)
    ) {
        Column {
            ProfileMenuRow(
                icon = Icons.Default.Person,
                title = "Dados pessoais",
                subtitle = if (session.name.isBlank()) "Complete seu cadastro" else session.name,
                isComplete = session.name.isNotBlank() && session.cpfCnpj.isNotBlank(),
                onClick = { onNavigateToRoute("home") }
            )
            ProfileDivider()
            val addressText = listOf(session.addressStreet, session.addressNumber, session.addressNeighborhood)
                .filter { it.isNotBlank() }
                .joinToString(", ")
            ProfileMenuRow(
                icon = Icons.Default.LocationOn,
                title = "Endereço de entrega",
                subtitle = addressText.ifBlank { "Toque para cadastrar seu endereço" },
                isComplete = session.addressStreet.isNotBlank() && session.addressNumber.isNotBlank(),
                onClick = { onNavigateToRoute("home") }
            )
            ProfileDivider()
            ProfileMenuRow(
                icon = Icons.Default.HelpOutline,
                title = "Ajuda e suporte",
                subtitle = "Fale com o atendimento ItaSuper",
                isComplete = null,
                onClick = { onNavigateToRoute("home") }
            )
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isComplete: Boolean?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(ItaSuperHighlightBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = ItaSuperPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                if (isComplete != null) {
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = if (isComplete) "OK" else "PENDENTE",
                        modifier = Modifier
                            .background(
                                if (isComplete) ItaSuperSuccess.copy(alpha = 0.12f) else Color(0xFFFFF0D5),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        color = if (isComplete) ItaSuperSuccess else Color(0xFFB96A00),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(subtitle, color = ItaSuperTextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ItaSuperTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ProfileDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ItaSuperBorder.copy(alpha = 0.6f))
    )
}

@Composable
private fun LogoutCard(onLogout: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF4C4C2))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onLogout)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFFFECEB), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFCE3730), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Sair da conta", color = Color(0xFFCE3730), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("Encerrar esta sessão neste aparelho", color = ItaSuperTextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFCE3730).copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}
