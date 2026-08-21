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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.domain.model.TransactionType

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

/**
 * Card genérica con 2 páginas deslizables: "Mensual" y "Total".
 * El título cambia según la página visible; puntos indicadores abajo.
 */
@Composable
fun DistributionPagerCard(
    baseTitle: String,
    monthlyContent: @Composable () -> Unit,
    totalContent: @Composable () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                repeat(2) { i ->
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
fun EarningsDistributionCard(monthly: Map<VBucksSource, Int>, total: Map<VBucksSource, Int>) {
    DistributionPagerCard(
        baseTitle = "Distribución de Ingresos",
        monthlyContent = { EarningsPieContent(monthly) },
        totalContent = { EarningsPieContent(total) }
    )
}

@Composable
fun ExpensesDistributionCard(monthly: Map<String, Int>, total: Map<String, Int>) {
    DistributionPagerCard(
        baseTitle = "Distribución de Egresos",
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
    val expenseDistribution by viewModel.expenseDistribution.collectAsState()
    val expenseDistributionMensual by viewModel.expenseDistributionMensual.collectAsState()

    var showExternalDialog by remember { mutableStateOf(false) }
    var showAddDependentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is AccountDetailViewModel.UiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.name ?: "Detalle") },
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

            if (earningsDesglosadas.isNotEmpty() || earningsDesglosadasMensual.isNotEmpty()) {
                item {
                    EarningsDistributionCard(earningsDesglosadasMensual, earningsDesglosadas)
                }
            }

            if (expenseDistribution.isNotEmpty() || expenseDistributionMensual.isNotEmpty()) {
                item {
                    ExpensesDistributionCard(expenseDistributionMensual, expenseDistribution)
                }
            }

            item {
                Text("Acciones Rápidas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (account?.parentAccountId == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.onAddDailyClick(100) },
                            modifier = Modifier.weight(1f),
                            enabled = !isDailyRegistered
                        ) {
                            Text("Diaria +100")
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
                            onClick = { showExternalDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Externo (+)")
                        }
                        OutlinedButton(
                            onClick = { onNavigateToAddExpense(account?.id ?: 0) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Gasto / Regalo")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { onNavigateToAddExpense(account?.id ?: 0) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Registrar Gasto de esta cuenta")
                    }
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
                        onClick = { onNavigateToHistory(relation.account.id) }
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

private fun formatSigned(amount: Int): String = if (amount >= 0) "+$amount" else "$amount"
private fun colorFor(amount: Int): Color = if (amount >= 0) Color(0xFF4CAF50) else Color.Red

@Composable
fun RelationItem(relation: DependentRelation, onClick: () -> Unit) {
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
            Column {
                Text(relation.account.name, fontWeight = FontWeight.Medium)
                Text(
                    text = "Este mes: ${formatSigned(relation.monthlyBalance)}",
                    fontSize = 11.sp,
                    color = colorFor(relation.monthlyBalance)
                )
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