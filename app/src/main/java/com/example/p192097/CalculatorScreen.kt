package com.example.p192097

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// ---------------- 颜色（支持白天/夜间两套主题） ----------------
// AppTheme.isLight 由顶部工具栏的"亮度/主题"图标切换，
// 不再调节屏幕真实亮度，而是整体切换深色/浅色配色。
private object AppTheme {
    var isLight by mutableStateOf(false)
}

private val Yellow = Color(0xFFFFC107)

// 深色主题配色
private val BgMainDark = Color(0xFF101014)
private val BgKeyboardDark = Color(0xFF141417)
private val BgKeyDark = Color(0xFF1E1E23)
private val TextGrayDark = Color(0xFF9E9E9E)
private val TextLightDark = Color(0xFFE8E8E8)
private val DrawerBgDark = Color(0xFF16161A)

// 浅色主题配色
private val BgMainLight = Color(0xFFF5F5F5)
private val BgKeyboardLight = Color(0xFFECECEC)
private val BgKeyLight = Color(0xFFFFFFFF)
private val TextGrayLight = Color(0xFF757575)
private val TextLightLight = Color(0xFF1A1A1A)
private val DrawerBgLight = Color(0xFFFFFFFF)

// 以下作为"当前主题"取值，读取处会随 AppTheme.isLight 的变化自动重组
private val BgMain: Color get() = if (AppTheme.isLight) BgMainLight else BgMainDark
private val BgKeyboard: Color get() = if (AppTheme.isLight) BgKeyboardLight else BgKeyboardDark
private val BgKey: Color get() = if (AppTheme.isLight) BgKeyLight else BgKeyDark
private val TextGray: Color get() = if (AppTheme.isLight) TextGrayLight else TextGrayDark
private val TextLight: Color get() = if (AppTheme.isLight) TextLightLight else TextLightDark
private val DrawerBg: Color get() = if (AppTheme.isLight) DrawerBgLight else DrawerBgDark

// ---------------- 键盘模式 ----------------
private enum class KeyboardMode(val label: String) {
    ARITH("+−×÷"), FUNC("f"), LETTERS("a-z"), CUSTOM("f(x)")
}

private data class CKey(val label: String, val insert: String? = null, val action: String? = null)

private val ARITH_KEYS = listOf(
    listOf(CKey("log", "lg("), CKey("xʸ", "^"), CKey("√", "√"), CKey("frac", "/"), CKey("÷", "÷")),
    listOf(CKey("x", "x"), CKey("7", "7"), CKey("8", "8"), CKey("9", "9"), CKey("×", "×")),
    listOf(CKey("y", "y"), CKey("4", "4"), CKey("5", "5"), CKey("6", "6"), CKey("−", "−")),
    listOf(CKey("%", "%"), CKey("1", "1"), CKey("2", "2"), CKey("3", "3"), CKey("+", "+")),
    listOf(CKey("(", "("), CKey(")", ")"), CKey("0", "0"), CKey(".", "."), CKey("=", null, "eval"))
)

private val FUNC_KEYS = listOf(
    listOf(CKey("z", "z"), CKey("{", "{"), CKey("}", "}"), CKey("[", "["), CKey("]", "]")),
    listOf(CKey("sin", "sin("), CKey("cos", "cos("), CKey("tan", "tan("), CKey("矩阵A", "A"), CKey("矩阵C", "C")),
    listOf(CKey("arcsin", "asin("), CKey("arccos", "acos("), CKey("arctan", "atan("), CKey("公倍数", "lcm("), CKey("公约数", "gcd(")),
    listOf(CKey("°", "°"), CKey("′", "′"), CKey("″", "″"), CKey("π", "π"), CKey("e", "e")),
    listOf(CKey("i", "i"), CKey("∞", "∞"), CKey("∠", "∠"), CKey("!", "!"), CKey("‼", "!!")),
    listOf(CKey("| |", "|"), CKey("log₁₀", "lg("), CKey("log₂", "log2("), CKey("mod", "mod("), CKey("ln", "ln(")),
    listOf(CKey("∫", "∫"), CKey("导数", "d"), CKey(">", ">"), CKey("≥", ">="), CKey("", null)),
    listOf(CKey("<", "<"), CKey("≤", "<="), CKey(":", ":"), CKey("Σ", "Σ"), CKey("Π", "Π")),
    listOf(CKey("矩阵[ ]", "["), CKey("·", "·"), CKey("×", "×"), CKey("统计", "统计"), CKey("lim", "lim("))
)

private val LETTER_KEYS = listOf(
    listOf(CKey("a", "a"), CKey("b", "b"), CKey("c", "c"), CKey("k", "k"), CKey("m", "m")),
    listOf(CKey("n", "n"), CKey("ρ", "ρ"), CKey("θ", "θ"))
)

private val CUSTOM_TEMPLATES = listOf(
    "y=kx+b", "y=k/x", "y=ax²+bx+c", "y=a(x−h)²+k", "y=aˣ", "y=logₐx",
    "(x−a)²+(y−b)²=r²", "x²+y²+ax+by+c=0",
    "x²/a²+y²/b²=1", "(x−k)²/a²+(y−h)²/b²=1",
    "x²/a²−y²/b²=1", "(x−k)²/a²−(y−h)²/b²=1",
    "y²=2px"
)

private val NAV_ITEMS = listOf(
    "计算" to "calc", "分数" to "fraction", "教程" to "book", "单位转换" to "convert",
    "日期计算" to "date", "算法管理" to "algo", "自定义函数管理" to "f", "设置" to "settings"
)

// ================= 主界面 =================

@Composable
fun CalculatorScreen() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var tfv by remember { mutableStateOf(TextFieldValue("")) }
    var mode by remember { mutableStateOf(KeyboardMode.ARITH) }
    var vibrateOn by remember { mutableStateOf(true) }
    var showCopyTip by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }

    LaunchedEffect(tfv.text) {
        result = Evaluator.evaluate(tfv.text)?.let { formatNumber(it) } ?: ""
    }
    LaunchedEffect(showCopyTip) {
        if (showCopyTip) { delay(1800); showCopyTip = false }
    }

    fun vibrate() {
        if (vibrateOn) vibrateDevice(context)
    }

    fun insertText(t: String) {
        vibrate()
        val sel = tfv.selection
        val text = tfv.text.substring(0, sel.start) + t + tfv.text.substring(sel.end)
        tfv = TextFieldValue(text, TextRange(sel.start + t.length))
    }

    fun onKey(k: CKey) {
        when {
            k.action == "eval" -> {
                vibrate()
                val r = Evaluator.evaluate(tfv.text)
                if (r != null) {
                    val s = formatRaw(r)
                    tfv = TextFieldValue(s, TextRange(s.length))
                }
            }
            k.insert != null -> insertText(k.insert)
        }
    }

    fun backspace() {
        vibrate()
        val sel = tfv.selection
        when {
            sel.start != sel.end -> tfv = tfv.copy(
                text = tfv.text.substring(0, sel.start) + tfv.text.substring(sel.end),
                selection = TextRange(sel.start)
            )
            sel.start > 0 -> tfv = tfv.copy(
                text = tfv.text.substring(0, sel.start - 1) + tfv.text.substring(sel.start),
                selection = TextRange(sel.start - 1)
            )
        }
    }

    fun moveCursor(d: Int) {
        vibrate()
        val p = (tfv.selection.start + d).coerceIn(0, tfv.text.length)
        tfv = tfv.copy(selection = TextRange(p))
    }

    fun onEnter() {
        vibrate()
        // 不再往文本里插入 "{" 字符，改为纯换行；
        // 真正的花括号由 DisplayArea 用 Canvas 单独画出来，随行数自动变高。
        val expr = tfv.text
        val cur = tfv.selection.start
        val ne = expr.substring(0, cur) + "\n" + expr.substring(cur)
        tfv = TextFieldValue(ne, TextRange(cur + 1))
    }

    fun clearAll() {
        vibrate()
        tfv = TextFieldValue("")
    }

    fun toggleTheme() {
        vibrate()
        AppTheme.isLight = !AppTheme.isLight
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.62f),
                drawerContainerColor = DrawerBg,
                drawerShape = RoundedCornerShape(0.dp)
            ) {
                DrawerContent { title ->
                    scope.launch { drawerState.close() }
                    if (title != "计算") {
                        Toast.makeText(context, "「$title」功能开发中", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(BgMain)
        ) {
            TopBar(
                onMenu = { scope.launch { drawerState.open() } },
                vibrateOn = vibrateOn,
                onToggleVibrate = { vibrateOn = !vibrateOn },
                onToggleTheme = { toggleTheme() },
                onHistory = { Toast.makeText(context, "历史记录开发中", Toast.LENGTH_SHORT).show() },
                onGame = { Toast.makeText(context, "趣味游戏开发中", Toast.LENGTH_SHORT).show() }
            )
            DisplayArea(
                modifier = Modifier.weight(1f),
                tfv = tfv,
                onValueChange = { tfv = it },
                result = result,
                showCopyTip = showCopyTip,
                onCopy = {
                    if (tfv.text.isNotEmpty()) {
                        clipboard.setText(AnnotatedString(tfv.text))
                        showCopyTip = true
                    }
                }
            )
            BottomBar(
                onClear = { clearAll() },
                onEnter = { onEnter() },
                onLeft = { moveCursor(-1) },
                onRight = { moveCursor(1) },
                onBack = { backspace() }
            )
            KeyboardArea(
                modifier = Modifier.weight(1.3f),
                mode = mode,
                onModeChange = { mode = it },
                onKey = { onKey(it) }
            )
        }
    }
}

// ================= 顶部工具栏 =================

@Composable
private fun TopBar(
    onMenu: () -> Unit,
    vibrateOn: Boolean,
    onToggleVibrate: () -> Unit,
    onToggleTheme: () -> Unit,
    onHistory: () -> Unit,
    onGame: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(BgMain),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Default.Menu, contentDescription = "菜单", tint = TextLight)
        }
        Text(
            "计算",
            color = TextLight,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        // 深色主题下显示"太阳"图标（点击切到白天模式）；
        // 浅色主题下显示"月亮"图标（点击切回夜间模式）。默认夜间。
        IconButton(onClick = onToggleTheme) {
            TopIcon(if (AppTheme.isLight) "moon" else "bright", TextLight)
        }
        IconButton(onClick = onToggleVibrate) { TopIcon("vibrate", TextLight, dimmed = !vibrateOn) }
        IconButton(onClick = onHistory) { TopIcon("history", TextLight) }
        IconButton(onClick = onGame) { TopIcon("game", TextLight) }
    }
}

// ================= 显示区 =================

@Composable
private fun DisplayArea(
    modifier: Modifier = Modifier,
    tfv: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    result: String,
    showCopyTip: Boolean,
    onCopy: () -> Unit
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(BgMain)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            val text = tfv.text
            val lineCount = tfv.text.count { it == '\n' } + 1
            val braceHeightDp = with(LocalDensity.current) { (42.sp.toPx() * lineCount).toDp() }

            Row(Modifier.weight(1f).fillMaxWidth()) {
                if (lineCount > 1) {
                    // 宽度固定不变，只有高度随行数拉伸，避免"越按越胖/越按越变形"
                    Canvas(
                        Modifier
                            .width(20.dp)
                            .height(braceHeightDp)
                    ) {
                        drawBrace(TextLight)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    val lineHeightPx = with(LocalDensity.current) { 42.sp.toPx() }
                    // 光标闪烁
                    var cursorOn by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(500)
                            cursorOn = !cursorOn
                        }
                    }
                    val sel = tfv.selection.start
                    // 点击定位：点击第几行，光标就跳到该行行尾
                    Column(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(text) {
                                detectTapGestures { pos ->
                                    val line = (pos.y / lineHeightPx).toInt().coerceAtLeast(0)
                                    val ls = lineStartOf(text, line)
                                    val le = text.indexOf('\n', ls).let { if (it < 0) text.length else it }
                                    onValueChange(tfv.copy(selection = TextRange(le)))
                                }
                            }
                    ) {
                        val lines = if (text.isEmpty()) listOf("") else text.split("\n")
                        lines.forEachIndexed { i, line ->
                            val ls = lineStartOf(text, i)
                            val le = ls + line.length
                            val hasCursor = sel in ls..le
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (hasCursor) {
                                    val prefix = text.substring(ls, minOf(sel, le))
                                    val suffix = text.substring(minOf(sel, le), le)
                                    if (prefix.isNotEmpty()) Text(prefix, color = TextLight, fontSize = 32.sp, lineHeight = 42.sp)
                                    Box(
                                        Modifier
                                            .width(2.dp)
                                            .height(30.dp)
                                            .background(if (cursorOn) Yellow else Color.Transparent)
                                    )
                                    if (suffix.isNotEmpty()) Text(suffix, color = TextLight, fontSize = 32.sp, lineHeight = 42.sp)
                                } else {
                                    Text(line, color = TextLight, fontSize = 32.sp, lineHeight = 42.sp)
                                }
                            }
                        }
                    }
                }
            }
            if (result.isNotEmpty()) {
                Text(
                    result,
                    color = TextGray,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (tfv.text.isNotEmpty()) {
            IconButton(
                onClick = onCopy,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
            ) {
                TopIcon("copy", Yellow)
            }
        }
        if (showCopyTip && tfv.text.isNotEmpty()) {
            Column(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("已复制", color = Color(0xFF999999), fontSize = 11.sp)
                Text(
                    tfv.text,
                    color = Color(0xFF222222),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ================= 底部操作栏 =================

@Composable
private fun BottomBar(
    onClear: () -> Unit,
    onEnter: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onBack: () -> Unit
) {
    // 固定只有 5 个图标，均匀分布，不再显示实时算式文本
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(BgMain),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onClear) {
            Icon(Icons.Default.Delete, contentDescription = "清空", tint = TextLight)
        }
        IconButton(onClick = onEnter) { Text("⏎", color = TextLight, fontSize = 20.sp) }
        IconButton(onClick = onLeft) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "左移", tint = TextLight)
        }
        IconButton(onClick = onRight) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "右移", tint = TextLight)
        }
        IconButton(onClick = onBack) { Text("⌫", color = TextLight, fontSize = 20.sp) }
    }
}

// ================= 键盘区 =================

@Composable
private fun KeyboardArea(
    modifier: Modifier = Modifier,
    mode: KeyboardMode,
    onModeChange: (KeyboardMode) -> Unit,
    onKey: (CKey) -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(BgKeyboard)
    ) {
        ModeRail(mode, onModeChange)
        when (mode) {
            KeyboardMode.ARITH -> GridKeyboard(Modifier.weight(1f), ARITH_KEYS, rowWeight = true, onKey = onKey)
            KeyboardMode.FUNC -> GridKeyboard(Modifier.weight(1f), FUNC_KEYS, rowWeight = false, onKey = onKey)
            KeyboardMode.LETTERS -> LettersKeyboard(Modifier.weight(1f), onKey)
            KeyboardMode.CUSTOM -> CustomKeyboard(Modifier.weight(1f), onKey)
        }
    }
}

@Composable
private fun ModeRail(selected: KeyboardMode, onSelect: (KeyboardMode) -> Unit) {
    Column(
        Modifier
            .width(54.dp)
            .fillMaxHeight()
            .background(BgKeyboard)
    ) {
        KeyboardMode.values().forEach { m ->
            val on = m == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(if (on) Yellow else Color.Transparent)
                    .clickable { onSelect(m) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    m.label,
                    color = if (on) Color(0xFF1A1A1A) else TextGray,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun GridKeyboard(modifier: Modifier, rows: List<List<CKey>>, rowWeight: Boolean, onKey: (CKey) -> Unit) {
    Column(
        modifier
            .fillMaxHeight()
            .padding(start = 3.dp, end = 3.dp, top = 3.dp, bottom = 3.dp)
    ) {
        rows.forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (rowWeight) Modifier.weight(1f) else Modifier.height(46.dp)),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                row.forEach { k -> KeyButton(k, Modifier.weight(1f).fillMaxHeight(), onKey) }
            }
        }
    }
}

@Composable
private fun KeyButton(k: CKey, modifier: Modifier, onKey: (CKey) -> Unit) {
    val isNum = k.label.length == 1 && (k.label[0].isDigit() || k.label[0] == '.')
    val enabled = k.insert != null || k.action != null
    val isEval = k.action == "eval"
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isEval) Yellow else BgKey)
            .clickable(enabled = enabled) { onKey(k) },
        contentAlignment = Alignment.Center
    ) {
        if (k.label.isNotEmpty()) {
            Text(
                k.label,
                color = when {
                    isEval -> Color(0xFF1A1A1A)
                    isNum -> TextLight
                    else -> Yellow
                },
                fontSize = if (isNum) 22.sp else 16.sp,
                fontWeight = if (isNum) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LettersKeyboard(modifier: Modifier, onKey: (CKey) -> Unit) {
    Column(
        modifier
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LETTER_KEYS[0].forEach { k -> KeyButton(k, Modifier.weight(1f).height(64.dp), onKey) }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.weight(1f))
            LETTER_KEYS[1].forEach { k -> KeyButton(k, Modifier.weight(1f).height(64.dp), onKey) }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CustomKeyboard(modifier: Modifier, onKey: (CKey) -> Unit) {
    LazyColumn(
        modifier
            .fillMaxHeight(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(CUSTOM_TEMPLATES) { t ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgKey)
                    .clickable { onKey(CKey(t, t)) },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    t,
                    color = Yellow,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

// ================= 抽屉 =================

@Composable
private fun DrawerContent(onSelect: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(DrawerBg)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 22.dp, bottom = 18.dp)
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF2E7D32)),
                contentAlignment = Alignment.Center
            ) {
                Text("3.14", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Row(
                Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LogoTag("π", Color(0xFF2E7D32))
                LogoTag("sin", Color(0xFF2E7D32))
                LogoTag("f(x)", Color(0xFF2E7D32))
                LogoTag("%", Yellow)
            }
        }
        NAV_ITEMS.forEach { (title, icon) ->
            NavItem(title, icon, selected = title == "计算") { onSelect(title) }
        }
    }
}

@Composable
private fun LogoTag(t: String, bg: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(t, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun NavItem(title: String, iconType: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(if (selected) Color(0xFF2A2A30) else Color.Transparent)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(26.dp)
                .background(if (selected) Yellow else Color.Transparent)
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.width(30.dp), contentAlignment = Alignment.Center) {
            when (iconType) {
                "calc" -> Text("+−\n×÷", color = if (selected) Yellow else Color(0xFFCCCCCC), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 13.sp)
                "f" -> Text("f×", color = if (selected) Yellow else Color(0xFFCCCCCC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                else -> NavCanvasIcon(iconType, if (selected) Yellow else Color(0xFFCCCCCC))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(title, color = if (selected) Yellow else TextLight, fontSize = 15.sp)
    }
}

// ================= 花括号（多行方程组用，Canvas 单独绘制，随行数自动变高） =================

// ================= 花括号（多行方程组用，Canvas 单独绘制，随行数自动变高） =================
// 宽度固定，只有高度会变，避免用字号缩放导致的"越来越胖/变形/顶点跑位"问题。

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBrace(color: Color) {
    val w = size.width
    val h = size.height
    val tipX = -w * 0.55f
    val topCurveEndY = h * 0.12f
    val bottomCurveStartY = h - h * 0.12f
    val path = Path().apply {
        moveTo(w * 0.9f, 0f)
        cubicTo(w * 0.05f, 0f, w * 0.15f, topCurveEndY * 0.3f, w * 0.15f, topCurveEndY)
        lineTo(w * 0.15f, h * 0.42f - 8f)
        cubicTo(w * 0.15f, h * 0.46f, tipX, h * 0.47f, tipX, h * 0.5f)
        cubicTo(tipX, h * 0.53f, w * 0.15f, h * 0.54f, w * 0.15f, h * 0.58f + 8f)
        lineTo(w * 0.15f, bottomCurveStartY)
        cubicTo(w * 0.15f, h - topCurveEndY * 0.3f, w * 0.05f, h, w * 0.9f, h)
    }
    drawPath(
        path,
        color = color,
        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
    )
}


// ================= 线框图标（Canvas） =================

@Composable
private fun TopIcon(type: String, tint: Color, dimmed: Boolean = false) {
    Canvas(Modifier.size(22.dp)) {
        val s = size.minDimension
        val c = if (dimmed) tint.copy(alpha = 0.35f) else tint
        val w = 1.6.dp.toPx()
        val st = Stroke(width = w, cap = StrokeCap.Round)
        when (type) {
            "bright" -> {
                val r = s * 0.16f
                drawCircle(c, r, style = st)
                for (i in 0 until 8) {
                    val ang = i * PI / 4
                    val x1 = s / 2 + cos(ang).toFloat() * r * 1.8f
                    val y1 = s / 2 + sin(ang).toFloat() * r * 1.8f
                    val x2 = s / 2 + cos(ang).toFloat() * r * 2.5f
                    val y2 = s / 2 + sin(ang).toFloat() * r * 2.5f
                    drawLine(c, Offset(x1, y1), Offset(x2, y2), w, StrokeCap.Round)
                }
            }
            "moon" -> {
                // 弯月形状：大圆减去偏移的小圆，得到新月轮廓，再描边绘制
                val r = s * 0.3f
                val cx = s / 2f
                val cy = s / 2f
                val outer = Path().apply {
                    addOval(Rect(Offset(cx - r, cy - r), Size(r * 2f, r * 2f)))
                }
                val offsetX = r * 0.75f
                val inner = Path().apply {
                    addOval(Rect(Offset(cx - r + offsetX, cy - r), Size(r * 2f, r * 2f)))
                }
                val crescent = Path().apply { op(outer, inner, PathOperation.Difference) }
                drawPath(crescent, c, style = st)
            }
            "vibrate" -> {
                val xs = floatArrayOf(0.22f, 0.5f, 0.78f)
                val hs = floatArrayOf(0.42f, 0.68f, 0.52f)
                for (i in 0..2) {
                    val x = s * xs[i]
                    val h = s * hs[i]
                    drawLine(c, Offset(x, s / 2 - h / 2), Offset(x, s / 2 + h / 2), w, StrokeCap.Round)
                }
            }
            "history" -> {
                val r = s * 0.3f
                drawCircle(c, r, style = st)
                drawLine(c, Offset(s / 2, s / 2), Offset(s / 2 + r * 0.6f, s / 2 - r * 0.5f), w, StrokeCap.Round)
                drawLine(c, Offset(s / 2 + r * 0.6f, s / 2 - r * 0.5f), Offset(s / 2 + r * 0.85f, s / 2 - r * 0.3f), w, StrokeCap.Round)
                drawLine(c, Offset(s / 2 + r * 0.6f, s / 2 - r * 0.5f), Offset(s / 2 + r * 0.5f, s / 2 - r * 0.72f), w, StrokeCap.Round)
            }
            "game" -> {
                val bw = s * 0.88f
                val bh = s * 0.5f
                val left = (s - bw) / 2
                val top = (s - bh) / 2
                drawRoundRect(c, Offset(left, top), Size(bw, bh), CornerRadius(s * 0.14f), style = st)
                drawLine(c, Offset(s / 2, s * 0.4f), Offset(s / 2, s * 0.6f), w, StrokeCap.Round)
                drawLine(c, Offset(s / 2 - s * 0.08f, s / 2), Offset(s / 2 + s * 0.08f, s / 2), w, StrokeCap.Round)
                drawCircle(c, s * 0.05f, Offset(s * 0.3f, s / 2), style = st)
                drawCircle(c, s * 0.05f, Offset(s * 0.7f, s / 2), style = st)
            }
            "copy" -> {
                drawRoundRect(c, Offset(s * 0.33f, s * 0.16f), Size(s * 0.55f, s * 0.55f), CornerRadius(s * 0.08f), style = st)
                drawRoundRect(c, Offset(s * 0.16f, s * 0.33f), Size(s * 0.55f, s * 0.55f), CornerRadius(s * 0.08f), style = st)
            }
        }
    }
}

@Composable
private fun NavCanvasIcon(type: String, color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val s = size.minDimension
        val w = 1.6.dp.toPx()
        val st = Stroke(width = w, cap = StrokeCap.Round)
        when (type) {
            "fraction" -> {
                drawRoundRect(color, Offset(s * 0.12f, s * 0.08f), Size(s * 0.76f, s * 0.36f), CornerRadius(s * 0.08f), style = st)
                drawRoundRect(color, Offset(s * 0.12f, s * 0.56f), Size(s * 0.76f, s * 0.36f), CornerRadius(s * 0.08f), style = st)
            }
            "book" -> {
                drawLine(color, Offset(s / 2, s * 0.1f), Offset(s / 2, s * 0.9f), w, StrokeCap.Round)
                drawArc(color, startAngle = 90f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(s * 0.1f, s * 0.15f), size = Size(s * 0.4f, s * 0.7f), style = st)
                drawArc(color, startAngle = -90f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(s * 0.5f, s * 0.15f), size = Size(s * 0.4f, s * 0.7f), style = st)
            }
            "convert" -> {
                drawArc(color, startAngle = 40f, sweepAngle = 160f, useCenter = false,
                    topLeft = Offset(s * 0.15f, s * 0.15f), size = Size(s * 0.7f, s * 0.7f), style = st)
                val a1 = (40f + 160f) * PI / 180
                val x1 = s * 0.5f + cos(a1).toFloat() * s * 0.35f
                val y1 = s * 0.5f + sin(a1).toFloat() * s * 0.35f
                drawLine(color, Offset(x1, y1), Offset(x1 + s * 0.15f, y1 + s * 0.04f), w, StrokeCap.Round)
                drawLine(color, Offset(x1, y1), Offset(x1 + s * 0.05f, y1 + s * 0.15f), w, StrokeCap.Round)
                drawArc(color, startAngle = 220f, sweepAngle = 160f, useCenter = false,
                    topLeft = Offset(s * 0.15f, s * 0.15f), size = Size(s * 0.7f, s * 0.7f), style = st)
                val a2 = (220f + 160f) * PI / 180
                val x2 = s * 0.5f + cos(a2).toFloat() * s * 0.35f
                val y2 = s * 0.5f + sin(a2).toFloat() * s * 0.35f
                drawLine(color, Offset(x2, y2), Offset(x2 - s * 0.15f, y2 - s * 0.04f), w, StrokeCap.Round)
                drawLine(color, Offset(x2, y2), Offset(x2 - s * 0.05f, y2 - s * 0.15f), w, StrokeCap.Round)
            }
            "date" -> {
                drawRoundRect(color, Offset(s * 0.12f, s * 0.22f), Size(s * 0.76f, s * 0.62f), CornerRadius(s * 0.08f), style = st)
                drawLine(color, Offset(s * 0.12f, s * 0.42f), Offset(s * 0.88f, s * 0.42f), w, StrokeCap.Round)
                drawLine(color, Offset(s * 0.3f, s * 0.1f), Offset(s * 0.3f, s * 0.28f), w, StrokeCap.Round)
                drawLine(color, Offset(s * 0.7f, s * 0.1f), Offset(s * 0.7f, s * 0.28f), w, StrokeCap.Round)
                drawCircle(color, s * 0.05f, Offset(s * 0.36f, s * 0.62f), style = st)
                drawCircle(color, s * 0.05f, Offset(s * 0.64f, s * 0.62f), style = st)
            }
            "algo" -> {
                val pts = listOf(
                    Offset(s * 0.25f, s * 0.3f), Offset(s * 0.75f, s * 0.3f),
                    Offset(s * 0.25f, s * 0.75f), Offset(s * 0.75f, s * 0.75f)
                )
                drawLine(color, pts[0], pts[1], w, StrokeCap.Round)
                drawLine(color, pts[1], pts[3], w, StrokeCap.Round)
                drawLine(color, pts[0], pts[2], w, StrokeCap.Round)
                drawLine(color, pts[2], pts[3], w, StrokeCap.Round)
                pts.forEach { drawCircle(color, s * 0.09f, it, style = st) }
            }
            "settings" -> {
                drawCircle(color, s * 0.26f, style = st)
                for (i in 0 until 8) {
                    val ang = i * PI / 4
                    val x1 = s * 0.5f + cos(ang).toFloat() * s * 0.34f
                    val y1 = s * 0.5f + sin(ang).toFloat() * s * 0.34f
                    val x2 = s * 0.5f + cos(ang).toFloat() * s * 0.46f
                    val y2 = s * 0.5f + sin(ang).toFloat() * s * 0.46f
                    drawLine(color, Offset(x1, y1), Offset(x2, y2), w, StrokeCap.Round)
                }
            }
        }
    }
}

// ================= 工具 =================

private fun lineStartOf(text: String, line: Int): Int {
    if (line <= 0) return 0
    var idx = 0
    var cur = 0
    while (cur < line && idx < text.length) {
        if (text[idx] == '\n') cur++
        idx++
    }
    return idx
}

private fun vibrateDevice(context: Context) {
    try {
        val v = context.getSystemService(Vibrator::class.java) ?: return
        if (v.hasVibrator()) {
            v.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    } catch (_: Exception) {
    }
}

private fun formatNumber(d: Double): String {
    if (d.isNaN()) return "错误"
    if (d.isInfinite()) return if (d > 0) "∞" else "-∞"
    val a = abs(d)
    return if (a != 0.0 && (a >= 1e15 || a < 1e-9)) {
        String.format(Locale.US, "%.8e", d).replace("e+", "e")
    } else {
        DecimalFormat("#,##0.##########", DecimalFormatSymbols(Locale.US)).format(d)
    }
}

private fun formatRaw(d: Double): String {
    if (d.isNaN()) return ""
    if (d.isInfinite()) return if (d > 0) "1e999" else "-1e999"
    val a = abs(d)
    return if (a != 0.0 && (a >= 1e15 || a < 1e-9)) {
        String.format(Locale.US, "%.10e", d)
    } else {
        DecimalFormat("0.##########", DecimalFormatSymbols(Locale.US)).format(d)
    }
}
