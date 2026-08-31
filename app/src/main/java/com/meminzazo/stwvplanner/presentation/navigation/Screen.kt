package com.meminzazo.stwvplanner.presentation.navigation

sealed class Screen(val route: String) {
    object AccountSelection : Screen("account_selection")
    
    object AccountDetail : Screen("account_detail/{accountId}") {
        fun createRoute(accountId: Long) = "account_detail/$accountId"
    }
    
    object AddExpense : Screen("add_expense/{accountId}") {
        fun createRoute(accountId: Long) = "add_expense/$accountId"
    }
    
    object History : Screen("history/{accountId}") {
        fun createRoute(accountId: Long) = "history/$accountId"
    }

    object DependentSummary : Screen("dependent_summary/{accountId}") {
        fun createRoute(accountId: Long) = "dependent_summary/$accountId"
    }
}
