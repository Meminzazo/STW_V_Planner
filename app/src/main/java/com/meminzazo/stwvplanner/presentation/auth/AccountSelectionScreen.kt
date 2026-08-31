package com.meminzazo.stwvplanner.presentation.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.presentation.dashboard.DashboardViewModel
import com.meminzazo.stwvplanner.presentation.theme.FortAccent
import com.meminzazo.stwvplanner.presentation.theme.SpendRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectionScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onAccountSelected: (Long) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val titleFontSize = if (configuration.screenWidthDp < 360) 18.sp else 22.sp

    val accounts by viewModel.accounts.collectAsState()
    val deletedAccounts by viewModel.deletedAccounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLocalMode by viewModel.isLocalMode.collectAsState()
    var isGuestBannerMinimized by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showCloudMenu by remember { mutableStateOf(false) }
    var showTransferCodeDialog by remember { mutableStateOf<String?>(null) }
    var showImportCodeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is DashboardViewModel.UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is DashboardViewModel.UiEvent.ShowTransferCode -> {
                    showTransferCodeDialog = event.code
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restaurar Respaldo") },
            text = { Text("¿Estás seguro? Esto reemplazará todos tus datos actuales por los que están guardados en la nube.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onRestoreClick()
                    showRestoreConfirm = false
                }) { Text("Restaurar") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    if (showTransferCodeDialog != null) {
        AlertDialog(
            onDismissRequest = { showTransferCodeDialog = null },
            title = { Text("Código de Transferencia") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Comparte este código con tu amigo. Tiene una validez de 24 horas.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = showTransferCodeDialog!!,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showTransferCodeDialog = null }) { Text("Cerrar") }
            }
        )
    }

    if (showImportCodeDialog) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportCodeDialog = false },
            title = { Text("Importar con Código") },
            text = {
                Column {
                    Text("Ingresa el código de 10 caracteres que te compartieron:")
                    OutlinedTextField(
                        value = code,
                        onValueChange = { if (it.length <= 10) code = it.uppercase() },
                        label = { Text("Código") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onImportWithCode(code)
                        showImportCodeDialog = false
                    },
                    enabled = code.length == 10
                ) { Text("Importar") }
            },
            dismissButton = {
                TextButton(onClick = { showImportCodeDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MIS CUENTAS", fontWeight = FontWeight.Bold, fontSize = titleFontSize) },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 12.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (!isLocalMode) {
                        Box {
                            IconButton(onClick = { showCloudMenu = true }) {
                                Icon(Icons.Default.Cloud, contentDescription = "Nube")
                            }
                            DropdownMenu(
                                expanded = showCloudMenu,
                                onDismissRequest = { showCloudMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Respaldar todo (Mi Cuenta)") },
                                    onClick = {
                                        viewModel.onBackupClick()
                                        showCloudMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Restaurar todo (Mi Cuenta)") },
                                    onClick = {
                                        showRestoreConfirm = true
                                        showCloudMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Generar código para amigo") },
                                    onClick = {
                                        viewModel.onGenerateTransferCode()
                                        showCloudMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Importar código de amigo") },
                                    onClick = {
                                        showImportCodeDialog = true
                                        showCloudMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) }
                                )
                            }
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
        if (showAddAccountDialog) {
            AddAccountDialog(
                onDismiss = { showAddAccountDialog = false },
                onConfirm = { name ->
                    viewModel.onCreateAccountClick(name)
                    showAddAccountDialog = false
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLocalMode) {
                item {
                    if (isGuestBannerMinimized) {
                        // Banner minimizado: solo un texto pequeño y clickeable
                        Text(
                            text = "☁️ ACTIVAR RESPALDO EN LA NUBE",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isGuestBannerMinimized = false }
                                .padding(vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Modo Invitado Activo", fontWeight = FontWeight.Bold)
                                    TextButton(onClick = { isGuestBannerMinimized = true }) {
                                        Text("OCULTAR", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Text("Tus datos no están respaldados en la nube.", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.onUpgradeToGoogle(context) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("VINCULAR CON GOOGLE")
                                }
                            }
                        }
                    }
                }
            }

            items(accounts) { account ->
                AccountItem(
                    account = account,
                    onClick = { onAccountSelected(account.id) },
                    onDelete = { viewModel.onDeleteAccountClick(account.id) }
                )
            }

            if (deletedAccounts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "GESTIONAR CUENTAS OCULTAS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(deletedAccounts) { account ->
                    DeletedAccountItem(
                        account = account,
                        onRestore = { viewModel.onRestoreAccountClick(account.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun AccountItem(
    account: Account,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar Cuenta") },
            text = { Text("¿Estás seguro de que deseas eliminar esta cuenta? Se perderán todos sus datos.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (account.isMain) {
                    Text(
                        text = "Cuenta Principal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun DeletedAccountItem(
    account: Account,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(account.name, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text("OCULTA", fontSize = 10.sp, color = Color.Gray)
            }
            IconButton(onClick = onRestore, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Restore, contentDescription = "Restaurar", tint = FortAccent)
            }
        }
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la cuenta") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        onConfirm(name)
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }) {
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