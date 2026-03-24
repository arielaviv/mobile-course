package com.field.survey.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Map : Screen("map")
    data object AddDp : Screen("add-dp")
    data object MyDps : Screen("my-dps")
    data object EditDp : Screen("edit-dp/{dpId}") {
        fun createRoute(dpId: String): String = "edit-dp/$dpId"
    }
    data object Settings : Screen("settings")
}
