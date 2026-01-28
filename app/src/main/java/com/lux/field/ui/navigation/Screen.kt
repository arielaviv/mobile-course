package com.lux.field.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Map : Screen("map")
    data object WorkOrderDetail : Screen("workorder/{workOrderId}") {
        fun createRoute(workOrderId: String): String = "workorder/$workOrderId"
    }
    data object TaskDetail : Screen("task/{workOrderId}/{taskId}") {
        fun createRoute(workOrderId: String, taskId: String): String = "task/$workOrderId/$taskId"
    }
}
