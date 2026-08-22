package com.meminzazo.stwvplanner.presentation.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.presentation.common.ManualEntryDialog
import com.meminzazo.stwvplanner.presentation.theme.*

/**
 * Pantalla de inicio con la lista de cuentas principales.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
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
                else -> { /* Otros eventos */ }
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
                title = { Text("MIS CUENTAS", fontWeight = FontWeight.Black) },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 12.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { viewModel.onSyncClick() }) {
                            Icon(Icons.Default.CloudSync, contentDescription = "Sincronizar", tint = FortAccent)
                        }
                    }
                    IconButton(onClick = { viewModel.onSignOutClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Salir", tint = SpendRed)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddAccountDialog = true },
                containerColor = FortAccent,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
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
                Text("CUENTAS ACTIVAS", style = MaterialTheme.typography.labelLarge, color = FortAccent)
            }

            if (accounts.isEmpty()) {
                item {
                    Text("No hay cuentas registradas. Crea una presionando el botón '+'.", color = Color.Gray)
                }
            }

            items(accounts) { account ->
                AccountCard(
                    account = account,
                    dependents = allAccounts.filter { it.parentAccountId == account.id },
                    onAddDaily = { amount -> viewModel.onAddDailyClick(account.id, amount) },
                    onAddAlert = { viewModel.onAddAlertClick(account.id) },
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
fun AccountCard(
    account: Account,
    dependents: List<Account>,
    onAddDaily: (Int) -> Unit,
    onAddAlert: () -> Unit,
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
            title = { Text("ELIMINAR CUENTA", fontWeight = FontWeight.Black) },
            text = { Text("¿Seguro que quieres borrar '${account.name}'? Todos los datos se perderán.") },
            confirmButton = {
                TextButton(onClick = { onDeleteAccount(); showDeleteConfirm = false }, colors = ButtonDefaults.textButtonColors(contentColor = SpendRed)) {
                    Text("ELIMINAR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("CANCELAR") }
            }
        )
    }

    if (showRenameDialog) {
        RenameAccountDialog(
            initialName = account.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName -> onRenameAccount(newName); showRenameDialog = false }
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
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StwCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = account.name.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.clickable { showRenameDialog = true }
                    )
                    if (account.isMain) {
                        Text("CUENTA PRINCIPAL", fontSize = 10.sp, color = FortAccent, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = SpendRed.copy(alpha = 0.7f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${account.balance}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = VBucksGold,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("V-BUCKS", style = MaterialTheme.typography.titleMedium, color = VBucksGold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onAddDaily(100) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = EarnGreen),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("+100 D", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
                Button(
                    onClick = { onAddAlert() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertBlue),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("+50 A", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
                IconButton(
                    onClick = { showManualEntryDialog = true },
                    modifier = Modifier.size(40.dp).background(FortPurple, MaterialTheme.shapes.small)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun RenameAccountDialog(initialName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("RENOMBRAR", fontWeight = FontWeight.Black) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(onClick = { onConfirm(name) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("NO") } })
}

@Composable
fun AddAccountDialog(onDismiss: () -> Unit, onConfirm: (String, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var isMain by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("NUEVA CUENTA", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isMain, onCheckedChange = { isMain = it })
                    Text("Es cuenta principal")
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name, isMain) }) { Text("CREAR") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } })
}
