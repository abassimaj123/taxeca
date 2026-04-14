package com.taxeca.calculator.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.taxeca.calculator.R
import com.taxeca.calculator.ui.screens.CalculatorScreen
import com.taxeca.calculator.ui.screens.HistoryDetailScreen
import com.taxeca.calculator.ui.screens.HistoryScreen
import com.taxeca.calculator.ui.screens.RestaurantScreen
import com.taxeca.calculator.ui.screens.ShoppingScreen
import com.taxeca.calculator.ui.theme.GradientEnd
import com.taxeca.calculator.ui.theme.GradientMid
import com.taxeca.calculator.ui.theme.GradientStart
import com.taxeca.calculator.ui.viewmodel.FreemiumViewModel

val LocalFreemiumViewModel = compositionLocalOf<FreemiumViewModel> {
    error("FreemiumViewModel not provided")
}

sealed class Screen(val route: String) {
    data object Calculator   : Screen("calculator")
    data object Shopping     : Screen("shopping")
    data object Restaurant   : Screen("restaurant")
    data object History      : Screen("history")
    data object HistoryDetail : Screen("history_detail/{entryId}") {
        fun route(id: Long) = "history_detail/$id"
    }
}

private data class NavItem(
    val screen: Screen,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int
)

private val headerGradient = Brush.horizontalGradient(
    colors = listOf(GradientStart, GradientMid, GradientEnd)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController  = rememberNavController()
    val freemiumVm: FreemiumViewModel = hiltViewModel()

    val navItems = listOf(
        NavItem(Screen.Calculator, Icons.Default.Calculate,    R.string.tab_calculator),
        NavItem(Screen.Shopping,   Icons.Default.ShoppingCart, R.string.tab_shopping),
        NavItem(Screen.Restaurant, Icons.Default.Restaurant,   R.string.tab_restaurant),
        NavItem(Screen.History,    Icons.Default.History,      R.string.tab_history)
    )

    val navBackStackEntry    by navController.currentBackStackEntryAsState()
    val currentDestination   = navBackStackEntry?.destination
    val isOnHistoryDetail    = currentDestination?.route?.startsWith("history_detail") == true

    CompositionLocalProvider(LocalFreemiumViewModel provides freemiumVm) {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerGradient)
                ) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "TaxeCA",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "🇨🇦",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            },
            bottomBar = {
                if (!isOnHistoryDetail) {
                    NavigationBar(tonalElevation = 8.dp) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hierarchy
                                ?.any { it.route == item.screen.route } == true
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = {
                                    Text(
                                        text = stringResource(item.labelRes),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = selected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                                    indicatorColor      = MaterialTheme.colorScheme.primaryContainer
                                ),
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState    = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController    = navController,
                startDestination = Screen.Calculator.route,
                modifier         = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Calculator.route)  { CalculatorScreen() }
                composable(Screen.Shopping.route)    { ShoppingScreen() }
                composable(Screen.Restaurant.route)  { RestaurantScreen() }
                composable(Screen.History.route) {
                    HistoryScreen(
                        onNavigateToDetail = { id ->
                            navController.navigate(Screen.HistoryDetail.route(id))
                        }
                    )
                }
                composable(
                    route     = Screen.HistoryDetail.route,
                    arguments = listOf(navArgument("entryId") { type = NavType.LongType })
                ) {
                    HistoryDetailScreen(onNavigateBack = { navController.navigateUp() })
                }
            }
        }
    }
}
