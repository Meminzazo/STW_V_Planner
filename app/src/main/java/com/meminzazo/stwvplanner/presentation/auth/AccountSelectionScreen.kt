package com.meminzazo.stwvplanner.presentation.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
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
import com.meminzazo.stwvplanner.BuildConfig
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.UpdateCheckResult
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.DateFormat
import java.util.Date
import androidx.navigation.NavController
import com.meminzazo.stwvplanner.domain.model.SharedLink
import com.meminzazo.stwvplanner.presentation.navigation.Screen
import com.meminzazo.stwvplanner.presentation.dashboard.DashboardViewModel
import com.meminzazo.stwvplanner.domain.model.BackupFrequency
import com.meminzazo.stwvplanner.domain.usecase.ConfigureAutomaticBackupUseCase
import com.meminzazo.stwvplanner.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateDialog(
    update: UpdateCheckResult.UpdateAvailable,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = StormCardSurface,
            border = BorderStroke(1.dp, StormBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = StormCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, StormCyan.copy(alpha = 0.5f))
                ) {
                    Text(
                        "ACTUALIZACIÓN DISPONIBLE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = StormCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Nueva versión ${update.remoteVersionName}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = StormAmber,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    "Tienes la v${BuildConfig.VERSION_NAME} instalada",
                    style = MaterialTheme.typography.bodySmall,
                    color = StormTextMuted
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    "NOVEDADES",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    color = StormCyan,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .fillMaxWidth(),
                    color = StormCardElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StormBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = update.changelog,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = StormTextMain
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ahora no", color = StormTextMuted)
                    }
                    
                    Button(
                        onClick = onUpdate,
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = StormCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ACTUALIZAR AHORA", color = StormBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun frequencyLabel(frequency: BackupFrequency) = when (frequency) {
    BackupFrequency.ON_APP_OPEN -> "Al abrir la app (máx. 2/día)"
    BackupFrequency.DAILY -> "Diaria"
    BackupFrequency.WEEKLY -> "Semanal"
    BackupFrequency.MONTHLY -> "Mensual"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectionScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onAccountSelected: (Long) -> Unit,
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val backupScheduler = remember { ConfigureAutomaticBackupUseCase(context) }
    var automaticBackupEnabled by remember { mutableStateOf(backupScheduler.enabled()) }
    var backupFrequency by remember { mutableStateOf(backupScheduler.frequency()) }

    val accounts by viewModel.accounts.collectAsState()
    val deletedAccounts by viewModel.deletedAccounts.collectAsState()
    val sharedLinks by viewModel.sharedLinks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLocalMode by viewModel.isLocalMode.collectAsState()
    val isGuestBannerMinimized by viewModel.isGuestBannerMinimized.collectAsState()

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showCloudMenu by remember { mutableStateOf(false) }
    var showCloudBackupDialog by remember { mutableStateOf(false) }
    var showAutomaticBackupDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showTransferImportConfirm by remember { mutableStateOf<String?>(null) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var showTransferCodeDialog by remember { mutableStateOf<String?>(null) }
    var showImportCodeDialog by remember { mutableStateOf(false) }
    var showDebugDialog by remember { mutableStateOf<String?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var updateToShow by remember { mutableStateOf<UpdateCheckResult.UpdateAvailable?>(null) }
    var showReadOnlyCodeDialog by remember { mutableStateOf(false) }
    var showInfraApprovalDialog by remember { mutableStateOf(false) }

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
        viewModel.cleanupOldFiles(context)
        viewModel.checkForUpdates()
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
                is DashboardViewModel.UiEvent.UpdateAvailable -> {
                    updateToShow = event.update
                }
                is DashboardViewModel.UiEvent.DownloadingUpdate -> {
                    snackbarHostState.showSnackbar("Descargando actualización: ${event.versionName}")
                }
                is DashboardViewModel.UiEvent.ShowInfraApprovalDialog -> {
                    showInfraApprovalDialog = true
                }
//                is DashboardViewModel.UiEvent.NavigateToOnboarding -> {
//                    navController.navigate(Screen.Onboarding.route)
//                }
            }
        }
    }

    if (showInfraApprovalDialog) {
        AlertDialog(
            onDismissRequest = { showInfraApprovalDialog = false },
            title = { Text("¡Acceso a la Nube Aprobado!", fontWeight = FontWeight.Bold) },
            text = { Text("Tu dispositivo ha sido autorizado. Para activar las funciones de respaldo, es necesario cerrar esta sesión temporal e iniciar con Google.") },
            confirmButton = {
                Button(
                    onClick = {
                        showInfraApprovalDialog = false
                        viewModel.onSignOutClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EarnGreen)
                ) { Text("Cerrar Sesión", color = StormBackground, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showInfraApprovalDialog = false }) { Text("Ahora no") }
            }
        )
    }

    if (updateToShow != null) {
        UpdateDialog(
            update = updateToShow!!,
            onDismiss = { updateToShow = null },
            onUpdate = {
                viewModel.downloadAndInstall(context, updateToShow!!)
                updateToShow = null
            }
        )
    }

    LaunchedEffect(isLocalMode) {
        if (isLocalMode) showReadOnlyCodeDialog = false
    }

    if (showReadOnlyCodeDialog && !isLocalMode) {
        ReadOnlyCodeDialog(
            onDismiss = { showReadOnlyCodeDialog = false },
            onConfirm = { code ->
                showReadOnlyCodeDialog = false
                navController.navigate(Screen.ReadOnlyView.createRoute(code))
            }
        )
    }

    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Importar Respaldo", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro? Esto reemplazará todos tus datos actuales por los del archivo seleccionado. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingImportUri?.let { viewModel.onImportFromFile(it, context) }
                        pendingImportUri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StormCyan)
                ) { Text("Reemplazar Datos", color = StormBackground, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Cancelar") }
            }
        )
    }

    if (showCloudBackupDialog) {
        AlertDialog(
            onDismissRequest = { showCloudBackupDialog = false },
            title = { Text("Respaldo en la nube", fontWeight = FontWeight.Bold) },
            text = { Text("Guarda una copia completa de las cuentas y transacciones de este dispositivo, o restaura la última copia disponible.") },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.onBackupClick()
                            showCloudBackupDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StormCyan)
                    ) { Text("Subir respaldo ahora", color = StormBackground, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = {
                            showCloudBackupDialog = false
                            showRestoreConfirm = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Restaurar desde la nube") }
                    TextButton(onClick = { showCloudBackupDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    if (showAutomaticBackupDialog) {
        AlertDialog(
            onDismissRequest = { showAutomaticBackupDialog = false },
            title = { Text("Respaldo automático", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Activar respaldo automático", modifier = Modifier.weight(1f))
                        Switch(
                            checked = automaticBackupEnabled,
                            onCheckedChange = {
                                automaticBackupEnabled = it
                                backupScheduler.setEnabled(it, backupFrequency)
                            }
                        )
                    }
                    BackupFrequency.values().forEach { frequency ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                backupFrequency = frequency
                                if (automaticBackupEnabled) backupScheduler.setEnabled(true, frequency)
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = backupFrequency == frequency, onClick = null)
                            Text(frequencyLabel(frequency))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAutomaticBackupDialog = false }) { Text("Listo") }
            }
        )
    }

    if (showTransferDialog) {
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = { Text("Transferir datos", fontWeight = FontWeight.Bold) },
            text = { Text("Genera un código temporal para mover una copia completa de tus datos a otro dispositivo, o importa uno recibido.") },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.onGenerateTransferCode()
                            showTransferDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StormCyan)
                    ) { Text("Generar código", color = StormBackground, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = {
                            viewModel.onStartImportCode()
                            showTransferDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Importar con código") }
                    TextButton(onClick = { showTransferDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restaurar Respaldo", fontWeight = FontWeight.Bold) },
            text = { Text("La copia de la nube reemplazará todas las cuentas y transacciones guardadas en este dispositivo. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onRestoreClick()
                        showRestoreConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StormCyan)
                ) { Text("Reemplazar datos", color = StormBackground, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    if (showTransferImportConfirm != null) {
        AlertDialog(
            onDismissRequest = { showTransferImportConfirm = null },
            title = { Text("Importar transferencia", fontWeight = FontWeight.Bold) },
            text = { Text("Los datos del código reemplazarán todas las cuentas y transacciones de este dispositivo. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onImportWithCode(showTransferImportConfirm!!)
                        showTransferImportConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StormCyan)
                ) { Text("Reemplazar datos", color = StormBackground, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showTransferImportConfirm = null }) { Text("Cancelar") } }
        )
    }

    if (showTransferCodeDialog != null) {
        AlertDialog(
            onDismissRequest = { showTransferCodeDialog = null },
            title = { Text("Código de Transferencia", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Comparte este código de 10 dígitos. Validez: 1 hora.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = StormCardElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StormBorder)
                    ) {
                        Text(
                            text = showTransferCodeDialog!!,
                            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Black,
                            color = StormCyan,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
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
            title = { Text("Importar con Código", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Ingresa el código numérico de 10 dígitos:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = {
                            if (it.length <= 10 && it.all { c -> c.isDigit() }) code = it
                        },
                        label = { Text("Código") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTransferImportConfirm = code
                        showImportCodeDialog = false
                    },
                    enabled = code.length == 10,
                    colors = ButtonDefaults.buttonColors(containerColor = StormCyan)
                ) { Text("Importar", color = StormBackground, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showImportCodeDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showExportOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showExportOptionsDialog = false },
            title = { Text("Exportar Respaldo", fontWeight = FontWeight.Bold) },
            text = { Text("¿Cómo deseas guardar la copia de seguridad?") },
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
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StormCardElevated)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = StormCyan)
                        Spacer(Modifier.width(8.dp))
                        Text("Guardar en archivo local", color = StormTextMain)
                    }
                    OutlinedButton(
                        onClick = {
                            showExportOptionsDialog = false
                            viewModel.onPerformShare(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, StormBorder)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = StormAmber)
                        Spacer(Modifier.width(8.dp))
                        Text("Compartir directamente", color = StormTextMain)
                    }
                    TextButton(
                        onClick = { showExportOptionsDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar", color = StormTextMuted)
                    }
                }
            }
        )
    }

    if (showDebugDialog != null) {
        val clipboardManager = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showDebugDialog = null },
            title = { Text("Identificador de Desarrollador", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Código para acceso a servicios de prueba.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StormTextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = StormCardElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, StormBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = showDebugDialog!!,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center,
                            color = StormCyan
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StormCyan)
                ) {
                    Text("Copiar y Cerrar", color = StormBackground, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        containerColor = StormBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StormBackground,
                    titleContentColor = StormTextMain,
                    actionIconContentColor = StormTextMuted
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "STW PLANNER",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = StormCardElevated,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, StormBorder)
                        ) {
                            Text(
                                text = "CUENTAS",
                                style = MaterialTheme.typography.labelSmall,
                                color = StormCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 12.dp),
                            strokeWidth = 2.dp,
                            color = StormCyan
                        )
                    } else {
                        Box {
                            IconButton(onClick = { showCloudMenu = true }) {
                                Icon(Icons.Default.Cloud, contentDescription = "Gestión de Datos", tint = StormCyan)
                            }
                            DropdownMenu(
                                expanded = showCloudMenu,
                                onDismissRequest = { showCloudMenu = false },
                                containerColor = StormCardElevated,
                                border = BorderStroke(1.dp, StormBorder)
                            ) {
                                Text(
                                    "RESPALDO LOCAL",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StormCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                DropdownMenuItem(
                                    text = { Text("Exportar archivo JSON", color = StormTextMain) },
                                    onClick = {
                                        viewModel.onStartExport()
                                        showCloudMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null, tint = StormCyan) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Importar archivo JSON", color = StormTextMain) },
                                    onClick = {
                                        filePickerLauncher.launch("application/json")
                                        showCloudMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = StormCyan) }
                                )

                                Text(
                                    "ACCESO A LA NUBE",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StormCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                DropdownMenuItem(
                                    text = { Text("Solicitar / Ver acceso", color = StormTextMain) },
                                    onClick = {
                                        showCloudMenu = false
                                        navController.navigate(Screen.InfrastructureRequest.route)
                                    },
                                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = StormCyan) }
                                )

                                HorizontalDivider(color = StormBorder)

                                Text(
                                    "NUBE FIREBASE",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isLocalMode) StormTextMuted else StormAmber,
                                    fontWeight = FontWeight.Bold
                                )
                                DropdownMenuItem(
                                    text = { Text("Respaldo en la nube") },
                                    onClick = {
                                        showCloudMenu = false
                                        showCloudBackupDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, tint = StormAmber) },
                                    enabled = !isLocalMode
                                )
                                DropdownMenuItem(
                                    text = { Text("Respaldo automático") },
                                    onClick = {
                                        showCloudMenu = false
                                        showAutomaticBackupDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null, tint = StormAmber) },
                                    enabled = !isLocalMode
                                )
                                DropdownMenuItem(
                                    text = { Text("Transferir datos") },
                                    onClick = {
                                        showCloudMenu = false
                                        showTransferDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = StormAmber) },
                                    enabled = !isLocalMode
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.onSignOutClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión", tint = SpendRed.copy(alpha = 0.8f))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddAccountDialog = true },
                containerColor = StormCyan,
                contentColor = StormBackground,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
            ) {
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLocalMode) {
                item {
                    if (isGuestBannerMinimized) {
                        Surface(
                            color = StormCardElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StormBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setGuestBannerMinimized(false) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "☁️ Modo Local Activo · Conectar Nube",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StormCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("MOSTRAR", style = MaterialTheme.typography.labelSmall, color = StormTextMuted)
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = StormCardElevated),
                            border = BorderStroke(1.dp, StormBorder)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("MODO INVITADO (LOCAL)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = StormAmber)
                                    TextButton(onClick = { viewModel.setGuestBannerMinimized(true) }) {
                                        Text("OCULTAR", style = MaterialTheme.typography.labelSmall, color = StormTextMuted)
                                    }
                                }
                                Text("Tus registros solo están en este teléfono. Vincula Google para activar respaldo automático.", style = MaterialTheme.typography.bodySmall, color = StormTextMuted)
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = { viewModel.onUpgradeToGoogle(context) },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StormCyan)
                                ) {
                                    Text("VINCULAR CON GOOGLE", fontWeight = FontWeight.Bold, color = StormBackground, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

            if (accounts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = StormCardSurface),
                        border = BorderStroke(1.dp, StormBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("SIN CUENTAS REGISTRADAS", style = MaterialTheme.typography.labelLarge, color = StormTextMuted)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Pulsa el botón '+' abajo para registrar tu primera cuenta de Salvar el Mundo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StormTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(accounts) { account ->
                AccountCardItem(
                    account = account,
                    onClick = { onAccountSelected(account.id) },
                    onDelete = { viewModel.onDeleteAccountClick(account.id) }
                )
            }

            if (!isLocalMode && sharedLinks.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "CUENTAS VINCULADAS",
                        style = MaterialTheme.typography.labelSmall,
                        color = StormCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(sharedLinks) { link ->
                    SharedLinkCardItem(
                        link = link,
                        onClick = { navController.navigate(Screen.ReadOnlyView.createRoute(link.code)) },
                        onDelete = { viewModel.deleteSharedLink(link.code) }
                    )
                }
            }

            if (!isLocalMode) item {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showReadOnlyCodeDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StormCardElevated.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, StormBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = StormCyan)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("VER CUENTA COMPARTIDA", style = MaterialTheme.typography.labelSmall, color = StormCyan, fontWeight = FontWeight.Bold)
                            Text("Introduce un código de 10 caracteres", style = MaterialTheme.typography.bodySmall, color = StormTextMuted)
                        }
                    }
                }
            }

            if (deletedAccounts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "CUENTAS OCULTAS",
                        style = MaterialTheme.typography.labelSmall,
                        color = StormTextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(deletedAccounts) { account ->
                    DeletedAccountCardItem(
                        account = account,
                        onRestore = { viewModel.onRestoreAccountClick(account.id) }
                    )
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
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
                    color = StormTextMuted.copy(alpha = 0.5f)
                )
            }
        }

    }
}

@Composable
fun ReadOnlyCodeDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ver Cuenta Compartida", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Introduce el código de 10 caracteres para ver el snapshot de otra cuenta:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { 
                        val filtered = it.uppercase().filter { c -> c.isLetterOrDigit() }
                        if (filtered.length <= 10) code = filtered 
                    },
                    label = { Text("Código de 10 caracteres") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code) },
                enabled = code.length == 10,
                colors = ButtonDefaults.buttonColors(containerColor = StormCyan)
            ) { Text("VER", color = StormBackground, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun SharedLinkCardItem(
    link: SharedLink,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar Vínculo", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas eliminar el acceso directo a '${link.accountName}'?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }, colors = ButtonDefaults.textButtonColors(contentColor = SpendRed)) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") } }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StormCardElevated.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, StormBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(link.accountName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StormTextMain)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = StormAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, StormAmber.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "COMPARTIDA",
                            fontSize = 8.sp,
                            color = StormAmber,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Text("Dueño: ${link.ownerName ?: "No disponible"}", style = MaterialTheme.typography.bodySmall, color = StormTextMuted)
                Text(
                    if (link.snapshotUpdatedAt > 0) "Actualizado el ${DateFormat.getDateTimeInstance().format(Date(link.snapshotUpdatedAt))}" else "Actualizado: fecha no disponible",
                    style = MaterialTheme.typography.bodySmall,
                    color = StormTextMuted
                )
            }
            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.LinkOff, contentDescription = "Eliminar vínculo", tint = StormTextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AccountCardItem(
    account: Account,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar Cuenta", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas eliminar '${account.name}'? Se conservará su historial si decides restaurarla después.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = SpendRed)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StormCardSurface),
        border = BorderStroke(1.dp, StormBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = StormTextMain
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${account.balance}",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Black,
                        color = StormAmber
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "V-BUCKS",
                        style = MaterialTheme.typography.labelSmall,
                        color = StormAmber.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = SpendRed.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Abrir",
                    tint = StormTextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun DeletedAccountCardItem(
    account: Account,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StormCardSurface.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, StormBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(account.name, style = MaterialTheme.typography.bodyMedium, color = StormTextMuted, fontWeight = FontWeight.Bold)
                Text("OCULTA", fontSize = 9.sp, color = StormTextMuted)
            }
            IconButton(onClick = onRestore, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Restore, contentDescription = "Restaurar", tint = StormCyan, modifier = Modifier.size(18.dp))
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
        title = { Text("Nueva Cuenta", fontWeight = FontWeight.Bold) },
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
                        if (name.isNotBlank()) onConfirm(name.trim())
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = StormCyan)
            ) {
                Text("Crear", color = StormBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
