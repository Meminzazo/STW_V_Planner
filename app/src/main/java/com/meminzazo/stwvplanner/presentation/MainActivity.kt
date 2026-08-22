package com.meminzazo.stwvplanner.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.meminzazo.stwvplanner.presentation.auth.AccountSelectionScreen
import com.meminzazo.stwvplanner.presentation.auth.LoginScreen
import com.meminzazo.stwvplanner.presentation.detail.AccountDetailScreen
import com.meminzazo.stwvplanner.presentation.expense.AddExpenseScreen
import com.meminzazo.stwvplanner.presentation.history.HistoryScreen
import com.meminzazo.stwvplanner.presentation.navigation.Screen
import com.meminzazo.stwvplanner.presentation.theme.STWVPlannerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val currentUser by mainViewModel.currentUser.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val navController = rememberNavController()

            // Gestión de permisos de notificaciones para Android 13+
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    if (!isGranted) {
                        // Opcional: Avisar al usuario que no recibirá recordatorios
                    }
                }
            )

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    if (!hasPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            STWVPlannerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    if (currentUser == null) {
                        LoginScreen(snackbarHostState = snackbarHostState)
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.AccountSelection.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.AccountSelection.route) {
                                AccountSelectionScreen(
                                    onAccountSelected = { accountId ->
                                        navController.navigate(Screen.AccountDetail.createRoute(accountId))
                                    },
                                    snackbarHostState = snackbarHostState
                                )
                            }
                            composable(
                                route = Screen.AccountDetail.route,
                                arguments = listOf(
                                    navArgument("accountId") { type = NavType.LongType }
                                )
                            ) {
                                AccountDetailScreen(
                                    onPopBackStack = { navController.popBackStack() },
                                    onNavigateToHistory = { accountId ->
                                        navController.navigate(Screen.History.createRoute(accountId))
                                    },
                                    snackbarHostState = snackbarHostState
                                )
                            }
                            composable(
                                route = Screen.AddExpense.route,
                                arguments = listOf(
                                    navArgument("accountId") { type = NavType.LongType }
                                )
                            ) {
                                AddExpenseScreen(
                                    onPopBackStack = { navController.popBackStack() },
                                    snackbarHostState = snackbarHostState
                                )
                            }
                            composable(
                                route = Screen.History.route,
                                arguments = listOf(
                                    navArgument("accountId") { type = NavType.LongType }
                                )
                            ) {
                                HistoryScreen(
                                    onPopBackStack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
