package com.meminzazo.stwvplanner.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meminzazo.stwvplanner.domain.model.Account

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToExpenses: (Long) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val accounts by viewModel.accounts.collectAsState()
    var showAddAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is DashboardViewModel.UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
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
            CenterAlignedTopAppBar(
                title = { Text("STW V Planner", fontWeight = FontWeight.Bold) },
                actions = {
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
                    onAddDaily = { amount -> viewModel.onAddDailyClick(account.id, amount) },
                    onAddAlert = { viewModel.onAddAlertClick(account.id) },
                    onRecordExpense = { onNavigateToExpenses(account.id) }
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
fun AccountCard(
    account: Account,
    onAddDaily: (Int) -> Unit,
    onAddAlert: () -> Unit,
    onRecordExpense: () -> Unit
) {
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
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (account.isMain) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text("Principal") }
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onRecordExpense,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Registrar Gasto / Regalo")
            }
        }
    }
}
