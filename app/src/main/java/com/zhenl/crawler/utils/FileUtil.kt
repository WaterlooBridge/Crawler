package com.zhenl.crawler.utils

import android.text.TextUtils
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by lin on 20-1-21.
 */
object FileUtil {

    private const val MAX_EXTENSION_LENGTH = 4

    fun getFileNameFromUrl(url: String): String {
        return if (url.contains("?")) {
            url.substring(url.lastIndexOf("/") + 1, url.indexOf("?"))
        } else {
            url.substring(url.lastIndexOf("/") + 1)
        }
    }

    fun md5(url: String): String {
        return computeMD5(url)
    }

    fun md5FileName(url: String): String {
        val extension = getExtension(url)
        val name: String = computeMD5(url)
        return if (TextUtils.isEmpty(extension)) name else "$name.$extension"
    }

    private val units = arrayOf("B", "KB", "MB", "GB", "TB")
    private val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun getFormatSize(size: Double): String {
        var sizeUnit = size
        var index = 0
        while (sizeUnit > 1024 && index < 4) {
            sizeUnit /= 1024.0
            index++
        }
        return String.format(Locale.getDefault(), "%.2f %s", sizeUnit, units[index])
    }

    private fun getExtension(url: String): String {
        val dotIndex = url.lastIndexOf('.')
        val slashIndex = url.lastIndexOf('/')
        return if (dotIndex != -1 && dotIndex > slashIndex && dotIndex + 2 + MAX_EXTENSION_LENGTH > url.length) url.substring(
            dotIndex + 1,
            url.length
        ) else ""
    }

    private fun computeMD5(string: String): String {
        return try {
            val messageDigest = MessageDigest.getInstance("MD5")
            val digestBytes: ByteArray = messageDigest.digest(string.toByteArray())
            bytesToHexString(digestBytes)
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalStateException(e)
        }
    }

    private fun bytesToHexString(bytes: ByteArray): String {
        val sb = StringBuffer()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}