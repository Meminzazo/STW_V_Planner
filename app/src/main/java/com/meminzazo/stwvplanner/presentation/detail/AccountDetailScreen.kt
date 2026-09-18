package com.meminzazo.stwvplanner.presentation.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.presentation.common.ManualEntryDialog
import com.meminzazo.stwvplanner.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: AccountDetailViewModel = hiltViewModel(),
    onPopBackStack: () -> Unit,
    onNavigateToHistory: (Long) -> Unit,
    onNavigateToSummary: (Long) -> Unit,
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

    var showAddDependentDialog by remember { mutableStateOf(false) }
    var showManualEntryDialog by remember { mutableStateOf(false) }
    var showDailyAmountDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var accountToRename by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var manualEntryInitialType by remember { mutableStateOf<TransactionType?>(null) }
    var manualEntryInitialSource by remember { mutableStateOf<VBucksSource?>(null) }
    var distributionToShow by remember { mutableStateOf<Pair<String, List<Transaction>>?>(null) }
    var showShareReadOnlyDialog by remember { mutableStateOf(false) }
    var shareCode by remember { mutableStateOf<String?>(null) }
    var isGeneratingCode by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is AccountDetailViewModel.UiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is AccountDetailViewModel.UiEvent.ShareCodeGenerated -> {
                    shareCode = event.code
                    isGeneratingCode = false
                }
            }
        }
    }

    if (showShareReadOnlyDialog) {
        ShareReadOnlyDialog(
            shareCode = shareCode,
            isGenerating = isGeneratingCode,
            onDismiss = {
                showShareReadOnlyDialog = false
                shareCode = null
                isGeneratingCode = false
            },
            onGenerate = {
                isGeneratingCode = true
                viewModel.generateShareCode()
            },
            onCopy = {
                clipboardManager.setText(AnnotatedString(shareCode ?: ""))
                // Snackbar se mostrará al usuario
            }
        )
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
            title = { Text("Ocultar Dependiente", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas ocultar a '${accountToDelete!!.name}'? Se conservará su historial de regalos.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeleteAccountClick(accountToDelete!!.id)
                    accountToDelete = null
                }, colors = ButtonDefaults.textButtonColors(contentColor = SpendRed)) {
                    Text("Ocultar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) { Text("Cancelar") }
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
        containerColor = StormBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StormBackground,
                    titleContentColor = StormTextMain
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showRenameDialog = true }
                    ) {
                        Text(
                            text = account?.name ?: "DETALLE",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar nombre",
                            tint = StormTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showShareReadOnlyDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", tint = StormCyan)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = StormTextMain)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // --- HERO SALDO ---
            item {
                BalanceHeroCard(
                    balance = balance,
                    isDailyRegistered = isDailyRegistered,
                    onAddDaily100 = { viewModel.onAddDailyClick(100) },
                    onAddDaily150 = { viewModel.onAddDailyClick(150) },
                    isMainAccount = account?.parentAccountId == null
                )
            }

            // --- ACCIONES RÁPIDAS ---
            item {
                SectionHeader("ACCIONES RÁPIDAS")
                Spacer(modifier = Modifier.height(10.dp))
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
            if (earningsDesglosadas.isNotEmpty() || earningsDesglosadasMensual.isNotEmpty() ||
                expenseDistribution.isNotEmpty() || expenseDistributionMensual.isNotEmpty()) {
                item {
                    SectionHeader("ESTADÍSTICAS & DESGLOSE")
                }
            }

            if (earningsDesglosadas.isNotEmpty() || earningsDesglosadasMensual.isNotEmpty()) {
                item {
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

            if (expenseDistribution.isNotEmpty() || expenseDistributionMensual.isNotEmpty()) {
                item {
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

            // --- ACCESO AL HISTORIAL ---
            item {
                Button(
                    onClick = { onNavigateToHistory(account?.id ?: 0) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StormCardElevated),
                    border = BorderStroke(1.dp, StormBorder)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = StormCyan)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "VER HISTORIAL COMPLETO",
                        fontWeight = FontWeight.Bold,
                        color = StormTextMain,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // --- CUENTAS DEPENDIENTES ---
            if (account?.parentAccountId == null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader("DEPENDIENTES / VÍNCULOS")
                        IconButton(onClick = { showAddDependentDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir", tint = StormCyan)
                        }
                    }
                }

                items(dependentRelations) { relation ->
                    RelationItemCard(
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
                            "DEPENDIENTES OCULTOS",
                            style = MaterialTheme.typography.labelSmall,
                            color = StormTextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(deletedDependents) { dep ->
                        DeletedDependentItemCard(
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
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = StormCyan,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun BalanceHeroCard(
    balance: Int,
    isDailyRegistered: Boolean,
    onAddDaily100: () -> Unit,
    onAddDaily150: () -> Unit,
    isMainAccount: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StormCardSurface),
        border = BorderStroke(1.5.dp, StormBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 28.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "SALDO ACTUAL",
                style = MaterialTheme.typography.labelMedium,
                color = StormTextMuted,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$balance",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 42.sp
                    ),
                    fontWeight = FontWeight.Black,
                    color = StormAmber
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "V-BUCKS",
                    style = MaterialTheme.typography.titleLarge,
                    color = StormAmber.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
            }

            // --- DIRECT DAILY STATUS BANNER ---
            if (isMainAccount) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = StormBorder.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(14.dp))

                if (isDailyRegistered) {
                    Surface(
                        color = EarnGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EarnGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EarnGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Misión Diaria de Hoy Completada",
                                style = MaterialTheme.typography.labelLarge,
                                color = EarnGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "⚡ REGISTRAR MISIÓN DIARIA DE HOY",
                            style = MaterialTheme.typography.labelSmall,
                            color = StormCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onAddDaily100,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EarnGreen)
                            ) {
                                Text("+100 V", fontWeight = FontWeight.Black, color = StormBackground, style = MaterialTheme.typography.titleMedium)
                            }
                            Button(
                                onClick = onAddDaily150,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EarnGreen)
                            ) {
                                Text("+150 V", fontWeight = FontWeight.Black, color = StormBackground, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
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
    val buttonHeight = 46.dp

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (account?.parentAccountId == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onAlertClick,
                    modifier = Modifier.weight(1f).height(buttonHeight),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, StormCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StormCyan)
                ) {
                    Text("ALERTA +50", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onExternalClick,
                    modifier = Modifier.weight(1f).height(buttonHeight),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, YellowAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = YellowAccent)
                ) {
                    Text("EXTERNO (+)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onExpenseClick,
                    modifier = Modifier.weight(1f).height(buttonHeight),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SpendRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SpendRed)
                ) {
                    Text("GASTO / REGALO", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onManualClick,
                    modifier = Modifier.weight(1f).height(buttonHeight),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PurpleAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent)
                ) {
                    Text("MANUAL", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
        } else {
            OutlinedButton(
                onClick = onManualClick,
                modifier = Modifier.fillMaxWidth().height(buttonHeight),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, PurpleAccent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent)
            ) {
                Text("REGISTRO MANUAL", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StormCardSurface),
        border = BorderStroke(1.dp, StormBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title + if (pagerState.currentPage == 0) " · MES" else " · TOTAL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = StormTextMuted
                )
                Text(
                    text = "Toca para ver lista 🔍",
                    style = MaterialTheme.typography.labelSmall,
                    color = StormCyan,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                if (page == 0) monthlyContent() else totalContent()
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(pagerState.pageCount) { i ->
                    val selected = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (selected) 14.dp else 6.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(if (selected) StormCyan else StormBorder)
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
        title = "INGRESOS",
        pagerState = pagerState,
        onClick = { onClick(pagerState.currentPage == 0) },
        monthlyContent = { EarningsPieContent(monthly.mapKeys { it.key.name }, totalIncomeMensual) },
        totalContent = { EarningsPieContent(total.mapKeys { it.key.name }, totalIncome) }
    )
}

@Composable
fun GenericDistributionCard(
    title: String,
    monthly: Map<String, Int>,
    total: Map<String, Int>,
    totalSum: Int = 0,
    totalSumMensual: Int = 0,
    isIncome: Boolean = true,
    onClick: (Boolean) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    DistributionPagerCard(
        title = title,
        pagerState = pagerState,
        onClick = { onClick(pagerState.currentPage == 0) },
        monthlyContent = { if (isIncome) EarningsPieContent(monthly, totalSumMensual) else ExpensesPieContent(monthly, totalSumMensual) },
        totalContent = { if (isIncome) EarningsPieContent(total, totalSum) else ExpensesPieContent(total, totalSum) }
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
        title = "EGRESOS",
        pagerState = pagerState,
        onClick = { onClick(pagerState.currentPage == 0) },
        monthlyContent = { ExpensesPieContent(monthly, totalExpensesMensual) },
        totalContent = { ExpensesPieContent(total, totalExpenses) }
    )
}

private val STATS_COLORS = listOf(EarnGreen, StormCyan, PurpleAccent, YellowAccent, StormIndigo)

@Composable
fun EarningsPieContent(data: Map<String, Int>, totalIncome: Int = 0) {
    val incomeSum = data.values.sum().toFloat()
    if (data.isEmpty() || incomeSum == 0f) {
        Text("SIN INGRESOS REGISTRADOS", fontSize = 11.sp, color = StormTextMuted, modifier = Modifier.padding(vertical = 16.dp))
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Canvas(modifier = Modifier.size(72.dp)) {
            var startAngle = 0f
            data.entries.forEachIndexed { index, entry ->
                val sweepAngle = (entry.value / incomeSum) * 360f
                drawArc(color = STATS_COLORS.getOrElse(index) { Color.Gray }, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true)
                startAngle += sweepAngle
            }
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            data.entries.forEachIndexed { index, entry ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(STATS_COLORS.getOrElse(index) { Color.Gray }, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${entry.key}: ${entry.value}", fontSize = 11.sp, color = StormTextMain)
                }
            }
            if (totalIncome > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Total: +$totalIncome V", fontSize = 11.sp, color = EarnGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ExpensesPieContent(data: Map<String, Int>, totalExpenses: Int = 0) {
    val expensesSum = data.values.sum().toFloat()
    if (data.isEmpty() || expensesSum == 0f) {
        Text("SIN GASTOS REGISTRADOS", fontSize = 11.sp, color = StormTextMuted, modifier = Modifier.padding(vertical = 16.dp))
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Canvas(modifier = Modifier.size(72.dp)) {
            var startAngle = 0f
            data.entries.forEachIndexed { index, entry ->
                val sweepAngle = (entry.value.toFloat() / expensesSum) * 360f
                drawArc(color = STATS_COLORS.getOrElse(index + 2) { Color.Gray }, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true)
                startAngle += sweepAngle
            }
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            data.entries.forEachIndexed { index, entry ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(STATS_COLORS.getOrElse(index + 2) { Color.Gray }, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${entry.key}: ${entry.value}", fontSize = 11.sp, color = StormTextMain)
                }
            }
            if (totalExpenses > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Total: -$totalExpenses V", fontSize = 11.sp, color = SpendRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RelationItemCard(relation: DependentRelation, onClick: () -> Unit, onEditName: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StormCardSurface),
        border = BorderStroke(1.dp, StormBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Column {
                    Text(relation.account.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = StormTextMain)
                    Text(
                        text = "RECIBIDOS ESTE MES: ${relation.monthlyReceived} V",
                        fontSize = 11.sp,
                        color = StormTextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onEditName, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = StormCyan, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = SpendRed.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("BALANCE", fontSize = 9.sp, color = StormTextMuted)
                Text(
                    text = "${if (relation.totalBalance > 0) "+" else ""}${relation.totalBalance}",
                    color = if (relation.totalBalance >= 0) EarnGreen else SpendRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun DeletedDependentItemCard(
    account: Account,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = StormCardSurface.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, StormBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(account.name, style = MaterialTheme.typography.bodySmall, color = StormTextMuted, fontWeight = FontWeight.Bold)
                Text("OCULTA", fontSize = 9.sp, color = StormTextMuted)
            }
            IconButton(onClick = onRestore, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Restore, contentDescription = "Restaurar", tint = StormCyan, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun DailyAmountDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Misión Diaria", fontWeight = FontWeight.Bold) },
        text = { Text("Selecciona la recompensa de hoy:") },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onConfirm(100) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = EarnGreen)) {
                    Text("100 V", fontWeight = FontWeight.Bold, color = StormBackground)
                }
                Button(onClick = { onConfirm(150) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = EarnGreen)) {
                    Text("150 V", fontWeight = FontWeight.Bold, color = StormBackground)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun DistributionHistoryDialog(title: String, transactions: List<Transaction>, onDismiss: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }
    val isIncome = title.contains("Ingresos", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("FECHA", Modifier.weight(if (isIncome) 1.5f else 1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = StormTextMuted)
                    if (!isIncome) {
                        Text("CUENTA", Modifier.weight(1.8f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = StormTextMuted)
                    }
                    Text("DETALLE", Modifier.weight(if (isIncome) 4.3f else 2.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = StormTextMuted)
                    Text("MONTO", Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = StormTextMuted, textAlign = TextAlign.End)
                }
                HorizontalDivider(color = StormBorder)
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(transactions.sortedByDescending { it.date }) { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sdf.format(Date(tx.date)), Modifier.weight(if (isIncome) 1.5f else 1.2f), fontSize = 11.sp, color = StormTextMuted)
                            if (!isIncome) {
                                val account = tx.recipientAccountName ?: "-"
                                Text(account, Modifier.weight(1.8f), fontSize = 11.sp, maxLines = 1, color = StormTextMain)
                            }
                            val detail = tx.itemName ?: tx.description
                            Text(detail, Modifier.weight(if (isIncome) 4.3f else 2.5f), fontSize = 11.sp, maxLines = 2, color = StormTextMain)
                            val color = if (tx.type == TransactionType.EARN) EarnGreen else SpendRed
                            Text("${if (tx.type == TransactionType.EARN) "+" else "-"}${tx.amount}", Modifier.weight(1.2f), fontSize = 12.sp, textAlign = TextAlign.End, color = color, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = StormBorder.copy(alpha = 0.5f))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = StormCyan)) { Text("Cerrar", color = StormBackground, fontWeight = FontWeight.Bold) } }
    )
}

@Composable
fun AddDependentDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Dependiente", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (name.isNotBlank()) onConfirm(name.trim())
                }),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }, colors = ButtonDefaults.buttonColors(containerColor = StormCyan)) { Text("Crear", color = StormBackground, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun ShareReadOnlyDialog(
    shareCode: String?,
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onGenerate: () -> Unit,
    onCopy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = if (isGenerating) ({}) else onDismiss,
        title = { Text(if (shareCode == null) "Compartir en modo lectura" else "¡Código generado!", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isGenerating) {
                    CircularProgressIndicator(color = StormCyan)
                    Spacer(Modifier.height(16.dp))
                    Text("Generando código...", color = StormTextMuted)
                } else if (shareCode == null) {
                    Text(
                        "Al generar un código, se subirá un snapshot de esta cuenta a la nube. Cualquiera con el código podrá ver el balance y transacciones, pero NO podrá modificar nada.\n\nEl código caduca en 7 días.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text("Comparte este código con la otra persona:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = StormCardElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StormBorder)
                    ) {
                        Text(
                            text = shareCode,
                            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Black,
                            color = StormCyan,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Válido 7 días · Solo visualización", style = MaterialTheme.typography.labelSmall, color = StormTextMuted)
                }
            }
        },
        confirmButton = {
            if (!isGenerating) {
                if (shareCode == null) {
                    Button(onClick = onGenerate, colors = ButtonDefaults.buttonColors(containerColor = StormCyan)) {
                        Text("GENERAR CÓDIGO", color = StormBackground, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(onClick = {
                        onCopy()
                        onDismiss()
                    }, colors = ButtonDefaults.buttonColors(containerColor = StormCyan)) {
                        Text("COPIAR Y CERRAR", color = StormBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            if (!isGenerating) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

@Composable
fun RenameAccountDialog(initialName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    val focusManager = LocalFocusManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renombrar Cuenta", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (name.isNotBlank()) onConfirm(name.trim())
                }),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }, colors = ButtonDefaults.buttonColors(containerColor = StormCyan)) { Text("Aceptar", color = StormBackground, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
