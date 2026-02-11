package com.lux.field.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lux.field.ui.camera.CameraScreen
import com.lux.field.ui.login.LoginScreen
import com.lux.field.ui.map.MapScreen
import com.lux.field.ui.navigation.NavigationScreen
import com.lux.field.ui.settings.SettingsScreen
import com.lux.field.ui.workorder.TaskDetailScreen
import com.lux.field.ui.workorder.TaskDetailViewModel
import com.lux.field.ui.workorder.WorkOrderDetailScreen

private const val NAV_ANIM_DURATION = 300

@Composable
fun LuxNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing),
            ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing),
            ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
        },
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
                onNavigateToWorkOrder = { workOrderId, lat, lng ->
                    navController.navigate(Screen.Navigation.createRoute(workOrderId, lat, lng))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
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
                onNavigateClick = { woId, lat, lng ->
                    navController.navigate(Screen.Navigation.createRoute(woId, lat, lng))
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

            // Observe saved photo ID result from camera screen
            val savedPhotoId = backStackEntry.savedStateHandle.get<String>("savedPhotoId")
            val viewModel: TaskDetailViewModel = hiltViewModel()

            LaunchedEffect(savedPhotoId) {
                savedPhotoId?.let {
                    viewModel.onPhotoSaved(it)
                    backStackEntry.savedStateHandle.remove<String>("savedPhotoId")
                }
            }

            TaskDetailScreen(
                workOrderId = workOrderId,
                taskId = taskId,
                onBack = { navController.popBackStack() },
                onNavigateToCamera = { woId, tId, facing ->
                    navController.navigate(
                        Screen.Camera.createRoute(
                            workOrderId = woId,
                            taskId = tId,
                            cameraFacing = facing,
                        )
                    )
                },
                viewModel = viewModel,
            )
        }

        composable(
            route = Screen.Camera.route,
            arguments = listOf(
                navArgument("workOrderId") { type = NavType.StringType },
                navArgument("taskId") { type = NavType.StringType },
                navArgument("stepId") { type = NavType.StringType },
                navArgument("cameraFacing") { type = NavType.StringType },
            ),
        ) {
            CameraScreen(
                onPhotoSaved = { photoId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("savedPhotoId", photoId)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Navigation.route,
            arguments = listOf(
                navArgument("workOrderId") { type = NavType.StringType },
                navArgument("destLat") { type = NavType.StringType },
                navArgument("destLng") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val workOrderId = backStackEntry.arguments?.getString("workOrderId") ?: return@composable
            val destLat = backStackEntry.arguments?.getString("destLat")?.toDoubleOrNull() ?: return@composable
            val destLng = backStackEntry.arguments?.getString("destLng")?.toDoubleOrNull() ?: return@composable
            NavigationScreen(
                workOrderId = workOrderId,
                destLat = destLat,
                destLng = destLng,
                onBack = { navController.popBackStack() },
                onArrived = { navController.popBackStack() },
            )
        }
    }
}
