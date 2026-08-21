package com.meminzazo.stwvplanner.presentation.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun EarningsPieChart(data: Map<VBucksSource, Int>) {
    val total = data.values.sum().toFloat()
    val colors = listOf(
        Color(0xFF4CAF50), // DAILY
        Color(0xFF2196F3), // ALERT
        Color(0xFFFFC107), // EXTERNAL / PACK
        Color(0xFF9C27B0), // SSD
        Color(0xFFE91E63)  // OTHERS
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Distribución de Ingresos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    var startAngle = 0f
                    data.entries.forEachIndexed { index, entry ->
                        val sweepAngle = (entry.value / total) * 360f
                        drawArc(
                            color = colors.getOrElse(index) { Color.Gray },
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
                            Box(modifier = Modifier.size(12.dp).background(colors.getOrElse(index) { Color.Gray }))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${entry.key.name}: ${entry.value}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesPieChart(data: Map<String, Int>) {
    val total = data.values.sum().toFloat()
    if (total == 0f) return

    val colors = listOf(
        Color(0xFFE91E63), // Pink
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF795548), // Brown
        Color(0xFF607D8B), // Blue Grey
        Color(0xFF3F51B5), // Indigo
        Color(0xFF009688)  // Teal
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Distribución de Egresos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    var startAngle = 0f
                    data.entries.forEachIndexed { index, entry ->
                        val sweepAngle = (entry.value.toFloat() / total) * 360f
                        drawArc(
                            color = colors.getOrElse(index) { Color.Gray },
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
                            Box(modifier = Modifier.size(12.dp).background(colors.getOrElse(index) { Color.Gray }))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${entry.key}: ${entry.value}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
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
    val dependentAccounts by viewModel.dependentAccounts.collectAsState()
    val earningsDesglosadas by viewModel.earningsDesglosadas.collectAsState()
    val expenseDistribution by viewModel.expenseDistribution.collectAsState()

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

            if (earningsDesglosadas.isNotEmpty()) {
                item {
                    EarningsPieChart(earningsDesglosadas)
                }
            }

            if (expenseDistribution.isNotEmpty()) {
                item {
                    ExpensesPieChart(expenseDistribution)
                }
            }

            item {
                Text("Acciones Rápidas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (account?.parentAccountId == null) {
                    // Cuentas Principales: Acciones de Ganancia
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
                    // Cuentas Dependientes: Solo mostrar regalos o gastos específicos
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

                items(dependentAccounts) { other ->
                    RelationItem(
                        other = other,
                        onClick = { onNavigateToHistory(other.id) }
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

@Composable
fun RelationItem(other: Account, onClick: () -> Unit) {
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
            Text(other.name, fontWeight = FontWeight.Medium)
            Text(
                text = if (other.balance >= 0) "+${other.balance}" else "${other.balance}",
                color = if (other.balance >= 0) Color(0xFF4CAF50) else Color.Red,
                fontWeight = FontWeight.Bold
            )
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
