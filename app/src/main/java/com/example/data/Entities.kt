package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val balance: Double,
    val currency: String = "VND",
    val icon: String,
    val isExcluded: Boolean = false
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String, // EXPENSE, INCOME, TRANSFER
    val icon: String,
    val parentId: Long? = null // Cấu trúc đa cấp cha-con
)

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val walletId: Long, // -1L đại diện cho tất cả các ví
    val categoryId: Long, // -1L đại diện cho tất cả các danh mục
    val amount: Double,
    val spent: Double = 0.0,
    val startDate: Long,
    val endDate: Long
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val walletId: Long,
    val categoryId: Long,
    val amount: Double,
    val title: String,
    val description: String = "",
    val timestamp: Long,
    val source: String = "Thủ công", // Thủ công, ZaloPay, ZaloPay (AI)
    val transferToWalletId: Long? = null, // ID ví nhận nếu là giao dịch chuyển khoản
    val withPerson: String = ""
)
