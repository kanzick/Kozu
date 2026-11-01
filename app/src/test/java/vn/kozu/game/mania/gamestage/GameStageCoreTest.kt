package vn.kozu.game.mania.gamestage

import vn.kozu.game.mania.gamestage.audio.DefaultHitsound
import vn.kozu.game.mania.gamestage.audio.SampleLookup
import vn.kozu.game.mania.gamestage.chart.ChartNote
import vn.kozu.game.mania.gamestage.chart.ChartParseException
import vn.kozu.game.mania.gamestage.chart.ManiaChart
import vn.kozu.game.mania.gamestage.chart.ManiaChartParser
import vn.kozu.game.mania.gamestage.engine.AudioAnchor
import vn.kozu.game.mania.gamestage.engine.GameClock
import vn.kozu.game.mania.gamestage.engine.GameCore
import vn.kozu.game.mania.gamestage.engine.JudgementEngine
import vn.kozu.game.mania.gamestage.engine.NoteScroll
import vn.kozu.game.mania.gamestage.playtype.Landscape3DPerspective
import vn.kozu.game.mania.gamestage.playtype.LandscapeProjection
import vn.kozu.game.mania.gamestage.playtype.LandscapeVerticalScrolling
import vn.kozu.game.mania.gamestage.playtype.Portrait
import vn.kozu.game.mania.gamestage.scoring.HitWindows
import vn.kozu.game.mania.gamestage.scoring.Judgement
import vn.kozu.game.mania.gamestage.scoring.ScoreTracker
import vn.kozu.game.mania.gamestage.settings.GameSettings
import vn.kozu.game.mania.gamestage.settings.SettingsStore
import vn.kozu.game.mania.gamestage.ui.PlayfieldLayout
import vn.kozu.game.mania.gamestage.ui.TouchInputRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.math.abs

class GameStageCoreTest {

    // ------------------------------------------------------------------ helpers

    private fun chartText(od: Double = 8.0, objects: List<String>, keys: Int = 4) = """
        osu file format v14

        [General]
        AudioFilename: a.mp3
        AudioLeadIn: 0
        SampleSet: Soft
        Mode: 3

        [Difficulty]
        HPDrainRate:8
        CircleSize:$keys
        OverallDifficulty:$od

        [TimingPoints]
        0,500,4,1,0,60,1,0
        5000,-100,4,2,3,40,0,0

        [HitObjects]
    """.trimIndent() + "\n" + objects.joinToString("\n")

    // x chọn tâm cột cho 4K: 64, 192, 320, 448
    private fun tap(col: Int, time: Int) = "${col * 128 + 64},192,$time,1,0,0:0:0:0:"
    private fun hold(col: Int, time: Int, end: Int) = "${col * 128 + 64},192,$time,128,0,$end:0:0:0:0:"

    private class Recorder : JudgementEngine.Listener {
        val judgements = ArrayList<Pair<Int, Judgement>>()
        val hits = ArrayList<Int>()
        override fun onJudgement(note: ChartNote, judgement: Judgement, errorMs: Double) {
            judgements += note.id to judgement
        }
        override fun onHit(note: ChartNote) { hits += note.id }
        fun last() = judgements.last().second
    }

    private fun engineFor(chart: ManiaChart): Triple<JudgementEngine, ScoreTracker, Recorder> {
        val tracker = ScoreTracker(chart.totalNotes)
        val rec = Recorder()
        return Triple(JudgementEngine(chart, tracker, rec), tracker, rec)
    }

    // ------------------------------------------------------------------ parser

    @Test
    fun parsesNotesHoldsColumnsAndSamples() {
        val chart = ManiaChartParser.parse(chartText(objects = listOf(hold(2, 1000, 1600), tap(0, 500), tap(3, 500), tap(1, 6000))))
        assertEquals(4, chart.keys)
        assertEquals(8.0, chart.overallDifficulty, 0.0)
        assertEquals(120.0, chart.bpm, 1e-9)
        assertEquals(4, chart.totalNotes)
        assertEquals(1, chart.holdCount)
        // sắp theo thời gian rồi theo cột
        assertEquals(listOf(0, 3, 2, 1), chart.notes.map { it.column })
        assertEquals(listOf(500, 500, 1000, 6000), chart.notes.map { it.timeMs })
        val h = chart.notes[2]
        assertTrue(h.isHold)
        assertEquals(1600, h.endTimeMs)
        assertEquals(6000, chart.lastEventTimeMs)
        // hit sample rơi về timing point: trước 5000 dùng điểm đầu (set 1, volume 60), sau 5000 dùng set 2, index 3, volume 40
        assertEquals(60, chart.notes[0].sample.volume)
        assertEquals(listOf("normal-hitnormal"), chart.notes[0].sample.sampleNames())
        val late = chart.notes[3].sample
        assertEquals(40, late.volume)
        assertEquals(listOf("soft-hitnormal3"), late.sampleNames())
    }

    @Test
    fun rejectsNonManiaAndEmpty() {
        try {
            ManiaChartParser.parse("[General]\nMode: 0\n[HitObjects]\n256,192,100,1,0")
            fail()
        } catch (e: ChartParseException) { /* ok */ }
        try {
            ManiaChartParser.parse(chartText(objects = emptyList()))
            fail()
        } catch (e: ChartParseException) { /* ok */ }
    }

    @Test
    fun supportsKeyCountsFromOneThroughTwenty() {
        for (keys in 1..20) {
            val chart = ManiaChartParser.parse(
                chartText(objects = listOf("511,192,1000,1,0,0:0:0:0:"), keys = keys)
            )
            assertEquals(keys, chart.keys)
            assertEquals(keys - 1, chart.notes.single().column)
        }
    }

    @Test
    fun playfieldFitsPortraitAndLandscapeForEverySupportedKeyCount() {
        val sizes = listOf(1080f to 2340f, 2340f to 1080f)
        for (keys in 1..20) {
            for ((width, height) in sizes) {
                val layout = PlayfieldLayout(width, height, keys)
                assertTrue(layout.columnWidth > 0f)
                assertTrue(layout.left >= 0f)
                assertTrue(layout.right <= width)
                assertEquals(keys - 1, layout.columnAt(layout.right))
            }
        }
    }

    @Test
    fun landscapePlayfieldKeepsSideGuttersForWideCharts() {
        for (keys in 1..20) {
            val width = 2340f
            val layout = PlayfieldLayout(width, 1080f, keys)
            assertTrue(layout.left >= width * 0.08f - 1f)
            assertTrue(width - layout.right >= width * 0.08f - 1f)
        }
    }

    @Test
    fun landscapeFourToSixKeyPlayfieldUsesAvailableWidth() {
        val sizes = listOf(1920f to 1080f, 1280f to 720f, 1600f to 1200f)
        for (keys in 4..6) {
            for ((width, height) in sizes) {
                val landscape = PlayfieldLayout(width, height, keys)
                val portrait = PlayfieldLayout(height, width, keys)
                assertEquals(width * 0.84f, landscape.right - landscape.left, 1f)
                assertTrue(landscape.right - landscape.left > portrait.right - portrait.left)
            }
        }
    }

    @Test
    fun perspectivePlayfieldAcceleratesNotesAndScalesThemInBothDimensions() {
        val layout = PlayfieldLayout(1920f, 1080f, 4)
        val projection = LandscapeProjection(layout, 1920f)
        val farScale = projection.scale(0f)
        val nearScale = projection.scale(1f)
        val earlyTravel = projection.yAt(0.2f) - projection.yAt(0.1f)
        val lateTravel = projection.yAt(0.9f) - projection.yAt(0.8f)

        val fieldWidth = layout.right - layout.left
        val originalBaseWidth = minOf(fieldWidth, 2f * layout.judgeY / 1.7320508f)
        assertEquals(originalBaseWidth * 0.125f / fieldWidth, farScale, 0.001f)
        assertEquals(1f, nearScale, 0.001f)
        assertTrue(projection.yAt(0f) >= projection.topY)
        assertEquals(layout.judgeY, projection.yAt(1f), 0.001f)
        assertTrue(lateTravel > earlyTravel)
        for (progress in listOf(0f, 0.2f, 0.5f, 0.9f, 1f, 1.2f)) {
            assertEquals(progress, projection.progressAtY(projection.yAt(progress)), 0.001f)
        }
        assertTrue(projection.boundaryAt(0, 0f) > layout.left)
        assertEquals(layout.left, projection.boundaryAt(0, 1f), 0.001f)
        assertEquals(layout.right, projection.boundaryAt(4, 1f), 0.001f)
        assertTrue(projection.boundaryAt(0, 1.2f) < layout.left)
        assertTrue(projection.noteWidth(0f) < projection.noteWidth(1f))
        assertTrue(projection.noteHeight(0f) < projection.noteHeight(1f))
        assertEquals(layout.noteHeight, projection.noteHeight(1f), 0.001f)
    }

    @Test
    fun parsesRealBeatmapIfPresent() {
        val dir = File("src/main/java/com/example/gamemodule/maniamodule/songs/2424991")
        val file = dir.listFiles { f -> f.name.contains("Dour") && f.extension == "osu" }?.firstOrNull()
        assumeTrue(file != null)
        val chart = ManiaChartParser.parse(file!!.readText())
        assertEquals(4, chart.keys)
        assertEquals(8.0, chart.overallDifficulty, 0.0)
        assertEquals(2143, chart.totalNotes)
        assertEquals(22, chart.holdCount)
        assertEquals(1452, chart.notes.first().timeMs)
        assertTrue(chart.notes.first().isHold)
        assertEquals(1637, chart.notes.first().endTimeMs)
        assertEquals("KASHIWA Daisuke - SHARAKU.mp3", chart.audioFilename)
        // mọi cột đều có nốt và mỗi cột được sắp theo thời gian
        for (c in 0 until 4) {
            val ids = chart.columns[c]
            assertTrue(ids.isNotEmpty())
            for (i in 1 until ids.size) assertTrue(chart.notes[ids[i]].timeMs >= chart.notes[ids[i - 1]].timeMs)
        }
    }

    // ------------------------------------------------------------------ hit windows / score

    @Test
    fun hitWindowsMatchOsuWiki() {
        val w = HitWindows(8.0)
        assertEquals(16.0, w.perfect, 1e-9)
        assertEquals(40.0, w.great, 1e-9)   // 64 - 24
        assertEquals(73.0, w.good, 1e-9)    // 97 - 24
        assertEquals(103.0, w.ok, 1e-9)     // 127 - 24
        assertEquals(127.0, w.meh, 1e-9)    // 151 - 24
        assertEquals(164.0, w.miss, 1e-9)   // 188 - 24
        val w0 = HitWindows(0.0)
        assertEquals(64.0, w0.great, 1e-9)
        assertEquals(188.0, w0.miss, 1e-9)
    }

    @Test
    fun allPerfectIsExactlyOneMillionAndSS() {
        val t = ScoreTracker(250)
        repeat(250) { t.apply(Judgement.PERFECT) }
        assertEquals(1_000_000L, t.score)
        assertEquals(1.0, t.accuracy, 0.0)
        assertEquals("SS", t.rank)
        assertEquals(250, t.result().trueCombo)
        assertEquals(250, t.maxCombo)
    }

    @Test
    fun allGreatScoresBaseOnly() {
        // base = 500000 * 300/320, bonus giữ 100 nên phần bonus = 500000  ->  968750
        val t = ScoreTracker(400)
        repeat(400) { t.apply(Judgement.GREAT) }
        assertEquals(968_750L, t.score)
        assertEquals("SS", t.rank) // 100% accuracy vì GREAT tính 100%
    }

    @Test
    fun missResetsBonusAndCombo() {
        val t = ScoreTracker(4)
        t.apply(Judgement.PERFECT)
        t.apply(Judgement.PERFECT)
        t.apply(Judgement.MISS)
        assertEquals(0, t.combo)
        assertEquals(2, t.maxCombo)
        t.apply(Judgement.PERFECT) // bonus = 0 + 2 = 2 -> sqrt(2) thay vì 10
        assertEquals(2, t.result().trueCombo)
        assertEquals(4, t.result().maxCombo)
        val unit = 500_000.0 / 4
        val expected = unit * (320 / 320.0) + unit * (32 * 10.0 / 320) +   // nốt 1
            unit * (320 / 320.0) + unit * (32 * 10.0 / 320) +             // nốt 2
            0.0 +                                                          // miss
            unit * (320 / 320.0) + unit * (32 * Math.sqrt(2.0) / 320)      // nốt 3
        assertEquals(Math.round(expected), t.score)
    }

    @Test
    fun rankThresholds() {
        fun rank(sixths: Long, n: Int) = ScoreTracker.rankFor(sixths, n)
        val n = 100
        val full = 6L * n
        assertEquals("SS", rank(full, n))
        assertEquals("S", rank(full - 1, n))              // 99.83%
        assertEquals("A", rank(570, n))                   // đúng 95% -> không phải S (stable: > 95%)
        assertEquals("S", rank(571, n))
        assertEquals("B", rank(540, n))                   // đúng 90% -> B
        assertEquals("A", rank(541, n))
        assertEquals("C", rank(480, n))                   // đúng 80% -> C
        assertEquals("D", rank(420, n))                   // đúng 70% -> D
        assertEquals("C", rank(421, n))
    }

    @Test
    fun accuracyWeights() {
        val t = ScoreTracker(6)
        t.apply(Judgement.PERFECT); t.apply(Judgement.GREAT); t.apply(Judgement.GOOD)
        t.apply(Judgement.OK); t.apply(Judgement.MEH); t.apply(Judgement.MISS)
        // (300 + 300 + 200 + 100 + 50 + 0) / (300 * 6)
        assertEquals(950.0 / 1800.0, t.accuracy, 1e-12)
    }

    // ------------------------------------------------------------------ engine: tap

    @Test
    fun tapJudgementsFollowWindows() {
        val chart = ManiaChartParser.parse(chartText(objects = listOf(tap(0, 1000), tap(1, 2000), tap(2, 3000), tap(3, 4000))))
        val (e, _, rec) = engineFor(chart)
        e.press(0, 1010.0); assertEquals(Judgement.PERFECT, rec.last())          // 10ms
        e.press(1, 2030.0); assertEquals(Judgement.GREAT, rec.last())            // 30ms <= 40
        e.press(2, 2940.0); assertEquals(Judgement.GOOD, rec.last())             // 60ms: 40 < 60 <= 73
        e.press(3, 3900.0)                                                       // nốt tại 4000: sớm 100ms
        assertEquals(Judgement.OK, rec.last())                                   // 73 < 100 <= 103
    }

    @Test
    fun oneTouchCanJudgeEveryNoteOverlappingItsRegion() {
        val objects = (0 until 8).map { column ->
            "${column * 64 + 32},192,1000,1,0,0:0:0:0:"
        }
        val chart = ManiaChartParser.parse(chartText(objects = objects, keys = 8))
        val (engine, _, recorder) = engineFor(chart)
        val router = TouchInputRouter(keys = 8, left = 0f, columnWidth = 100f, touchRadius = 24f)

        router.press(pointerId = 1L, x = 99f)!!.pressed.forEach { engine.press(it, 1000.0) }

        assertEquals(2, recorder.judgements.size)
        assertTrue(recorder.judgements.all { it.second == Judgement.PERFECT })
    }

    @Test
    fun earlyAndLateEdgeCases() {
        val chart = ManiaChartParser.parse(chartText(objects = listOf(tap(0, 1000), tap(1, 2000), tap(2, 3000), tap(3, 4000))))
        val (e, tracker, rec) = engineFor(chart)

        // quá sớm (> miss window 164ms): không tác dụng
        e.press(0, 1000.0 - 165.0)
        assertTrue(rec.judgements.isEmpty())
        // trong (meh, miss] = 127..164 sớm: MISS và tiêu thụ nốt
        e.release(0, 900.0)
        e.press(0, 1000.0 - 150.0)
        assertEquals(Judgement.MISS, rec.last())
        // sớm trong cửa sổ MEH
        e.press(1, 2000.0 - 120.0)
        assertEquals(Judgement.MEH, rec.last())
        // muộn quá OK (103ms) là MISS do advance, không có MEH muộn
        e.advance(3000.0 + 104.0)
        assertEquals(Judgement.MISS, rec.last())
        assertEquals(3, rec.judgements.size)
        // nốt cuối nhấn chính xác
        e.press(3, 4000.0)
        assertEquals(Judgement.PERFECT, rec.last())
        assertTrue(e.isFinished)
        // chuỗi MISS, MEH, MISS, PERFECT -> combo chỉ đạt 1
        assertEquals(1, tracker.maxCombo)
        assertEquals(1, tracker.combo)
    }

    @Test
    fun ignoredPressDoesNotConsumeNote() {
        val chart = ManiaChartParser.parse(chartText(objects = listOf(tap(0, 1000))))
        val (e, _, rec) = engineFor(chart)
        e.press(0, 100.0)
        assertTrue(rec.judgements.isEmpty())
        e.release(0, 120.0)
        e.press(0, 1002.0)
        assertEquals(Judgement.PERFECT, rec.last())
        assertEquals(listOf(0), rec.hits)
    }

    @Test
    fun rendererHitStateDistinguishesHitNotesFromMissedNotes() {
        val chart = ManiaChartParser.parse(
            chartText(objects = listOf(tap(0, 1000), tap(1, 1500))),
        )
        val (e, _, _) = engineFor(chart)
        e.press(0, 1000.0)
        e.advance(1700.0)

        assertTrue(e.hit[chart.columns[0].single()])
        assertFalse(e.hit[chart.columns[1].single()])
    }

    // ------------------------------------------------------------------ engine: hold

    @Test
    fun holdPerfectPlayIsPerfect() {
        val chart = ManiaChartParser.parse(chartText(objects = listOf(hold(0, 1000, 2000))))
        val (e, tracker, rec) = engineFor(chart)
        e.press(0, 1005.0)
        assertTrue(rec.judgements.isEmpty()) // chưa có phán định cho tới khi nhả
        assertEquals(1, rec.hits.size)
        assertTrue(e.hit[chart.columns[0].single()])
        e.release(0, 2008.0)                 // head 5 + tail 8 = 13 <= 38.4
        assertEquals(Judgement.PERFECT, rec.last())
        assertEquals(1_000_000L, tracker.score)
    }

    @Test
    fun holdCombinedErrorDowngrades() {
        // head 10 (<= 19.2), tail 60 -> combined 70 > 38.4 (PERFECT) ; > great*2.2 = 88? 70 <= 88 và head 10 <= 44 -> GREAT
        run {
            val (e, _, rec) = engineFor(ManiaChartParser.parse(chartText(objects = listOf(hold(0, 1000, 2000)))))
            e.press(0, 1010.0); e.release(0, 2060.0)
            assertEquals(Judgement.GREAT, rec.last())
        }
        // head 10, tail 100 -> combined 110 > 88; good: head<=73, combined <= 146 -> GOOD
        run {
            val (e, _, rec) = engineFor(ManiaChartParser.parse(chartText(objects = listOf(hold(0, 1000, 2000)))))
            e.press(0, 1010.0); e.release(0, 1900.0)
            assertEquals(Judgement.GOOD, rec.last())
        }
    }

    @Test
    fun releasingInBodyCapsAtMehAndNoHoldIsMiss() {
        run {
            val (e, _, rec) = engineFor(ManiaChartParser.parse(chartText(objects = listOf(hold(0, 1000, 2000)))))
            e.press(0, 1000.0)
            e.release(0, 1400.0)               // nhả giữa thân
            assertTrue(rec.judgements.isEmpty())
            e.press(0, 1700.0)                 // bấm lại rồi giữ tới đuôi
            e.release(0, 2000.0)
            assertEquals(Judgement.MEH, rec.last())
        }
        run {
            val (e, tracker, rec) = engineFor(ManiaChartParser.parse(chartText(objects = listOf(hold(0, 1000, 2000)))))
            e.press(0, 1000.0)
            e.release(0, 1400.0)               // nhả và không bấm lại
            e.advance(2000.0 + 104.0)
            assertEquals(Judgement.MISS, rec.last())
            assertEquals(0, tracker.combo)
        }
        run {
            val (e, _, rec) = engineFor(ManiaChartParser.parse(chartText(objects = listOf(hold(0, 1000, 2000)))))
            e.advance(1200.0) // head không được nhấn: vẫn còn cứu muộn được tới cửa sổ đuôi
            assertTrue(rec.judgements.isEmpty())
            e.advance(2000.0 - 127.0 + 1.0)
            assertEquals(Judgement.MISS, rec.last())
        }
    }

    @Test
    fun heldPastTailDeadlineIsJudgedByDeadline() {
        val (e, _, rec) = engineFor(ManiaChartParser.parse(chartText(objects = listOf(hold(0, 1000, 2000)))))
        e.press(0, 1000.0)
        e.advance(2000.0 + 104.0) // vẫn giữ phím quá hạn
        // giữ quá hạn: xử lý như nhả đúng lúc hết cửa sổ OK muộn (tail = 103): head 0 + 103 <= GOOD*2 = 146 -> GOOD
        assertEquals(Judgement.GOOD, rec.last())
    }

    @Test
    fun lateHeadRescueOfHoldIsAtMostMeh() {
        val (e, _, rec) = engineFor(ManiaChartParser.parse(chartText(objects = listOf(hold(0, 1000, 2000)))))
        e.press(0, 1300.0)                 // quá cửa sổ OK của head
        e.release(0, 2000.0)
        assertEquals(Judgement.MEH, rec.last())
    }

    // ------------------------------------------------------------------ simulated full plays

    private fun simulate(chart: ManiaChart, jitter: (ChartNote, Boolean) -> Double): Pair<ScoreTracker, Recorder> {
        val (e, tracker, rec) = engineFor(chart)
        data class Ev(val t: Double, val press: Boolean, val col: Int)
        val evs = ArrayList<Ev>()
        for (n in chart.notes) {
            evs += Ev(n.timeMs + jitter(n, true), true, n.column)
            val end = if (n.isHold) n.endTimeMs + jitter(n, false) else n.timeMs + jitter(n, true) + 30.0
            evs += Ev(end, false, n.column)
        }
        evs.sortWith(compareBy({ it.t }, { if (it.press) 1 else 0 })) // nhả trước nhấn khi trùng giờ
        for (ev in evs) if (ev.press) e.press(ev.col, ev.t) else e.release(ev.col, ev.t)
        e.finish()
        assertTrue(e.isFinished)
        return tracker to rec
    }

    @Test
    fun perfectPlayOfRealBeatmapIsOneMillion() {
        val chart = loadReal() ?: return
        val (tracker, _) = simulate(chart) { _, _ -> 0.0 }
        assertEquals(1_000_000L, tracker.score)
        assertEquals("SS", tracker.rank)
        assertEquals(chart.totalNotes, tracker.maxCombo)
    }

    @Test
    fun jitteredPlayStaysWithinGreatWindow() {
        val chart = loadReal() ?: return
        var seed = 12345L
        fun rnd(): Double { seed = (seed * 6364136223846793005L + 1442695040888963407L); return ((seed ushr 33) % 2001) / 1000.0 - 1.0 }
        val (tracker, rec) = simulate(chart) { _, _ -> rnd() * 35.0 } // |sai lệch| <= 35ms (OD8 GREAT = 40ms)
        assertEquals(0, tracker.count(Judgement.MISS))
        assertEquals(0, tracker.count(Judgement.GOOD))
        assertTrue(tracker.score in 900_000..1_000_000)
        assertEquals(chart.totalNotes, rec.judgements.size)
        assertEquals("SS", tracker.rank)
    }

    @Test
    fun skippingEverythingIsAllMiss() {
        val chart = loadReal() ?: return
        val (e, tracker, _) = engineFor(chart)
        e.advance(chart.lastEventTimeMs + 1000.0)
        assertTrue(e.isFinished)
        assertEquals(0L, tracker.score)
        assertEquals(chart.totalNotes, tracker.count(Judgement.MISS))
        assertEquals("D", tracker.rank)
    }

    private fun loadReal(): ManiaChart? {
        val dir = File("src/main/java/com/example/gamemodule/maniamodule/songs/2424991")
        val file = dir.listFiles { f -> f.name.contains("Dour") && f.extension == "osu" }?.firstOrNull() ?: return null
        return ManiaChartParser.parse(file.readText())
    }

    // ------------------------------------------------------------------ scroll

    @Test
    fun scrollSpeedMapping() {
        assertEquals(600.0, NoteScroll.travelTimeMs(10.0), 1e-9)
        assertEquals(400.0, NoteScroll.travelTimeMs(15.0), 1e-9)
        assertEquals(60_000.0, NoteScroll.travelTimeMs(0.1), 1e-6)
        assertEquals(NoteScroll.travelTimeMs(15.0), NoteScroll.travelTimeMs(99.0), 1e-9) // kẹp biên
        val travel = 600.0
        assertEquals(800f, NoteScroll.yAt(1000.0, 1000.0, travel, 800f), 1e-3f)   // tới vạch phán định
        assertEquals(0f, NoteScroll.yAt(1600.0, 1000.0, travel, 800f), 1e-3f)     // vừa xuất hiện ở mép trên
        assertEquals(400f, NoteScroll.yAt(1300.0, 1000.0, travel, 800f), 1e-3f)
        assertTrue(NoteScroll.yAt(900.0, 1000.0, travel, 800f) > 800f)            // đã qua vạch
    }

    // ------------------------------------------------------------------ clock

    private class FakeNano { var ns = 1_000_000_000L; fun advanceMs(ms: Double) { ns += (ms * 1e6).toLong() } }

    @Test
    fun clockRunsFreeThenHoldsAtZeroUntilAudio() {
        val f = FakeNano()
        val c = GameClock { f.ns }
        c.start(-2000.0)
        f.advanceMs(1000.0)
        assertEquals(-1000.0, c.now(), 1e-6)
        assertFalse(c.shouldStartAudio())       // lead = 0: chưa tới t = 0
        assertTrue(c.shouldStartAudio(1500.0))  // lead 1500ms: t = -1000 đã tới lúc bắn start
        assertFalse(c.shouldStartAudio(1500.0)) // chỉ một lần
        f.advanceMs(1500.0)               // quá 0 mà nhạc chưa phát -> giữ ở 0
        assertEquals(0.0, c.now(), 1e-9)
        assertTrue(c.isWaitingForAudio)
        f.advanceMs(80.0)
        // nhạc thật sự phát: media time 0 tại thời điểm cách đây 60ms
        c.onAudioAnchor(AudioAnchor(0.0, f.ns - 60_000_000L))
        assertEquals(60.0, c.now(), 1e-6)
        f.advanceMs(16.0)
        assertEquals(76.0, c.now(), 1e-6)
    }

    @Test
    fun clockGivesUpOnAudio() {
        val f = FakeNano()
        val c = GameClock { f.ns }
        c.start(-100.0)
        f.advanceMs(500.0)
        assertEquals(0.0, c.now(), 1e-9)
        c.giveUpOnAudio()
        f.advanceMs(40.0)
        assertEquals(40.0, c.now(), 1e-6)
    }

    @Test
    fun clockFollowsAudioSmoothlyWithoutJumps() {
        val f = FakeNano()
        val c = GameClock { f.ns }
        c.start(0.0)
        // âm thanh đi lệch 30ms so với đồng hồ hệ thống + nhiễu +-3ms
        var seed = 7L
        fun noise(): Double { seed = seed * 6364136223846793005L + 1; return ((seed ushr 40) % 6001) / 1000.0 - 3.0 }
        c.onAudioAnchor(AudioAnchor(0.0, f.ns))
        var prev = c.now()
        var maxStep = 0.0
        var minStepRate = Double.MAX_VALUE
        repeat(600) { i ->
            f.advanceMs(16.667)
            val media = (i + 1) * 16.667 + 30.0 + noise()
            c.onAudioAnchor(AudioAnchor(media, f.ns))
            val now = c.now()
            val step = now - prev
            maxStep = maxOf(maxStep, step)
            minStepRate = minOf(minStepRate, step)
            prev = now
        }
        assertTrue("không được lùi/khựng đáng kể (min step=$minStepRate)", minStepRate > 16.667 - 2.0)
        assertTrue("không được nhảy (max step=$maxStep)", maxStep < 16.667 + 2.0)
        // và đã bám về đúng lệch 30ms
        assertEquals(600 * 16.667 + 30.0, c.now(), 4.0)
    }

    @Test
    fun clockCorrectsLargeAudioDriftWithoutRewindingRenderedTime() {
        val f = FakeNano()
        val c = GameClock { f.ns }
        c.start(0.0)
        c.onAudioAnchor(AudioAnchor(0.0, f.ns))

        var previousFrameTime = c.now()
        repeat(30) { frame ->
            f.advanceMs(16.667)
            val realAudioTime = (frame + 1) * 16.667
            val corruptedAudioTime = if (frame == 10) realAudioTime - 1000.0 else realAudioTime
            c.onAudioAnchor(AudioAnchor(corruptedAudioTime, f.ns))
            val currentFrameTime = c.now()
            assertTrue(
                "Chart time must not move backward (previous=$previousFrameTime, current=$currentFrameTime)",
                currentFrameTime >= previousFrameTime,
            )
            assertTrue(
                "A bad audio anchor must not cause a visible frame jump",
                currentFrameTime - previousFrameTime < 18.2,
            )
            previousFrameTime = currentFrameTime
        }
    }

    // ------------------------------------------------------------------ core end to end (60fps, offset)

    @Test
    fun coreEndToEndWithOffset() {
        val chart = ManiaChartParser.parse(chartText(objects = listOf(tap(0, 1000), hold(1, 1500, 2200), tap(2, 3000))))
        val f = FakeNano()
        val rec = Recorder()
        val core = GameCore(chart, GameSettings(offsetMs = 50), { f.ns }, rec)
        core.begin(withAudio = false)
        // nốt tới muộn 50ms so với nhạc => bấm tại thời gian nhạc = note + 50 là hoàn hảo
        val presses = ArrayList<Triple<Double, Boolean, Int>>()
        for (n in chart.notes) {
            presses += Triple(n.timeMs + 50.0, true, n.column)
            presses += Triple((if (n.isHold) n.endTimeMs else n.timeMs + 40).toDouble() + 50.0, false, n.column)
        }
        presses.sortBy { it.first }
        var next = 0
        val startNs = f.ns
        while (!core.isFinished()) {
            f.advanceMs(1000.0 / 60)
            val songMs = (f.ns - startNs) / 1e6 - core.prerollMs
            while (next < presses.size && presses[next].first <= songMs) {
                val (t, down, col) = presses[next++]
                val nano = startNs + ((t + core.prerollMs) * 1e6).toLong()
                if (down) core.press(col, nano) else core.release(col, nano)
            }
            core.frame()
            assertTrue(f.ns - startNs < 20_000_000_000L)
        }
        val r = core.result()
        assertEquals(3, r.totalNotes)
        assertEquals(3, r.perfect)
        assertEquals(1_000_000L, r.score)
        assertEquals("SS", r.rank)
        assertNotNull(rec)
    }

    // ------------------------------------------------------------------ settings

    @Test
    fun settingsRoundTripAndClamp() {
        val dir = createTempDirectory().toFile()
        val store = SettingsStore(File(dir, "sub/settings.properties"))
        assertEquals(GameSettings(), store.load()) // chưa có file -> mặc định

        val s = GameSettings(
            musicVolume = 33,
            hitsoundVolume = 12,
            speed = 12.3,
            backgroundDim = 100,
            offsetMs = -42,
            playerName = "Hoàng Anh",
            playType = Landscape3DPerspective,
        )
        store.save(s)
        assertEquals(s, store.load())

        store.save(GameSettings(musicVolume = 500, hitsoundVolume = -5, speed = 99.0, backgroundDim = 250, offsetMs = 9999, playerName = "   "))
        val c = store.load()
        assertEquals(100, c.musicVolume)
        assertEquals(0, c.hitsoundVolume)
        assertEquals(15.0, c.speed, 0.0)
        assertEquals(100, c.backgroundDim)
        assertEquals(GameSettings.OFFSET_MAX, c.offsetMs)
        assertEquals(GameSettings.DEFAULT_PLAYER_NAME, c.playerName)
        assertEquals(Portrait, c.playType)

        assertEquals(0.1, GameSettings(speed = 0.0).coerced().speed, 0.0)
        assertEquals(0.1, GameSettings(speed = 0.04).coerced().speed, 0.0)

        // file hỏng / giá trị rác -> dùng mặc định từng trường
        File(dir, "sub/settings.properties").writeText("speed=abc\naudio.music=77\noffset.ms=\n\u0000junk")
        val d = store.load()
        assertEquals(77, d.musicVolume)
        assertEquals(GameSettings().speed, d.speed, 0.0)
        assertEquals(GameSettings().offsetMs, d.offsetMs)
        assertEquals(GameSettings().playType, d.playType)
        dir.deleteRecursively()
    }

    @Test
    fun settingsStoreMigratesPreviousLandscapePreferences() {
        val dir = createTempDirectory().toFile()
        val file = File(dir, "settings.properties")
        val store = SettingsStore(file)
        file.writeText("screen.orientation=LANDSCAPE\nscroll.style=PERSPECTIVE_3D")

        assertEquals(Landscape3DPerspective, store.load().playType)

        file.writeText("screen.orientation=LANDSCAPE\nscroll.style=VERTICAL")
        assertEquals(LandscapeVerticalScrolling, store.load().playType)

        file.writeText("screen.orientation=PORTRAIT\nscroll.style=PERSPECTIVE_3D")
        assertEquals(Portrait, store.load().playType)
        dir.deleteRecursively()
    }

    @Test
    fun noteSampleNames() {
        val chart = ManiaChartParser.parse(
            chartText(objects = listOf("64,192,100,1,14,3:2:2:80:custom.wav", "192,192,200,1,0,0:0:0:0:"))
        )
        val a = chart.notes[0].sample
        assertEquals(listOf("custom.wav", "soft-hitwhistle2", "soft-hitfinish2", "soft-hitclap2"), a.sampleNames().map { it })
        assertEquals(80, a.volume)
        assertTrue(abs(a.volume - 80) == 0)
    }

    @Test
    fun defaultHitsoundWavAndSampleLookup() {
        val wav = DefaultHitsound.generate()
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals("WAVE", String(wav, 8, 4))
        assertEquals(44 + 44100 * 60 / 1000 * 2, wav.size)

        val dir = createTempDirectory().toFile()
        File(dir, "Normal-HitNormal.WAV").writeBytes(wav)
        File(dir, "soft-hitclap2.ogg").writeBytes(wav)
        val lookup = SampleLookup(dir)
        assertNotNull(lookup.find("normal-hitnormal"))
        assertNotNull(lookup.find("soft-hitclap2"))
        assertNotNull(lookup.find("NORMAL-hitnormal.wav"))
        assertEquals(null, lookup.find("drum-hitfinish"))
        assertEquals(null, SampleLookup(null).find("x"))
        dir.deleteRecursively()
    }
}
