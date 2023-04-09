package com.zhenl.crawler.utils

import com.zhenl.crawler.models.VideoModel

/**
 * Created by lin on 2021/8/8.
 */
object VideoHelper {

    fun getNextVideo(current: VideoModel, playlist: List<VideoModel>?): VideoModel? {
        if (playlist == null)
            return null
        val currentNum = extractNum(current.subtitle) ?: return null
        val position = playlist.indexOfFirst { it.url == current.url }
        if (position + 1 < playlist.size) {
            val next = playlist[position + 1]
            if (extractNum(next.subtitle) == currentNum + 1)
                return next
        }
        if (position - 1 >= 0) {
            val previous = playlist[position - 1]
            if (extractNum(previous.subtitle) == currentNum + 1)
                return previous
        }
        return null
    }

    private fun extractNum(text: String?): Int? {
        if (text == null)
            return null
        val result = text.filter { it.isDigit() }
        if (result.isEmpty())
            return null
        return result.toInt()
    }
}