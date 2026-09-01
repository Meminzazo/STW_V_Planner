package com.meminzazo.stwvplanner.presentation.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
    val isGuestBannerMinimized by viewModel.isGuestBannerMinimized.collectAsState()
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showCloudMenu by remember { mutableStateOf(false) }
    var showCloudOptions by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var showTransferCodeDialog by remember { mutableStateOf<String?>(null) }
    var showImportCodeDialog by remember { mutableStateOf(false) }
    var showDebugDialog by remember { mutableStateOf<String?>(null) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onFileSelectedForImport(it) }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.onPerformSave(it, context) }
    }

    LaunchedEffect(Unit) {
        viewModel.cleanupOldExports(context)
        viewModel.uiEvent.collect { event ->
            when (event) {
                is DashboardViewModel.UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is DashboardViewModel.UiEvent.ShowTransferCode -> {
                    showTransferCodeDialog = event.code
                }
                is DashboardViewModel.UiEvent.ShowExportOptions -> {
                    showExportOptionsDialog = true
                }
                is DashboardViewModel.UiEvent.ShowImportCodeDialog -> {
                    showImportCodeDialog = true
                }
                is DashboardViewModel.UiEvent.ShowDebugDialog -> {
                    showDebugDialog = event.token
                }
                is DashboardViewModel.UiEvent.LaunchCreateDocument -> {
                    createDocumentLauncher.launch(event.fileName)
                }
                is DashboardViewModel.UiEvent.ConfirmFileImport -> {
                    pendingImportUri = event.uri
                }
            }
        }
    }

    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Importar Respaldo") },
            text = { Text("¿Estás seguro? Esto reemplazará todos tus datos actuales por los del archivo seleccionado. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingImportUri?.let { viewModel.onImportFromFile(it, context) }
                        pendingImportUri = null
                    }
                ) { Text("Sí, reemplazar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Cancelar") }
            }
        )
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
                    Text("Ingresa el código de 10 dígitos que te compartieron:")
                    OutlinedTextField(
                        value = code,
                        onValueChange = {
                            if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                code = it
                            }
                        },
                        label = { Text("Código Numérico") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
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

    if (showExportOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showExportOptionsDialog = false },
            title = { Text("Exportar Respaldo") },
            text = { Text("¿Cómo deseas guardar tu respaldo?") },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showExportOptionsDialog = false
                            viewModel.onConfirmSaveExport()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Guardar en dispositivo")
                    }
                    OutlinedButton(
                        onClick = {
                            showExportOptionsDialog = false
                            viewModel.onPerformShare(context)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Compartir directamente")
                    }
                    TextButton(
                        onClick = { showExportOptionsDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    if (showDebugDialog != null) {
        val clipboardManager = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showDebugDialog = null },
            title = { Text("Identificador de Desarrollador") },
            text = {
                Column {
                    Text(
                        "Este código permite que este dispositivo acceda a los servicios de Firebase para pruebas. Solo compártelo con el administrador del proyecto.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = showDebugDialog!!,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(showDebugDialog!!))
                        showDebugDialog = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Copiar y Cerrar")
                }
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
                    } else {
                        Box {
                            IconButton(onClick = {
                                showCloudMenu = true
                                showCloudOptions = false // Cerrada por defecto al abrir el menú principal
                            }) {
                                Icon(Icons.Default.Cloud, contentDescription = "Gestión de Datos")
                            }
                            DropdownMenu(
                                expanded = showCloudMenu,
                                onDismissRequest = { showCloudMenu = false }
                            ) {
                                // Sección de Archivo (Local) - Visible siempre y arriba
                                Text(
                                    "📁 ARCHIVO LOCAL",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FortAccent
                                )
                                DropdownMenuItem(
                                    text = { Text("Exportar Respaldo") },
                                    onClick = {
                                        viewModel.onStartExport()
                                        showCloudMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Importar Respaldo") },
                                    onClick = {
                                        filePickerLauncher.launch("application/json")
                                        showCloudMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) }
                                )

                                HorizontalDivider()

                                // Sección de Nube (Firebase) - Minimizada por defecto
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "☁️ NUBE (FIREBASE)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isLocalMode) Color.Gray else MaterialTheme.colorScheme.primary
                                            )
                                            Text(if (showCloudOptions) "▲" else "▼", fontSize = 10.sp)
                                        }
                                    },
                                    onClick = { showCloudOptions = !showCloudOptions }
                                )

                                if (showCloudOptions) {
                                    DropdownMenuItem(
                                        text = { Text("Respaldar en la Nube") },
                                        onClick = {
                                            viewModel.onBackupClick()
                                            showCloudMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                                        enabled = !isLocalMode
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Restaurar de la Nube") },
                                        onClick = {
                                            showRestoreConfirm = true
                                            showCloudMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                                        enabled = !isLocalMode
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Generar código") },
                                        onClick = {
                                            viewModel.onGenerateTransferCode()
                                            showCloudMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                        enabled = !isLocalMode
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Importar código") },
                                        onClick = {
                                            viewModel.onStartImportCode()
                                            showCloudMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                        enabled = !isLocalMode
                                    )
                                }
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
                                .clickable { viewModel.setGuestBannerMinimized(false) }
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
                                    TextButton(onClick = { viewModel.setGuestBannerMinimized(true) }) {
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

            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = viewModel.appVersion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { viewModel.onVersionClick() }
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray.copy(alpha = 0.5f)
                )
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