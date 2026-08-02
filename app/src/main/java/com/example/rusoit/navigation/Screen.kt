package com.example.rusoit.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Statistics : Screen("statistics")
    object UnitActivity : Screen("unit_activity")
    object Calendar : Screen("calendar")
    object NewsReports : Screen("news_reports")
    object Vehicles : Screen("vehicles")
    object Personnel : Screen("personnel")
    object Incidents : Screen("incidents")
    object More : Screen("more")
}
