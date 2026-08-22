package com.meminzazo.stwvplanner.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meminzazo.stwvplanner.presentation.theme.VBucksGold

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoginMode by viewModel.isLoginMode.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is LoginViewModel.UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "STW V PLANNER",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = VBucksGold
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            // Formulario de Email
            OutlinedTextField(
                value = email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Correo Electrónico") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::onEmailAuthClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoginMode) "INICIAR SESIÓN" else "CREAR CUENTA")
            }
            
            TextButton(onClick = viewModel::toggleAuthMode) {
                Text(if (isLoginMode) "¿No tienes cuenta? Regístrate" else "¿Ya tienes cuenta? Inicia sesión")
            }

            if (isLoginMode) {
                TextButton(onClick = viewModel::onForgotPasswordClick) {
                    Text("Olvidé mi contraseña", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            OutlinedButton(
                onClick = { viewModel.onSignInWithGoogle(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CONTINUAR CON GOOGLE")
            }
        }
    }
}
