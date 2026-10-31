package vn.kozu.game.mania.gamestage.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import vn.kozu.game.mania.gamestage.engine.JudgementEngine
import vn.kozu.game.mania.gamestage.engine.NoteScroll
import vn.kozu.game.mania.gamestage.scoring.GameResult
import vn.kozu.game.mania.gamestage.session.GameSession
import kotlinx.coroutines.isActive
import kotlin.math.max

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Màn chơi. Chuyển động được vẽ HOÀN TOÀN từ thời gian (vị trí = hàm tuyến tính của thời gian chart),
 * không cộng dồn theo khung hình, và thời gian lấy từ mốc vsync của chính khung hình đó, nên không
 * có chuyện khựng khi một khung hình bị trễ. Cú chạm được chấm theo thời điểm của sự kiện, không theo
 * lúc xử lý.
 */
@Composable
fun PlayScreen(
    session: GameSession,
    onQuit: () -> Unit,
    onRestart: () -> Unit,
    onFinished: (GameResult) -> Unit,
) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        view.keepScreenOn = true
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = false
            session.release()
        }
    }

    BackHandler {
        if (session.phase == GameSession.Phase.PAUSED) {
            session.resume()
        } else if (session.phase == GameSession.Phase.PLAYING) {
            session.pause()
        } else {
            session.abort()
            onQuit()
        }
    }

    LaunchedEffect(session) { session.prepare() }

    val phase = session.phase
    LaunchedEffect(phase) {
        if (phase == GameSession.Phase.FINISHED) session.result?.let(onFinished)
    }

    val frameTick = remember(session) { mutableLongStateOf(0L) }
    val drawTime = remember(session) { doubleArrayOf(0.0) }
    LaunchedEffect(session) {
        while (isActive) {
            withFrameNanos { frameNanos ->
                session.frame()
                // thời gian vẽ theo mốc vsync của khung hình này
                drawTime[0] = session.core.chartTimeAt(frameNanos)
                frameTick.longValue = frameNanos
            }
        }
    }

    val chart = session.chart
    val engine = session.core.engine
    val travelMs = session.core.travelMs
    val dimAlpha = session.settings.backgroundDim / 100f
    val colors = MaterialTheme.colorScheme
    val firstVisible = remember(session) { IntArray(chart.keys) }
    val touchRouter = remember(session) { arrayOfNulls<TouchInputRouter>(1) }
    val touchRadiusPx = with(LocalDensity.current) { 24.dp.toPx() }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    val timestamp = SystemClock.uptimeMillis()
                    touchRouter[0]?.releaseAll()?.released?.forEach { column ->
                        session.release(column, timestamp)
                    }
                    val layout = PlayfieldLayout(size.width.toFloat(), size.height.toFloat(), chart.keys)
                    touchRouter[0] = TouchInputRouter(
                        keys = chart.keys,
                        left = layout.left,
                        columnWidth = layout.columnWidth,
                        touchRadius = touchRadiusPx,
                    )
                }
                .pointerInteropFilter { event ->
                    val router = touchRouter[0] ?: return@pointerInteropFilter false
                    val eventTime = event.eventTime
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                            val index = event.actionIndex
                            router.press(event.getPointerId(index).toLong(), event.getX(index))
                                ?.pressed
                                ?.forEach { column -> session.press(column, eventTime) }
                        }

                        MotionEvent.ACTION_MOVE -> {
                            for (index in 0 until event.pointerCount) {
                                val changes = router.move(
                                    event.getPointerId(index).toLong(),
                                    event.getX(index)
                                )
                                changes.released.forEach { column -> session.release(column, eventTime) }
                                changes.pressed.forEach { column -> session.press(column, eventTime) }
                            }
                        }

                        MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                            val index = event.actionIndex
                            router.release(event.getPointerId(index).toLong()).released
                                .forEach { column -> session.release(column, eventTime) }
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            router.releaseAll().released
                                .forEach { column -> session.release(column, eventTime) }
                        }
                    }
                    true
                }
        ) {
            frameTick.longValue // đăng ký: mỗi khung hình vẽ lại (không recompose)
            val w = size.width
            val h = size.height
            val t = drawTime[0]
            val layout = PlayfieldLayout(w, h, chart.keys)

            // ---- nền + độ trong
            val bg = session.background
            if (bg != null) {
                val scale = max(w / bg.width, h / bg.height)
                val srcW = (w / scale).toInt().coerceIn(1, bg.width)
                val srcH = (h / scale).toInt().coerceIn(1, bg.height)
                drawImage(
                    image = bg,
                    srcOffset = IntOffset((bg.width - srcW) / 2, (bg.height - srcH) / 2),
                    srcSize = IntSize(srcW, srcH),
                    dstSize = IntSize(w.toInt(), h.toInt()),
                )
            }
            drawRect(Color.Black.copy(alpha = if (bg == null) 0.82f else dimAlpha))

            // ---- sân chơi
            drawRoundRect(
                color = colors.surface.copy(alpha = 0.78f),
                topLeft = Offset(layout.left, 0f),
                size = Size(layout.right - layout.left, h),
                cornerRadius = CornerRadius(layout.columnWidth * 0.12f)
            )
            for (c in 0..chart.keys) {
                val x = layout.left + c * layout.columnWidth
                drawRect(colors.onSurface.copy(alpha = 0.16f), Offset(x - 1f, 0f), Size(2f, h))
            }

            // ---- phím đang giữ
            for (c in 0 until chart.keys) {
                if (engine.keyDown[c]) {
                    drawRect(
                        colors.primary.copy(alpha = 0.22f),
                        Offset(layout.columnX(c), layout.judgeY - h * 0.2f),
                        Size(layout.columnWidth, h * 0.2f),
                    )
                }
            }

            // ---- nốt
            val pad = layout.columnWidth * 0.06f
            val noteW = layout.columnWidth - pad * 2
            for (c in 0 until chart.keys) {
                val ids = chart.columns[c]
                val x = layout.columnX(c) + pad
                val color = PlayColors.noteColor(c, chart.keys, colors.primary, colors.tertiary)
                var i = firstVisible[c]
                while (i < ids.size) {
                    val id = ids[i]
                    val n = chart.notes[id]
                    val st = engine.state[id]
                    val radius = CornerRadius(minOf(noteW * 0.28f, layout.noteHeight * 0.45f))
                    if (st == JudgementEngine.STATE_DONE) {
                        if (i == firstVisible[c]) firstVisible[c]++
                        i++
                        continue
                    }
                    if (n.timeMs - t > travelMs + 50) break // chưa xuất hiện; các nốt sau còn xa hơn
                    val headY = NoteScroll.yAt(n.timeMs.toDouble(), t, travelMs, layout.judgeY)
                    if (!n.isHold) {
                        if (headY - layout.noteHeight < h && headY > 0f) {
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(x, headY - layout.noteHeight),
                                size = Size(noteW, layout.noteHeight),
                                cornerRadius = radius
                            )
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.32f),
                                topLeft = Offset(x, headY - layout.noteHeight),
                                size = Size(noteW, layout.noteHeight),
                                cornerRadius = radius,
                                style = Stroke(width = (layout.columnWidth * 0.035f).coerceAtLeast(1f))
                            )
                        }
                    } else {
                        val holding = st == JudgementEngine.STATE_HOLDING
                        val tailY = NoteScroll.yAt(n.endTimeMs.toDouble(), t, travelMs, layout.judgeY)
                        val headDrawY = if (holding) minOf(headY, layout.judgeY) else headY
                        val bodyColor = if (engine.broken[id]) PlayColors.broken else color
                        val top = max(tailY - layout.noteHeight, 0f)
                        val bottom = minOf(headDrawY, h)
                        if (bottom > top) {
                            drawRoundRect(
                                color = bodyColor.copy(alpha = if (holding) 0.85f else 0.55f),
                                topLeft = Offset(x + noteW * 0.15f, top),
                                size = Size(noteW * 0.7f, bottom - top),
                                cornerRadius = CornerRadius(noteW * 0.18f)
                            )
                        }
                        drawRoundRect(
                            color = bodyColor,
                            topLeft = Offset(x, headDrawY - layout.noteHeight),
                            size = Size(noteW, layout.noteHeight),
                            cornerRadius = radius
                        )
                        drawRoundRect(
                            color = bodyColor.copy(alpha = 0.92f),
                            topLeft = Offset(x, tailY - layout.noteHeight),
                            size = Size(noteW, layout.noteHeight * 0.7f),
                            cornerRadius = radius
                        )
                    }
                    i++
                }
            }

            // ---- vạch phán định
            drawRoundRect(
                colors.primary,
                Offset(layout.left, layout.judgeY),
                Size(layout.right - layout.left, 4.dp.toPx()),
                CornerRadius(2.dp.toPx())
            )

            // ---- tiến độ bài
            val progress = (t / max(1.0, chart.lastEventTimeMs.toDouble())).coerceIn(0.0, 1.0).toFloat()
            drawRect(colors.onSurface.copy(alpha = 0.28f), Offset(0f, 0f), Size(w, 4.dp.toPx()))
            drawRect(colors.primary, Offset(0f, 0f), Size(w * progress, 4.dp.toPx()))
        }

        // ---- HUD (chỉ recompose khi có phán định)
        if (phase == GameSession.Phase.PLAYING || phase == GameSession.Phase.FINISHED) {
            Hud(session)
        }

        if (phase == GameSession.Phase.PLAYING) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
            ) {
                IconButton(onClick = session::pause) {
                    Icon(
                        Icons.Rounded.Pause,
                        contentDescription = "Pause game",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (phase == GameSession.Phase.PAUSED) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).padding(24.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                    tonalElevation = 12.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Game paused", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = session::resume, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Resume")
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Restart")
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                session.abort()
                                onQuit()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Quit")
                        }
                    }
                }
            }
        }

        when (phase) {
            GameSession.Phase.LOADING, GameSession.Phase.READY -> PreparationPanel(
                session = session,
                ready = phase == GameSession.Phase.READY,
                onStart = session::start,
                modifier = Modifier.align(Alignment.Center),
            )
            GameSession.Phase.FAILED -> session.error?.let { msg ->
                Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Unable to start this chart", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(msg, color = Color(0xFFFFB4B4), modifier = Modifier.padding(vertical = 12.dp))
                    Button(onClick = onQuit) { Text("Back") }
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun PreparationPanel(
    session: GameSession,
    ready: Boolean,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = if (ready) "Ready to play" else "Preparing chart",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${session.difficulty.artist} — ${session.difficulty.title}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "${session.difficulty.version} • ${session.chart.keys}K • ${session.chart.notes.size} notes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Preparation log",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            session.preparationLog.forEach { entry ->
                Text(
                    "›  $entry",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            if (ready) {
                Spacer(Modifier.height(20.dp))
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Text("START", fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun Hud(session: GameSession) {
    Box(Modifier.fillMaxSize().padding(12.dp)) {
        Column(Modifier.align(Alignment.TopEnd), horizontalAlignment = Alignment.End) {
            Text("%07d".format(session.score), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("%.2f%%".format(session.accuracy * 100), color = Color(0xFFDDDDDD), fontSize = 18.sp)
        }

        val flash = remember(session) { Animatable(0f) }
        LaunchedEffect(session.judgementSeq) {
            if (session.judgementSeq > 0) {
                flash.snapTo(1f)
                flash.animateTo(0f, tween(500))
            }
        }
        Column(
            Modifier.align(Alignment.Center).alpha(flash.value),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            session.lastJudgement?.let { j ->
                Text(j.label, color = PlayColors.judgement(j), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        if (session.combo > 1) {
            Text(
                "${session.combo}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center).padding(top = 120.dp),
            )
        }
    }
}
