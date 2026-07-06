package com.chefpro.ui.navigation

import com.chefpro.ui.screens.MoreDestination

object Routes {
    const val LOGIN = "login"
    const val ONBOARDING = "onboarding"

    const val DASHBOARD = "dashboard"
    const val TECH_CARDS = "tech_cards"
    const val SEARCH = "search"
    const val INVENTORY = "inventory"
    const val MORE = "more"

    const val DISH_DETAIL = "dish_detail/{dishId}"
    const val DISH_EDIT = "dish_edit/{dishId}"
    const val DISH_ADD = "dish_add"
    const val RECIPE_VERSIONS = "recipe_versions/{dishId}"

    const val INVENTORY_EDIT = "inventory_edit/{itemId}"
    const val INVENTORY_ADD = "inventory_add"
    const val BARCODE_SCAN = "barcode_scan/{itemId}"
    const val BARCODE_SCAN_NEW = "barcode_scan_new"

    const val ARG_DISH_ID = "dishId"
    const val ARG_ITEM_ID = "itemId"

    const val MORE_ANALYTICS = "more_analytics"
    const val MORE_MENU_ENGINEERING = "more_menu_engineering"
    const val MORE_PROFIT_LOSS = "more_profit_loss"
    const val MORE_FOOD_COST_TREND = "more_food_cost_trend"
    const val MORE_ABC_ANALYSIS = "more_abc_analysis"
    const val MORE_BREAKEVEN = "more_breakeven"
    const val MORE_SUPPLIER_ANALYTICS = "more_supplier_analytics"
    const val MORE_SALES = "more_sales"
    const val MORE_OPERATING_EXPENSES = "more_operating_expenses"
    const val MORE_PURCHASE_BUDGET = "more_purchase_budget"
    const val MORE_PLAN_VS_FACT = "more_plan_vs_fact"
    const val MORE_MARKUP_CALC = "more_markup_calc"
    const val MORE_PROFITABILITY = "more_profitability"
    const val MORE_PRICE_CALC = "more_price_calc"
    const val MORE_REPORTS = "more_reports"
    const val MORE_PDF_EXPORT = "more_pdf_export"
    const val MORE_CSV_EXPORT = "more_csv_export"
    const val MORE_WRITE_OFF_REPORT = "more_write_off_report"
    const val MORE_PURCHASE_FORECAST = "more_purchase_forecast"
    const val MORE_FC_BY_PERIOD = "more_fc_by_period"
    const val MORE_TOP_DISH_COST = "more_top_dish_cost"
    const val MORE_EXPIRY = "more_expiry"
    const val MORE_STOCK_MOVEMENTS = "more_stock_movements"
    const val MORE_AUDIT = "more_audit"
    const val MORE_AUTO_ORDER = "more_auto_order"
    const val MORE_TEMPERATURE = "more_temperature"
    const val MORE_BARCODE = "more_barcode"
    const val MORE_RECIPE_TEMPLATES = "more_recipe_templates"
    const val MORE_DISH_GALLERY = "more_dish_gallery"
    const val MORE_PRODUCTION_PLAN = "more_production_plan"
    const val MORE_DIGITAL_MENU = "more_digital_menu"
    const val MORE_MENU_COLLECTIONS = "more_menu_collections"
    const val MORE_UNIT_CONVERTER = "more_unit_converter"
    const val MORE_WORK_SCHEDULE = "more_work_schedule"
    const val MORE_EMPLOYEE_ACTIVITY = "more_employee_activity"
    const val MORE_CHECKLISTS = "more_checklists"
    const val MORE_FLOOR_PLAN = "more_floor_plan"
    const val MORE_RESERVATIONS = "more_reservations"
    const val MORE_LOYALTY = "more_loyalty"
    const val MORE_POS = "more_pos"
    const val MORE_RESTAURANTS = "more_restaurants"
    const val MORE_SUPPLIERS = "more_suppliers"
    const val MORE_EMPLOYEES = "more_employees"
    const val MORE_SYNC = "more_sync"
    const val MORE_BACKUP = "more_backup"
    const val MORE_SETTINGS = "more_settings"
    const val MORE_PROFILE = "more_profile"
    const val MORE_SHIFT = "more_shift"
    const val MORE_KITCHEN_BOARD = "more_kitchen_board"
    const val MORE_STOP_GO = "more_stop_go"
    const val MORE_WAITER = "more_waiter"
    const val MORE_WRITE_OFFS = "more_write_offs"
    const val MORE_PURCHASES = "more_purchases"
    const val MORE_KITCHEN_MODE = "more_kitchen_mode"
    const val MORE_SEARCH = "more_search"

    val bottomTabRoutes = setOf(DASHBOARD, TECH_CARDS, SEARCH, INVENTORY, MORE)

    fun isMoreSubRoute(route: String?): Boolean = route?.startsWith("more_") == true

    fun tabRouteFor(route: String?): String? = when {
        route == null -> null
        route == MORE || isMoreSubRoute(route) -> MORE
        else -> route
    }

    fun dishDetail(dishId: String) = "dish_detail/$dishId"

    fun dishEdit(dishId: String) = "dish_edit/$dishId"

    fun recipeVersions(dishId: String) = "recipe_versions/$dishId"

    fun inventoryEdit(itemId: String) = "inventory_edit/$itemId"

    fun barcodeScan(itemId: String) = "barcode_scan/$itemId"
}

fun MoreDestination.toRoute(): String = when (this) {
    MoreDestination.ANALYTICS -> Routes.MORE_ANALYTICS
    MoreDestination.MENU_ENGINEERING -> Routes.MORE_MENU_ENGINEERING
    MoreDestination.PROFIT_LOSS -> Routes.MORE_PROFIT_LOSS
    MoreDestination.FOOD_COST_TREND -> Routes.MORE_FOOD_COST_TREND
    MoreDestination.ABC_ANALYSIS -> Routes.MORE_ABC_ANALYSIS
    MoreDestination.BREAKEVEN -> Routes.MORE_BREAKEVEN
    MoreDestination.SUPPLIER_ANALYTICS -> Routes.MORE_SUPPLIER_ANALYTICS
    MoreDestination.SALES -> Routes.MORE_SALES
    MoreDestination.OPERATING_EXPENSES -> Routes.MORE_OPERATING_EXPENSES
    MoreDestination.PURCHASE_BUDGET -> Routes.MORE_PURCHASE_BUDGET
    MoreDestination.PLAN_VS_FACT -> Routes.MORE_PLAN_VS_FACT
    MoreDestination.MARKUP_CALC -> Routes.MORE_MARKUP_CALC
    MoreDestination.PROFITABILITY -> Routes.MORE_PROFITABILITY
    MoreDestination.PRICE_CALC -> Routes.MORE_PRICE_CALC
    MoreDestination.REPORTS -> Routes.MORE_REPORTS
    MoreDestination.PDF_EXPORT -> Routes.MORE_PDF_EXPORT
    MoreDestination.CSV_EXPORT -> Routes.MORE_CSV_EXPORT
    MoreDestination.WRITE_OFF_REPORT -> Routes.MORE_WRITE_OFF_REPORT
    MoreDestination.PURCHASE_FORECAST -> Routes.MORE_PURCHASE_FORECAST
    MoreDestination.FC_BY_PERIOD -> Routes.MORE_FC_BY_PERIOD
    MoreDestination.TOP_DISH_COST -> Routes.MORE_TOP_DISH_COST
    MoreDestination.EXPIRY -> Routes.MORE_EXPIRY
    MoreDestination.STOCK_MOVEMENTS -> Routes.MORE_STOCK_MOVEMENTS
    MoreDestination.AUDIT -> Routes.MORE_AUDIT
    MoreDestination.AUTO_ORDER -> Routes.MORE_AUTO_ORDER
    MoreDestination.TEMPERATURE -> Routes.MORE_TEMPERATURE
    MoreDestination.BARCODE -> Routes.MORE_BARCODE
    MoreDestination.RECIPE_TEMPLATES -> Routes.MORE_RECIPE_TEMPLATES
    MoreDestination.DISH_GALLERY -> Routes.MORE_DISH_GALLERY
    MoreDestination.PRODUCTION_PLAN -> Routes.MORE_PRODUCTION_PLAN
    MoreDestination.DIGITAL_MENU -> Routes.MORE_DIGITAL_MENU
    MoreDestination.MENU_COLLECTIONS -> Routes.MORE_MENU_COLLECTIONS
    MoreDestination.UNIT_CONVERTER -> Routes.MORE_UNIT_CONVERTER
    MoreDestination.WORK_SCHEDULE -> Routes.MORE_WORK_SCHEDULE
    MoreDestination.EMPLOYEE_ACTIVITY -> Routes.MORE_EMPLOYEE_ACTIVITY
    MoreDestination.CHECKLISTS -> Routes.MORE_CHECKLISTS
    MoreDestination.FLOOR_PLAN -> Routes.MORE_FLOOR_PLAN
    MoreDestination.RESERVATIONS -> Routes.MORE_RESERVATIONS
    MoreDestination.LOYALTY -> Routes.MORE_LOYALTY
    MoreDestination.POS -> Routes.MORE_POS
    MoreDestination.RESTAURANTS -> Routes.MORE_RESTAURANTS
    MoreDestination.SUPPLIERS -> Routes.MORE_SUPPLIERS
    MoreDestination.EMPLOYEES -> Routes.MORE_EMPLOYEES
    MoreDestination.SYNC -> Routes.MORE_SYNC
    MoreDestination.BACKUP -> Routes.MORE_BACKUP
    MoreDestination.SETTINGS -> Routes.MORE_SETTINGS
    MoreDestination.PROFILE -> Routes.MORE_PROFILE
    MoreDestination.SHIFT -> Routes.MORE_SHIFT
    MoreDestination.KITCHEN_BOARD -> Routes.MORE_KITCHEN_BOARD
    MoreDestination.STOP_GO -> Routes.MORE_STOP_GO
    MoreDestination.WAITER -> Routes.MORE_WAITER
    MoreDestination.WRITE_OFFS -> Routes.MORE_WRITE_OFFS
    MoreDestination.PURCHASES -> Routes.MORE_PURCHASES
    MoreDestination.KITCHEN_MODE -> Routes.MORE_KITCHEN_MODE
    MoreDestination.SEARCH -> Routes.MORE_SEARCH
}
