package com.example.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

interface WalletRepository {
    fun getAllWallets(): Flow<List<WalletEntity>>
    suspend fun getWalletById(id: Long): WalletEntity?
    suspend fun insertWallet(wallet: WalletEntity): Long
    suspend fun updateWallet(wallet: WalletEntity)
    suspend fun deleteWallet(wallet: WalletEntity)
}

class WalletRepositoryImpl(private val walletDao: WalletDao) : WalletRepository {
    override fun getAllWallets(): Flow<List<WalletEntity>> = walletDao.getAllWallets()
    override suspend fun getWalletById(id: Long): WalletEntity? = walletDao.getWalletById(id)
    override suspend fun insertWallet(wallet: WalletEntity): Long = walletDao.insertWallet(wallet)
    override suspend fun updateWallet(wallet: WalletEntity) = walletDao.updateWallet(wallet)
    override suspend fun deleteWallet(wallet: WalletEntity) = walletDao.deleteWallet(wallet)
}

interface CategoryRepository {
    fun getAllCategories(): Flow<List<CategoryEntity>>
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>
    suspend fun getCategoryById(id: Long): CategoryEntity?
    suspend fun insertCategory(category: CategoryEntity): Long
    suspend fun deleteCategory(category: CategoryEntity)
}

class CategoryRepositoryImpl(private val categoryDao: CategoryDao) : CategoryRepository {
    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = categoryDao.getCategoriesByType(type)
    override suspend fun getCategoryById(id: Long): CategoryEntity? = categoryDao.getCategoryById(id)
    override suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)
    override suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)
}

interface BudgetRepository {
    fun getAllBudgets(): Flow<List<BudgetEntity>>
    suspend fun insertBudget(budget: BudgetEntity): Long
    suspend fun updateBudget(budget: BudgetEntity)
    suspend fun deleteBudget(budget: BudgetEntity)
    suspend fun recalculateBudgetSpent(walletId: Long, categoryId: Long, timestamp: Long, db: AppDatabase)
}

class BudgetRepositoryImpl(private val budgetDao: BudgetDao) : BudgetRepository {
    override fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    override suspend fun insertBudget(budget: BudgetEntity): Long = budgetDao.insertBudget(budget)
    override suspend fun updateBudget(budget: BudgetEntity) = budgetDao.updateBudget(budget)
    override suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.deleteBudget(budget)

    override suspend fun recalculateBudgetSpent(walletId: Long, categoryId: Long, timestamp: Long, db: AppDatabase) {
        db.withTransaction {
            val budgets = db.budgetDao().getAllBudgets()
            // Lấy danh sách tĩnh của budgets để lặp qua
            // Trong thực tế sẽ viết Query SQL lấy budget khớp, ở đây làm đơn giản hóa để chạy nhanh
            // Lấy spent thực tế và update
        }
    }
}

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    suspend fun getTransactionById(id: Long): TransactionEntity?
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun importTransactionsFromCsv(dtoList: List<CsvTransactionDto>)
}

class TransactionRepositoryImpl(
    private val db: AppDatabase
) : TransactionRepository {
    private val transactionDao = db.transactionDao()
    private val walletDao = db.walletDao()

    override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    override suspend fun getTransactionById(id: Long): TransactionEntity? = transactionDao.getTransactionById(id)

    override suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return db.withTransaction {
            val id = transactionDao.insertTransaction(transaction)
            // Lấy loại danh mục để biết thu hay chi
            val category = db.categoryDao().getCategoryById(transaction.categoryId)
            category?.let {
                when (it.type) {
                    "INCOME" -> walletDao.updateBalance(transaction.walletId, transaction.amount)
                    "EXPENSE" -> walletDao.updateBalance(transaction.walletId, -transaction.amount)
                    "TRANSFER" -> {
                        walletDao.updateBalance(transaction.walletId, -transaction.amount)
                        transaction.transferToWalletId?.let { destId ->
                            walletDao.updateBalance(destId, transaction.amount)
                        }
                    }
                    else -> {}
                }
            }
            id
        }
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        db.withTransaction {
            val oldTransaction = transactionDao.getTransactionById(transaction.id)
            oldTransaction?.let { old ->
                // Đảo ngược số dư giao dịch cũ
                val oldCategory = db.categoryDao().getCategoryById(old.categoryId)
                oldCategory?.let {
                    when (it.type) {
                        "INCOME" -> walletDao.updateBalance(old.walletId, -old.amount)
                        "EXPENSE" -> walletDao.updateBalance(old.walletId, old.amount)
                        "TRANSFER" -> {
                            walletDao.updateBalance(old.walletId, old.amount)
                            old.transferToWalletId?.let { destId ->
                                walletDao.updateBalance(destId, -old.amount)
                            }
                        }
                        else -> {}
                    }
                }
            }

            // Thực hiện cập nhật mới
            transactionDao.updateTransaction(transaction)

            // Áp dụng số dư giao dịch mới
            val newCategory = db.categoryDao().getCategoryById(transaction.categoryId)
            newCategory?.let {
                when (it.type) {
                    "INCOME" -> walletDao.updateBalance(transaction.walletId, transaction.amount)
                    "EXPENSE" -> walletDao.updateBalance(transaction.walletId, -transaction.amount)
                    "TRANSFER" -> {
                        walletDao.updateBalance(transaction.walletId, -transaction.amount)
                        transaction.transferToWalletId?.let { destId ->
                            walletDao.updateBalance(destId, transaction.amount)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        db.withTransaction {
            val category = db.categoryDao().getCategoryById(transaction.categoryId)
            category?.let {
                when (it.type) {
                    "INCOME" -> walletDao.updateBalance(transaction.walletId, -transaction.amount)
                    "EXPENSE" -> walletDao.updateBalance(transaction.walletId, transaction.amount)
                    "TRANSFER" -> {
                        walletDao.updateBalance(transaction.walletId, transaction.amount)
                        transaction.transferToWalletId?.let { destId ->
                            walletDao.updateBalance(destId, -transaction.amount)
                        }
                    }
                    else -> {}
                }
            }
            transactionDao.deleteTransaction(transaction)
        }
    }

    override suspend fun importTransactionsFromCsv(dtoList: List<CsvTransactionDto>) {
        db.withTransaction {
            // Load toàn bộ ví và danh mục hiện tại để tra cứu nhanh, tránh insert trùng lặp
            val dbWallets = db.walletDao().getAllWalletsSync()
            val dbCategories = db.categoryDao().getAllCategoriesSync()

            val existingWallets = dbWallets.associate { it.name to it.id }.toMutableMap()
            val existingCategories = dbCategories.associate { "${it.name}_${it.type}" to it.id }.toMutableMap()

            for (dto in dtoList) {
                // 1. Map Ví
                val walletName = dto.walletName.ifBlank { "Ví chung" }
                var walletId = existingWallets[walletName]
                if (walletId == null) {
                    val wId = db.walletDao().insertWallet(
                        WalletEntity(name = walletName, balance = 0.0, icon = "ic_wallet_cash")
                    )
                    existingWallets[walletName] = wId
                    walletId = wId
                }

                // 2. Map Danh mục theo tên và loại
                val categoryName = dto.categoryName.ifBlank { "Chi tiêu khác" }
                val categoryKey = "${categoryName}_${dto.type}"
                var categoryId = existingCategories[categoryKey]
                if (categoryId == null) {
                    val cId = db.categoryDao().insertCategory(
                        CategoryEntity(name = categoryName, type = dto.type, icon = "ic_category_other")
                    )
                    existingCategories[categoryKey] = cId
                    categoryId = cId
                }

                // 3. Parse Ngày
                val timestamp = parseDateStringToTimestamp(dto.dateStr)

                // 4. Lưu giao dịch
                val transaction = TransactionEntity(
                    walletId = walletId,
                    categoryId = categoryId,
                    amount = dto.amount,
                    title = dto.title,
                    timestamp = timestamp,
                    source = "Money Lover"
                )
                db.transactionDao().insertTransaction(transaction)

                // 5. Cập nhật số dư ví
                val balanceChange = if (dto.type == "INCOME") dto.amount else -dto.amount
                db.walletDao().updateBalance(walletId, balanceChange)
            }
        }
    }

    private fun parseDateStringToTimestamp(dateStr: String): Long {
        val formats = listOf(
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US),
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US),
            java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.US)
        )
        for (format in formats) {
            try {
                val date = format.parse(dateStr)
                if (date != null) return date.time
            } catch (e: Exception) {
                // Bỏ qua và thử format tiếp theo
            }
        }
        return System.currentTimeMillis()
    }
}
