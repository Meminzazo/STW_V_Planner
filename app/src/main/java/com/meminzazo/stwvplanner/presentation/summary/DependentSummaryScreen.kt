package com.meminzazo.stwvplanner.presentation.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DependentSummaryScreen(
    viewModel: DependentSummaryViewModel = hiltViewModel(),
    onPopBackStack: () -> Unit
) {
    val account by viewModel.account.collectAsState()
    val monthlyGifts by viewModel.monthlyGifts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Regalos recibidos: ${account?.name ?: ""}",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = StormTextMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StormBackground,
                    titleContentColor = StormTextMain
                )
            )
        },
        containerColor = StormBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (monthlyGifts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text("No se han recibido regalos todavía.", color = StormTextMuted)
                    }
                }
            }

            items(monthlyGifts) { monthly ->
                MonthSection(monthly)
            }
        }
    }
}

@Composable
fun MonthSection(monthly: MonthlyGifts) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = monthly.monthName.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = StormCyan
                )
                Text(
                    text = "${monthly.totalAmount} V",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Black,
                    color = StormTextMuted
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = StormBorder)

            monthly.gifts.forEachIndexed { index, gift ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val displayTitle = gift.itemName?.takeIf { it.isNotBlank() }
                            ?: gift.description.takeIf { it.isNotBlank() }
                            ?: "Regalo"

                        Text(
                            text = displayTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = StormTextMain
                        )
                        Text(
                            text = sdf.format(Date(gift.date)),
                            fontSize = 12.sp,
                            color = StormTextMuted
                        )
                    }
                    Text(
                        text = "${gift.amount}",
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Black,
                        color = StormAmber
                    )
                }
            }
        }
    }
}
