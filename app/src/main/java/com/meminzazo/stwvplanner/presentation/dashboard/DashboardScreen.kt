package com.meminzazo.stwvplanner.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.presentation.common.ManualEntryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToExpenses: (Long) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val accounts by viewModel.accounts.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showAddAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is DashboardViewModel.UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> { /* Ignore other events */ }
            }
        }
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name, isMain ->
                viewModel.onCreateAccountClick(name, isMain)
                showAddAccountDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("STW V Planner", fontWeight = FontWeight.Bold) },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 12.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(onClick = { viewModel.onSyncClick() }) {
                            Icon(Icons.Default.CloudSync, contentDescription = "Sincronizar ahora")
                        }
                    }
                    IconButton(onClick = { viewModel.onSignOutClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddAccountDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Cuenta")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Tus Cuentas",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (accounts.isEmpty()) {
                item {
                    Text("No hay cuentas registradas. Crea una para empezar.")
                }
            }

            items(accounts) { account ->
                AccountCard(
                    account = account,
                    dependents = allAccounts.filter { it.parentAccountId == account.id },
                    onAddDaily = { amount -> viewModel.onAddDailyClick(account.id, amount) },
                    onAddAlert = { viewModel.onAddAlertClick(account.id) },
                    onRecordExpense = { onNavigateToExpenses(account.id) },
                    onDeleteAccount = { viewModel.onDeleteAccountClick(account.id) },
                    onRenameAccount = { newName -> viewModel.onRenameAccountClick(account.id, newName) },
                    onManualEntry = { amount, type, source, desc, date, receiverId, receiverName ->
                        viewModel.onManualEntryClick(account.id, amount, type, source, desc, date, receiverId, receiverName)
                    }
                )
            }
        }
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isMain by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la cuenta") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isMain, onCheckedChange = { isMain = it })
                    Text("Es cuenta principal")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, isMain) }) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun RenameAccountDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renombrar Cuenta") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la cuenta") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun AccountCard(
    account: Account,
    dependents: List<Account>,
    onAddDaily: (Int) -> Unit,
    onAddAlert: () -> Unit,
    onRecordExpense: () -> Unit,
    onDeleteAccount: () -> Unit,
    onRenameAccount: (String) -> Unit,
    onManualEntry: (Int, TransactionType, VBucksSource, String, Long, Long?, String?) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showManualEntryDialog by remember { mutableStateOf(false) }
    var manualEntryInitialType by remember { mutableStateOf<TransactionType?>(null) }
    var manualEntryInitialSource by remember { mutableStateOf<VBucksSource?>(null) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar Cuenta") },
            text = { Text("¿Estás seguro de que deseas eliminar esta cuenta? Se perderán todos sus datos.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAccount()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showRenameDialog) {
        RenameAccountDialog(
            initialName = account.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                onRenameAccount(newName)
                showRenameDialog = false
            }
        )
    }

    if (showManualEntryDialog) {
        ManualEntryDialog(
            dependents = dependents,
            initialType = manualEntryInitialType,
            initialSource = manualEntryInitialSource,
            onDismiss = { 
                showManualEntryDialog = false 
                manualEntryInitialType = null
                manualEntryInitialSource = null
            },
            onConfirm = { amount, type, source, desc, date, receiverId, receiverName ->
                onManualEntry(amount, type, source, desc, date, receiverId, receiverName)
                showManualEntryDialog = false
                manualEntryInitialType = null
                manualEntryInitialSource = null
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showRenameDialog = true }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (account.isMain) {
                        SuggestionChip(
                            onClick = { },
                            label = { Text("Principal") }
                        )
                    }
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${account.balance} V-Bucks",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Registro Rápido", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onAddDaily(100) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text("+100 D", fontSize = 12.sp)
                }
                Button(
                    onClick = { onAddDaily(150) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text("+150 D", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { onAddAlert() },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text("+50 A", fontSize = 12.sp)
                }
                IconButton(
                    onClick = { 
                        manualEntryInitialType = null
                        manualEntryInitialSource = null
                        showManualEntryDialog = true 
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Manual",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    manualEntryInitialType = TransactionType.SPEND
                    manualEntryInitialSource = VBucksSource.GIFT
                    showManualEntryDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Registrar Gasto / Regalo")
            }
        }
    }
}
