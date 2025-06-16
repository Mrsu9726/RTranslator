package nie.translator.rtranslator.tools

/**
 * targetStatus 目标状态标志，含义如下：
 * - 0x00: 无目标
 * - 0x01: 有运动目标
 * - 0x02: 有静止目标
 * - 0x03: 同时有运动+静止目标
 *
 * movingDistanceCm
 * 运动目标距离，单位：厘米（cm），例如 0x0046 = 70cm
 *
 * movingEnergy
 * 运动目标能量值，范围 0~100（表示运动信号强度）
 *
 * staticDistanceCm
 * 静止目标距离，单位：厘米（cm），例如 0x0096 = 150cm
 *
 * staticEnergy
 * 静止目标能量值，范围 0~100（表示静止信号强度）
 *
 * detectDistanceCm
 * 雷达探测距离门范围的最大值（即雷达此时正在工作的最远探测距离），单位：厘米（cm）
 * 例如 0x007F = 127cm，表示当前最大探测范围是 1.27m
 */
data class RadarTargetData(
    val targetStatus: Int,
    val movingDistanceCm: Int,
    val movingEnergy: Int,
    val staticDistanceCm: Int,
    val staticEnergy: Int,
    val detectDistanceCm: Int
)

object RadarPacketParser {

    private val FRAME_HEADER = byteArrayOf(0xF4.toByte(), 0xF3.toByte(), 0xF2.toByte(), 0xF1.toByte())
    private val FRAME_TAIL = byteArrayOf(0xF8.toByte(), 0xF7.toByte(), 0xF6.toByte(), 0xF5.toByte())

    private data class TimedEntry(val timestamp: Long, val distance: Int)
    private val recentDistances = ArrayDeque<Int>()
    private const val MAX_SIZE = 10
    private const val GROUP_WINDOW_CM = 10 // 窗口宽度：10cm
    private val VALID_MOVING_STATES = setOf(0x01, 0x03)

    fun parse(data: ByteArray): RadarTargetData? {
        if (data.size < 23) return null
        if (!data.take(4).toByteArray().contentEquals(FRAME_HEADER)) return null
        if (!data.takeLast(4).toByteArray().contentEquals(FRAME_TAIL)) return null

        val length = (data[5].toInt() and 0xFF shl 8) or (data[4].toInt() and 0xFF)
        if (length != 13) return null
        if (data[6] != 0x02.toByte() || data[7] != 0xAA.toByte() || data[17] != 0x55.toByte()) return null

        val targetStatus = data[8].toInt() and 0xFF
        val movingDistance = (data[9].toInt() and 0xFF) or ((data[10].toInt() and 0xFF) shl 8)
        val movingEnergy = data[11].toInt() and 0xFF
        val staticDistance = (data[12].toInt() and 0xFF) or ((data[13].toInt() and 0xFF) shl 8)
        val staticEnergy = data[14].toInt() and 0xFF
        val detectDistance = (data[15].toInt() and 0xFF) or ((data[16].toInt() and 0xFF) shl 8)

        if (targetStatus in VALID_MOVING_STATES) {
            addRecentDistance(movingDistance)
        }

        val smoothed = getModeLikeValue(recentDistances.toList(), GROUP_WINDOW_CM)

        return RadarTargetData(
            targetStatus = targetStatus,
            movingDistanceCm = smoothed,
            movingEnergy = movingEnergy,
            staticDistanceCm = staticDistance,
            staticEnergy = staticEnergy,
            detectDistanceCm = detectDistance
        )
    }

    private fun addRecentDistance(value: Int) {
        if (recentDistances.size >= MAX_SIZE) recentDistances.removeFirst()
        recentDistances.addLast(value)
    }

    private fun getModeLikeValue(distances: List<Int>, windowSizeCm: Int): Int {
        if (distances.isEmpty()) return 0
        val sorted = distances.sorted()
        var maxCount = 0
        var bestAvg = sorted[0]

        for (i in sorted.indices) {
            var count = 1
            var sum = sorted[i]
            for (j in i + 1 until sorted.size) {
                if (sorted[j] - sorted[i] <= windowSizeCm) {
                    count++
                    sum += sorted[j]
                } else break
            }
            if (count > maxCount) {
                maxCount = count
                bestAvg = sum / count
            }
        }
        return bestAvg
    }

    fun clearHistory() {
        recentDistances.clear()
    }
}



