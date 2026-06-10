package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class FinanceRepository(private val financeDao: FinanceDao) {

    val allTransactions: Flow<List<Transaction>> = financeDao.getAllTransactions()
    val allCategories: Flow<List<CustomCategory>> = financeDao.getAllCategories()
    val allReminders: Flow<List<RecurringReminder>> = financeDao.getAllReminders()
    val allBills: Flow<List<Bill>> = financeDao.getAllBills()
    val securityConfig: Flow<SecurityConfig?> = financeDao.getSecurityConfig()

    suspend fun insertTransaction(transaction: Transaction) {
        financeDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        financeDao.deleteTransaction(transaction)
    }

    suspend fun clearAllTransactions() {
        financeDao.clearAllTransactions()
    }

    suspend fun insertCategory(category: CustomCategory) {
        financeDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: CustomCategory) {
        financeDao.deleteCategory(category)
    }

    suspend fun insertReminder(reminder: RecurringReminder) {
        financeDao.insertReminder(reminder)
    }

    suspend fun deleteReminder(reminder: RecurringReminder) {
        financeDao.deleteReminder(reminder)
    }

    suspend fun insertBill(bill: Bill) {
        financeDao.insertBill(bill)
    }

    suspend fun deleteBill(bill: Bill) {
        financeDao.deleteBill(bill)
    }

    suspend fun updateSecurityConfig(config: SecurityConfig) {
        financeDao.insertSecurityConfig(config)
    }

    suspend fun prepopulateDefaultCategories() {
        val current = allCategories.firstOrNull() ?: emptyList()
        if (current.isEmpty()) {
            val defaults = listOf(
                CustomCategory("Food & Dining", "fastfood", "#FF9800"),
                CustomCategory("Rent & Housing", "home", "#3F51B5"),
                CustomCategory("Transport & Gas", "directions_car", "#009688"),
                CustomCategory("Utilities", "bolt", "#9C27B0"),
                CustomCategory("Entertainment", "sports_esports", "#E91E63"),
                CustomCategory("Shopping", "shopping_bag", "#4CAF50"),
                CustomCategory("Healthcare", "medical_services", "#F44336"),
                CustomCategory("Income", "payments", "#00BCD4")
            )
            for (category in defaults) {
                financeDao.insertCategory(category)
            }
        }
    }

    suspend fun prepopulateInitialTransactions() {
        val current = allTransactions.firstOrNull() ?: emptyList()
        if (current.isEmpty()) {
            val now = System.currentTimeMillis()
            val day = 24 * 60 * 60 * 1000L
            val samples = listOf(
                Transaction(amount = 2450.00, description = "Payroll Direct Deposit", category = "Income", date = now - day * 1, bankAccountName = "Chase Checking", isPending = false),
                Transaction(amount = -14.50, description = "Starbucks Coffee", category = "Food & Dining", date = now - day * 2, bankAccountName = "Chase Checking", isPending = false),
                Transaction(amount = -85.00, description = "Chevron Gasoline", category = "Transport & Gas", date = now - day * 3, bankAccountName = "Chase Checking", isPending = false),
                Transaction(amount = -1200.00, description = "Monthly House Rent", category = "Rent & Housing", date = now - day * 4, bankAccountName = "Chase Checking", isPending = false),
                Transaction(amount = -95.15, description = "Safeway Supermarket", category = "Food & Dining", date = now - day * 5, bankAccountName = "Chase Checking", isPending = false),
                Transaction(amount = -14.99, description = "Netflix Subscription", category = "Entertainment", date = now - day * 6, bankAccountName = "Chase Checking", isPending = false, isRecurring = true),
                Transaction(amount = -45.00, description = "Comcast Broadband", category = "Utilities", date = now - day * 7, bankAccountName = "Chase Checking", isPending = false, isRecurring = true)
            )
            financeDao.insertTransactions(samples)
        }
    }

    suspend fun prepopulateInitialBills() {
        val current = allBills.firstOrNull() ?: emptyList()
        if (current.isEmpty()) {
            val now = System.currentTimeMillis()
            val day = 24 * 60 * 60 * 1000L
            val samples = listOf(
                Bill(payee = "Comcast Broadband", amount = 75.00, dueDate = now + day * 3, category = "Utilities", reminderDaysBefore = 3, isPaid = false, notes = "Automatic billing to checkings"),
                Bill(payee = "PG&E Electric & Gas", amount = 142.50, dueDate = now + day * 6, category = "Utilities", reminderDaysBefore = 5, isPaid = false, notes = "Estimated bill"),
                Bill(payee = "Chase Credit Card Payment", amount = 250.00, dueDate = now + day * 10, category = "Shopping", reminderDaysBefore = 3, isPaid = false, notes = "Minimum payment due"),
                Bill(payee = "Rent Payment", amount = 1200.00, dueDate = now - day * 2, category = "Rent & Housing", reminderDaysBefore = 3, isPaid = true, notes = "Paid via bank wire")
            )
            for (bill in samples) {
                financeDao.insertBill(bill)
            }
        }
    }
}
