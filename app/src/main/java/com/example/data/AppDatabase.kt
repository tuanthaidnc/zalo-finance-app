package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        WalletEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        TransactionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zalo_finance_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.walletDao(), database.categoryDao())
                }
            }
        }

        suspend fun populateDatabase(walletDao: WalletDao, categoryDao: CategoryDao) {
            // Seed ví mặc định
            walletDao.insertWallet(
                WalletEntity(id = 1L, name = "Ví ZaloPay", balance = 0.0, icon = "ic_wallet_zalopay")
            )
            walletDao.insertWallet(
                WalletEntity(id = 2L, name = "Ví Tiền Mặt", balance = 0.0, icon = "ic_wallet_cash")
            )

            // Seed danh mục mặc định
            // Chi tiêu (EXPENSE)
            categoryDao.insertCategory(CategoryEntity(id = 1L, name = "Ăn uống", type = "EXPENSE", icon = "ic_category_food"))
            categoryDao.insertCategory(CategoryEntity(id = 2L, name = "Mua sắm", type = "EXPENSE", icon = "ic_category_shopping"))
            categoryDao.insertCategory(CategoryEntity(id = 3L, name = "Hóa đơn", type = "EXPENSE", icon = "ic_category_bills"))
            categoryDao.insertCategory(CategoryEntity(id = 4L, name = "Di chuyển", type = "EXPENSE", icon = "ic_category_transport"))

            // Danh mục con Chi tiêu
            categoryDao.insertCategory(CategoryEntity(id = 5L, name = "Đi chợ", type = "EXPENSE", icon = "ic_category_groceries", parentId = 1L))
            categoryDao.insertCategory(CategoryEntity(id = 6L, name = "Nhà hàng", type = "EXPENSE", icon = "ic_category_restaurant", parentId = 1L))
            categoryDao.insertCategory(CategoryEntity(id = 7L, name = "Quần áo", type = "EXPENSE", icon = "ic_category_clothes", parentId = 2L))
            categoryDao.insertCategory(CategoryEntity(id = 8L, name = "Thiết bị", type = "EXPENSE", icon = "ic_category_devices", parentId = 2L))
            categoryDao.insertCategory(CategoryEntity(id = 9L, name = "Điện", type = "EXPENSE", icon = "ic_category_electricity", parentId = 3L))
            categoryDao.insertCategory(CategoryEntity(id = 10L, name = "Nước", type = "EXPENSE", icon = "ic_category_water", parentId = 3L))
            categoryDao.insertCategory(CategoryEntity(id = 11L, name = "Internet", type = "EXPENSE", icon = "ic_category_internet", parentId = 3L))
            categoryDao.insertCategory(CategoryEntity(id = 12L, name = "Xăng xe", type = "EXPENSE", icon = "ic_category_gas", parentId = 4L))
            categoryDao.insertCategory(CategoryEntity(id = 13L, name = "Taxi", type = "EXPENSE", icon = "ic_category_taxi", parentId = 4L))

            // Thu nhập (INCOME)
            categoryDao.insertCategory(CategoryEntity(id = 14L, name = "Lương", type = "INCOME", icon = "ic_category_salary"))
            categoryDao.insertCategory(CategoryEntity(id = 15L, name = "Thưởng", type = "INCOME", icon = "ic_category_bonus"))
            categoryDao.insertCategory(CategoryEntity(id = 16L, name = "Thu nhập khác", type = "INCOME", icon = "ic_category_other_income"))

            // Chuyển khoản (TRANSFER)
            categoryDao.insertCategory(CategoryEntity(id = 17L, name = "Chuyển ví", type = "TRANSFER", icon = "ic_category_transfer"))
        }
    }
}
