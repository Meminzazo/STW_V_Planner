package com.meminzazo.stwvplanner.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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

/**
 * Vista para cuentas principales: una fila por transacción, una columna
 * por cada dependiente para ver a quién se le dio cada regalo.
 * La tabla completa se desliza horizontalmente si hay muchos dependientes.
 */
@Composable
fun MainAccountTable(state: HistoryState, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    val sdf = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }

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

            state.transactions.forEach { tx ->
                Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cell(sdf.format(Date(tx.date)), COL_DATE)
                    NumCell(tx, VBucksSource.DAILY, Color(0xFF4CAF50))
                    NumCell(tx, VBucksSource.ALERT, Color(0xFF2196F3))
                    NumCell(tx, VBucksSource.EXTERNAL, Color(0xFFFF9800))
                    state.dependents.forEach { dep -> DependentGiftCell(tx, dep) }
                    OtherCell(tx, state.dependents.map { it.id }.toSet())
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
 * Vista para cuentas dependientes: ledger simple (fecha, detalle, monto, saldo),
 * sin columnas de Diaria/Alerta/Externo que no les aplican.
 */
@Composable
fun DependentLedger(state: HistoryState, modifier: Modifier = Modifier) {
    val sdf = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }
    var running = 0
    val rows = state.transactions.sortedBy { it.date }.map { tx ->
        running += if (tx.type == TransactionType.SPEND) -tx.amount else tx.amount
        Triple(tx, if (tx.type == TransactionType.SPEND) -tx.amount else tx.amount, running)
    }.asReversed()

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)
        ) {
            Text("Fecha", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Detalle", Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Monto", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
            Text("Saldo", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
        }
        HorizontalDivider()
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(rows) { (tx, signed, saldo) ->
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(sdf.format(Date(tx.date)), Modifier.weight(1f), fontSize = 12.sp)
                    Text(
                        tx.itemName?.takeIf { it.isNotBlank() } ?: tx.description,
                        Modifier.weight(2f), fontSize = 12.sp
                    )
                    Text(
                        if (signed >= 0) "+$signed" else "$signed",
                        Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.End,
                        color = if (signed >= 0) Color(0xFF4CAF50) else Color.Red
                    )
                    Text("$saldo", Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
            }
            if (rows.isEmpty()) {
                item { Text("Sin movimientos todavía.", Modifier.padding(16.dp), fontSize = 12.sp, color = Color.Gray) }
            }
        }
    }
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

@Composable
private fun NumCell(tx: Transaction, source: VBucksSource, activeColor: Color) {
    val active = tx.source == source
    Cell(if (active) "${tx.amount}" else "-", COL_NUM, color = if (active) activeColor else Color.Gray)
}

@Composable
private fun DependentGiftCell(tx: Transaction, dependent: Account) {
    val active = tx.source == VBucksSource.GIFT && tx.type == TransactionType.SPEND && tx.receiverAccountId == dependent.id
    Cell(if (active) "-${tx.amount}" else "-", COL_NUM, color = if (active) Color.Red else Color.Gray)
}

@Composable
private fun OtherCell(tx: Transaction, dependentIds: Set<Long>) {
    val isDependentGift = tx.source == VBucksSource.GIFT && tx.type == TransactionType.SPEND && tx.receiverAccountId in dependentIds
    val isCore = tx.source == VBucksSource.DAILY || tx.source == VBucksSource.ALERT || tx.source == VBucksSource.EXTERNAL
    val text = if (isCore || isDependentGift) "-" else {
        val prefix = if (tx.type == TransactionType.SPEND) "-" else "+"
        "$prefix${tx.amount}"
    }
    Cell(text, COL_NUM, color = if (text.startsWith("-") && text != "-") Color.Red else Color.Unspecified)
}