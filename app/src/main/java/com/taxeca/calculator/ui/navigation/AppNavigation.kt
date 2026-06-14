package com.taxeca.calculator.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
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
import com.taxeca.calculator.data.repository.LanguageManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taxeca.calculator.ui.components.AdBanner
import com.taxeca.calculator.ui.components.UnlockBottomSheet
import com.taxeca.calculator.ui.screens.CalculatorScreen
import com.taxeca.calculator.ui.screens.HistoryDetailScreen
import com.taxeca.calculator.ui.screens.HistoryScreen
import com.taxeca.calculator.ui.screens.RestaurantScreen
import com.taxeca.calculator.ui.screens.SettingsScreen
import com.taxeca.calculator.ui.screens.ShoppingScreen
import com.taxeca.calculator.ui.theme.GradientEnd
import com.taxeca.calculator.ui.theme.GradientMid
import com.taxeca.calculator.ui.theme.GradientStart
import com.taxeca.calculator.ui.theme.PremiumGold
import com.taxeca.calculator.ui.viewmodel.FreemiumViewModel
import com.taxeca.calculator.ui.viewmodel.SettingsViewModel

val LocalFreemiumViewModel = compositionLocalOf<FreemiumViewModel> {
    error("FreemiumViewModel not provided")
}

sealed class Screen(val route: String) {
    data object Calculator      : Screen("calculator")
    data object Shopping        : Screen("shopping")
    data object Restaurant      : Screen("restaurant")
    data object History         : Screen("history")
    data object Settings        : Screen("settings")
    data object HistoryDetail   : Screen("history_detail/{entryId}") {
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
    val settingsVm: SettingsViewModel = hiltViewModel()

    val navItems = listOf(
        NavItem(Screen.Calculator, Icons.Default.Calculate,    R.string.tab_calculator),
        NavItem(Screen.Shopping,   Icons.Default.ShoppingCart, R.string.tab_shopping),
        NavItem(Screen.Restaurant, Icons.Default.Restaurant,   R.string.tab_restaurant),
        NavItem(Screen.History,    Icons.Default.History,      R.string.tab_history)
    )

    val focusManager          = LocalFocusManager.current
    val navBackStackEntry    by navController.currentBackStackEntryAsState()
    val currentDestination   = navBackStackEntry?.destination
    val isOnHistoryDetail    = currentDestination?.route?.startsWith("history_detail") == true
    val isOnSettings         = currentDestination?.route == Screen.Settings.route
    val isOnSecondary        = isOnHistoryDetail || isOnSettings

    val isPremium by freemiumVm.isPremium.collectAsStateWithLifecycle()

    // Record session once per launch
    LaunchedEffect(Unit) { freemiumVm.recordSession() }

    // Observe paywall trigger
    val showPaywall by freemiumVm.showPaywall.collectAsStateWithLifecycle()
    var paywallVisible by remember { mutableStateOf(false) }
    LaunchedEffect(showPaywall) { if (showPaywall) paywallVisible = true }

    CompositionLocalProvider(LocalFreemiumViewModel provides freemiumVm) {
        if (paywallVisible) {
            UnlockBottomSheet(onDismiss = {
                paywallVisible = false
                freemiumVm.dismissPaywall()
            })
        }
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
                        actions = {
                            if (freemiumVm.shouldShowRewardedShield) {
                                IconButton(onClick = { paywallVisible = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = stringResource(R.string.content_desc_watch_ad_free),
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            if (isPremium) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = stringResource(R.string.premium_active_label),
                                    tint = PremiumGold,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            } else {
                                TextButton(
                                    onClick = { paywallVisible = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = stringResource(R.string.premium_unlock_btn),
                                        tint = PremiumGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.settings_premium_section),
                                        color = PremiumGold,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    navController.navigate(Screen.Settings.route) {
                                        launchSingleTop = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.tab_settings),
                                    tint = Color.White
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
                if (!isOnSecondary) {
                    Column {
                    AdBanner()
                    NavigationBar(tonalElevation = 8.dp) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hierarchy
                                ?.any { it.route == item.screen.route } == true
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
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
                                    focusManager.clearFocus()
                                    freemiumVm.logTabChanged(item.screen.route)
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState    = true
                                    }
                                    freemiumVm.recordAction()
                                }
                            )
                        }
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
                composable(Screen.Settings.route) {
                    SettingsScreen(languageManager = settingsVm.languageManager)
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
