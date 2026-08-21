package com.meminzazo.stwvplanner.presentation.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object AddExpense : Screen("add_expense/{accountId}") {
        fun createRoute(accountId: Long) = "add_expense/$accountId"
    }
}
