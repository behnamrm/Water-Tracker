package io.github.behnamrm.watertracker.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.behnamrm.watertracker.R
import io.github.behnamrm.watertracker.ui.components.CircleIconButton
import io.github.behnamrm.watertracker.ui.home.HomeScreen
import io.github.behnamrm.watertracker.ui.settings.SettingsScreen
import io.github.behnamrm.watertracker.ui.stats.StatsScreen

private enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Today("today", R.string.tab_today, Icons.Outlined.WaterDrop),
    Stats("stats", R.string.tab_stats, Icons.Outlined.BarChart),
    Settings("settings", R.string.tab_settings, Icons.Outlined.Settings),
}

@Composable
fun WaterTrackerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // A floating rounded bar with one outlined circle per tab.
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 40.dp, vertical = 12.dp)
                    .fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 10.dp),
                ) {
                    Tab.entries.forEach { tab ->
                        CircleIconButton(
                            icon = tab.icon,
                            contentDescription = stringResource(tab.label),
                            selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = { navController.openTab(tab) },
                            size = 48.dp,
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Today.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Today.route) {
                HomeScreen(snackbarHostState, onOpenStats = { navController.openTab(Tab.Stats) })
            }
            composable(Tab.Stats.route) { StatsScreen() }
            composable(Tab.Settings.route) { SettingsScreen(snackbarHostState) }
        }
    }
}

private fun NavHostController.openTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
