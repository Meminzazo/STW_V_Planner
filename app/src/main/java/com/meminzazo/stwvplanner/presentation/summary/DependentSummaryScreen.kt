package com.meminzazo.stwvplanner.presentation.summary

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
                        text = "REGALOS RECIBIDOS: ${account?.name?.uppercase() ?: ""}",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StwBackground,
                    titleContentColor = FortAccent
                )
            )
        },
        containerColor = StwBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (monthlyGifts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se han recibido regalos todavía.", color = Color.Gray)
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
        colors = CardDefaults.cardColors(containerColor = StwCardSurface),
        border = androidx.compose.foundation.BorderStroke(2.dp, FortPurple.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthly.monthName.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = FortAccent
                )
                Text(
                    text = "+${monthly.totalAmount} V",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = EarnGreen
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.3f))

            monthly.gifts.forEach { gift ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val displayTitle = gift.itemName?.takeIf { it.isNotBlank() }
                            ?: gift.description.takeIf { it.isNotBlank() }
                            ?: "REGALO"
                        
                        Text(
                            text = displayTitle.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = sdf.format(Date(gift.date)),
                            fontSize = 10.sp,
                            color = VBucksSilver
                        )
                    }
                    Text(
                        text = "${gift.amount}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = VBucksGold
                    )
                }
            }
        }
    }
}
