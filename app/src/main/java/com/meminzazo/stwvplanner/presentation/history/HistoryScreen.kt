package com.meminzazo.stwvplanner.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import java.text.SimpleDateFormat
import java.util.*

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
                title = { Text("Historial Detallado") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            HeaderRow()
            HorizontalDivider()
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.transactions) { transaction ->
                    TransactionRow(transaction)
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                }
            }
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
            TotalRow(state)
        }
    }
}

@Composable
fun HeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Fecha", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("Diaria", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text("Alerta", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text("Externo", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text("Otros/Reg", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
    }
}

@Composable
fun TransactionRow(transaction: Transaction) {
    val sdf = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }
    val dateStr = sdf.format(Date(transaction.date))

    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(dateStr, modifier = Modifier.weight(1.5f), fontSize = 12.sp)
        
        // Columna Diaria
        Text(
            text = if (transaction.source == VBucksSource.DAILY) "${transaction.amount}" else "-",
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = if (transaction.source == VBucksSource.DAILY) Color(0xFF4CAF50) else Color.Gray
        )
        
        // Columna Alerta
        Text(
            text = if (transaction.source == VBucksSource.ALERT) "${transaction.amount}" else "-",
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = if (transaction.source == VBucksSource.ALERT) Color(0xFF2196F3) else Color.Gray
        )

        // Columna Externo
        Text(
            text = if (transaction.source == VBucksSource.EXTERNAL) "${transaction.amount}" else "-",
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = if (transaction.source == VBucksSource.EXTERNAL) Color(0xFFFF9800) else Color.Gray
        )
        
        // Columna Otros / Regalos
        val otherText = when {
            transaction.source == VBucksSource.GIFT -> {
                val prefix = if (transaction.type == TransactionType.SPEND) "-" else "+"
                "$prefix${transaction.amount}\n${transaction.itemName ?: "Regalo"}"
            }
            transaction.source != VBucksSource.DAILY && 
            transaction.source != VBucksSource.ALERT && 
            transaction.source != VBucksSource.EXTERNAL -> {
                val prefix = if (transaction.type == TransactionType.SPEND) "-" else "+"
                "$prefix${transaction.amount}\n${transaction.source.name}"
            }
            else -> "-"
        }
        
        Text(
            text = otherText,
            modifier = Modifier.weight(2f),
            fontSize = 11.sp,
            textAlign = TextAlign.End,
            lineHeight = 14.sp,
            color = if (transaction.type == TransactionType.SPEND) Color.Red else Color.Black
        )
    }
}

@Composable
fun TotalRow(state: HistoryState) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("TOTAL", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        Text("${state.totalDaily}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text("${state.totalAlert}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text("${state.totalExternal}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text(
            text = "${state.totalOthers}",
            modifier = Modifier.weight(2f),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            color = if (state.totalOthers < 0) Color.Red else Color.Black
        )
    }
}
