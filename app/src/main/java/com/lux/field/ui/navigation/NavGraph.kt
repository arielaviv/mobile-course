package com.lux.field.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lux.field.ui.login.LoginScreen
import com.lux.field.ui.map.MapScreen
import com.lux.field.ui.settings.SettingsScreen
import com.lux.field.ui.workorder.TaskDetailScreen
import com.lux.field.ui.workorder.WorkOrderDetailScreen

@Composable
fun LuxNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Map.route) {
            MapScreen(
                onWorkOrderClick = { workOrderId ->
                    navController.navigate(Screen.WorkOrderDetail.createRoute(workOrderId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.WorkOrderDetail.route,
            arguments = listOf(navArgument("workOrderId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val workOrderId = backStackEntry.arguments?.getString("workOrderId") ?: return@composable
            WorkOrderDetailScreen(
                workOrderId = workOrderId,
                onTaskClick = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(workOrderId, taskId))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(
                navArgument("workOrderId") { type = NavType.StringType },
                navArgument("taskId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val workOrderId = backStackEntry.arguments?.getString("workOrderId") ?: return@composable
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            TaskDetailScreen(
                workOrderId = workOrderId,
                taskId = taskId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
