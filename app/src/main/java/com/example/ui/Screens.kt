package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryEntity
import com.example.data.TransactionEntity
import com.example.data.WalletEntity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.ZaloBlue

// --- MÀN HÌNH DANH SÁCH GIAO DỊCH ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel,
    onAddClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val selectedWalletId by viewModel.selectedWalletId.collectAsState()
    val selectedTimeFilter by viewModel.selectedTimeFilter.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = ZaloBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm giao dịch")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Phần Header: Số dư khả dụng
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Số dư ZaloPay khả dụng",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%,.0f đ", totalBalance),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZaloBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chọn Ví nhanh (LazyRow)
            Text(text = "Tài khoản Ví", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedWalletId == -1L,
                        onClick = { viewModel.selectWallet(-1L) },
                        label = { Text("Tất cả") }
                    )
                }
                items(wallets) { wallet ->
                    FilterChip(
                        selected = selectedWalletId == wallet.id,
                        onClick = { viewModel.selectWallet(wallet.id) },
                        label = { Text(wallet.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bộ lọc thời gian nhanh
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTimeFilter == "ALL",
                        onClick = { viewModel.selectTimeFilter("ALL") },
                        label = { Text("Tất cả") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTimeFilter == "THIS_MONTH",
                        onClick = { viewModel.selectTimeFilter("THIS_MONTH") },
                        label = { Text("Tháng này") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTimeFilter == "LAST_MONTH",
                        onClick = { viewModel.selectTimeFilter("LAST_MONTH") },
                        label = { Text("Tháng trước") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sổ giao dịch (Timeline View)
            Text(text = "Lịch sử giao dịch", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Chưa có giao dịch nào.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.forEach { (date, transactionsForDate) ->
                        item {
                            Text(
                                text = date,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        items(transactionsForDate) { transaction ->
                            TransactionItem(transaction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = transaction.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (transaction.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.description,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(ZaloBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = transaction.source.firstOrNull()?.toString() ?: "T",
                            color = ZaloBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = transaction.source, fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Số tiền
            val isExpense = transaction.transferToWalletId == null && transaction.amount > 0.0 // Tạm quy định trong Compose này
            val color = if (transaction.transferToWalletId != null) Color.Gray else if (isExpense) ExpenseRed else GrowthGreen
            val prefix = if (transaction.transferToWalletId != null) "" else if (isExpense) "-" else "+"

            Text(
                text = String.format("%s%,.0f đ", prefix, transaction.amount),
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// --- MÀN HÌNH THÊM GIAO DỊCH ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: AddEditTransactionViewModel,
    onBackClick: () -> Unit
) {
    val amount by viewModel.amount.collectAsState()
    val title by viewModel.title.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedWallet by viewModel.selectedWallet.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTransferWallet by viewModel.selectedTransferWallet.collectAsState()
    val isValid by viewModel.isValid.collectAsState()

    var showWalletDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showTransferWalletDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm giao dịch mới") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nhập số tiền lớn
            OutlinedTextField(
                value = amount,
                onValueChange = { viewModel.onAmountChange(it) },
                label = { Text("Số tiền") },
                placeholder = { Text("0") },
                textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ZaloBlue),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Nhập tiêu đề / Ghi chú
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("Ghi chú / Mô tả") },
                modifier = Modifier.fillMaxWidth()
            )

            // Chọn Ví
            Button(
                onClick = { showWalletDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = ZaloBlue)
            ) {
                Text(text = selectedWallet?.name ?: "Chọn Ví thanh toán")
            }

            // Chọn Danh mục
            Button(
                onClick = { showCategoryDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = ZaloBlue)
            ) {
                Text(text = selectedCategory?.name ?: "Chọn Danh mục phân loại")
            }

            // Chọn Ví nhận (nếu danh mục là chuyển khoản)
            if (selectedCategory?.type == "TRANSFER") {
                Button(
                    onClick = { showTransferWalletDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = ZaloBlue)
                ) {
                    Text(text = selectedTransferWallet?.name ?: "Chọn Ví nhận tiền")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nút Lưu
            Button(
                onClick = {
                    viewModel.saveTransaction {
                        onBackClick()
                    }
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ZaloBlue, contentColor = Color.White)
            ) {
                Text("Lưu giao dịch")
            }
        }
    }

    // Wallet Dialog
    if (showWalletDialog) {
        AlertDialog(
            onDismissRequest = { showWalletDialog = false },
            title = { Text("Chọn ví") },
            text = {
                LazyColumn {
                    items(wallets) { wallet ->
                        Text(
                            text = "${wallet.name} (Số dư: ${String.format("%,.0fđ", wallet.balance)})",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onWalletSelect(wallet)
                                    showWalletDialog = false
                                }
                                .padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Transfer Wallet Dialog
    if (showTransferWalletDialog) {
        AlertDialog(
            onDismissRequest = { showTransferWalletDialog = false },
            title = { Text("Chọn ví nhận") },
            text = {
                LazyColumn {
                    items(wallets) { wallet ->
                        if (wallet.id != selectedWallet?.id) {
                            Text(
                                text = wallet.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.onTransferWalletSelect(wallet)
                                        showTransferWalletDialog = false
                                    }
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Category Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Chọn danh mục") },
            text = {
                LazyColumn {
                    items(categories) { category ->
                        // Đẩy lùi đầu dòng nếu là danh mục con
                        val padding = if (category.parentId != null) 24.dp else 8.dp
                        Text(
                            text = if (category.parentId != null) "↳ ${category.name}" else category.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onCategorySelect(category)
                                    showCategoryDialog = false
                                }
                                .padding(start = padding, top = 12.dp, bottom = 12.dp, end = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

// --- MÀN HÌNH NGÂN SÁCH ---
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel
) {
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Lập ngân sách & Hạn mức", fontWeight = FontWeight.Bold, fontSize = 20.sp)

        if (budgets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Bạn chưa đặt hạn mức chi tiêu nào.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(budgets) { budget ->
                    val category = categories.find { it.id == budget.categoryId }
                    val percent = if (budget.amount > 0.0) (budget.spent / budget.amount) else 0.0
                    val color = if (percent >= 1.0) ExpenseRed else if (percent >= 0.8) Color(0xFFF39C12) else GrowthGreen

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Hạn mức: ${category?.name ?: "Tất cả"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = percent.toFloat().coerceAtMost(1f),
                                modifier = Modifier.fillMaxWidth(),
                                color = color,
                                trackColor = color.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = String.format("Đã tiêu: %,.0fđ", budget.spent),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = String.format("Hạn mức: %,.0fđ", budget.amount),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- MÀN HÌNH BÁO CÁO THỐNG KÊ (PIE CHART VẼ BẰNG CANVAS) ---
@Composable
fun ReportScreen(
    viewModel: ReportViewModel
) {
    val categoryShare by viewModel.categoryShare.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Báo cáo phân tích chi tiêu", fontWeight = FontWeight.Bold, fontSize = 20.sp)

        if (categoryShare.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Chưa có dữ liệu để lập báo cáo.", color = Color.Gray)
            }
        } else {
            // Vẽ biểu đồ tròn Pie Chart bằng Canvas
            val shares = categoryShare.toList()
            val colors = listOf(ZaloBlue, GrowthGreen, ExpenseRed, Color(0xFFF1C40F), Color(0xFF9B59B6), Color(0xFF1ABC9C))

            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .padding(16.dp)
            ) {
                var startAngle = 0f
                shares.forEachIndexed { index, (_, share) ->
                    val sweepAngle = (share / 100 * 360).toFloat()
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = Size(size.width, size.height)
                    )
                    startAngle += sweepAngle
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bảng xếp hạng chi phí bên dưới
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shares.size) { index ->
                    val (name, share) = shares[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(colors[index % colors.size], CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = name, fontWeight = FontWeight.Bold)
                        }
                        Text(text = String.format("%.1f%%", share), color = Color.Gray)
                    }
                }
            }
        }
    }
}
