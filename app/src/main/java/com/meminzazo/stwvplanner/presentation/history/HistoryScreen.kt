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
import java.text.SimpleDateFormat
import java.util.*

private val COL_DATE = 68.dp
private val COL_NUM = 66.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onPopBackStack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.accountName.isNotBlank()) state.accountName else "Historial") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            MonthSelector(
                selectedMonth = state.selectedMonthName,
                onPreviousMonth = { viewModel.onMonthChange(-1) },
                onNextMonth = { viewModel.onMonthChange(1) }
            )

            if (state.isDependent) {
                DependentLedger(state, Modifier.weight(1f))
            } else {
                MainAccountTable(state, Modifier.weight(1f))
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
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes Anterior")
            }
            Text(
                text = selectedMonth,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes Siguiente")
            }
        }
    }
}

private data class DayRow(
    val dateMillis: Long,
    val daily: Int,
    val alert: Int,
    val external: Int,
    val dependentAmounts: Map<Long, Int>,
    val others: Int,
    val transactions: List<Transaction>
)

private fun groupTransactionsByDay(transactions: List<Transaction>, dependentIds: Set<Long>): List<DayRow> {
    val dayKeyFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    fun isDependentGift(tx: Transaction) =
        tx.source == VBucksSource.GIFT && tx.type == TransactionType.SPEND && tx.receiverAccountId in dependentIds
    fun isCore(tx: Transaction) =
        tx.source == VBucksSource.DAILY || tx.source == VBucksSource.ALERT || tx.source == VBucksSource.EXTERNAL

    return transactions
        .groupBy { dayKeyFmt.format(Date(it.date)) }
        .map { (_, txsOfDay) ->
            DayRow(
                dateMillis = txsOfDay.first().date,
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
        }
        .sortedByDescending { it.dateMillis }
}

/**
 * Vista para cuentas principales: una fila POR DÍA (sumando todo lo ocurrido ese día),
 * una columna por cada dependiente para ver a quién se le dio cada regalo.
 * La tabla completa se desliza horizontalmente si hay muchos dependientes.
 */
@Composable
fun MainAccountTable(state: HistoryState, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    val sdf = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }
    val dependentIds = remember(state.dependents) { state.dependents.map { it.id }.toSet() }
    val dayRows = remember(state.transactions, dependentIds) {
        groupTransactionsByDay(state.transactions, dependentIds)
    }
    var selectedDay by remember { mutableStateOf<DayRow?>(null) }

    selectedDay?.let { day ->
        DayDetailDialog(day = day, dependents = state.dependents, onDismiss = { selectedDay = null })
    }

    Column(modifier = modifier) {
        Column(modifier = Modifier.horizontalScroll(scrollState)) {
            Row(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderCell("Fecha", COL_DATE)
                HeaderCell("Diaria", COL_NUM)
                HeaderCell("Alerta", COL_NUM)
                HeaderCell("Externo", COL_NUM)
                state.dependents.forEach { dep -> HeaderCell(dep.name.take(10), COL_NUM) }
                HeaderCell("Otros", COL_NUM)
            }
            HorizontalDivider()

            dayRows.forEach { row ->
                Row(
                    modifier = Modifier
                        .clickable { selectedDay = row }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Cell(sdf.format(Date(row.dateMillis)), COL_DATE)
                    Cell(if (row.daily > 0) "${row.daily}" else "-", COL_NUM, color = if (row.daily > 0) Color(0xFF4CAF50) else Color.Gray)
                    Cell(if (row.alert > 0) "${row.alert}" else "-", COL_NUM, color = if (row.alert > 0) Color(0xFF2196F3) else Color.Gray)
                    Cell(if (row.external > 0) "${row.external}" else "-", COL_NUM, color = if (row.external > 0) Color(0xFFFF9800) else Color.Gray)
                    state.dependents.forEach { dep ->
                        val amt = row.dependentAmounts[dep.id] ?: 0
                        Cell(if (amt > 0) "-$amt" else "-", COL_NUM, color = if (amt > 0) Color.Red else Color.Gray)
                    }
                    val othersText = when {
                        row.others > 0 -> "+${row.others}"
                        row.others < 0 -> "${row.others}"
                        else -> "-"
                    }
                    Cell(othersText, COL_NUM, color = if (row.others < 0) Color.Red else Color.Unspecified)
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
            }

            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Cell("TOTAL", COL_DATE, bold = true)
                Cell("${state.totalDaily}", COL_NUM, bold = true)
                Cell("${state.totalAlert}", COL_NUM, bold = true)
                Cell("${state.totalExternal}", COL_NUM, bold = true)
                state.dependents.forEach { dep ->
                    val total = state.totalsByDependent[dep.id] ?: 0
                    Cell(if (total > 0) "-$total" else "-", COL_NUM, bold = true, color = Color.Red)
                }
                Cell("${state.totalOthers}", COL_NUM, bold = true, color = if (state.totalOthers < 0) Color.Red else Color.Unspecified)
            }
        }
    }
}

/**
 * Detalle emergente de un día: lista transacción por transacción,
 * tal como pediste, en vez de amontonarlas en la tabla principal.
 */
@Composable
private fun DayDetailDialog(day: DayRow, dependents: List<Account>, onDismiss: () -> Unit) {
    val headerFmt = remember { SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES")) }
    val dependentNames = remember(dependents) { dependents.associate { it.id to it.name } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(headerFmt.format(Date(day.dateMillis)).replaceFirstChar { it.uppercase() })
        },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
                        Text(
                            if (signed >= 0) "+$signed" else "$signed",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (signed >= 0) Color(0xFF4CAF50) else Color.Red
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                }
                if (day.transactions.isEmpty()) {
                    Text("Sin movimientos.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

/**
 * Vista para cuentas dependientes: bitácora simple de lo recibido (los
 * dependientes no regalan nada, solo reciben — el "balance" comparativo entre
 * hermanos vive en la lista de Cuentas Dependientes del padre, no aquí).
 * Agrupado por día, con detalle emergente al tocar.
 */
private fun dependentBalanceSigned(tx: Transaction): Int =
    if (tx.type == TransactionType.SPEND) -tx.amount else tx.amount

private fun dependentBalanceColor(amount: Int): Color = when {
    amount > 0 -> Color(0xFF4CAF50)
    amount < 0 -> Color.Red
    else -> Color.Unspecified
}

private data class DependentDayRow(
    val dateMillis: Long,
    val netAmount: Int,
    val runningBalance: Int,
    val transactions: List<Transaction>
)

private fun groupDependentTransactionsByDay(transactions: List<Transaction>): List<DependentDayRow> {
    val dayKeyFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val chronoGroups = transactions
        .sortedBy { it.date }
        .groupBy { dayKeyFmt.format(Date(it.date)) }
        .entries
        .sortedBy { it.value.first().date }

    var running = 0
    val rows = chronoGroups.map { (_, txsOfDay) ->
        val net = txsOfDay.sumOf { dependentBalanceSigned(it) }
        running += net
        DependentDayRow(txsOfDay.first().date, net, running, txsOfDay)
    }
    return rows.asReversed()
}

@Composable
fun DependentLedger(state: HistoryState, modifier: Modifier = Modifier) {
    val sdf = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }
    val dayRows = remember(state.transactions) { groupDependentTransactionsByDay(state.transactions) }
    var selectedDay by remember { mutableStateOf<DependentDayRow?>(null) }

    selectedDay?.let { day ->
        DependentDayDetailDialog(day = day, onDismiss = { selectedDay = null })
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)
        ) {
            Text("Fecha", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Del día", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
            Text("Recibido", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
        }
        HorizontalDivider()
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(dayRows) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { selectedDay = row }.padding(8.dp)
                ) {
                    Text(sdf.format(Date(row.dateMillis)), Modifier.weight(1f), fontSize = 12.sp)
                    Text(
                        if (row.netAmount >= 0) "+${row.netAmount}" else "${row.netAmount}",
                        Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.End,
                        color = dependentBalanceColor(row.netAmount)
                    )
                    Text(
                        "${row.runningBalance}",
                        Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.End, fontWeight = FontWeight.Bold,
                        color = dependentBalanceColor(row.runningBalance)
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
            }
            if (dayRows.isEmpty()) {
                item { Text("Sin movimientos todavía.", Modifier.padding(16.dp), fontSize = 12.sp, color = Color.Gray) }
            }
        }
    }
}

@Composable
private fun DependentDayDetailDialog(day: DependentDayRow, onDismiss: () -> Unit) {
    val headerFmt = remember { SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(headerFmt.format(Date(day.dateMillis)).replaceFirstChar { it.uppercase() })
        },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                day.transactions.forEach { tx ->
                    val itemLabel = tx.itemName?.takeIf { it.isNotBlank() } ?: tx.description.ifBlank { "Regalo" }
                    val label = if (tx.type == TransactionType.EARN) "Recibido: $itemLabel" else "Enviado: $itemLabel"
                    val signed = dependentBalanceSigned(tx)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
                        Text(
                            if (signed >= 0) "+$signed" else "$signed",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = dependentBalanceColor(signed)
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                }
                if (day.transactions.isEmpty()) {
                    Text("Sin movimientos.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Text(text, Modifier.width(width), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
}

@Composable
private fun Cell(text: String, width: Dp, bold: Boolean = false, color: Color = Color.Unspecified) {
    Text(
        text, Modifier.width(width), fontSize = 12.sp, textAlign = TextAlign.Center,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = color
    )
}