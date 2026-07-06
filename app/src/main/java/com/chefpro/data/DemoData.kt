package com.chefpro.data

import com.chefpro.model.ChefProState
import com.chefpro.model.Delivery
import com.chefpro.model.Dish
import com.chefpro.model.Employee
import com.chefpro.model.InventoryItem
import com.chefpro.model.RecipeIngredient
import com.chefpro.model.Supplier
import com.chefpro.model.UserProfile

object DemoData {

    fun populateDemoEmployees(): List<Employee> = listOf(
        Employee(
            name = "Иван Петров",
            position = "Шеф-повар",
            phone = "+47 000 00 000",
            pin = "1111",
            permissions = listOf(
                "Техкарты", "Склад", "Приемка", "Списания", "Отчеты", "Настройки",
            ),
        ),
        Employee(
            name = "Анна Смирнова",
            position = "Су-шеф",
            phone = "+47 111 11 111",
            pin = "2222",
            permissions = listOf("Техкарты", "Склад", "Списания", "Отчеты"),
        ),
        Employee(
            name = "Олег Иванов",
            position = "Кладовщик",
            phone = "+47 222 22 222",
            pin = "3333",
            permissions = listOf("Склад", "Приемка", "Списания"),
        ),
        Employee(
            name = "Мария Кузнецова",
            position = "Администратор",
            phone = "+47 333 33 333",
            pin = "4444",
            permissions = listOf("Отчеты", "Настройки"),
        ),
    )

    fun loadExtendedDemoData(
        existing: ChefProState = ChefProState(),
        nowMillis: Long = System.currentTimeMillis(),
    ): ChefProState {
        val inventoryItems = listOf(
            InventoryItem(name = "Мука", category = "Бакалея", quantity = 10.0, unit = "кг", minQuantity = 3.0, pricePerUnit = 1.2),
            InventoryItem(name = "Молоко", category = "Молочные продукты", quantity = 8.0, unit = "л", minQuantity = 2.0, pricePerUnit = 1.5),
            InventoryItem(name = "Яйца", category = "Молочные продукты", quantity = 30.0, unit = "шт", minQuantity = 12.0, pricePerUnit = 0.4),
            InventoryItem(name = "Масло сливочное", category = "Молочные продукты", quantity = 2.0, unit = "кг", minQuantity = 0.5, pricePerUnit = 12.0),
            InventoryItem(name = "Говядина", category = "Мясо", quantity = 5.0, unit = "кг", minQuantity = 2.0, pricePerUnit = 22.0),
            InventoryItem(name = "Помидоры", category = "Овощи", quantity = 4.0, unit = "кг", minQuantity = 1.0, pricePerUnit = 2.5),
            InventoryItem(name = "Сыр", category = "Молочные продукты", quantity = 2.0, unit = "кг", minQuantity = 0.5, pricePerUnit = 9.5),
            InventoryItem(name = "Сахар", category = "Бакалея", quantity = 5.0, unit = "кг", minQuantity = 1.0, pricePerUnit = 1.0),
            InventoryItem(name = "Рис", category = "Бакалея", quantity = 12.0, unit = "кг", minQuantity = 5.0, pricePerUnit = 2.2),
            InventoryItem(name = "Лосось", category = "Рыба", quantity = 4.0, unit = "кг", minQuantity = 3.0, pricePerUnit = 18.5),
            InventoryItem(name = "Курица", category = "Мясо", quantity = 8.0, unit = "кг", minQuantity = 5.0, pricePerUnit = 6.8),
            InventoryItem(name = "Нори", category = "Суши", quantity = 50.0, unit = "шт", minQuantity = 10.0, pricePerUnit = 0.25),
            InventoryItem(name = "Салат", category = "Овощи", quantity = 6.0, unit = "кг", minQuantity = 2.0, pricePerUnit = 3.1),
            InventoryItem(name = "Соус", category = "Соусы", quantity = 3.0, unit = "кг", minQuantity = 1.0, pricePerUnit = 5.4),
            InventoryItem(name = "Маскарпоне", category = "Молочные продукты", quantity = 1.5, unit = "кг", minQuantity = 0.5, pricePerUnit = 14.0),
            InventoryItem(name = "Кофе эспрессо", category = "Напитки", quantity = 1.0, unit = "л", minQuantity = 0.3, pricePerUnit = 6.0),
            InventoryItem(name = "Тесто для пиццы", category = "Бакалея", quantity = 3.0, unit = "кг", minQuantity = 1.0, pricePerUnit = 4.0),
        )

        val dishes = listOf(
            Dish(
                name = "Борщ",
                category = "Супы",
                salePrice = 8.90,
                ingredients = listOf(
                    RecipeIngredient(productName = "Говядина", quantity = 200.0, unit = "г"),
                    RecipeIngredient(productName = "Помидоры", quantity = 100.0, unit = "г"),
                    RecipeIngredient(productName = "Масло сливочное", quantity = 20.0, unit = "г"),
                ),
                cookTime = 90,
            ),
            Dish(
                name = "Пицца Маргарита",
                category = "Пицца",
                salePrice = 13.50,
                ingredients = listOf(
                    RecipeIngredient(productName = "Тесто для пиццы", quantity = 250.0, unit = "г"),
                    RecipeIngredient(productName = "Помидоры", quantity = 150.0, unit = "г"),
                    RecipeIngredient(productName = "Сыр", quantity = 100.0, unit = "г"),
                ),
                cookTime = 20,
            ),
            Dish(
                name = "Тирамису",
                category = "Десерты",
                salePrice = 9.90,
                ingredients = listOf(
                    RecipeIngredient(productName = "Маскарпоне", quantity = 250.0, unit = "г"),
                    RecipeIngredient(productName = "Яйца", quantity = 3.0, unit = "шт"),
                    RecipeIngredient(productName = "Сахар", quantity = 80.0, unit = "г"),
                    RecipeIngredient(productName = "Кофе эспрессо", quantity = 100.0, unit = "мл"),
                ),
                cookTime = 30,
            ),
            Dish(
                name = "Стейк Рибай",
                category = "Горячее",
                salePrice = 32.00,
                ingredients = listOf(
                    RecipeIngredient(productName = "Говядина", quantity = 350.0, unit = "г"),
                    RecipeIngredient(productName = "Масло сливочное", quantity = 30.0, unit = "г"),
                ),
                cookTime = 15,
            ),
            Dish(
                name = "Паста Карбонара",
                category = "Паста",
                salePrice = 14.90,
                ingredients = listOf(
                    RecipeIngredient(productName = "Яйца", quantity = 2.0, unit = "шт"),
                    RecipeIngredient(productName = "Сыр", quantity = 60.0, unit = "г"),
                    RecipeIngredient(productName = "Масло сливочное", quantity = 20.0, unit = "г"),
                ),
                cookTime = 20,
            ),
            Dish(
                name = "Филадельфия ролл",
                category = "Суши",
                salePrice = 14.90,
                ingredients = listOf(
                    RecipeIngredient(productName = "Рис", quantity = 120.0, unit = "г"),
                    RecipeIngredient(productName = "Лосось", quantity = 60.0, unit = "г"),
                    RecipeIngredient(productName = "Сыр", quantity = 35.0, unit = "г"),
                    RecipeIngredient(productName = "Нори", quantity = 1.0, unit = "шт"),
                ),
            ),
            Dish(
                name = "Цезарь с курицей",
                category = "Салаты",
                salePrice = 12.50,
                ingredients = listOf(
                    RecipeIngredient(productName = "Курица", quantity = 120.0, unit = "г"),
                    RecipeIngredient(productName = "Салат", quantity = 80.0, unit = "г"),
                    RecipeIngredient(productName = "Соус", quantity = 40.0, unit = "г"),
                    RecipeIngredient(productName = "Сыр", quantity = 20.0, unit = "г"),
                ),
            ),
        )

        val suppliers = listOf(
            Supplier(
                name = "ООО Продукты",
                phone = "+7 800 555 0101",
                email = "info@ooo-produkty.ru",
                notes = "Основной поставщик сухих продуктов",
            ),
            Supplier(
                name = "ИП Молочник",
                phone = "+7 800 555 0202",
                email = "molochnik@mail.ru",
                notes = "Молочная продукция и яйца",
            ),
        )

        val employees = existing.employees.ifEmpty {
            listOf(
                Employee(
                    name = "Иван Иванов",
                    position = "Шеф-повар",
                    phone = "+7 900 000 0001",
                    pin = "1111",
                    permissions = listOf(
                        "Техкарты", "Склад", "Приемка", "Списания", "Отчеты", "Настройки",
                    ),
                ),
            )
        }

        val deliveries = listOf(
            Delivery(
                supplier = "ООО Продукты",
                productName = "Мука",
                quantity = 10.0,
                unit = "кг",
                price = 12.0,
                date = nowMillis,
                acceptedBy = "Иван Иванов",
            ),
        )

        return existing.copy(
            restaurantName = existing.restaurantName.ifBlank { "Demo Restaurant" },
            profile = existing.profile.copy(
                name = existing.profile.name.ifBlank { "Иван Петров" },
                position = existing.profile.position.ifBlank { "Шеф-повар" },
            ),
            inventoryItems = inventoryItems,
            dishes = dishes,
            suppliers = suppliers,
            employees = employees,
            deliveries = deliveries,
            writeOffs = emptyList(),
            productions = emptyList(),
        )
    }

    fun populateDemoData(state: ChefProState): ChefProState {
        if (state.dishes.isNotEmpty() || state.inventoryItems.isNotEmpty()) {
            return state
        }
        return loadExtendedDemoData(state)
    }

    fun resetDemoData(): ChefProState {
        val employees = populateDemoEmployees()
        return loadExtendedDemoData(
            ChefProState(
                employees = employees,
                profile = UserProfile(
                    name = "Иван Петров",
                    position = "Шеф-повар",
                    phone = "+47 000 00 000",
                    permissions = listOf(
                        "Техкарты", "Склад", "Приемка", "Списания", "Отчеты", "Настройки",
                    ),
                ),
            ),
        )
    }
}
