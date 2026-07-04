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

    var isKeyboardVisible by remember { mutableStateOf(false) }
    var expr by remember { mutableStateOf(amount) }

    // Đồng bộ lại expr khi amount từ viewModel thay đổi bên ngoài
    LaunchedEffect(amount) {
        if (amount != expr) {
            expr = amount
        }
    }

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
        },
        bottomBar = {
            if (isKeyboardVisible) {
                CustomCalculatorKeyboard(
                    onKeyPress = { key ->
                        if (expr == "0" || expr.isEmpty()) {
                            expr = if (key != "+" && key != "-" && key != "*" && key != "/") key else "0$key"
                        } else {
                            // Chặn viết liên tiếp các phép toán
                            val lastChar = expr.lastOrNull()
                            if (lastChar != null && lastChar in listOf('+', '-', '*', '/') && key in listOf("+", "-", "*", "/")) {
                                expr = expr.dropLast(1) + key
                            } else {
                                expr += key
                            }
                        }
                        viewModel.onAmountChange(expr)
                    },
                    onBackspace = {
                        if (expr.isNotEmpty()) {
                            expr = expr.dropLast(1)
                        }
                        viewModel.onAmountChange(expr.ifEmpty { "0" })
                    },
                    onClear = {
                        expr = "0"
                        viewModel.onAmountChange("0")
                    },
                    onDone = {
                        val result = evaluateMathExpression(expr)
                        expr = if (result % 1 == 0.0) {
                            result.toLong().toString()
                        } else {
                            result.toString()
                        }
                        viewModel.onAmountChange(expr)
                        isKeyboardVisible = false
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nhập số tiền lớn (Click hiện Custom Keyboard, chặn phím ảo hệ thống)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isKeyboardVisible = true }
            ) {
                OutlinedTextField(
                    value = expr,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false, // Vô hiệu hóa để click chuyển sang Box nhận sự kiện
                    label = { Text("Số tiền") },
                    placeholder = { Text("0") },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = ZaloBlue,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth()
                )
            }

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

// --- BÀN PHÍM TÙY CHỈNH KIỂU MONEY LOVER ---
@Composable
fun CustomCalculatorKeyboard(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit
) {
    val keys = listOf(
        listOf("7", "8", "9", "/"),
        listOf("4", "5", "6", "*"),
        listOf("1", "2", "3", "-"),
        listOf("0", "000", "⌫", "+"),
        listOf("C", "✓")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val weight = if (key == "✓") 3f else 1f
                    val containerColor = if (key == "✓") ZaloBlue else if (key in listOf("+", "-", "*", "/", "⌫", "C")) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    val contentColor = if (key == "✓") Color.White else MaterialTheme.colorScheme.onSurface

                    Button(
                        onClick = {
                            when (key) {
                                "⌫" -> onBackspace()
                                "C" -> onClear()
                                "✓" -> onDone()
                                else -> onKeyPress(key)
                            }
                        },
                        modifier = Modifier
                            .weight(weight)
                            .height(54.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = containerColor,
                            contentColor = contentColor
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(text = key, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Bộ tính toán biểu thức toán học đệ quy
fun evaluateMathExpression(expr: String): Double {
    if (expr.isBlank()) return 0.0
    val clean = expr.replace(",", "").trim()
    
    return try {
        object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < clean.length) clean[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < clean.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else break
                }
                return x
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) {
                        val divisor = parseFactor()
                        x /= if (divisor == 0.0) 1.0 else divisor
                    }
                    else break
                }
                return x
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = this.pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = clean.substring(startPos, this.pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                return x
            }
        }.parse()
    } catch (e: Exception) {
        0.0
    }
}
