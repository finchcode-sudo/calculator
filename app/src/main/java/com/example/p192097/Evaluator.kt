package com.example.p192097

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log
import kotlin.math.log2
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * 表达式求值引擎（自研，无第三方依赖）。
 *
 * 支持：四则 + - * /、乘方 ^、开方 √、阶乘 !、双阶乘 !!、百分号 %、
 *      角度分秒 ° ′ ″、常量 π e ∞、log/ln/lg/log2/mod/gcd/lcm、
 *      sin/cos/tan/arcsin/arccos/arctan、比较 > < >= <=、比号 :、隐式乘法。
 *
 * 不支持的记号（字母变量、矩阵、积分、求和、虚数单位等）时整体返回 null，由 UI 显示为空。
 */
object Evaluator {

    fun evaluate(expr: String): Double? {
        val text = prepare(expr)
        if (text.isEmpty()) return null
        val tokens = tokenize(text) ?: return null
        if (tokens.any { it.type == T.UNSUP }) return null
        val rpn = toRpn(tokens) ?: return null
        return try {
            evalRpn(rpn)
        } catch (e: Exception) {
            null
        }
    }

    // ---------- 内部结构 ----------

    private enum class T { NUM, ANGLE, OP, LP, RP, COMMA, FUNC, POST, UNSUP }

    private class Token(val type: T, val s: String, val num: Double = 0.0, var unary: Boolean = false)

    private val CONSTS = mapOf('π' to PI, 'e' to E, '∞' to Double.POSITIVE_INFINITY)
    private val FUNC1 = setOf("sin", "cos", "tan", "asin", "acos", "atan", "ln", "lg", "log2", "sqrt", "abs", "floor", "ceil", "exp")
    private val FUNC2 = setOf("log", "mod", "gcd", "lcm", "min", "max")

    private const val E = 2.718281828459045

    // ---------- 预处理 ----------

    private fun prepare(s: String): String = s
        .replace(" ", "")
        .replace("…", "")
        .replace("²", "^2")
        .replace("³", "^3")
        .replace("⁴", "^4")

    // ---------- 分词 ----------

    private fun tokenize(s: String): List<Token>? {
        val out = ArrayList<Token>()
        var i = 0
        val n = s.length
        while (i < n) {
            val c = s[i]
            when {
                c.isDigit() || (c == '.' && i + 1 < n && s[i + 1].isDigit()) -> {
                    var j = i
                    while (j < n && (s[j].isDigit() || s[j] == '.')) j++
                    if (j < n && (s[j] == 'e' || s[j] == 'E')) {
                        var k = j + 1
                        if (k < n && (s[k] == '+' || s[k] == '-')) k++
                        if (k < n && s[k].isDigit()) {
                            while (k < n && (s[k].isDigit() || s[k] == '.')) k++
                            j = k
                        }
                    }
                    val v = s.substring(i, j).toDoubleOrNull() ?: return null
                    var k = j
                    var deg = 0.0; var min = 0.0; var sec = 0.0; var has = false
                    while (k < n && (s[k] == '°' || s[k] == '′' || s[k] == '″')) {
                        when (s[k]) { '°' -> deg = 1.0; '′' -> min = 1.0; '″' -> sec = 1.0 }
                        has = true; k++
                    }
                    if (has) out.add(Token(T.ANGLE, "angle", v + min / 60.0 + sec / 3600.0))
                    else out.add(Token(T.NUM, "num", v))
                    i = k
                }
                c == '×' || c == '·' -> { out.add(Token(T.OP, "*")); i++ }
                c == '÷' -> { out.add(Token(T.OP, "/")); i++ }
                c == '−' -> { out.add(Token(T.OP, "-")); i++ }
                c == '+' || c == '-' || c == '*' || c == '/' || c == '^' -> { out.add(Token(T.OP, c.toString())); i++ }
                c == '(' || c == '[' || c == '{' -> { out.add(Token(T.LP, c.toString())); i++ }
                c == ')' || c == ']' || c == '}' -> { out.add(Token(T.RP, c.toString())); i++ }
                c == ',' -> { out.add(Token(T.COMMA, ",")); i++ }
                c == '√' -> { out.add(Token(T.FUNC, "sqrt")); i++ }
                c == '%' -> { out.add(Token(T.POST, "%")); i++ }
                c == '!' -> {
                    if (i + 1 < n && s[i + 1] == '!') { out.add(Token(T.POST, "!!")); i += 2 }
                    else { out.add(Token(T.POST, "!")); i++ }
                }
                c == '>' -> {
                    if (i + 1 < n && s[i + 1] == '=') { out.add(Token(T.OP, ">=")); i += 2 }
                    else { out.add(Token(T.OP, ">")); i++ }
                }
                c == '<' -> {
                    if (i + 1 < n && s[i + 1] == '=') { out.add(Token(T.OP, "<=")); i += 2 }
                    else { out.add(Token(T.OP, "<")); i++ }
                }
                c == ':' -> { out.add(Token(T.OP, ":")); i++ }
                c == '=' -> { out.add(Token(T.UNSUP, "=")); i++ }
                c == '|' -> { out.add(Token(T.UNSUP, "|")); i++ }
                c == '°' || c == '′' || c == '″' -> { out.add(Token(T.UNSUP, c.toString())); i++ }
                c.isLetter() || c == 'ρ' || c == 'θ' -> {
                    var j = i
                    while (j < n && (s[j].isLetter() || s[j] == 'ρ' || s[j] == 'θ')) j++
                    val w = s.substring(i, j)
                    when {
                        w in FUNC1 -> out.add(Token(T.FUNC, w))
                        w in FUNC2 -> out.add(Token(T.FUNC, w))
                        w.length == 1 && CONSTS.containsKey(w[0]) -> out.add(Token(T.NUM, "c", CONSTS[w[0]]!!))
                        w == "pi" || w == "PI" -> out.add(Token(T.NUM, "c", PI))
                        else -> out.add(Token(T.UNSUP, w))
                    }
                    i = j
                }
                else -> { out.add(Token(T.UNSUP, c.toString())); i++ }
            }
        }
        return implicitMult(out)
    }

    /** 隐式乘法：2π、2(3)、(2)(3)、(2)3、2sin( 等 */
    private fun implicitMult(t: List<Token>): List<Token> {
        if (t.isEmpty()) return t
        val out = ArrayList<Token>()
        for (cur in t) {
            val prev = out.lastOrNull()
            if (prev != null) {
                val curStarts = cur.type == T.NUM || cur.type == T.ANGLE || cur.type == T.LP || cur.type == T.FUNC
                val prevEnds = prev.type == T.NUM || prev.type == T.ANGLE || prev.type == T.RP
                if (prevEnds && curStarts) out.add(Token(T.OP, "*"))
            }
            out.add(cur)
        }
        return out
    }

    // ---------- Shunting-yard ----------

    private fun prec(s: String): Int = when (s) {
        ">", "<", ">=", "<=" -> 1
        "+", "-" -> 2
        "*", "/", ":" -> 3
        "^" -> 4
        else -> -1
    }

    private fun isRightAssoc(s: String) = s == "^"

    private fun toRpn(t: List<Token>): List<Token>? {
        val out = ArrayList<Token>()
        val stack = ArrayList<Token>()
        for ((idx, tok) in t.withIndex()) {
            when (tok.type) {
                T.NUM, T.ANGLE -> out.add(tok)
                T.FUNC -> stack.add(tok)
                T.POST -> out.add(tok)
                T.COMMA -> {
                    while (stack.isNotEmpty() && stack.last().type != T.LP) out.add(stack.removeAt(stack.size - 1))
                    if (stack.isEmpty()) return null
                }
                T.OP -> {
                    val isUnary = tok.s == "-" && (idx == 0 ||
                            t[idx - 1].type == T.LP || t[idx - 1].type == T.COMMA ||
                            (t[idx - 1].type == T.OP && !t[idx - 1].unary))
                    if (isUnary) { tok.unary = true; stack.add(tok); continue }
                    val p = prec(tok.s)
                    while (stack.isNotEmpty()) {
                        val top = stack.last()
                        val topPrec = when (top.type) {
                            T.OP -> prec(top.s)
                            T.FUNC -> 5
                            else -> -1
                        }
                        if (topPrec < p || (topPrec == p && isRightAssoc(tok.s))) break
                        out.add(stack.removeAt(stack.size - 1))
                    }
                    stack.add(tok)
                }
                T.LP -> stack.add(tok)
                T.RP -> {
                    while (stack.isNotEmpty() && stack.last().type != T.LP) out.add(stack.removeAt(stack.size - 1))
                    if (stack.isEmpty()) return null
                    stack.removeAt(stack.size - 1)
                    if (stack.isNotEmpty() && stack.last().type == T.FUNC) out.add(stack.removeAt(stack.size - 1))
                }
                T.UNSUP -> return null
            }
        }
        while (stack.isNotEmpty()) {
            val top = stack.removeAt(stack.size - 1)
            if (top.type == T.LP || top.type == T.COMMA) return null
            out.add(top)
        }
        return out
    }

    // ---------- 后缀求值 ----------

    private fun evalRpn(rpn: List<Token>): Double? {
        val stack = ArrayList<Double>()
        for (tok in rpn) {
            when (tok.type) {
                T.NUM -> stack.add(tok.num)
                T.ANGLE -> stack.add(tok.num * PI / 180.0)
                T.POST -> {
                    val a = stack.removeAt(stack.size - 1)
                    when (tok.s) {
                        "!" -> stack.add(fact(a) ?: return null)
                        "!!" -> stack.add(fact2(a) ?: return null)
                        "%" -> stack.add(a / 100.0)
                    }
                }
                T.OP -> {
                    if (tok.unary) {
                        val a = stack.removeAt(stack.size - 1)
                        stack.add(-a)
                    } else {
                        if (stack.size < 2) return null
                        val b = stack.removeAt(stack.size - 1)
                        val a = stack.removeAt(stack.size - 1)
                        stack.add(applyOp(tok.s, a, b) ?: return null)
                    }
                }
                T.FUNC -> {
                    val n = if (tok.s in FUNC2) 2 else 1
                    if (stack.size < n) return null
                    val args = ArrayList<Double>()
                    repeat(n) { args.add(0, stack.removeAt(stack.size - 1)) }
                    stack.add(applyFunc(tok.s, args) ?: return null)
                }
                else -> return null
            }
        }
        if (stack.size != 1) return null
        return stack[0]
    }

    private fun applyOp(op: String, a: Double, b: Double): Double? = when (op) {
        "+" -> a + b
        "-" -> a - b
        "*" -> a * b
        "/" -> if (b == 0.0) null else a / b
        "^" -> a.pow(b)
        ":" -> if (b == 0.0) null else a / b
        ">" -> if (a > b) 1.0 else 0.0
        "<" -> if (a < b) 1.0 else 0.0
        ">=" -> if (a >= b) 1.0 else 0.0
        "<=" -> if (a <= b) 1.0 else 0.0
        else -> null
    }

    private fun applyFunc(f: String, a: List<Double>): Double? {
        val x = a[0]
        return when (f) {
            "sin" -> sin(x)
            "cos" -> cos(x)
            "tan" -> tan(x)
            "asin" -> asin(x)
            "acos" -> acos(x)
            "atan" -> atan(x)
            "ln" -> if (x <= 0) null else ln(x)
            "lg" -> if (x <= 0) null else log10(x)
            "log2" -> if (x <= 0) null else log2(x)
            "sqrt" -> if (x < 0) null else sqrt(x)
            "abs" -> abs(x)
            "floor" -> floor(x)
            "ceil" -> ceil(x)
            "exp" -> exp(x)
            "log" -> if (a.size < 2 || a[0] <= 0 || a[1] <= 0 || a[1] == 1.0) null else ln(a[0]) / ln(a[1])
            "mod" -> if (a.size < 2 || a[1] == 0.0) null else a[0] % a[1]
            "gcd" -> if (a.size < 2) null else gcd(a[0], a[1])
            "lcm" -> if (a.size < 2) null else lcm(a[0], a[1])
            "min" -> if (a.size < 2) null else min(a[0], a[1])
            "max" -> if (a.size < 2) null else max(a[0], a[1])
            else -> null
        }
    }

    private fun fact(a: Double): Double? {
        if (a < 0 || a != floor(a) || a > 170) return null
        var r = 1.0
        for (i in 2..a.toInt()) r *= i
        return r
    }

    private fun fact2(a: Double): Double? {
        if (a < 0 || a != floor(a) || a > 300) return null
        var r = 1.0
        var i = a.toInt()
        while (i > 1) { r *= i; i -= 2 }
        return r
    }

    private fun gcd(a: Double, b: Double): Double? {
        var x = abs(round(a)); var y = abs(round(b))
        if (x > 1e9 || y > 1e9) return null
        while (y > 0) { val t = x % y; x = y; y = t }
        return x
    }

    private fun lcm(a: Double, b: Double): Double? {
        val g = gcd(a, b) ?: return null
        if (g == 0.0) return 0.0
        return abs(a * b) / g
    }
}
