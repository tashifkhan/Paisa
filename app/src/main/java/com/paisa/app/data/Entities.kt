package com.paisa.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // Cash, Bank Account, UPI, Credit Card, Debit Card, Wallet, Other
    val openingBalance: Double,
    val currentBalance: Double,
    val icon: String,
    val color: String
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // income or expense
    val icon: String,
    val color: String,
    val isDefault: Boolean = false,
    val orderIndex: Int = 0
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("transactionDate"),
        Index("categoryId"),
        Index("accountId")
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // income or expense
    val categoryId: Int,
    val accountId: Int,
    val note: String,
    val transactionDate: String, // ISO-8601
    val createdAt: String,
    val updatedAt: String,
    val attachmentPath: String? = null,
    val tags: String = ""
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int?,
    val budgetAmount: Double,
    val month: Int,
    val year: Int,
    val budgetType: String = "MONTHLY",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val budgetName: String? = null
)

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: String,
    val status: String,
    val icon: String,
    val color: String
)

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("nextExecutionDate"),
        Index("categoryId"),
        Index("accountId")
    ]
)
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String,
    val categoryId: Int,
    val accountId: Int,
    val note: String,
    val frequency: String, // daily, weekly, monthly, yearly
    val nextExecutionDate: String,
    val enabled: Boolean = true
)

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val themeMode: String = "system", // system, light, dark
    val currency: String = "₹",
    val pinEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val colorPalette: String = "Default"
)
