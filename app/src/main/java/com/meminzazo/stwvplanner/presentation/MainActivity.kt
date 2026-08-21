package com.meminzazo.stwvplanner.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
                                    onNavigateToAddExpense = { accountId ->
                                        navController.navigate(Screen.AddExpense.createRoute(accountId))
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
