package vn.kozu.game.mania.gamestage.ui

/**
 * Maps up to ten simultaneous pointers to gameplay columns.
 * Wide charts treat the touch as a finite horizontal region that may overlap several columns.
 */
internal class TouchInputRouter(
    private val keys: Int,
    private val left: Float,
    private val columnWidth: Float,
    private val touchRadius: Float,
) {
    private val pointers = HashMap<Long, IntArray>()
    private val activePointersByColumn = IntArray(keys)

    data class Changes(val pressed: IntArray = intArrayOf(), val released: IntArray = intArrayOf())

    fun press(pointerId: Long, x: Float): Changes? {
        if (pointerId in pointers || pointers.size >= MAX_SIMULTANEOUS_TOUCHES) return null

        val columns = columnsAt(x)
        pointers[pointerId] = columns
        return Changes(pressed = columns.filter { activePointersByColumn[it]++ == 0 }.toIntArray())
    }

    fun move(pointerId: Long, x: Float): Changes {
        val previous = pointers[pointerId] ?: return Changes()
        val current = columnsAt(x)
        if (previous.contentEquals(current)) return Changes()

        val released = previous.filter { it !in current && --activePointersByColumn[it] == 0 }.toIntArray()
        val pressed = current.filter { it !in previous && activePointersByColumn[it]++ == 0 }.toIntArray()
        pointers[pointerId] = current
        return Changes(pressed = pressed, released = released)
    }

    /** Returns columns that are no longer held after this pointer is lifted. */
    fun release(pointerId: Long): Changes {
        val columns = pointers.remove(pointerId) ?: return Changes()
        return Changes(released = columns.filter { --activePointersByColumn[it] == 0 }.toIntArray())
    }

    private fun columnsAt(x: Float): IntArray {
        return if (keys > WIDE_CHART_THRESHOLD) {
            val touchLeft = x - touchRadius
            val touchRight = x + touchRadius
            val overlappingColumns = (0 until keys).filter { column ->
                val columnLeft = left + column * columnWidth
                val columnRight = columnLeft + columnWidth
                columnRight > touchLeft && columnLeft < touchRight
            }
            if (overlappingColumns.isEmpty()) {
                intArrayOf(((x - left) / columnWidth).toInt().coerceIn(0, keys - 1))
            } else {
                overlappingColumns.toIntArray()
            }
        } else {
            intArrayOf(((x - left) / columnWidth).toInt().coerceIn(0, keys - 1))
        }
    }

    companion object {
        const val MAX_SIMULTANEOUS_TOUCHES = 10
        private const val WIDE_CHART_THRESHOLD = 6
    }
}
