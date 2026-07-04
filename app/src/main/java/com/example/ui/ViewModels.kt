package com.example.ui

import java.io.InputStream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

// --- VIEWMODEL DANH SÁCH GIAO DỊCH ---
class TransactionListViewModel(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _selectedWalletId = MutableStateFlow(-1L) // -1L đại diện cho tất cả các ví
    val selectedWalletId: StateFlow<Long> = _selectedWalletId.asStateFlow()

    private val _selectedTimeFilter = MutableStateFlow("ALL") // ALL, THIS_MONTH, LAST_MONTH
    val selectedTimeFilter: StateFlow<String> = _selectedTimeFilter.asStateFlow()

    val wallets: StateFlow<List<WalletEntity>> = walletRepository.getAllWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double> = wallets.map { list ->
        list.filter { !it.isExcluded }.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val transactions: StateFlow<List<TransactionEntity>> = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI State: Lọc và Nhóm giao dịch theo ngày
    val uiState: StateFlow<Map<String, List<TransactionEntity>>> = combine(
        transactions, _selectedWalletId, _selectedTimeFilter
    ) { list, walletId, timeFilter ->
        var filtered = list
        if (walletId != -1L) {
            filtered = filtered.filter { it.walletId == walletId }
        }
        filtered = when (timeFilter) {
            "THIS_MONTH" -> filterByMonth(filtered, 0)
            "LAST_MONTH" -> filterByMonth(filtered, -1)
            else -> filtered
        }
        // Nhóm theo ngày (định dạng dd/MM/yyyy)
        filtered.groupBy { timestampToDateString(it.timestamp) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectWallet(id: Long) {
        _selectedWalletId.value = id
    }

    fun selectTimeFilter(filter: String) {
        _selectedTimeFilter.value = filter
    }

    private fun filterByMonth(list: List<TransactionEntity>, offset: Int): List<TransactionEntity> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, offset)
        val targetMonth = cal.get(Calendar.MONTH)
        val targetYear = cal.get(Calendar.YEAR)

        return list.filter {
            val tCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            tCal.get(Calendar.MONTH) == targetMonth && tCal.get(Calendar.YEAR) == targetYear
        }
    }

    private fun timestampToDateString(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
    }

    private val _importState = MutableStateFlow<String>("IDLE") // IDLE, LOADING, SUCCESS, ERROR
    val importState: StateFlow<String> = _importState.asStateFlow()

    fun importFromCsv(inputStream: InputStream) {
        viewModelScope.launch {
            _importState.value = "LOADING"
            try {
                val dtoList = CsvParser.parse(inputStream)
                if (dtoList.isNotEmpty()) {
                    transactionRepository.importTransactionsFromCsv(dtoList)
                    _importState.value = "SUCCESS"
                } else {
                    _importState.value = "ERROR"
                }
            } catch (e: Exception) {
                _importState.value = "ERROR"
            }
        }
    }

    fun resetImportState() {
        _importState.value = "IDLE"
    }
}

// --- VIEWMODEL THÊM/SỬA GIAO DỊCH ---
class AddEditTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _amount = MutableStateFlow("")
    val amount = _amount.asStateFlow()

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()

    private val _selectedWallet = MutableStateFlow<WalletEntity?>(null)
    val selectedWallet = _selectedWallet.asStateFlow()

    private val _selectedCategory = MutableStateFlow<CategoryEntity?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedTransferWallet = MutableStateFlow<WalletEntity?>(null)
    val selectedTransferWallet = _selectedTransferWallet.asStateFlow()

    private val _timestamp = MutableStateFlow(System.currentTimeMillis())
    val timestamp = _timestamp.asStateFlow()

    val wallets: StateFlow<List<WalletEntity>> = walletRepository.getAllWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isValid: StateFlow<Boolean> = combine(
        _amount, _selectedWallet, _selectedCategory, _selectedTransferWallet
    ) { amt, wallet, cat, transWallet ->
        val amtVal = MathUtils.evaluate(amt)
        val baseValid = amtVal > 0.0 && wallet != null && cat != null
        if (cat?.type == "TRANSFER") {
            baseValid && transWallet != null && transWallet.id != wallet?.id
        } else {
            baseValid
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onAmountChange(value: String) { _amount.value = value }
    fun onTitleChange(value: String) { _title.value = value }
    fun onWalletSelect(wallet: WalletEntity) { _selectedWallet.value = wallet }
    fun onCategorySelect(category: CategoryEntity) { _selectedCategory.value = category }
    fun onTransferWalletSelect(wallet: WalletEntity) { _selectedTransferWallet.value = wallet }
    fun onDateChange(time: Long) { _timestamp.value = time }

    fun saveTransaction(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (isValid.value) {
                val amtVal = MathUtils.evaluate(_amount.value)
                val transaction = TransactionEntity(
                    walletId = _selectedWallet.value!!.id,
                    categoryId = _selectedCategory.value!!.id,
                    amount = amtVal,
                    title = _title.value.ifBlank { _selectedCategory.value!!.name },
                    timestamp = _timestamp.value,
                    transferToWalletId = if (_selectedCategory.value!!.type == "TRANSFER") _selectedTransferWallet.value?.id else null
                )
                transactionRepository.insertTransaction(transaction)
                onSuccess()
            }
        }
    }
}

// --- VIEWMODEL NGÂN SÁCH ---
class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val budgets: StateFlow<List<BudgetEntity>> = budgetRepository.getAllBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wallets: StateFlow<List<WalletEntity>> = walletRepository.getAllWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createBudget(walletId: Long, categoryId: Long, amount: Double, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            budgetRepository.insertBudget(
                BudgetEntity(
                    walletId = walletId,
                    categoryId = categoryId,
                    amount = amount,
                    startDate = startDate,
                    endDate = endDate
                )
            )
        }
    }
}

// --- VIEWMODEL BÁO CÁO THỐNG KÊ ---
class ReportViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val transactions: StateFlow<List<TransactionEntity>> = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Phân bổ chi tiêu (%)
    val categoryShare: StateFlow<Map<String, Double>> = combine(
        transactions, categories
    ) { list, cats ->
        val expenseList = list.filter {
            val cat = cats.find { c -> c.id == it.categoryId }
            cat?.type == "EXPENSE"
        }
        val totalExpense = expenseList.sumOf { it.amount }
        if (totalExpense == 0.0) emptyMap()
        else {
            expenseList.groupBy {
                val cat = cats.find { c -> c.id == it.categoryId }
                cat?.name ?: "Khác"
            }.mapValues { (_, transactions) ->
                (transactions.sumOf { it.amount } / totalExpense) * 100
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}
