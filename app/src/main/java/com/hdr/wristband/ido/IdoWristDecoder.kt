package com.hdr.wristband.ido

import android.content.Context
import android.util.Log
import com.hdr.wristband.ble.CommandSender
import com.hdr.wristband.ble.IdoWristBleManager
import com.hdr.wristband.ble.WristDecoder
import com.hdr.wristband.utils.StringUtils
import com.hdr.wristband.xrz.ProtocolHelper
import java.util.*

/**
 * Created by hdr on 17/7/5.
 */

class IdoWristDecoder(context: Context, commandSender: CommandSender) : WristDecoder(context, commandSender) {
    override val TIME_OUT_MILLISECOND = 3000L

    var data: ByteArray? = null

    override fun doSynData() {
        sendCmd(byteArrayOf(0x08, 0x01, 0x1))
    }

    override fun onReceiveData(uuid: UUID, pkgData: ByteArray) {
        gotFeedback()
        this.data = pkgData
        val code = StringUtils.format(data!!)
        Log.i("hdr", "收到数据:$code")
        nextCmd()
    }

    override fun doPair() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun synTime() {
        sendCmd(synTimeCmd)
    }

    override fun getSleepRecordData(dayIndex: Int) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSportRecordData(dayIndex: Int) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun prepareOTA() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDeviceBattery() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSportData(dayIndex: Int) {
        sendCmd(byteArrayOf(0x08, 0x03, 0x01))
    }


    override fun getDeviceInfo() {
        sendCmd(byteArrayOf(0x02, 0x01))
    }

    val synTimeCmd: ByteArray
        get() {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1

            val day = cal.get(Calendar.DATE)
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val second = cal.get(Calendar.MINUTE)

            return byteArrayOf(0x03, 0x01, (year shr 8).toByte(), (year and 0xFF).toByte(), month.toByte(), day.toByte(), hour.toByte(), minute.toByte(), second.toByte());
        }

    override fun sendCmd(value: ByteArray) {
        val uuid: UUID
        if (BaseCmdUtil.isHealthCmd(value)) {
            uuid = IdoWristBleManager.UUID_CHARACTERISTIC_HEALTH_WRITE
        } else {
            uuid = IdoWristBleManager.UUID_CHARACTERISTIC_NORMAL_WRITE
        }
        sendCmd(CmdGroup<Any>(value, uuid))
    }
}
