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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.presentation.common.ManualEntryDialog
import java.text.SimpleDateFormat
import java.util.*

private val EARNINGS_COLORS = listOf(
    Color(0xFF4CAF50), // DAILY
    Color(0xFF2196F3), // ALERT
    Color(0xFFFFC107), // EXTERNAL / PACK
    Color(0xFF9C27B0), // SSD
    Color(0xFFE91E63)  // OTHERS
)

private val EXPENSES_COLORS = listOf(
    Color(0xFFE91E63),
    Color(0xFFFF5722),
    Color(0xFF795548),
    Color(0xFF607D8B),
    Color(0xFF3F51B5),
    Color(0xFF009688)
)

@Composable
fun DistributionPagerCard(
    baseTitle: String,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onClick: () -> Unit,
    monthlyContent: @Composable () -> Unit,
    totalContent: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = baseTitle + if (pagerState.currentPage == 0) " · Mensual" else " · Total",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                if (page == 0) monthlyContent() else totalContent()
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(pagerState.pageCount) { i ->
                    val selected = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .padding(3.dp)
                            .size(if (selected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primary else Color.LightGray)
                    )
                }
            }
        }
    }
}

@Composable
private fun EarningsPieContent(data: Map<VBucksSource, Int>) {
    val total = data.values.sum().toFloat()
    if (data.isEmpty() || total == 0f) {
        Text("Sin ingresos en este periodo.", fontSize = 12.sp, color = Color.Gray)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(100.dp)) {
            var startAngle = 0f
            data.entries.forEachIndexed { index, entry ->
                val sweepAngle = (entry.value / total) * 360f
                drawArc(
                    color = EARNINGS_COLORS.getOrElse(index) { Color.Gray },
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )
                startAngle += sweepAngle
            }
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            data.entries.forEachIndexed { index, entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(EARNINGS_COLORS.getOrElse(index) { Color.Gray }))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${entry.key.name}: ${entry.value}", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ExpensesPieContent(data: Map<String, Int>) {
    val total = data.values.sum().toFloat()
    if (data.isEmpty() || total == 0f) {
        Text("Sin egresos en este periodo.", fontSize = 12.sp, color = Color.Gray)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(100.dp)) {
            var startAngle = 0f
            data.entries.forEachIndexed { index, entry ->
                val sweepAngle = (entry.value.toFloat() / total) * 360f
                drawArc(
                    color = EXPENSES_COLORS.getOrElse(index) { Color.Gray },
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )
                startAngle += sweepAngle
            }
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            data.entries.forEachIndexed { index, entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(EXPENSES_COLORS.getOrElse(index) { Color.Gray }))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${entry.key}: ${entry.value}", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun EarningsDistributionCard(
    monthly: Map<VBucksSource, Int>,
    total: Map<VBucksSource, Int>,
    onClick: (Boolean) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    DistributionPagerCard(
        baseTitle = "Distribución de Ingresos",
        pagerState = pagerState,
        onClick = { onClick(pagerState.currentPage == 0) },
        monthlyContent = { EarningsPieContent(monthly) },
        totalContent = { EarningsPieContent(total) }
    )
}

@Composable
fun ExpensesDistributionCard(
    monthly: Map<String, Int>,
    total: Map<String, Int>,
    onClick: (Boolean) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    DistributionPagerCard(
        baseTitle = "Distribución de Egresos",
        pagerState = pagerState,
        onClick = { onClick(pagerState.currentPage == 0) },
        monthlyContent = { ExpensesPieContent(monthly) },
        totalContent = { ExpensesPieContent(total) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: AccountDetailViewModel = hiltViewModel(),
    onPopBackStack: () -> Unit,
    onNavigateToHistory: (Long) -> Unit,
    onNavigateToAddExpense: (Long) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val account by viewModel.account.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val isDailyRegistered by viewModel.isDailyRegistered.collectAsState()
    val dependentRelations by viewModel.dependentRelations.collectAsState()
    val earningsDesglosadas by viewModel.earningsDesglosadas.collectAsState()
    val earningsDesglosadasMensual by viewModel.earningsDesglosadasMensual.collectAsState()
    val earningsTransactions by viewModel.earningsTransactions.collectAsState()
    val earningsTransactionsMensual by viewModel.earningsTransactionsMensual.collectAsState()
    val expenseDistribution by viewModel.expenseDistribution.collectAsState()
    val expenseDistributionMensual by viewModel.expenseDistributionMensual.collectAsState()
    val expenseTransactions by viewModel.expenseTransactions.collectAsState()
    val expenseTransactionsMensual by viewModel.expenseTransactionsMensual.collectAsState()
    val dependentAccounts by viewModel.dependentAccounts.collectAsState()

    var showExternalDialog by remember { mutableStateOf(false) }
    var showAddDependentDialog by remember { mutableStateOf(false) }
    var showManualEntryDialog by remember { mutableStateOf(false) }
    var showDailyAmountDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var accountToRename by remember { mutableStateOf<Account?>(null) }
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

    if (showExternalDialog) {
        AddExternalDialog(
            onDismiss = { showExternalDialog = false },
            onConfirm = { amount, desc ->
                viewModel.onAddExternalClick(amount, desc)
                showExternalDialog = false
            }
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
                        text = account?.name ?: "Detalle",
                        modifier = Modifier.clickable { showRenameDialog = true }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BalanceCard(balance)
            }

            item {
                Text("Acciones Rápidas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (account?.parentAccountId == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showDailyAmountDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled = !isDailyRegistered
                        ) {
                            Text("Diaria (Seleccionar)")
                        }
                        Button(
                            onClick = { viewModel.onAddAlertClick() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Alerta +50")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                manualEntryInitialType = TransactionType.EARN
                                manualEntryInitialSource = VBucksSource.EXTERNAL
                                showManualEntryDialog = true 
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Externo (+)")
                        }
                        OutlinedButton(
                            onClick = { 
                                manualEntryInitialType = TransactionType.SPEND
                                manualEntryInitialSource = VBucksSource.GIFT
                                showManualEntryDialog = true 
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Gasto / Regalo")
                        }
                    }
                    Button(
                        onClick = { 
                            manualEntryInitialType = null
                            manualEntryInitialSource = null
                            showManualEntryDialog = true 
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Registro Manual")
                    }
                } else {
                    OutlinedButton(
                        onClick = { 
                            manualEntryInitialType = TransactionType.SPEND
                            manualEntryInitialSource = VBucksSource.GIFT
                            showManualEntryDialog = true 
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Registrar Gasto de esta cuenta")
                    }
                    Button(
                        onClick = { 
                            manualEntryInitialType = null
                            manualEntryInitialSource = null
                            showManualEntryDialog = true 
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Registro Manual")
                    }
                }
            }

            if (earningsDesglosadas.isNotEmpty() || earningsDesglosadasMensual.isNotEmpty()) {
                item {
                    EarningsDistributionCard(
                        monthly = earningsDesglosadasMensual,
                        total = earningsDesglosadas,
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
                        onClick = { isMonthly ->
                            val txs = if (isMonthly) expenseTransactionsMensual else expenseTransactions
                            distributionToShow = "Egresos (${if (isMonthly) "Mes" else "Total"})" to txs
                        }
                    )
                }
            }

            item {
                Button(
                    onClick = { onNavigateToHistory(account?.id ?: 0) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Ver Historial Detallado")
                }
            }

            if (account?.parentAccountId == null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cuentas Dependientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showAddDependentDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir Dependiente")
                        }
                    }
                }

                items(dependentRelations) { relation ->
                    RelationItem(
                        relation = relation,
                        onClick = { onNavigateToHistory(relation.account.id) },
                        onEditName = { accountToRename = relation.account }
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceCard(balance: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Saldo Actual", style = MaterialTheme.typography.labelLarge)
            Text("$balance V-Bucks", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun formatSigned(amount: Int): String = if (amount > 0) "+$amount" else "$amount"
private fun colorFor(amount: Int): Color = when {
    amount > 0 -> Color(0xFF4CAF50)
    amount < 0 -> Color.Red
    else -> Color.Unspecified
}

@Composable
fun RelationItem(relation: DependentRelation, onClick: () -> Unit, onEditName: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Column {
                    Text(relation.account.name, fontWeight = FontWeight.Medium)
                    Text(
                        text = "Este mes: ${formatSigned(relation.monthlyBalance)}",
                        fontSize = 11.sp,
                        color = colorFor(relation.monthlyBalance)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onEditName, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Renombrar", modifier = Modifier.size(16.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Total", fontSize = 10.sp, color = Color.Gray)
                Text(
                    text = formatSigned(relation.totalBalance),
                    color = colorFor(relation.totalBalance),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
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
fun AddDependentDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Cuenta Dependiente") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la cuenta") },
                modifier = Modifier.fillMaxWidth()
            )
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

@Composable
fun DistributionHistoryDialog(
    title: String,
    transactions: List<Transaction>,
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Fecha", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Detalle", Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Monto", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                }
                HorizontalDivider()
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(transactions.sortedByDescending { it.date }) { tx ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sdf.format(Date(tx.date)), Modifier.weight(1f), fontSize = 11.sp)
                            val detailText = when {
                                tx.itemName?.isNotBlank() == true -> tx.itemName
                                tx.recipientAccountName?.isNotBlank() == true -> "A: ${tx.recipientAccountName}"
                                else -> tx.description.takeIf { it.isNotBlank() } ?: tx.source.name
                            }
                            Text(detailText, Modifier.weight(2f), fontSize = 11.sp)
                            val signed = if (tx.type == TransactionType.EARN) "+${tx.amount}" else "-${tx.amount}"
                            val color = if (tx.type == TransactionType.EARN) Color(0xFF4CAF50) else Color.Red
                            Text(
                                signed,
                                Modifier.weight(1f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.End,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                    }
                    if (transactions.isEmpty()) {
                        item {
                            Text("Sin transacciones.", Modifier.padding(16.dp), fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL", fontWeight = FontWeight.ExtraBold)
                    val total = transactions.sumOf { if (it.type == TransactionType.EARN) it.amount else -it.amount }
                    Text(
                        if (total >= 0) "+$total" else "$total",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (total >= 0) Color(0xFF4CAF50) else Color.Red
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun DailyAmountDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Misión Diaria") },
        text = { Text("Selecciona la recompensa de la misión:") },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onConfirm(100) }, modifier = Modifier.weight(1f)) {
                    Text("+100")
                }
                Button(onClick = { onConfirm(150) }, modifier = Modifier.weight(1f)) {
                    Text("+150")
                }
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
fun AddExternalDialog(onDismiss: () -> Unit, onConfirm: (Int, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Pavos Externos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) {
                            amount = it
                        }
                    },
                    label = { Text("Cantidad") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Fuente (Pase, Pack, etc.)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(amount.toIntOrNull() ?: 0, description) }) {
                Text("Añadir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
