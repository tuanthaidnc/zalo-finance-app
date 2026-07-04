package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.TransactionEntity
import com.example.data.TransactionRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class ZaloPayNotificationService : NotificationListenerService() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var repository: TransactionRepositoryImpl

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(applicationContext, scope)
        repository = TransactionRepositoryImpl(db)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName != "com.egame.zalopay" && packageName != "com.zing.zalo") return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getString("android.text") ?: ""

        Log.d("ZaloPaySync", "Nhận thông báo từ $packageName: $title - $text")

        // Xử lý thông báo ZaloPay
        parseAndSaveNotification(title, text)
    }

    private fun parseAndSaveNotification(title: String, text: String) {
        // Regex mẫu để tìm số tiền
        // Ví dụ: "Thanh toán 50.000đ", "Chuyển tiền 100.000 đ", "Nhận 200.000đ"
        val moneyPattern = Pattern.compile("([+-]?\\s*\\d{1,3}(\\.\\d{3})*)\\s*(đ|VND|VND)", Pattern.CASE_INSENSITIVE)
        val matcher = moneyPattern.matcher(text)

        if (matcher.find()) {
            val moneyStr = matcher.group(1) ?: return
            // Chuyển "50.000" hoặc "+ 100.000" thành Double
            val cleanMoneyStr = moneyStr.replace(".", "").replace(" ", "").replace("+", "").replace("-", "")
            val amount = cleanMoneyStr.toDoubleOrNull() ?: return

            // Xác định loại giao dịch
            val isIncome = text.contains("nhận", ignoreCase = true) || text.contains("được chuyển", ignoreCase = true) || text.contains("+")
            
            // Tìm đối tác/cửa hàng
            val merchant = extractMerchant(text)

            // Tự động phân loại danh mục thông minh (Smart Categorization)
            val categoryId = determineCategoryId(merchant, isIncome)

            scope.launch {
                val transaction = TransactionEntity(
                    walletId = 1L, // Mặc định là Ví ZaloPay
                    categoryId = categoryId,
                    amount = amount,
                    title = if (isIncome) "Nhận tiền từ $merchant" else "Thanh toán cho $merchant",
                    description = text,
                    timestamp = System.currentTimeMillis(),
                    source = "ZaloPay"
                )
                val newId = repository.insertTransaction(transaction)

                // Gửi thông báo đẩy của hệ thống báo cho người dùng
                sendLocalNotification(newId, amount, merchant, isIncome)
            }
        }
    }

    private fun extractMerchant(text: String): String {
        // Trích xuất tên đối tác đơn giản
        // Ví dụ: "Thanh toán 50.000đ cho Circle K" -> "Circle K"
        val choIndex = text.indexOf("cho ", ignoreCase = true)
        if (choIndex != -1 && choIndex + 4 < text.length) {
            return text.substring(choIndex + 4).trim()
        }
        val tuIndex = text.indexOf("từ ", ignoreCase = true)
        if (tuIndex != -1 && tuIndex + 3 < text.length) {
            return text.substring(tuIndex + 3).trim()
        }
        return "Cửa hàng/Đối tác ZaloPay"
    }

    private fun determineCategoryId(merchant: String, isIncome: Boolean): Long {
        if (isIncome) {
            // Danh mục Lương (14) hoặc Thu nhập khác (16)
            return if (merchant.contains("salary", ignoreCase = true) || merchant.contains("luong", ignoreCase = true)) 14L else 16L
        }

        // Tự động phân loại chi tiêu dựa trên từ khóa đối tác
        val m = merchant.lowercase()
        return when {
            m.contains("circle k") || m.contains("familymart") || m.contains("kfc") || m.contains("lotteria") || 
            m.contains("grabfood") || m.contains("shopeefood") || m.contains("starbucks") || m.contains("highlands") || 
            m.contains("phuc long") || m.contains("tra sua") || m.contains("nhahang") || m.contains("food") -> 1L // Ăn uống (Cha)

            m.contains("shopee") || m.contains("lazada") || m.contains("tiki") || m.contains("tiktok") || 
            m.contains("winmart") || m.contains("co.op") || m.contains("bach hoa xanh") || m.contains("uniqlo") -> 2L // Mua sắm (Cha)

            m.contains("dien") || m.contains("nuoc") || m.contains("internet") || m.contains("viettel") || 
            m.contains("mobifone") || m.contains("vinaphone") || m.contains("fpt") || m.contains("vnpt") -> 3L // Hóa đơn (Cha)

            m.contains("grab") || m.contains("be ") || m.contains("xanh sm") || m.contains("gojek") || 
            m.contains("xang") || m.contains("petrolimex") || m.contains("taxi") -> 4L // Di chuyển (Cha)

            else -> 17L // Mặc định gán danh mục khác/chuyển ví hoặc danh mục chung
        }
    }

    private fun sendLocalNotification(transactionId: Long, amount: Double, merchant: String, isIncome: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "zalopay_sync_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Đồng bộ ZaloPay",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Tạo intent mở màn hình chính (hoặc deep link sau này)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("transaction_id", transactionId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            transactionId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val amountStr = String.format("%,.0f", amount)
        val title = if (isIncome) "Tự động ghi nhận khoản thu" else "Tự động ghi nhận chi tiêu"
        val text = if (isIncome) {
            "Đã thêm +${amountStr}đ từ $merchant vào ví ZaloPay."
        } else {
            "Đã thêm -${amountStr}đ cho $merchant vào ví ZaloPay."
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(transactionId.toInt(), builder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
