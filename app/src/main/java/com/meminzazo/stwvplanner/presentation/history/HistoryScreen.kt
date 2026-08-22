package com.meminzazo.stwvplanner.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.presentation.common.ManualEntryDialog
import java.text.SimpleDateFormat
import java.util.*
import com.meminzazo.stwvplanner.presentation.theme.*

private val COL_DATE = 80.dp
private val COL_NUM = 70.dp

/**
 * Pantalla de Historial Detallado.
 * Muestra una tabla con todos los días del mes (1-31) y las transacciones asociadas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onPopBackStack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showManualEntryDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    // Diálogo para agregar o editar registros manualmente
    if (showManualEntryDialog || transactionToEdit != null) {
        ManualEntryDialog(
            dependents = state.dependents,
            transactionToEdit = transactionToEdit,
            onDismiss = { 
                showManualEntryDialog = false
                transactionToEdit = null
            },
            onConfirm = { amount, type, source, desc, date, receiverId, receiverName ->
                if (transactionToEdit != null) {
                    viewModel.onUpdateTransaction(transactionToEdit!!.copy(
                        amount = amount,
                        type = type,
                        source = source,
                        description = desc,
                        date = date,
                        receiverAccountId = receiverId,
                        recipientAccountName = receiverName
                    ))
                } else {
                    viewModel.onAddTransaction(amount, type, source, desc, date, receiverId, receiverName)
                }
                showManualEntryDialog = false
                transactionToEdit = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.accountName.uppercase(), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showManualEntryDialog = true },
                containerColor = FortAccent,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // Selector de Mes/Año
            MonthSelector(
                selectedMonth = state.selectedMonthName,
                onPreviousMonth = { viewModel.onMonthChange(-1) },
                onNextMonth = { viewModel.onMonthChange(1) }
            )

            // Tabla principal según el tipo de cuenta
            if (state.isDependent) {
                DependentLedger(
                    state = state, 
                    modifier = Modifier.weight(1f),
                    onEditTransaction = { transactionToEdit = it },
                    onDeleteTransaction = { viewModel.onDeleteTransaction(it) }
                )
            } else {
                MainAccountTable(
                    state = state, 
                    modifier = Modifier.weight(1f),
                    onEditTransaction = { transactionToEdit = it },
                    onDeleteTransaction = { viewModel.onDeleteTransaction(it) }
                )
            }
        }
    }
}

@Composable
fun MonthSelector(
    selectedMonth: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Surface(
        color = StwCardSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Anterior", tint = FortAccent)
            }
            Text(
                text = selectedMonth.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = VBucksGold
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Siguiente", tint = FortAccent)
            }
        }
    }
}

/**
 * Tabla para cuentas principales: vista tipo Excel con columnas fijas.
 */
@Composable
fun MainAccountTable(
    state: HistoryState, 
    modifier: Modifier = Modifier,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    val scrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val sdf = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }
    val dependentIds = remember(state.dependents) { state.dependents.map { it.id }.toSet() }
    
    // Agrupa transacciones por día, rellenando los días vacíos del mes
    val dayRows = remember(state.transactions, dependentIds, state.selectedMonth, state.selectedYear) {
        groupTransactionsByDay(state.transactions, dependentIds, state.selectedMonth, state.selectedYear)
    }
    
    var selectedDay by remember { mutableStateOf<DayRow?>(null) }

    selectedDay?.let { day ->
        DayDetailDialog(
            day = day, 
            dependents = state.dependents, 
            onDismiss = { selectedDay = null },
            onEditTransaction = onEditTransaction,
            onDeleteTransaction = onDeleteTransaction
        )
    }

    Column(modifier = modifier.verticalScroll(verticalScrollState)) {
        Column(modifier = Modifier.horizontalScroll(scrollState)) {
            // Encabezado de Tabla
            Row(
                modifier = Modifier.background(FortDarkBlue).padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderCell("FECHA", COL_DATE)
                HeaderCell("DIARIA", COL_NUM)
                HeaderCell("ALERTA", COL_NUM)
                HeaderCell("EXTERNO", COL_NUM)
                state.dependents.forEach { dep -> HeaderCell(dep.name.uppercase().take(8), COL_NUM) }
                HeaderCell("OTROS", COL_NUM)
            }
            HorizontalDivider(color = FortAccent.copy(alpha = 0.5f))

            // Filas de Datos (Días)
            dayRows.forEach { row ->
                Row(
                    modifier = Modifier
                        .clickable { if (row.transactions.isNotEmpty()) selectedDay = row }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Cell(sdf.format(Date(row.dateMillis)), COL_DATE, color = VBucksSilver)
                    Cell(if (row.daily > 0) "${row.daily}" else "-", COL_NUM, color = if (row.daily > 0) EarnGreen else Color.Gray)
                    Cell(if (row.alert > 0) "${row.alert}" else "-", COL_NUM, color = if (row.alert > 0) AlertBlue else Color.Gray)
                    Cell(if (row.external > 0) "${row.external}" else "-", COL_NUM, color = if (row.external > 0) FortPurple else Color.Gray)
                    state.dependents.forEach { dep ->
                        val amt = row.dependentAmounts[dep.id] ?: 0
                        Cell(if (amt > 0) "-$amt" else "-", COL_NUM, color = if (amt > 0) SpendRed else Color.Gray)
                    }
                    val othersText = when {
                        row.others > 0 -> "+${row.others}"
                        row.others < 0 -> "${row.others}"
                        else -> "-"
                    }
                    Cell(othersText, COL_NUM, color = if (row.others < 0) SpendRed else if (row.others > 0) EarnGreen else Color.Gray)
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.2f))
            }

            // Fila de TOTALES
            Row(
                modifier = Modifier.background(StwCardSurface).padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Cell("TOTAL", COL_DATE, bold = true, color = VBucksGold)
                Cell("${state.totalDaily}", COL_NUM, bold = true, color = EarnGreen)
                Cell("${state.totalAlert}", COL_NUM, bold = true, color = AlertBlue)
                Cell("${state.totalExternal}", COL_NUM, bold = true, color = FortPurple)
                state.dependents.forEach { dep ->
                    val total = state.totalsByDependent[dep.id] ?: 0
                    Cell(if (total > 0) "-$total" else "-", COL_NUM, bold = true, color = SpendRed)
                }
                Cell("${state.totalOthers}", COL_NUM, bold = true, color = if (state.totalOthers < 0) SpendRed else EarnGreen)
            }
        }
    }
}

// ... Funciones auxiliares de agrupamiento ...

private data class DayRow(
    val dateMillis: Long,
    val daily: Int,
    val alert: Int,
    val external: Int,
    val dependentAmounts: Map<Long, Int>,
    val others: Int,
    val transactions: List<Transaction>
)

private fun groupTransactionsByDay(
    transactions: List<Transaction>, 
    dependentIds: Set<Long>,
    month: Int,
    year: Int
): List<DayRow> {
    val dayKeyFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    fun isDependentGift(tx: Transaction) =
        tx.source == VBucksSource.GIFT && tx.type == TransactionType.SPEND && tx.receiverAccountId in dependentIds
    fun isCore(tx: Transaction) =
        tx.source == VBucksSource.DAILY || tx.source == VBucksSource.ALERT || tx.source == VBucksSource.EXTERNAL

    val grouped = transactions.groupBy { dayKeyFmt.format(Date(it.date)) }
    
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val result = mutableListOf<DayRow>()
    
    for (day in 1..daysInMonth) {
        calendar.set(Calendar.DAY_OF_MONTH, day)
        val key = dayKeyFmt.format(calendar.time)
        val txsOfDay = grouped[key] ?: emptyList()
        
        result.add(
            DayRow(
                dateMillis = calendar.timeInMillis,
                daily = txsOfDay.filter { it.source == VBucksSource.DAILY }.sumOf { it.amount },
                alert = txsOfDay.filter { it.source == VBucksSource.ALERT }.sumOf { it.amount },
                external = txsOfDay.filter { it.source == VBucksSource.EXTERNAL }.sumOf { it.amount },
                dependentAmounts = dependentIds.associateWith { depId ->
                    txsOfDay.filter { it.receiverAccountId == depId && isDependentGift(it) }.sumOf { it.amount }
                },
                others = txsOfDay.filter { !isCore(it) && !isDependentGift(it) }
                    .sumOf { if (it.type == TransactionType.SPEND) -it.amount else it.amount },
                transactions = txsOfDay.sortedBy { it.date }
            )
        )
    }

    return result.sortedByDescending { it.dateMillis }
}

@Composable
private fun DayDetailDialog(
    day: DayRow, 
    dependents: List<Account>, 
    onDismiss: () -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    val headerFmt = remember { SimpleDateFormat("EEEE d 'DE' MMMM", Locale("es", "ES")) }
    val dependentNames = remember(dependents) { dependents.associate { it.id to it.name } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(headerFmt.format(Date(day.dateMillis)).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                day.transactions.forEach { tx ->
                    val label = when {
                        tx.source == VBucksSource.DAILY -> "Misión Diaria"
                        tx.source == VBucksSource.ALERT -> "Alerta de Misión"
                        tx.source == VBucksSource.EXTERNAL -> tx.description.ifBlank { "Externo" }
                        tx.source == VBucksSource.GIFT && tx.receiverAccountId != null ->
                            "Regalo a ${dependentNames[tx.receiverAccountId] ?: (tx.recipientAccountName ?: "cuenta")}"
                        tx.source == VBucksSource.GIFT && tx.senderAccountId != null ->
                            "Recibido de ${dependentNames[tx.senderAccountId] ?: "cuenta"}"
                        else -> tx.itemName?.takeIf { it.isNotBlank() } ?: tx.description.ifBlank { tx.source.name }
                    }
                    val signed = if (tx.type == TransactionType.SPEND) -tx.amount else tx.amount
                    
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label.uppercase(), fontSize = 12.sp, color = VBucksSilver)
                                Text(
                                    if (signed >= 0) "+$signed" else "$signed",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (signed >= 0) EarnGreen else SpendRed
                                )
                            }
                            IconButton(onClick = { onEditTransaction(tx); onDismiss() }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = FortAccent, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { onDeleteTransaction(tx); onDismiss() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = SpendRed, modifier = Modifier.size(20.dp))
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.2f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CERRAR") }
        }
    )
}

/**
 * Vista simplificada para cuentas dependientes.
 */
@Composable
fun DependentLedger(
    state: HistoryState, 
    modifier: Modifier = Modifier,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }
    val dayRows = remember(state.transactions) { groupDependentTransactionsByDay(state.transactions) }
    var selectedDay by remember { mutableStateOf<DependentDayRow?>(null) }

    selectedDay?.let { day ->
        DependentDayDetailDialog(
            day = day, 
            onDismiss = { selectedDay = null },
            onEditTransaction = onEditTransaction,
            onDeleteTransaction = onDeleteTransaction
        )
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().background(FortDarkBlue).padding(12.dp)
        ) {
            Text("FECHA", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            Text("DEL DÍA", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, color = Color.White)
            Text("RECIBIDO", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, color = Color.White)
        }
        HorizontalDivider(color = FortAccent)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(dayRows) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { selectedDay = row }.padding(12.dp)
                ) {
                    Text(sdf.format(Date(row.dateMillis)), Modifier.weight(1f), fontSize = 12.sp, color = VBucksSilver)
                    Text(
                        if (row.netAmount >= 0) "+${row.netAmount}" else "${row.netAmount}",
                        Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.End,
                        color = if (row.netAmount >= 0) EarnGreen else SpendRed
                    )
                    Text(
                        "${row.runningBalance}",
                        Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.End, fontWeight = FontWeight.Black,
                        color = VBucksGold
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.2f))
            }
        }
    }
}

// ... Resto de componentes internos con estilos actualizados ...

private data class DependentDayRow(val dateMillis: Long, val netAmount: Int, val runningBalance: Int, val transactions: List<Transaction>)

private fun groupDependentTransactionsByDay(transactions: List<Transaction>): List<DependentDayRow> {
    val dayKeyFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val chronoGroups = transactions.sortedBy { it.date }.groupBy { dayKeyFmt.format(Date(it.date)) }.entries.sortedBy { it.value.first().date }
    var running = 0
    val rows = chronoGroups.map { (_, txsOfDay) ->
        val net = txsOfDay.sumOf { if (it.type == TransactionType.SPEND) -it.amount else it.amount }
        running += net
        DependentDayRow(txsOfDay.first().date, net, running, txsOfDay)
    }
    return rows.asReversed()
}

@Composable
private fun DependentDayDetailDialog(day: DependentDayRow, onDismiss: () -> Unit, onEditTransaction: (Transaction) -> Unit, onDeleteTransaction: (Transaction) -> Unit) {
    val headerFmt = remember { SimpleDateFormat("EEEE d 'DE' MMMM", Locale("es", "ES")) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(headerFmt.format(Date(day.dateMillis)).uppercase(), fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                day.transactions.forEach { tx ->
                    val signed = if (tx.type == TransactionType.SPEND) -tx.amount else tx.amount
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.description.uppercase(), fontSize = 12.sp, color = VBucksSilver)
                                Text(if (signed >= 0) "+$signed" else "$signed", fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (signed >= 0) EarnGreen else SpendRed)
                            }
                            IconButton(onClick = { onEditTransaction(tx); onDismiss() }) { Icon(Icons.Default.Edit, contentDescription = null, tint = FortAccent) }
                            IconButton(onClick = { onDeleteTransaction(tx); onDismiss() }) { Icon(Icons.Default.Delete, contentDescription = null, tint = SpendRed) }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.2f))
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("CERRAR") } })
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Text(text, Modifier.width(width), fontWeight = FontWeight.Black, fontSize = 10.sp, textAlign = TextAlign.Center, color = Color.White)
}

@Composable
private fun Cell(text: String, width: Dp, bold: Boolean = false, color: Color = Color.Unspecified) {
    Text(text, Modifier.width(width), fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = if (bold) FontWeight.Black else FontWeight.Normal, color = color)
}
