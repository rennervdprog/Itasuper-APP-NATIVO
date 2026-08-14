package com.example.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ItaSuperPrimary
import com.example.ui.theme.ItaSuperTextPrimary
import com.example.ui.theme.ItaSuperTextSecondary

@Composable
fun LocationOrAddressDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAllowLocation: () -> Unit,
    onRegisterAddress: () -> Unit
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = ItaSuperPrimary
            )
        },
        title = {
            Text(
                text = "Como você quer informar sua região?",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = ItaSuperTextPrimary
            )
        },
        text = {
            Text(
                text = "A localização ajuda a encontrar lojas que entregam para você. Se preferir, cadastre seu endereço manualmente.",
                textAlign = TextAlign.Center,
                color = ItaSuperTextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onAllowLocation,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Permitir localização", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onRegisterAddress,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.PinDrop, contentDescription = null, tint = ItaSuperPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Cadastrar endereço", fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressRegistrationSheet(
    visible: Boolean,
    draft: AddressDraft,
    isLookingUpCep: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onDraftChange: ((AddressDraft) -> AddressDraft) -> Unit,
    onLookupCep: () -> Unit,
    onSave: () -> Unit
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Complete seu endereço",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = ItaSuperTextPrimary
            )
            Text(
                text = "Usaremos a cidade informada para mostrar lojas que atendem sua região.",
                style = MaterialTheme.typography.bodyMedium,
                color = ItaSuperTextSecondary
            )

            AddressField(
                label = "CEP",
                value = draft.cep,
                keyboardType = KeyboardType.Number,
                onValueChange = { value -> onDraftChange { it.copy(cep = value.filter(Char::isDigit).take(8)) } },
                trailing = {
                    if (isLookingUpCep) {
                        CircularProgressIndicator(modifier = Modifier.padding(12.dp), strokeWidth = 2.dp)
                    } else {
                        OutlinedButton(onClick = onLookupCep, enabled = draft.cep.filter(Char::isDigit).length == 8) {
                            Text("Buscar")
                        }
                    }
                }
            )
            AddressField("Rua", draft.street) { value -> onDraftChange { it.copy(street = value) } }
            AddressField("Número", draft.number, KeyboardType.Text) { value -> onDraftChange { it.copy(number = value) } }
            AddressField("Complemento", draft.complement) { value -> onDraftChange { it.copy(complement = value) } }
            AddressField("Bairro", draft.neighborhood) { value -> onDraftChange { it.copy(neighborhood = value) } }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AddressField(
                    label = "Cidade",
                    value = draft.city,
                    modifier = Modifier.weight(1f),
                    onValueChange = { value -> onDraftChange { it.copy(city = value) } }
                )
                AddressField(
                    label = "UF",
                    value = draft.state,
                    modifier = Modifier.width(88.dp),
                    onValueChange = { value -> onDraftChange { it.copy(state = value.uppercase().take(2)) } }
                )
            }
            AddressField("Ponto de referência", draft.referencePoint) { value -> onDraftChange { it.copy(referencePoint = value) } }
            AddressField("WhatsApp com DDD", draft.whatsapp, KeyboardType.Phone) { value -> onDraftChange { it.copy(whatsapp = value.filter { char -> char.isDigit() }.take(13)) } }

            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onSave,
                enabled = !isSaving && !isLookingUpCep,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.height(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("Salvar endereço e continuar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AddressField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailing,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ItaSuperPrimary)
    )
}
