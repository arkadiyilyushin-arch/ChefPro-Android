package com.chefpro.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chefpro.ui.localization.AppStrings
import com.chefpro.ui.localization.LocalAppStrings
import com.chefpro.ui.screens.ABCAnalysisView
import com.chefpro.ui.screens.AnalyticsView
import com.chefpro.ui.screens.BackupView
import com.chefpro.ui.screens.BarcodeScanScreen
import com.chefpro.ui.screens.BreakevenView
import com.chefpro.ui.screens.ChecklistsView
import com.chefpro.ui.screens.CsvExportView
import com.chefpro.ui.screens.DashboardScreen
import com.chefpro.ui.screens.DigitalMenuView
import com.chefpro.ui.screens.DishDetailScreen
import com.chefpro.ui.screens.DishEditScreen
import com.chefpro.ui.screens.DishGalleryView
import com.chefpro.ui.screens.EmployeesView
import com.chefpro.ui.screens.FloorPlanView
import com.chefpro.ui.screens.FoodCostByPeriodView
import com.chefpro.ui.screens.FoodCostTrendView
import com.chefpro.ui.screens.InventoryEditScreen
import com.chefpro.ui.screens.InventoryListScreen
import com.chefpro.ui.screens.KitchenBoardView
import com.chefpro.ui.screens.KitchenModeView
import com.chefpro.ui.screens.LoginScreen
import com.chefpro.ui.screens.LoyaltyView
import com.chefpro.ui.screens.MarkupCalculatorView
import com.chefpro.ui.screens.MenuCollectionsView
import com.chefpro.ui.screens.MenuEngineeringView
import com.chefpro.ui.screens.MoreScreen
import com.chefpro.ui.screens.MultitenancyView
import com.chefpro.ui.screens.OnboardingScreen
import com.chefpro.ui.screens.OperatingExpensesView
import com.chefpro.ui.screens.POSIntegrationView
import com.chefpro.ui.screens.PdfExportView
import com.chefpro.ui.screens.PlanVsFactView
import com.chefpro.ui.screens.PriceCalculatorView
import com.chefpro.ui.screens.ProductionPlanView
import com.chefpro.ui.screens.ProfitLossView
import com.chefpro.ui.screens.ProfitabilityRankingView
import com.chefpro.ui.screens.ProfileView
import com.chefpro.ui.screens.PurchaseBudgetView
import com.chefpro.ui.screens.PurchaseForecastView
import com.chefpro.ui.screens.PurchasesView
import com.chefpro.ui.screens.RecipeTemplatesView
import com.chefpro.ui.screens.RecipeVersionsScreen
import com.chefpro.ui.screens.ReportsView
import com.chefpro.ui.screens.SalesView
import com.chefpro.ui.screens.SearchScreen
import com.chefpro.ui.screens.SettingsView
import com.chefpro.ui.screens.ShiftView
import com.chefpro.ui.screens.SupplierAnalyticsView
import com.chefpro.ui.screens.SupplierAutoOrderView
import com.chefpro.ui.screens.SuppliersView
import com.chefpro.ui.screens.SyncView
import com.chefpro.ui.screens.TableReservationView
import com.chefpro.ui.screens.TechCardsListScreen
import com.chefpro.ui.screens.TemperatureLogView
import com.chefpro.ui.screens.TopDishCostView
import com.chefpro.ui.screens.UnitConverterView
import com.chefpro.ui.screens.WaiterModeView
import com.chefpro.ui.screens.WorkScheduleView
import com.chefpro.ui.screens.WriteOffReportView
import com.chefpro.ui.screens.WriteOffsView
import com.chefpro.ui.viewmodel.ChefProViewModel

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private fun buildNavTabs(strings: AppStrings): List<TabItem> = listOf(
    TabItem(Routes.DASHBOARD, strings.dashboard, Icons.Default.Home),
    TabItem(Routes.TECH_CARDS, strings.techCards, Icons.Default.MenuBook),
    TabItem(Routes.SEARCH, strings.search, Icons.Default.Search),
    TabItem(Routes.INVENTORY, strings.inventory, Icons.Default.Inventory2),
    TabItem(Routes.MORE, "Ещё", Icons.Default.GridView),
)

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(Routes.DASHBOARD) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun ChefProBottomBar(
    tabs: List<TabItem>,
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
) {
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onTabSelected(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}

@Composable
private fun ChefProNavigationRail(
    tabs: List<TabItem>,
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(modifier = modifier.fillMaxHeight()) {
        tabs.forEach { tab ->
            NavigationRailItem(
                selected = currentRoute == tab.route,
                onClick = { onTabSelected(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}

@Composable
fun AppNavigation(viewModel: ChefProViewModel) {
    val navController = rememberNavController()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val state by viewModel.state.collectAsState()
    val strings = LocalAppStrings.current
    val configuration = LocalConfiguration.current

    val startDestination = when {
        !isLoggedIn -> Routes.LOGIN
        !state.hasSeenOnboarding -> Routes.ONBOARDING
        else -> Routes.DASHBOARD
    }

    LaunchedEffect(isLoggedIn, state.hasSeenOnboarding) {
        val target = when {
            !isLoggedIn -> Routes.LOGIN
            !state.hasSeenOnboarding -> Routes.ONBOARDING
            else -> Routes.DASHBOARD
        }
        val current = navController.currentDestination?.route
        if (current == Routes.LOGIN || current == Routes.ONBOARDING) {
            if (current != target) {
                navController.navigate(target) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/{")
        ?: navBackStackEntry?.destination?.route
    val showPrimaryNav = currentRoute in Routes.bottomTabRoutes
    val useRailLayout = configuration.screenWidthDp >= 840 && isLoggedIn && state.hasSeenOnboarding
    val tabs = buildNavTabs(strings)
    val onTabSelected: (String) -> Unit = { route -> navigateToTab(navController, route) }

    if (useRailLayout) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (showPrimaryNav) {
                ChefProNavigationRail(
                    tabs = tabs,
                    currentRoute = currentRoute,
                    onTabSelected = onTabSelected,
                )
            }
            ChefProNavHost(
                navController = navController,
                viewModel = viewModel,
                startDestination = startDestination,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        Scaffold(
            bottomBar = {
                if (showPrimaryNav) {
                    ChefProBottomBar(
                        tabs = tabs,
                        currentRoute = currentRoute,
                        onTabSelected = onTabSelected,
                    )
                }
            },
        ) { padding ->
            ChefProNavHost(
                navController = navController,
                viewModel = viewModel,
                startDestination = startDestination,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun ChefProNavHost(
    navController: NavHostController,
    viewModel: ChefProViewModel,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(viewModel) {
                val destination = if (viewModel.state.value.hasSeenOnboarding) {
                    Routes.DASHBOARD
                } else {
                    Routes.ONBOARDING
                }
                navController.navigate(destination) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            }
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(viewModel) {
                navController.navigate(Routes.DASHBOARD) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            }
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToTechCards = { navController.navigate(Routes.TECH_CARDS) },
                onNavigateToInventory = { navController.navigate(Routes.INVENTORY) },
                onNavigateToSearch = { navController.navigate(Routes.SEARCH) },
                onAddDish = { navController.navigate(Routes.DISH_ADD) },
                onAddProduct = { navController.navigate(Routes.INVENTORY_ADD) },
            )
        }

        composable(Routes.TECH_CARDS) {
            TechCardsListScreen(
                viewModel = viewModel,
                onDishClick = { navController.navigate(Routes.dishDetail(it)) },
                onAddDish = { navController.navigate(Routes.DISH_ADD) },
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                viewModel = viewModel,
                onDishClick = { navController.navigate(Routes.dishDetail(it)) },
                onInventoryClick = { navController.navigate(Routes.inventoryEdit(it)) },
            )
        }

        composable(Routes.INVENTORY) {
            InventoryListScreen(
                viewModel = viewModel,
                onItemClick = { navController.navigate(Routes.inventoryEdit(it)) },
                onAddItem = { navController.navigate(Routes.INVENTORY_ADD) },
                onScanBarcode = { navController.navigate(Routes.BARCODE_SCAN_NEW) },
            )
        }

        composable(Routes.MORE) {
            MoreScreen(viewModel) { destination ->
                navController.navigate(destination.toRoute())
            }
        }

        composable(
            route = Routes.DISH_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_DISH_ID) { type = NavType.StringType }),
        ) { entry ->
            val dishId = entry.arguments?.getString(Routes.ARG_DISH_ID).orEmpty()
            DishDetailScreen(
                viewModel = viewModel,
                dishId = dishId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.dishEdit(it)) },
                onRecipeVersions = { navController.navigate(Routes.recipeVersions(it)) },
            )
        }

        composable(Routes.DISH_ADD) {
            DishEditScreen(
                viewModel = viewModel,
                dishId = null,
                onBack = { navController.popBackStack() },
                onSaved = { dishId ->
                    navController.popBackStack()
                    navController.navigate(Routes.dishDetail(dishId))
                },
            )
        }

        composable(
            route = Routes.DISH_EDIT,
            arguments = listOf(navArgument(Routes.ARG_DISH_ID) { type = NavType.StringType }),
        ) { entry ->
            val dishId = entry.arguments?.getString(Routes.ARG_DISH_ID).orEmpty()
            DishEditScreen(
                viewModel = viewModel,
                dishId = dishId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.RECIPE_VERSIONS,
            arguments = listOf(navArgument(Routes.ARG_DISH_ID) { type = NavType.StringType }),
        ) { entry ->
            val dishId = entry.arguments?.getString(Routes.ARG_DISH_ID).orEmpty()
            RecipeVersionsScreen(
                viewModel = viewModel,
                dishId = dishId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.INVENTORY_ADD) {
            InventoryEditScreen(
                viewModel = viewModel,
                itemId = null,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                onScanBarcode = { navController.navigate(Routes.BARCODE_SCAN_NEW) },
            )
        }

        composable(
            route = Routes.INVENTORY_EDIT,
            arguments = listOf(navArgument(Routes.ARG_ITEM_ID) { type = NavType.StringType }),
        ) { entry ->
            val itemId = entry.arguments?.getString(Routes.ARG_ITEM_ID).orEmpty()
            InventoryEditScreen(
                viewModel = viewModel,
                itemId = itemId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                onScanBarcode = { navController.navigate(Routes.barcodeScan(itemId)) },
            )
        }

        composable(Routes.BARCODE_SCAN_NEW) {
            BarcodeScanScreen(
                viewModel = viewModel,
                itemId = null,
                onBarcodeScanned = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.BARCODE_SCAN,
            arguments = listOf(navArgument(Routes.ARG_ITEM_ID) { type = NavType.StringType }),
        ) { entry ->
            val itemId = entry.arguments?.getString(Routes.ARG_ITEM_ID).orEmpty()
            BarcodeScanScreen(
                viewModel = viewModel,
                itemId = itemId,
                onBarcodeScanned = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.MORE_ANALYTICS) { AnalyticsView(viewModel) }
        composable(Routes.MORE_MENU_ENGINEERING) { MenuEngineeringView(viewModel) }
        composable(Routes.MORE_PROFIT_LOSS) { ProfitLossView(viewModel) }
        composable(Routes.MORE_FOOD_COST_TREND) { FoodCostTrendView(viewModel) }
        composable(Routes.MORE_ABC_ANALYSIS) { ABCAnalysisView(viewModel) }
        composable(Routes.MORE_BREAKEVEN) { BreakevenView(viewModel) }
        composable(Routes.MORE_SUPPLIER_ANALYTICS) { SupplierAnalyticsView(viewModel) }
        composable(Routes.MORE_SALES) { SalesView(viewModel) }
        composable(Routes.MORE_OPERATING_EXPENSES) { OperatingExpensesView(viewModel) }
        composable(Routes.MORE_PURCHASE_BUDGET) { PurchaseBudgetView(viewModel) }
        composable(Routes.MORE_PLAN_VS_FACT) { PlanVsFactView(viewModel) }
        composable(Routes.MORE_MARKUP_CALC) { MarkupCalculatorView(viewModel) }
        composable(Routes.MORE_PROFITABILITY) { ProfitabilityRankingView(viewModel) }
        composable(Routes.MORE_PRICE_CALC) { PriceCalculatorView(viewModel) }
        composable(Routes.MORE_REPORTS) { ReportsView(viewModel) }
        composable(Routes.MORE_PDF_EXPORT) { PdfExportView(viewModel) }
        composable(Routes.MORE_CSV_EXPORT) { CsvExportView(viewModel) }
        composable(Routes.MORE_WRITE_OFF_REPORT) { WriteOffReportView(viewModel) }
        composable(Routes.MORE_PURCHASE_FORECAST) { PurchaseForecastView(viewModel) }
        composable(Routes.MORE_FC_BY_PERIOD) { FoodCostByPeriodView(viewModel) }
        composable(Routes.MORE_TOP_DISH_COST) { TopDishCostView(viewModel) }
        composable(Routes.MORE_EXPIRY) {
            InventoryListScreen(
                viewModel = viewModel,
                onItemClick = { navController.navigate(Routes.inventoryEdit(it)) },
                onAddItem = { navController.navigate(Routes.INVENTORY_ADD) },
            )
        }
        composable(Routes.MORE_STOCK_MOVEMENTS) { ReportsView(viewModel) }
        composable(Routes.MORE_AUDIT) { WriteOffReportView(viewModel) }
        composable(Routes.MORE_AUTO_ORDER) { SupplierAutoOrderView(viewModel) }
        composable(Routes.MORE_TEMPERATURE) { TemperatureLogView(viewModel) }
        composable(Routes.MORE_BARCODE) {
            BarcodeScanScreen(
                viewModel = viewModel,
                itemId = null,
                onBarcodeScanned = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.MORE_RECIPE_TEMPLATES) { RecipeTemplatesView(viewModel) }
        composable(Routes.MORE_DISH_GALLERY) { DishGalleryView(viewModel) }
        composable(Routes.MORE_PRODUCTION_PLAN) { ProductionPlanView(viewModel) }
        composable(Routes.MORE_DIGITAL_MENU) { DigitalMenuView(viewModel) }
        composable(Routes.MORE_MENU_COLLECTIONS) { MenuCollectionsView(viewModel) }
        composable(Routes.MORE_UNIT_CONVERTER) { UnitConverterView() }
        composable(Routes.MORE_WORK_SCHEDULE) { WorkScheduleView(viewModel) }
        composable(Routes.MORE_EMPLOYEE_ACTIVITY) { EmployeesView(viewModel) }
        composable(Routes.MORE_CHECKLISTS) { ChecklistsView(viewModel) }
        composable(Routes.MORE_FLOOR_PLAN) { FloorPlanView(viewModel) }
        composable(Routes.MORE_RESERVATIONS) { TableReservationView(viewModel) }
        composable(Routes.MORE_LOYALTY) { LoyaltyView(viewModel) }
        composable(Routes.MORE_POS) { POSIntegrationView(viewModel) }
        composable(Routes.MORE_RESTAURANTS) { MultitenancyView(viewModel) }
        composable(Routes.MORE_SUPPLIERS) { SuppliersView(viewModel) }
        composable(Routes.MORE_EMPLOYEES) { EmployeesView(viewModel) }
        composable(Routes.MORE_SYNC) { SyncView(viewModel) }
        composable(Routes.MORE_BACKUP) { BackupView(viewModel) }
        composable(Routes.MORE_SETTINGS) { SettingsView(viewModel) }
        composable(Routes.MORE_PROFILE) { ProfileView(viewModel) }
        composable(Routes.MORE_SHIFT) { ShiftView(viewModel) }
        composable(Routes.MORE_KITCHEN_BOARD) { KitchenBoardView(viewModel) }
        composable(Routes.MORE_STOP_GO) { DigitalMenuView(viewModel) }
        composable(Routes.MORE_WAITER) { WaiterModeView(viewModel) }
        composable(Routes.MORE_WRITE_OFFS) { WriteOffsView(viewModel) }
        composable(Routes.MORE_PURCHASES) { PurchasesView(viewModel) }
        composable(Routes.MORE_KITCHEN_MODE) { KitchenModeView(viewModel) }
        composable(Routes.MORE_SEARCH) {
            SearchScreen(
                viewModel = viewModel,
                onDishClick = { navController.navigate(Routes.dishDetail(it)) },
                onInventoryClick = { navController.navigate(Routes.inventoryEdit(it)) },
            )
        }
    }
}
