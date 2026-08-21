package com.meminzazo.stwvplanner.presentation.expense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

import com.meminzazo.stwvplanner.domain.model.ItemType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: AddExpenseViewModel = hiltViewModel(),
    onPopBackStack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val recipientName by viewModel.recipientName.collectAsState()
    val description by viewModel.description.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val itemType by viewModel.itemType.collectAsState()
    val otherAccounts by viewModel.otherAccounts.collectAsState()

    var expandedRecipient by remember { mutableStateOf(false) }
    var expandedItemType by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is AddExpenseViewModel.UiEvent.SaveSuccess -> onPopBackStack()
                is AddExpenseViewModel.UiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Gasto / Regalo") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expandedRecipient,
                onExpandedChange = { expandedRecipient = !expandedRecipient }
            ) {
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = viewModel::onRecipientNameChange,
                    label = { Text("Cuenta que recibe (Amigo/Secundaria)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRecipient) }
                )

                if (otherAccounts.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedRecipient,
                        onDismissRequest = { expandedRecipient = false }
                    ) {
                        otherAccounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    viewModel.onRecipientSelected(account)
                                    expandedRecipient = false
                                }
                            )
                        }
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = expandedItemType,
                onExpandedChange = { expandedItemType = !expandedItemType }
            ) {
                OutlinedTextField(
                    value = when (itemType) {
                        ItemType.SKIN -> "Skin"
                        ItemType.DANCE -> "Baile"
                        ItemType.SONG -> "Canción"
                        ItemType.PACK -> "Paquete"
                        ItemType.OTHER -> "Otro"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de Objeto") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedItemType) }
                )

                ExposedDropdownMenu(
                    expanded = expandedItemType,
                    onDismissRequest = { expandedItemType = false }
                ) {
                    ItemType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { 
                                Text(when (type) {
                                    ItemType.SKIN -> "Skin"
                                    ItemType.DANCE -> "Baile"
                                    ItemType.SONG -> "Canción"
                                    ItemType.PACK -> "Paquete"
                                    ItemType.OTHER -> "Otro"
                                })
                            },
                            onClick = {
                                viewModel.onItemTypeChange(type)
                                expandedItemType = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Objeto (Skin, Baile, etc.)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Precio en V-Bucks") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::onSaveClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar")
            }
        }
    }
}
