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
    data object Settings : Screen("settings")
    data object Navigation : Screen("navigation/{workOrderId}/{destLat}/{destLng}") {
        fun createRoute(workOrderId: String, destLat: Double, destLng: Double): String =
            "navigation/$workOrderId/$destLat/$destLng"
    }
    data object Camera : Screen("camera/{workOrderId}/{taskId}/{stepId}/{cameraFacing}") {
        fun createRoute(
            workOrderId: String,
            taskId: String,
            stepId: String = "none",
            cameraFacing: String,
        ): String = "camera/$workOrderId/$taskId/$stepId/$cameraFacing"
    }
}
