package com.meminzazo.stwvplanner.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meminzazo.stwvplanner.presentation.theme.*

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is LoginViewModel.UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = StormBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Tactical HUD Badge / Header
            Surface(
                color = StormCyan.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, StormCyan.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "STW V-BUCKS COMMAND",
                    style = MaterialTheme.typography.labelMedium,
                    color = StormCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "V-PLANNER",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = StormAmber
            )

            Text(
                text = "SYSTEM v3.1 · SECURE HUD",
                style = MaterialTheme.typography.labelSmall,
                color = StormTextMuted,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(56.dp))

            if (isLoading) {
                CircularProgressIndicator(color = StormCyan)
            } else {
                Button(
                    onClick = { viewModel.onSignInWithGoogle(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = StormCyan)
                ) {
                    Text(
                        "INICIAR CON GOOGLE",
                        fontWeight = FontWeight.Black,
                        color = StormBackground,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = viewModel::onContinueAsGuest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, StormBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StormTextMuted)
                ) {
                    Text(
                        "MODO INVITADO (LOCAL)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = StormCardSurface),
                    border = BorderStroke(1.dp, StormBorder),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "El modo invitado almacena registros localmente en el dispositivo. Conecta Google para sincronización segura en la nube.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StormTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
