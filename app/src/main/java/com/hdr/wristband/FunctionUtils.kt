package com.hdr.wristband

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.hdr.wristband.ble.WristBleService
import org.jetbrains.anko.AnkoException
import java.io.Serializable
import java.util.concurrent.atomic.AtomicInteger


val sNextGeneratedId: AtomicInteger = AtomicInteger(1);
fun viewId(): Int {
    if (Build.VERSION.SDK_INT < 17) {
        while (true) {
            val result: Int = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            var newValue: Int = result + 1;
            if (newValue > 0x00FFFFFF)
                newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    } else {
        return View.generateViewId();
    }
}

//想办法放数据到 Intent中,如果无法放入则抛出异常
fun Intent.put(vararg params: Pair<String, Any>) {
    val intent = this
    params.forEach {
        val value = it.second
        when (value) {
            is Int -> intent.putExtra(it.first, value)
            is Long -> intent.putExtra(it.first, value)
            is CharSequence -> intent.putExtra(it.first, value)
            is String -> intent.putExtra(it.first, value)
            is Float -> intent.putExtra(it.first, value)
            is Double -> intent.putExtra(it.first, value)
            is Char -> intent.putExtra(it.first, value)
            is Short -> intent.putExtra(it.first, value)
            is Boolean -> intent.putExtra(it.first, value)
            is Serializable -> intent.putExtra(it.first, value)
            is Bundle -> intent.putExtra(it.first, value)
            is Parcelable -> intent.putExtra(it.first, value)
            is Array<*> -> when {
                value.isArrayOf<CharSequence>() -> intent.putExtra(it.first, value)
                value.isArrayOf<String>() -> intent.putExtra(it.first, value)
                value.isArrayOf<Parcelable>() -> intent.putExtra(it.first, value)
                else -> throw AnkoException("Intent extra ${it.first} has wrong type ${value.javaClass.name}")
            }
            is IntArray -> intent.putExtra(it.first, value)
            is LongArray -> intent.putExtra(it.first, value)
            is FloatArray -> intent.putExtra(it.first, value)
            is DoubleArray -> intent.putExtra(it.first, value)
            is CharArray -> intent.putExtra(it.first, value)
            is ShortArray -> intent.putExtra(it.first, value)
            is BooleanArray -> intent.putExtra(it.first, value)
            else -> throw AnkoException("Intent extra ${it.first} has wrong type ${value.javaClass.name}")
        }
    }
}

fun Context.wristSend(action: String) {
    val intent = Intent(this, WristBleService::class.java)
    intent.action = action
    startService(intent)
}

fun Context.wristSendCmd(cmd: String) {
    val intent = Intent(this, WristBleService::class.java)
    intent.action = WristBleService.ACTION_SEND_CMD
    intent.putExtra(WristBleService.KEY_CMD, cmd)
    startService(intent)
}

fun Context.wristSendCmd(cmd: String, data: Any) {
    return this.wristSendCmd(cmd, WristBleService.KEY_DATA to data,WristBleService.KEY_DATA to data,WristBleService.KEY_DATA to data,WristBleService.KEY_DATA to data)
}

fun Context.wristSendCmd(cmd: String, vararg args: Pair<String, Any>) {
    val intent = Intent(this, WristBleService::class.java)
    intent.action = WristBleService.ACTION_SEND_CMD
    intent.putExtra(WristBleService.KEY_CMD, cmd)
    args.forEach {
        intent.put(it)
    }
    startService(intent)
}

fun Context.wristDisconnect() = wristSend(WristBleService.ACTION_DISCONNECT)

fun Context.wristConnect() = wristSend(WristBleService.ACTION_CONNECT)

fun Context.wristBind(address: String) {
    val intent = Intent(this, WristBleService::class.java)
    intent.action = WristBleService.ACTION_BIND
    intent.put(WristBleService.KEY_DATA to address)
    startService(intent)
}

fun Context.wristUnbind() = wristSend(WristBleService.ACTION_UNBIND)


fun Byte.convertInt(): Int = (this + 256) % 256

fun Byte.convertInt(b1: Byte): Int = this.convertInt() * 256 + b1.convertInt()