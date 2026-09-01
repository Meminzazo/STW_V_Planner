package com.meminzazo.stwvplanner.presentation.dashboard

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.meminzazo.stwvplanner.BuildConfig
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.domain.repository.AuthRepository
import com.meminzazo.stwvplanner.domain.repository.SyncRepository
import com.meminzazo.stwvplanner.domain.repository.VBucksRepository
import com.meminzazo.stwvplanner.domain.usecase.AddAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: VBucksRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    private val addAccountUseCase: AddAccountUseCase
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = repository.getMainAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedAccounts: StateFlow<List<Account>> = repository.getDeletedMainAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<Account>> = repository.getAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isLocalMode = MutableStateFlow(false)
    val isLocalMode = _isLocalMode.asStateFlow()

    val isGuestBannerMinimized: StateFlow<Boolean> = authRepository.isGuestBannerMinimized()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var lastActionTime = 0L
    private val CLOUD_COOLDOWN = 60000L

    private var importFailCount = 0
    private var lockoutUntil = 0L

    private var pendingBackupJson: String? = null

    private var versionClickCount = 0
    private var lastVersionClickTime = 0L

    val appVersion = "v${BuildConfig.VERSION_NAME}"

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            _isLocalMode.value = authRepository.isUserLocal()
        }
    }

    /**
     * Limpia respaldos temporales de más de 24h que pudieran haber quedado
     * en cache/exports tras un "Compartir directamente" (ver onPerformShare).
     */
    fun cleanupOldExports(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val exportsDir = File(context.cacheDir, "exports")
                val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
                exportsDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < cutoff) file.delete()
                }
            } catch (_: Exception) { /* limpieza best-effort, no bloquea la app */ }
        }
    }

    private fun isCloudActionAllowed(): Boolean {
        if (_isLocalMode.value) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("La nube está deshabilitada en modo local")) }
            return false
        }
        val now = System.currentTimeMillis()
        if (now - lastActionTime < CLOUD_COOLDOWN) {
            val wait = ((CLOUD_COOLDOWN - (now - lastActionTime)) / 1000) + 1
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Seguridad: Espera $wait segundos")) }
            return false
        }
        lastActionTime = now
        return true
    }

    private fun isImportAllowed(): Boolean {
        val now = System.currentTimeMillis()
        if (now < lockoutUntil) {
            val wait = ((lockoutUntil - now) / 60000) + 1
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Bloqueo de seguridad: intenta en $wait minutos")) }
            return false
        }
        return true
    }

    private fun Context.findActivity(): ComponentActivity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is ComponentActivity) return context
            context = context.baseContext
        }
        return null
    }

    fun onSignOutClick() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun onSyncClick() {
        onBackupClick()
    }

    fun onBackupClick() {
        if (!isCloudActionAllowed()) return
        viewModelScope.launch {
            _isLoading.value = true
            val user = authRepository.currentUser.first()
            if (user != null) {
                val result = syncRepository.backupFullDatabase(user.id)
                _uiEvent.emit(UiEvent.ShowError(if (result.isSuccess) "Respaldo total guardado" else result.exceptionOrNull()?.message ?: "Error al respaldar"))
            }
            _isLoading.value = false
        }
    }

    fun onRestoreClick() {
        if (!isCloudActionAllowed()) return
        viewModelScope.launch {
            _isLoading.value = true
            val user = authRepository.currentUser.first()
            if (user != null) {
                val result = syncRepository.restoreFullDatabase(user.id)
                _uiEvent.emit(UiEvent.ShowError(if (result.isSuccess) "Restauración completada" else result.exceptionOrNull()?.message ?: "Error al restaurar"))
            }
            _isLoading.value = false
        }
    }

    fun onGenerateTransferCode() {
        if (!isCloudActionAllowed()) return
        viewModelScope.launch {
            _isLoading.value = true
            val user = authRepository.currentUser.first()
            if (user != null) {
                val result = syncRepository.generateTransferCode(user.id)
                if (result.isSuccess) {
                    _uiEvent.emit(UiEvent.ShowTransferCode(result.getOrNull() ?: ""))
                } else {
                    _uiEvent.emit(UiEvent.ShowError("Error al generar código"))
                }
            }
            _isLoading.value = false
        }
    }

    fun onStartImportCode() {
        if (isCloudActionAllowed()) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowImportCodeDialog)
            }
        }
    }

    fun onImportWithCode(code: String) {
        if (!isImportAllowed() || !isCloudActionAllowed()) return
        viewModelScope.launch {
            // Limpieza básica de la entrada
            val cleanCode = code.trim().filter { it.isDigit() }
            if (cleanCode.length != 10) {
                _uiEvent.emit(UiEvent.ShowError("El código debe tener 10 números"))
                return@launch
            }
            _isLoading.value = true
            val result = syncRepository.restoreFromTransferCode(cleanCode)
            if (result.isSuccess) {
                importFailCount = 0
                _uiEvent.emit(UiEvent.ShowError("Registros importados con éxito"))
            } else {
                importFailCount++
                if (importFailCount >= 3) {
                    lockoutUntil = System.currentTimeMillis() + 900000L
                    _uiEvent.emit(UiEvent.ShowError("Seguridad: Demasiados fallos. Bloqueo de 15 min activado."))
                } else {
                    _uiEvent.emit(UiEvent.ShowError("Código inválido o expirado"))
                }
            }
            _isLoading.value = false
        }
    }

    fun onStartExport() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = syncRepository.getFullDatabaseJson()
            if (result.isSuccess) {
                pendingBackupJson = result.getOrThrow()
                _uiEvent.emit(UiEvent.ShowExportOptions)
            } else {
                _uiEvent.emit(UiEvent.ShowError("Error al generar respaldo"))
            }
            _isLoading.value = false
        }
    }

    fun onConfirmSaveExport() {
        viewModelScope.launch {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val fileName = "VPlanner_Backup_$timeStamp.json"
            _uiEvent.emit(UiEvent.LaunchCreateDocument(fileName))
        }
    }

    fun onPerformSave(uri: Uri, context: Context) {
        val json = pendingBackupJson ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray())
                    }
                }
                _uiEvent.emit(UiEvent.ShowError("Archivo guardado con éxito"))
                pendingBackupJson = null
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Error al guardar: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onPerformShare(context: Context) {
        val json = pendingBackupJson ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                val fileName = "VPlanner_Backup_$timeStamp.json"
                val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(exportsDir, fileName)

                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { it.write(json.toByteArray()) }
                }

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Compartir Respaldo"))
                pendingBackupJson = null
                // No se borra el archivo aquí: la app receptora aún puede estar leyendo el stream.
                // Se limpia solo (>24h) en cleanupOldExports(), llamado al abrir esta pantalla.
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Error al compartir: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Paso 1: el usuario ya eligió un archivo. Antes de tocar la BD, se pide confirmación en la UI. */
    fun onFileSelectedForImport(uri: Uri) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ConfirmFileImport(uri))
        }
    }

    /** Paso 2: se ejecuta solo tras la confirmación explícita del usuario en el diálogo. */
    fun onImportFromFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {
                        it.bufferedReader().readText()
                    }
                } ?: ""

                if (json.isBlank()) {
                    _uiEvent.emit(UiEvent.ShowError("El archivo está vacío"))
                } else {
                    val result = syncRepository.restoreDatabaseFromJson(json)
                    _uiEvent.emit(UiEvent.ShowError(if (result.isSuccess) "Importación completada" else "Error al importar: ${result.exceptionOrNull()?.message}"))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Error al leer archivo: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setGuestBannerMinimized(minimized: Boolean) {
        viewModelScope.launch {
            authRepository.setGuestBannerMinimized(minimized)
        }
    }

    fun onUpgradeToGoogle(context: Context) {
        val activity = context.findActivity()
        if (activity == null) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Error interno: No se encontró la actividad")) }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val credentialManager = CredentialManager.create(activity)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(com.meminzazo.stwvplanner.R.string.default_web_client_id))
                    .setAutoSelectEnabled(false) // Desactivamos auto-select para forzar el diálogo
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential

                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val res = authRepository.signInWithGoogle(googleIdTokenCredential.idToken)
                    if (res.isSuccess) {
                        _isLocalMode.value = false
                        _uiEvent.emit(UiEvent.ShowError("¡Cuenta vinculada con éxito!"))
                    } else {
                        _uiEvent.emit(UiEvent.ShowError("Firebase: ${res.exceptionOrNull()?.message}"))
                    }
                } catch (e: Exception) {
                    _uiEvent.emit(UiEvent.ShowError("Error: Tipo '${credential.type}' no reconocido"))
                }
            } catch (e: GetCredentialException) {
                _uiEvent.emit(UiEvent.ShowError("Google: ${e.message}"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Error inesperado: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onCreateAccountClick(name: String, isMain: Boolean = true) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            addAccountUseCase(name = name, isMain = isMain)
        }
    }

    fun onRenameAccountClick(accountId: Long, newName: String) {
        viewModelScope.launch {
            if (newName.isBlank()) return@launch
            val account = repository.getAccountById(accountId)
            if (account != null) {
                repository.updateAccount(account.copy(name = newName))
            }
        }
    }

    fun onAddDailyClick(accountId: Long, amount: Int) {
        viewModelScope.launch {
            val transaction = Transaction(
                accountId = accountId,
                amount = amount,
                type = TransactionType.EARN,
                source = VBucksSource.DAILY,
                description = "Misión Diaria",
                date = System.currentTimeMillis()
            )
            repository.insertTransaction(transaction)
        }
    }

    fun onAddAlertClick(accountId: Long) {
        viewModelScope.launch {
            val transaction = Transaction(
                accountId = accountId,
                amount = 50,
                type = TransactionType.EARN,
                source = VBucksSource.ALERT,
                description = "Alerta de Misión",
                date = System.currentTimeMillis()
            )
            repository.insertTransaction(transaction)
        }
    }

    fun onManualEntryClick(
        accountId: Long,
        amount: Int,
        type: TransactionType,
        source: VBucksSource,
        description: String,
        date: Long,
        receiverId: Long? = null,
        receiverName: String? = null
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                accountId = accountId,
                amount = amount,
                type = type,
                source = source,
                description = description.ifBlank {
                    if (type == TransactionType.SPEND && receiverName != null) "Regalo a $receiverName"
                    else source.name
                },
                date = date,
                receiverAccountId = receiverId,
                recipientAccountName = receiverName
            )
            repository.insertTransaction(transaction)
        }
    }

    fun onDeleteAccountClick(accountId: Long) {
        viewModelScope.launch { repository.deleteAccount(accountId) }
    }

    fun onRestoreAccountClick(accountId: Long) {
        viewModelScope.launch { repository.restoreAccount(accountId) }
    }

    fun onVersionClick() {
        val now = System.currentTimeMillis()
        if (now - lastVersionClickTime > 2000) {
            versionClickCount = 1
        } else {
            versionClickCount++
        }
        lastVersionClickTime = now

        if (versionClickCount >= 7) {
            versionClickCount = 0
            viewModelScope.launch {
                // Forzamos la generación silenciosa antes de leer
                authRepository.ensureAppCheckTokenGenerated()
                
                val token = authRepository.getAppCheckDebugToken()
                _uiEvent.emit(UiEvent.ShowDebugDialog(token ?: "Error al generar token automático"))
            }
        }
    }

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
        data class ShowTransferCode(val code: String) : UiEvent()
        object ShowExportOptions : UiEvent()
        object ShowImportCodeDialog : UiEvent()
        data class ShowDebugDialog(val token: String) : UiEvent()
        data class LaunchCreateDocument(val fileName: String) : UiEvent()
        data class ConfirmFileImport(val uri: Uri) : UiEvent()
    }
}