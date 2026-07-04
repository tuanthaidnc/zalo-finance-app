package com.example

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.ZaloFinanceTheme
import com.example.ui.theme.ZaloBlue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.io.InputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {
    private val parentJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + parentJob)
    private lateinit var appDatabase: AppDatabase

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var walletRepository: WalletRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var budgetRepository: BudgetRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Khởi tạo Database và Repositories
        appDatabase = AppDatabase.getDatabase(this, scope)
        transactionRepository = TransactionRepositoryImpl(appDatabase)
        walletRepository = WalletRepositoryImpl(appDatabase.walletDao())
        categoryRepository = CategoryRepositoryImpl(appDatabase.categoryDao())
        budgetRepository = BudgetRepositoryImpl(appDatabase.budgetDao())

        setContent {
            ZaloFinanceTheme {
                var currentScreen by remember { mutableStateOf("home") } // home, add, budgets, reports, settings
                var selectedTabIndex by remember { mutableIntStateOf(0) }

                // ViewModel Factory
                val listViewModel = remember {
                    TransactionListViewModel(transactionRepository, walletRepository)
                }
                val addViewModel = remember {
                    AddEditTransactionViewModel(transactionRepository, walletRepository, categoryRepository)
                }
                val budgetViewModel = remember {
                    BudgetViewModel(budgetRepository, categoryRepository, walletRepository, transactionRepository)
                }
                val reportViewModel = remember {
                    ReportViewModel(transactionRepository, categoryRepository)
                }

                val importState by listViewModel.importState.collectAsState()

                if (currentScreen == "add") {
                    AddEditTransactionScreen(
                        viewModel = addViewModel,
                        onBackClick = { currentScreen = "home" }
                    )
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = selectedTabIndex == 0,
                                    onClick = { selectedTabIndex = 0; currentScreen = "home" },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Tổng quan") },
                                    label = { Text("Tổng quan") }
                                )
                                NavigationBarItem(
                                    selected = selectedTabIndex == 1,
                                    onClick = { selectedTabIndex = 1; currentScreen = "budgets" },
                                    icon = { Icon(Icons.Default.Star, contentDescription = "Ngân sách") },
                                    label = { Text("Ngân sách") }
                                )
                                NavigationBarItem(
                                    selected = selectedTabIndex == 2,
                                    onClick = { selectedTabIndex = 2; currentScreen = "reports" },
                                    icon = { Icon(Icons.Default.List, contentDescription = "Báo cáo") },
                                    label = { Text("Báo cáo") }
                                )
                                NavigationBarItem(
                                    selected = selectedTabIndex == 3,
                                    onClick = { selectedTabIndex = 3; currentScreen = "settings" },
                                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Đồng bộ") },
                                    label = { Text("Đồng bộ") }
                                )
                            }
                        }
                    ) { paddingValues ->
                        Box(modifier = Modifier.padding(paddingValues)) {
                            when (currentScreen) {
                                "home" -> TransactionListScreen(
                                    viewModel = listViewModel,
                                    onAddClick = { currentScreen = "add" }
                                )
                                "budgets" -> BudgetScreen(viewModel = budgetViewModel)
                                "reports" -> ReportScreen(viewModel = reportViewModel)
                                "settings" -> SettingsScreen(
                                    onEnableSyncClick = { checkAndRequestNotificationPermission() },
                                    importState = importState,
                                    onImportClick = { stream -> listViewModel.importFromCsv(stream) },
                                    onResetImportState = { listViewModel.resetImportState() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        val cn = ComponentName(this, "com.example.service.ZaloPayNotificationService")
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val enabled = flat != null && flat.contains(cn.flattenToString())
        if (!enabled) {
            // Mở màn hình cấp quyền Notification Listener của hệ thống
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        parentJob.cancel()
    }
}

// --- MÀN HÌNH ĐỒNG BỘ CÀI ĐẶT ---
@Composable
fun SettingsScreen(
    onEnableSyncClick: () -> Unit,
    importState: String,
    onImportClick: (InputStream) -> Unit,
    onResetImportState: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                if (inputStream != null) {
                    onImportClick(inputStream)
                }
            } catch (e: Exception) {
                // Xử lý lỗi mở stream nếu có
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Đồng bộ ZaloPay", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ứng dụng hỗ trợ đồng bộ tự động biến động số dư từ ZaloPay thông qua thông báo đẩy của hệ thống.",
            color = Color.Gray,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onEnableSyncClick,
            colors = ButtonDefaults.buttonColors(containerColor = ZaloBlue)
        ) {
            Text("Bật quyền đọc thông báo ZaloPay")
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Nhập dữ liệu cũ", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Bạn có thể chuyển dữ liệu lịch sử từ Money Lover sang ZaloFinance bằng cách chọn tệp CSV đã xuất từ Money Lover.",
            color = Color.Gray,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { launcher.launch("*/*") }, // Chọn file bất kỳ để hỗ trợ mọi kiểu đuôi CSV
            colors = ButtonDefaults.buttonColors(containerColor = GrowthGreen)
        ) {
            Text("Nhập tệp CSV từ Money Lover")
        }
    }

    // Hiển thị trạng thái Import dữ liệu
    when (importState) {
        "LOADING" -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Đang xử lý") },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = ZaloBlue)
                        Text("Đang nhập dữ liệu giao dịch từ Money Lover...")
                    }
                },
                confirmButton = {}
            )
        }
        "SUCCESS" -> {
            AlertDialog(
                onDismissRequest = onResetImportState,
                title = { Text("Thành công") },
                text = { Text("Toàn bộ dữ liệu ví và giao dịch cũ đã được đồng bộ thành công vào ZaloFinance!") },
                confirmButton = {
                    Button(onClick = onResetImportState) {
                        Text("Đóng")
                    }
                }
            )
        }
        "ERROR" -> {
            AlertDialog(
                onDismissRequest = onResetImportState,
                title = { Text("Thất bại") },
                text = { Text("Không thể nhập dữ liệu. Vui lòng kiểm tra lại định dạng tệp CSV của bạn.") },
                confirmButton = {
                    Button(onClick = onResetImportState) {
                        Text("Đóng")
                    }
                }
            )
        }
    }
}
