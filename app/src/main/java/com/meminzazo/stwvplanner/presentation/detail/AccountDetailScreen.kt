package com.meminzazo.stwvplanner.presentation.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.presentation.common.ManualEntryDialog
import java.text.SimpleDateFormat
import java.util.*
import com.meminzazo.stwvplanner.presentation.theme.*

/**
 * Pantalla de detalle de una cuenta con estética Fortnite/STW.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: AccountDetailViewModel = hiltViewModel(),
    onPopBackStack: () -> Unit,
    onNavigateToHistory: (Long) -> Unit,
    onNavigateToSummary: (Long) -> Unit, // Nueva navegación
    snackbarHostState: SnackbarHostState
) {
    val account by viewModel.account.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val isDailyRegistered by viewModel.isDailyRegistered.collectAsState()
    val dependentRelations by viewModel.dependentRelations.collectAsState()
    val deletedDependents by viewModel.deletedDependents.collectAsState()
    val earningsDesglosadas by viewModel.earningsDesglosadas.collectAsState()
    val earningsDesglosadasMensual by viewModel.earningsDesglosadasMensual.collectAsState()
    val earningsTransactions by viewModel.earningsTransactions.collectAsState()
    val earningsTransactionsMensual by viewModel.earningsTransactionsMensual.collectAsState()
    val expenseDistribution by viewModel.expenseDistribution.collectAsState()
    val expenseDistributionMensual by viewModel.expenseDistributionMensual.collectAsState()
    val expenseTransactions by viewModel.expenseTransactions.collectAsState()
    val expenseTransactionsMensual by viewModel.expenseTransactionsMensual.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val totalExpensesMensual by viewModel.totalExpensesMensual.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalIncomeMensual by viewModel.totalIncomeMensual.collectAsState()
    val dependentAccounts by viewModel.dependentAccounts.collectAsState()
    val focusManager = LocalFocusManager.current

    var showAddDependentDialog by remember { mutableStateOf(false) }
    var showManualEntryDialog by remember { mutableStateOf(false) }
    var showDailyAmountDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var accountToRename by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var manualEntryInitialType by remember { mutableStateOf<TransactionType?>(null) }
    var manualEntryInitialSource by remember { mutableStateOf<VBucksSource?>(null) }
    var distributionToShow by remember { mutableStateOf<Pair<String, List<Transaction>>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is AccountDetailViewModel.UiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    if (distributionToShow != null) {
        DistributionHistoryDialog(
            title = distributionToShow!!.first,
            transactions = distributionToShow!!.second,
            onDismiss = { distributionToShow = null }
        )
    }

    if (showAddDependentDialog) {
        AddDependentDialog(
            onDismiss = { showAddDependentDialog = false },
            onConfirm = { name ->
                viewModel.onCreateDependentAccount(name)
                showAddDependentDialog = false
            }
        )
    }

    if (showRenameDialog || accountToRename != null) {
        val acc = accountToRename ?: account
        acc?.let { a ->
            RenameAccountDialog(
                initialName = a.name,
                onDismiss = { 
                    showRenameDialog = false 
                    accountToRename = null
                },
                onConfirm = { newName ->
                    viewModel.onRenameAccountClick(a.id, newName)
                    showRenameDialog = false
                    accountToRename = null
                }
            )
        }
    }

    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("OCULTAR DEPENDIENTE", fontWeight = FontWeight.Black) },
            text = { Text("¿Deseas ocultar a '${accountToDelete!!.name}'? No se borrarán sus registros de regalo ni se alterará el balance histórico.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeleteAccountClick(accountToDelete!!.id)
                    accountToDelete = null
                }, colors = ButtonDefaults.textButtonColors(contentColor = SpendRed)) {
                    Text("OCULTAR")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) { Text("CANCELAR") }
            }
        )
    }

    if (showManualEntryDialog) {
        ManualEntryDialog(
            dependents = dependentAccounts,
            initialType = manualEntryInitialType,
            initialSource = manualEntryInitialSource,
            onDismiss = { 
                showManualEntryDialog = false 
                manualEntryInitialType = null
                manualEntryInitialSource = null
            },
            onConfirm = { amount, type, source, desc, date, receiverId, receiverName ->
                viewModel.onManualEntryClick(amount, type, source, desc, date, receiverId, receiverName)
                showManualEntryDialog = false
                manualEntryInitialType = null
                manualEntryInitialSource = null
            }
        )
    }

    if (showDailyAmountDialog) {
        DailyAmountDialog(
            onDismiss = { showDailyAmountDialog = false },
            onConfirm = { amount ->
                viewModel.onAddDailyClick(amount)
                showDailyAmountDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = account?.name ?: "DETALLE",
                        modifier = Modifier.clickable { showRenameDialog = true },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // --- SALDO ---
            item {
                SectionTitle("ESTADO FINANCIERO")
                BalanceCard(balance)
            }

            // --- ACCIONES RÁPIDAS ---
            item {
                SectionTitle("ACCIONES RÁPIDAS")
                QuickActionsGrid(
                    account = account,
                    isDailyRegistered = isDailyRegistered,
                    onDailyClick = { showDailyAmountDialog = true },
                    onAlertClick = { viewModel.onAddAlertClick() },
                    onExternalClick = {
                        manualEntryInitialType = TransactionType.EARN
                        manualEntryInitialSource = VBucksSource.EXTERNAL
                        showManualEntryDialog = true
                    },
                    onExpenseClick = {
                        manualEntryInitialType = TransactionType.SPEND
                        manualEntryInitialSource = VBucksSource.GIFT
                        showManualEntryDialog = true
                    },
                    onManualClick = {
                        manualEntryInitialType = null
                        manualEntryInitialSource = null
                        showManualEntryDialog = true
                    }
                )
            }

            // --- ESTADÍSTICAS ---
            item {
                SectionTitle("ESTADÍSTICAS")
                if (earningsDesglosadas.isNotEmpty() || earningsDesglosadasMensual.isNotEmpty()) {
                    EarningsDistributionCard(
                        monthly = earningsDesglosadasMensual,
                        total = earningsDesglosadas,
                        totalIncome = totalIncome,
                        totalIncomeMensual = totalIncomeMensual,
                        onClick = { isMonthly ->
                            val txs = if (isMonthly) earningsTransactionsMensual else earningsTransactions
                            distributionToShow = "Ingresos (${if (isMonthly) "Mes" else "Total"})" to txs
                        }
                    )
                }
            }

            item {
                if (expenseDistribution.isNotEmpty() || expenseDistributionMensual.isNotEmpty()) {
                    ExpensesDistributionCard(
                        monthly = expenseDistributionMensual,
                        total = expenseDistribution,
                        totalExpenses = totalExpenses,
                        totalExpensesMensual = totalExpensesMensual,
                        onClick = { isMonthly ->
                            val txs = if (isMonthly) expenseTransactionsMensual else expenseTransactions
                            distributionToShow = "Egresos (${if (isMonthly) "Mes" else "Total"})" to txs
                        }
                    )
                }
            }

            // --- HISTORIAL ---
            item {
                Button(
                    onClick = { onNavigateToHistory(account?.id ?: 0) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text("VER HISTORIAL DETALLADO", fontWeight = FontWeight.Black)
                }
            }

            // --- DEPENDIENTES ---
            if (account?.parentAccountId == null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle("CUENTAS DEPENDIENTES")
                        IconButton(onClick = { showAddDependentDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir", tint = FortAccent)
                        }
                    }
                }

                items(dependentRelations) { relation ->
                    RelationItem(
                        relation = relation,
                        onClick = { onNavigateToSummary(relation.account.id) },
                        onEditName = { accountToRename = relation.account },
                        onDelete = { accountToDelete = relation.account }
                    )
                }

                if (deletedDependents.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "GESTIONAR DEPENDIENTES OCULTOS", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(deletedDependents) { dep ->
                        DeletedDependentItem(
                            account = dep,
                            onRestore = { viewModel.onRestoreAccountClick(dep.id) }
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = FortAccent,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun QuickActionsGrid(
    account: Account?,
    isDailyRegistered: Boolean,
    onDailyClick: () -> Unit,
    onAlertClick: () -> Unit,
    onExternalClick: () -> Unit,
    onExpenseClick: () -> Unit,
    onManualClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val spacing = if (configuration.screenWidthDp < 360) 8.dp else 12.dp
    val buttonHeight = if (configuration.screenWidthDp < 360) 44.dp else 50.dp
    val fontSize = if (configuration.screenWidthDp < 360) 10.sp else 12.sp

    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        if (account?.parentAccountId == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                ActionButton(
                    text = "DIARIA",
                    color = DailyButtonColor,
                    enabled = !isDailyRegistered,
                    modifier = Modifier.weight(1f),
                    isOutlined = true,
                    height = buttonHeight,
                    fontSize = fontSize,
                    onClick = onDailyClick
                )
                ActionButton(
                    text = "ALERTA +50",
                    color = AlertButtonColor,
                    modifier = Modifier.weight(1f),
                    isOutlined = true,
                    height = buttonHeight,
                    fontSize = fontSize,
                    onClick = onAlertClick
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                ActionButton(
                    text = "EXTERNO (+)",
                    color = ExternalButtonColor,
                    modifier = Modifier.weight(1f),
                    isOutlined = true,
                    height = buttonHeight,
                    fontSize = fontSize,
                    onClick = onExternalClick
                )
                ActionButton(
                    text = "GASTO / REGALO",
                    color = ExpenseButtonColor,
                    modifier = Modifier.weight(1f),
                    isOutlined = true,
                    height = buttonHeight,
                    fontSize = fontSize,
                    onClick = onExpenseClick
                )
            }
        }
        ActionButton(
            text = "REGISTRO MANUAL",
            color = FortPurple,
            modifier = Modifier.fillMaxWidth(),
            isOutlined = true,
            height = buttonHeight,
            fontSize = fontSize,
            onClick = onManualClick
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isOutlined: Boolean = false,
    height: androidx.compose.ui.unit.Dp = 50.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    onClick: () -> Unit
) {
    val displayColor = if (enabled) color else Color.Gray // Gris si está deshabilitado
    
    if (isOutlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(height),
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
            border = androidx.compose.foundation.BorderStroke(2.dp, displayColor),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = displayColor,
                disabledContentColor = Color.Gray
            )
        ) {
            Text(text, fontWeight = FontWeight.Black, fontSize = fontSize, maxLines = 1)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(height),
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = color)
        ) {
            Text(text, fontWeight = FontWeight.Black, fontSize = fontSize, maxLines = 1)
        }
    }
}

@Composable
fun BalanceCard(balance: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FortDarkBlue),
        border = androidx.compose.foundation.BorderStroke(2.dp, VBucksGold.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SALDO ACTUAL", style = MaterialTheme.typography.labelLarge, color = VBucksSilver)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$balance", 
                    style = MaterialTheme.typography.headlineLarge, 
                    fontWeight = FontWeight.Black,
                    color = VBucksGold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("V-BUCKS", style = MaterialTheme.typography.titleLarge, color = VBucksGold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DistributionPagerCard(
    title: String,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onClick: () -> Unit,
    monthlyContent: @Composable () -> Unit,
    totalContent: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = StwCardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title + if (pagerState.currentPage == 0) " · MENSUAL" else " · TOTAL",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = VBucksSilver
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                if (page == 0) monthlyContent() else totalContent()
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(pagerState.pageCount) { i ->
                    val selected = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = if (selected) 16.dp else 8.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(if (selected) FortAccent else Color.Gray)
                    )
                }
            }
        }
    }
}

@Composable
fun EarningsDistributionCard(
    monthly: Map<VBucksSource, Int>,
    total: Map<VBucksSource, Int>,
    totalIncome: Int = 0,
    totalIncomeMensual: Int = 0,
    onClick: (Boolean) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    DistributionPagerCard(
        title = "DISTRIBUCIÓN DE INGRESOS",
        pagerState = pagerState,
        onClick = { onClick(pagerState.currentPage == 0) },
        monthlyContent = { EarningsPieContent(monthly, totalIncomeMensual) },
        totalContent = { EarningsPieContent(total, totalIncome) }
    )
}

@Composable
fun ExpensesDistributionCard(
    monthly: Map<String, Int>,
    total: Map<String, Int>,
    totalExpenses: Int = 0,
    totalExpensesMensual: Int = 0,
    onClick: (Boolean) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    DistributionPagerCard(
        title = "DISTRIBUCIÓN DE EGRESOS",
        pagerState = pagerState,
        onClick = { onClick(pagerState.currentPage == 0) },
        monthlyContent = { ExpensesPieContent(monthly, totalExpensesMensual) },
        totalContent = { ExpensesPieContent(total, totalExpenses) }
    )
}

private val STATS_COLORS = listOf(EarnGreen, AlertBlue, FortPurple, FortBlue, FortAccent)

@Composable
private fun EarningsPieContent(data: Map<VBucksSource, Int>, totalIncome: Int = 0) {
    val incomeSum = data.values.sum().toFloat()
    if (data.isEmpty() || incomeSum == 0f) {
        Text("SIN INGRESOS REGISTRADOS", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 20.dp))
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Canvas(modifier = Modifier.size(80.dp)) {
            var startAngle = 0f
            data.entries.forEachIndexed { index, entry ->
                val sweepAngle = (entry.value / incomeSum) * 360f
                drawArc(color = STATS_COLORS.getOrElse(index) { Color.Gray }, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true)
                startAngle += sweepAngle
            }
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            data.entries.forEachIndexed { index, entry ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Box(modifier = Modifier.size(10.dp).background(STATS_COLORS.getOrElse(index) { Color.Gray }))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${entry.key.name}: ${entry.value}", fontSize = 11.sp, color = VBucksSilver)
                }
            }
            // Mostrar total de ingresos si hay datos
            if (totalIncome > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total Pavos Ingresados: $totalIncome", fontSize = 11.sp, color = EarnGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ExpensesPieContent(data: Map<String, Int>, totalExpenses: Int = 0) {
    val expensesSum = data.values.sum().toFloat()
    if (data.isEmpty() || expensesSum == 0f) {
        Text("SIN GASTOS REGISTRADOS", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 20.dp))
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Canvas(modifier = Modifier.size(80.dp)) {
            var startAngle = 0f
            data.entries.forEachIndexed { index, entry ->
                val sweepAngle = (entry.value.toFloat() / expensesSum) * 360f
                drawArc(color = STATS_COLORS.getOrElse(index + 2) { Color.Gray }, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true)
                startAngle += sweepAngle
            }
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            data.entries.forEachIndexed { index, entry ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Box(modifier = Modifier.size(10.dp).background(STATS_COLORS.getOrElse(index + 2) { Color.Gray }))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${entry.key}: ${entry.value}", fontSize = 11.sp, color = VBucksSilver)
                }
            }
            // Mostrar total de gastos si hay datos
            if (totalExpenses > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total Pavos Gastados: $totalExpenses", fontSize = 11.sp, color = SpendRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RelationItem(relation: DependentRelation, onClick: () -> Unit, onEditName: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = StwCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Column {
                    Text(relation.account.name.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "V-BUCKS RECIBIDOS ESTE MES: ${relation.monthlyReceived}",
                        fontSize = 11.sp,
                        color = VBucksSilver,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onEditName, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = FortAccent, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = SpendRed, modifier = Modifier.size(16.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("BALANCE TOTAL", fontSize = 10.sp, color = VBucksSilver)
                Text(
                    text = "${if (relation.totalBalance > 0) "+" else ""}${relation.totalBalance}",
                    color = if (relation.totalBalance >= 0) EarnGreen else SpendRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun DeletedDependentItem(
    account: Account,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(account.name.uppercase(), style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text("OCULTA", fontSize = 8.sp, color = Color.Gray)
            }
            IconButton(onClick = onRestore, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Restore, contentDescription = "Restaurar", tint = FortAccent.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun DailyAmountDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("MISIÓN DIARIA", fontWeight = FontWeight.Black) },
        text = { Text("Selecciona la recompensa de hoy:") },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onConfirm(100) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = EarnGreen)) {
                    Text("100", fontWeight = FontWeight.Black)
                }
                Button(onClick = { onConfirm(150) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = EarnGreen)) {
                    Text("150", fontWeight = FontWeight.Black)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
    )
}

@Composable
fun DistributionHistoryDialog(title: String, transactions: List<Transaction>, onDismiss: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f), // Más ancho como se solicitó
        title = { Text(title.uppercase(), fontWeight = FontWeight.Black, fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("FECHA", Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = VBucksSilver)
                    Text("CUENTA", Modifier.weight(1.8f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = VBucksSilver)
                    Text("DETALLE", Modifier.weight(2.5f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = VBucksSilver)
                    Text("MONTO", Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = VBucksSilver, textAlign = TextAlign.End)
                }
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                    items(transactions.sortedByDescending { it.date }) { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp), // Más espacio entre filas
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sdf.format(Date(tx.date)), Modifier.weight(1.2f), fontSize = 11.sp)
                            val account = tx.recipientAccountName ?: "-"
                            Text(account.uppercase(), Modifier.weight(1.8f), fontSize = 11.sp, maxLines = 1)
                            val detail = tx.itemName ?: tx.description
                            Text(detail.uppercase(), Modifier.weight(2.5f), fontSize = 11.sp, maxLines = 2) // Max 2 líneas para detalles largos
                            val color = if (tx.type == TransactionType.EARN) EarnGreen else SpendRed
                            Text("${if (tx.type == TransactionType.EARN) "+" else "-"}${tx.amount}", Modifier.weight(1.2f), fontSize = 13.sp, textAlign = TextAlign.End, color = color, fontWeight = FontWeight.Black)
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.2f))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("CERRAR") } }
    )
}

@Composable
fun AddDependentDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    AlertDialog(onDismissRequest = onDismiss, title = { Text("NUEVO DEPENDIENTE", fontWeight = FontWeight.Black) },
        text = { 
            OutlinedTextField(
                value = name, 
                onValueChange = { name = it }, 
                label = { Text("Nombre") }, 
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onConfirm(name)
                }),
                modifier = Modifier.fillMaxWidth()
            ) 
        },
        confirmButton = { Button(onClick = { onConfirm(name) }) { Text("CREAR") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } })
}

@Composable
fun RenameAccountDialog(initialName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    val focusManager = LocalFocusManager.current
    AlertDialog(onDismissRequest = onDismiss, title = { Text("RENOMBRAR", fontWeight = FontWeight.Black) },
        text = { 
            OutlinedTextField(
                value = name, 
                onValueChange = { name = it }, 
                label = { Text("Nombre") }, 
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onConfirm(name)
                }),
                modifier = Modifier.fillMaxWidth()
            ) 
        },
        confirmButton = { Button(onClick = { onConfirm(name) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("NO") } })
}
