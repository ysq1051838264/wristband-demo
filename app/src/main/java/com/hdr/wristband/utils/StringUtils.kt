package com.hdr.wristband.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by hdr on 16/7/4.
 */
object StringUtils {
    fun format(bytes: ByteArray): String {
        val sb = StringBuilder()
        bytes.forEach {
            sb.append(String.format("%02X ", it))
        }
        return sb.toString()
    }
}

fun Date.format(format:String):String{
    val sdf = SimpleDateFormat(format)
    return sdf.format(this)
}

val Date.formatToDay: Date
    get() {
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        val dateString = sdf.format(this)
        return sdf.parse(dateString)
    }