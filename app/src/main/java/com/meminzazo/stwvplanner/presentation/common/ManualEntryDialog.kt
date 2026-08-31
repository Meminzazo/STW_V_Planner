package com.meminzazo.stwvplanner.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryDialog(
    dependents: List<Account>,
    transactionToEdit: Transaction? = null,
    initialType: TransactionType? = null,
    initialSource: VBucksSource? = null,
    onDismiss: () -> Unit,
    onConfirm: (Int, TransactionType, VBucksSource, String, Long, Long?, String?) -> Unit
) {
    var amount by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var type by remember { 
        mutableStateOf(transactionToEdit?.type ?: initialType ?: TransactionType.EARN) 
    }
    var source by remember { 
        mutableStateOf(transactionToEdit?.source ?: initialSource ?: VBucksSource.DAILY) 
    }
    var description by remember { mutableStateOf(transactionToEdit?.description ?: "") }
    var dateMillis by remember { mutableStateOf(transactionToEdit?.date ?: System.currentTimeMillis()) }
    val focusManager = LocalFocusManager.current

    // Para gastos (GIFT)
    var selectedReceiverId by remember { mutableStateOf<Long?>(transactionToEdit?.receiverAccountId) }
    var selectedReceiverName by remember { mutableStateOf<String?>(transactionToEdit?.recipientAccountName) }

    fun handleConfirm() {
        val amountInt = amount.toIntOrNull() ?: 0
        if (amountInt > 0) {
            onConfirm(amountInt, type, source, description, dateMillis, selectedReceiverId, selectedReceiverName)
        }
    }

    var expandedSource by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val utcMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    calendar.timeInMillis = utcMillis
                    val localCalendar = Calendar.getInstance()
                    localCalendar.set(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH),
                        12, 0, 0
                    )
                    dateMillis = localCalendar.timeInMillis
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transactionToEdit == null) "Registro Manual" else "Editar Registro") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tipo: Ingreso / Gasto
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == TransactionType.EARN,
                        onClick = { 
                            type = TransactionType.EARN 
                            if (transactionToEdit == null) source = VBucksSource.DAILY
                            selectedReceiverId = null
                            selectedReceiverName = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Ingreso")
                    }
                    SegmentedButton(
                        selected = type == TransactionType.SPEND,
                        onClick = { 
                            type = TransactionType.SPEND 
                            if (transactionToEdit == null) source = VBucksSource.SHOP
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Gasto")
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                    label = { Text("Monto (V-Bucks)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandedSource,
                    onExpandedChange = { expandedSource = !expandedSource }
                ) {
                    val label = if (type == TransactionType.EARN) "Fuente" else "Destino"
                    val displayValue = if (type == TransactionType.EARN) {
                        when (source) {
                            VBucksSource.DAILY -> "Diaria"
                            VBucksSource.ALERT -> "Alerta"
                            VBucksSource.BATTLE_PASS -> "Pase de batalla"
                            else -> "Otro"
                        }
                    } else {
                        selectedReceiverName ?: "Otros"
                    }

                    OutlinedTextField(
                        value = displayValue,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(label) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSource) }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSource,
                        onDismissRequest = { expandedSource = false }
                    ) {
                        if (type == TransactionType.EARN) {
                            listOf(
                                VBucksSource.DAILY to "Diaria",
                                VBucksSource.ALERT to "Alerta",
                                VBucksSource.BATTLE_PASS to "Pase de batalla",
                                VBucksSource.EXTERNAL to "Otro"
                            ).forEach { (s, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        source = s
                                        expandedSource = false
                                    }
                                )
                            }
                        } else {
                            dependents.forEach { dep ->
                                DropdownMenuItem(
                                    text = { Text(dep.name) },
                                    onClick = {
                                        source = VBucksSource.GIFT
                                        selectedReceiverId = dep.id
                                        selectedReceiverName = dep.name
                                        expandedSource = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Otros") },
                                onClick = {
                                    source = VBucksSource.SHOP
                                    selectedReceiverId = null
                                    selectedReceiverName = null
                                    expandedSource = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (Opcional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { 
                        focusManager.clearFocus()
                        handleConfirm() 
                    }),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fecha", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = sdf.format(Date(dateMillis)),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = ::handleConfirm,
                enabled = amount.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
