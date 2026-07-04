package com.example.data

object MathUtils {
    fun evaluate(expr: String): Double {
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
}
