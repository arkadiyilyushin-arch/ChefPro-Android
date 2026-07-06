package com.chefpro.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.chefpro.ChefProApplication
import com.chefpro.R
import com.chefpro.domain.ChefProEngine
import com.chefpro.model.OrderStatus

class ChefProWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val app = context.applicationContext as ChefProApplication
            val state = app.repository.loadState()
            val lowStockCount = ChefProEngine.lowStockItems(state).size
            val kitchenOrdersCount = state.kitchenOrders.count { it.status != OrderStatus.READY }

            val views = RemoteViews(context.packageName, R.layout.chefpro_widget).apply {
                setTextViewText(R.id.widget_title, context.getString(R.string.widget_title))
                setTextViewText(R.id.widget_low_stock, lowStockCount.toString())
                setTextViewText(R.id.widget_kitchen_orders, kitchenOrdersCount.toString())
                setTextViewText(
                    R.id.widget_subtitle,
                    context.getString(R.string.widget_subtitle, state.restaurantName),
                )
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
