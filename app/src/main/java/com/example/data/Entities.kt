package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val description: String,
    val category: String,
    val date: Long = System.currentTimeMillis(),
    val bankAccountName: String = "Manual",
    val isRecurring: Boolean = false,
    val isPending: Boolean = false
)

@Entity(tableName = "custom_categories")
data class CustomCategory(
    @PrimaryKey val name: String,
    val iconName: String, // e.g., "fastfood", "directions_car", etc.
    val colorHex: String
)

@Entity(tableName = "recurring_reminders")
data class RecurringReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val frequency: String, // "Weekly", "Monthly", "Yearly"
    val nextDueDate: Long,
    val isActive: Boolean = true
)

@Entity(tableName = "security_config")
data class SecurityConfig(
    @PrimaryKey val id: Int = 1,
    val isPinEnabled: Boolean = false,
    val hashedPin: String = "",
    val isTwoFactorEnabled: Boolean = false,
    val emailAddress: String = "",
    val isCloudSyncEnabled: Boolean = false,
    val lastSyncTimestamp: Long = 0L
)

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val payee: String,
    val amount: Double,
    val dueDate: Long,
    val category: String,
    val reminderDaysBefore: Int = 3,
    val isPaid: Boolean = false,
    val accountName: String = "Manual",
    val notes: String = ""
)
