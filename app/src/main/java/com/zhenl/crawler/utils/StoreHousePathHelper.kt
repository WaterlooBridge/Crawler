package com.zhenl.crawler.utils

import android.util.SparseArray
import java.util.*

/**
 * Created by lin on 2021/6/19.
 */
object StoreHousePathHelper {

    private var sPointList = SparseArray<FloatArray>()

    init {
        val letters = arrayOf(
            floatArrayOf(
                // A
                24f, 0f, 1f, 22f,
                1f, 22f, 1f, 72f,
                24f, 0f, 47f, 22f,
                47f, 22f, 47f, 72f,
                1f, 48f, 47f, 48f
            ),

            floatArrayOf(
                // B
                0f, 0f, 0f, 72f,
                0f, 0f, 37f, 0f,
                37f, 0f, 47f, 11f,
                47f, 11f, 47f, 26f,
                47f, 26f, 38f, 36f,
                38f, 36f, 0f, 36f,
                38f, 36f, 47f, 46f,
                47f, 46f, 47f, 61f,
                47f, 61f, 38f, 71f,
                37f, 72f, 0f, 72f,
            ),

            floatArrayOf(
                // C
                47f, 0f, 0f, 0f,
                0f, 0f, 0f, 72f,
                0f, 72f, 47f, 72f,
            ),

            floatArrayOf(
                // D
                0f, 0f, 0f, 72f,
                0f, 0f, 24f, 0f,
                24f, 0f, 47f, 22f,
                47f, 22f, 47f, 48f,
                47f, 48f, 23f, 72f,
                23f, 72f, 0f, 72f,
            ),

            floatArrayOf(
                // E
                0f, 0f, 0f, 72f,
                0f, 0f, 47f, 0f,
                0f, 36f, 37f, 36f,
                0f, 72f, 47f, 72f,
            ),

            floatArrayOf(
                // F
                0f, 0f, 0f, 72f,
                0f, 0f, 47f, 0f,
                0f, 36f, 37f, 36f,
            ),

            floatArrayOf(
                // G
                47f, 23f, 47f, 0f,
                47f, 0f, 0f, 0f,
                0f, 0f, 0f, 72f,
                0f, 72f, 47f, 72f,
                47f, 72f, 47f, 48f,
                47f, 48f, 24f, 48f,
            ),

            floatArrayOf(
                // H
                0f, 0f, 0f, 72f,
                0f, 36f, 47f, 36f,
                47f, 0f, 47f, 72f,
            ),

            floatArrayOf(
                // I
                0f, 0f, 47f, 0f,
                24f, 0f, 24f, 72f,
                0f, 72f, 47f, 72f,
            ),

            floatArrayOf(
                // J
                47f, 0f, 47f, 72f,
                47f, 72f, 24f, 72f,
                24f, 72f, 0f, 48f,
            ),

            floatArrayOf(
                // K
                0f, 0f, 0f, 72f,
                47f, 0f, 3f, 33f,
                3f, 38f, 47f, 72f,
            ),

            floatArrayOf(
                // L
                0f, 0f, 0f, 72f,
                0f, 72f, 47f, 72f,
            ),

            floatArrayOf(
                // M
                0f, 0f, 0f, 72f,
                0f, 0f, 24f, 23f,
                24f, 23f, 47f, 0f,
                47f, 0f, 47f, 72f,
            ),

            floatArrayOf(
                // N
                0f, 0f, 0f, 72f,
                0f, 0f, 47f, 72f,
                47f, 72f, 47f, 0f,
            ),

            floatArrayOf(
                // O
                0f, 0f, 0f, 72f,
                0f, 72f, 47f, 72f,
                47f, 72f, 47f, 0f,
                47f, 0f, 0f, 0f,
            ),

            floatArrayOf(
                // P
                0f, 0f, 0f, 72f,
                0f, 0f, 47f, 0f,
                47f, 0f, 47f, 36f,
                47f, 36f, 0f, 36f,
            ),

            floatArrayOf(
                // Q
                0f, 0f, 0f, 72f,
                0f, 72f, 23f, 72f,
                23f, 72f, 47f, 48f,
                47f, 48f, 47f, 0f,
                47f, 0f, 0f, 0f,
                24f, 28f, 47f, 71f,
            ),

            floatArrayOf(
                // R
                0f, 0f, 0f, 72f,
                0f, 0f, 47f, 0f,
                47f, 0f, 47f, 36f,
                47f, 36f, 0f, 36f,
                0f, 37f, 47f, 72f,
            ),

            floatArrayOf(
                // S
                47f, 0f, 0f, 0f,
                0f, 0f, 0f, 36f,
                0f, 36f, 47f, 36f,
                47f, 36f, 47f, 72f,
                47f, 72f, 0f, 72f,
            ),

            floatArrayOf(
                // T
                0f, 0f, 47f, 0f,
                24f, 0f, 24f, 72f,
            ),

            floatArrayOf(
                // U
                0f, 0f, 0f, 72f,
                0f, 72f, 47f, 72f,
                47f, 72f, 47f, 0f,
            ),

            floatArrayOf(
                // V
                0f, 0f, 24f, 72f,
                24f, 72f, 47f, 0f,
            ),

            floatArrayOf(
                // W
                0f, 0f, 0f, 72f,
                0f, 72f, 24f, 49f,
                24f, 49f, 47f, 72f,
                47f, 72f, 47f, 0f
            ),

            floatArrayOf(
                // X
                0f, 0f, 47f, 72f,
                47f, 0f, 0f, 72f
            ),

            floatArrayOf(
                // Y
                0f, 0f, 24f, 23f,
                47f, 0f, 24f, 23f,
                24f, 23f, 24f, 72f
            ),

            floatArrayOf(
                // Z
                0f, 0f, 47f, 0f,
                47f, 0f, 0f, 72f,
                0f, 72f, 47f, 72f
            ),
        )
        val numbers = arrayOf(
            floatArrayOf(
                // 0
                0f, 0f, 0f, 72f,
                0f, 72f, 47f, 72f,
                47f, 72f, 47f, 0f,
                47f, 0f, 0f, 0f,
            ),
            floatArrayOf(
                // 1
                24f, 0f, 24f, 72f,
            ),

            floatArrayOf(
                // 2
                0f, 0f, 47f, 0f,
                47f, 0f, 47f, 36f,
                47f, 36f, 0f, 36f,
                0f, 36f, 0f, 72f,
                0f, 72f, 47f, 72f
            ),

            floatArrayOf(
                // 3
                0f, 0f, 47f, 0f,
                47f, 0f, 47f, 36f,
                47f, 36f, 0f, 36f,
                47f, 36f, 47f, 72f,
                47f, 72f, 0f, 72f,
            ),

            floatArrayOf(
                // 4
                0f, 0f, 0f, 36f,
                0f, 36f, 47f, 36f,
                47f, 0f, 47f, 72f,
            ),

            floatArrayOf(
                // 5
                0f, 0f, 0f, 36f,
                0f, 36f, 47f, 36f,
                47f, 36f, 47f, 72f,
                47f, 72f, 0f, 72f,
                0f, 0f, 47f, 0f
            ),

            floatArrayOf(
                // 6
                0f, 0f, 0f, 72f,
                0f, 72f, 47f, 72f,
                47f, 72f, 47f, 36f,
                47f, 36f, 0f, 36f
            ),

            floatArrayOf(
                // 7
                0f, 0f, 47f, 0f,
                47f, 0f, 47f, 72f
            ),

            floatArrayOf(
                // 8
                0f, 0f, 0f, 72f,
                0f, 72f, 47f, 72f,
                47f, 72f, 47f, 0f,
                47f, 0f, 0f, 0f,
                0f, 36f, 47f, 36f
            ),

            floatArrayOf(
                // 9
                47f, 0f, 0f, 0f,
                0f, 0f, 0f, 36f,
                0f, 36f, 47f, 36f,
                47f, 0f, 47f, 72f,
            )
        )
        // A - Z
        for (i in letters.indices) {
            sPointList.append(
                i + 65,
                letters[i]
            )
        }
        // a - z
        for (i in letters.indices) {
            sPointList.append(
                i + 65 + 32,
                letters[i]
            )
        }
        // 0 - 9
        for (i in numbers.indices) {
            sPointList.append(
                i + 48,
                numbers[i]
            )
        }
        // blank
        sPointList.append(
            ' '.toInt(),
            floatArrayOf()
        )
        // -
        sPointList.append(
            '-'.toInt(),
            floatArrayOf(0f, 36f, 47f, 36f)
        )
        // .
        sPointList.append(
            '.'.toInt(),
            floatArrayOf(24f, 60f, 24f, 72f)
        )
    }

    /**
     * 根据符号和自提获取路径
     * @param str 字符串
     * @param scale 缩放
     * @param gapBetweenLetter 字符
     * @return ArrayList of float[] {x1, y1, x2, y2}
     */
    fun getPath(str: String, scale: Float, gapBetweenLetter: Int): MutableList<FloatArray> {
        val list: MutableList<FloatArray> = ArrayList()
        var offsetForWidth = 0f
        for (element in str) {
            val pos = element.toInt()
            val key = sPointList.indexOfKey(pos)
            if (key == -1) {
                continue
            }
            val points = sPointList[pos]
            val pointCount = points.size / 4
            for (j in 0 until pointCount) {
                val line = FloatArray(4)
                for (k in 0..3) {
                    val l = points[j * 4 + k]
                    // x
                    if (k % 2 == 0) {
                        line[k] = (l + offsetForWidth) * scale
                    } else {
                        line[k] = l * scale
                    }
                }
                list.add(line)
            }
            offsetForWidth += (57 + gapBetweenLetter).toFloat()
        }
        return list
    }
}