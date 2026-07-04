package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets")
    fun getAllWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets")
    suspend fun getAllWalletsSync(): List<WalletEntity>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletById(id: Long): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity): Long

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    @Delete
    suspend fun deleteWallet(wallet: WalletEntity)

    @Query("UPDATE wallets SET balance = balance + :amount WHERE id = :id")
    suspend fun updateBalance(id: Long, amount: Double)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesSync(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE type = :type")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("UPDATE budgets SET spent = :spent WHERE id = :id")
    suspend fun updateBudgetSpent(id: Long, spent: Double)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE categoryId = :categoryId 
          AND (:walletId = -1 OR walletId = :walletId)
          AND timestamp BETWEEN :startDate AND :endDate
    """)
    suspend fun getSpentForBudget(walletId: Long, categoryId: Long, startDate: Long, endDate: Long): Double?
}
