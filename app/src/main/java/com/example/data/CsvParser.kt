package com.example.data

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class CsvTransactionDto(
    val title: String,
    val amount: Double,
    val categoryName: String,
    val dateStr: String,
    val walletName: String
)

object CsvParser {
    fun parse(inputStream: InputStream): List<CsvTransactionDto> {
        val result = mutableListOf<CsvTransactionDto>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line = reader.readLine() ?: return emptyList()

        // Phân tích dòng tiêu đề (Header) để tìm index các cột
        val headers = parseCsvLine(line).map { it.lowercase() }
        val noteIdx = headers.indexOfFirst { it.contains("note") || it.contains("ghi chú") }
        val amountIdx = headers.indexOfFirst { it.contains("amount") || it.contains("số tiền") }
        val catIdx = headers.indexOfFirst { it.contains("category") || it.contains("danh mục") }
        val dateIdx = headers.indexOfFirst { it.contains("date") || it.contains("ngày") }
        val walletIdx = headers.indexOfFirst { it.contains("wallet") || it.contains("ví") }

        // Nếu thiếu các cột bắt buộc thì trả về rỗng
        if (amountIdx == -1 || catIdx == -1 || walletIdx == -1) return emptyList()

        line = reader.readLine()
        while (line != null) {
            val parts = parseCsvLine(line)
            if (parts.size > maxOf(amountIdx, catIdx, walletIdx)) {
                val amountStr = parts[amountIdx].replace(",", "").replace("đ", "").trim()
                val amount = amountStr.toDoubleOrNull() ?: 0.0

                if (amount > 0.0) {
                    val title = if (noteIdx != -1 && noteIdx < parts.size) parts[noteIdx] else ""
                    val categoryName = parts[catIdx]
                    val dateStr = if (dateIdx != -1 && dateIdx < parts.size) parts[dateIdx] else ""
                    val walletName = parts[walletIdx]

                    result.add(
                        CsvTransactionDto(
                            title = title.ifBlank { categoryName },
                            amount = amount,
                            categoryName = categoryName,
                            dateStr = dateStr,
                            walletName = walletName
                        )
                    )
                }
            }
            line = reader.readLine()
        }
        return result
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        var curVal = StringBuilder()
        for (ch in line.toCharArray()) {
            if (ch == '\"') {
                inQuotes = !inQuotes
            } else if (ch == ',' && !inQuotes) {
                result.add(curVal.toString().trim())
                curVal = StringBuilder()
            } else {
                curVal.append(ch)
            }
        }
        result.add(curVal.toString().trim())
        return result
    }
}
