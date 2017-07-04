package com.hdr.wristband.xrz

import android.content.Context
import android.util.Log
import com.hdr.wristband.ble.*
import com.hdr.wristband.convertInt
import com.hdr.wristband.utils.StringUtils
import com.hdr.wristband.utils.format
import com.hdr.wristband.utils.formatToDay
import java.util.*

/**
 * 鑫锐志公司手环的解码器
 * Created by hdr on 16/7/11.
 */
class XrzWristDecoder(context: Context, commandSender: CommandSender) : WristDecoder(context, commandSender) {


    var data: ByteArray? = null
    var dataLength = -1


    //从发送命令里面拿到 日期 命令中指定的日期,默认返回今天
    val CmdGroup<*>.cmdDate: Date
        get() {

            val curCmd = this.curCmd
            if (curCmd == null || curCmd.size <= 11) {
                return Date().formatToDay
            }
            val code = curCmd.clone()
            ProtocolHelper.decryptData(code)
            return Date(Date().formatToDay.time - code[9] * (1000 * 60 * 60 * 24))
        }


    override fun onReceiveData(uuid: UUID, pkgData: ByteArray) {
        //收到了手环的反馈,就移除超时判断
        gotFeedback()
        //先判断包的完整性,如果不完整,则接上包,并且等待下一数据
        if (data == null || (pkgData[0].convertInt() == 0x8A && pkgData[1].convertInt() == 0xFF)) {
            //这是一个新包
            data = pkgData
            dataLength = pkgData[2].convertInt(pkgData[3])
        } else {
            data = data!!.plus(pkgData)
        }
        val length = dataLength
        val data = data!!
        val curCmdGroup = curCmdGroup ?: return
        if (data.size != length) {
            return
        } else {
            this.data = null
            dataLength = -1
        }
        if (data.size > 11) {
            //说明指定中有数据段,先对数据段进行解密
            ProtocolHelper.decryptData(data)
        }
        val code = StringUtils.format(data)
        Log.i("hdr", "收到数据:$code")
        if (data[5] != curCmdGroup.curCmd!![5]) {
            Log.i("hdr", "数据不对应丢掉包")
            return
        }
        when (data[5].convertInt()) {
            0x05 -> {
                //这个是获取电量的
                sendResult(WristBleService.CMD_GET_DEVICE_BATTERY, data[9].toInt())
            }
            0x16 -> {
                Log.i("hdr", "用户又触摸了屏幕")
            }
            0x0A -> {

                val sportData = WristSportData(
                        date = curCmdGroup.cmdDate,
                        steps = data[9].convertInt(data[10]),
                        distance = data[11].convertInt(data[12]),
                        calories = data[13].convertInt(data[14]),
                        sleepTimeCnt = data[15].convertInt(data[16])
                )

                sendResult(WristBleService.CMD_GET_SPORT_DATA, sportData)
            }
            0x14 -> {
                sendBroadcast(WristBleService.CMD_PREPARE_OTA)
            }
            0x0B -> {
                val sleepDatas = ArrayList<SleepRecordData>()

                val date = curCmdGroup.cmdDate
                val hour = 1000 * 60 * 60
                val quarterHour = 1000 * 60 * 15
                fun generateSleepRecordData(type: Int, hourIndex: Int, minuteIndex: Int): SleepRecordData? {
                    val typeString: String
                    when (type) {
                        0 -> typeString = "sleep"
                        1, 2 -> typeString = "light"
                        else -> return null
                    }
                    val startTime = Date(date.time + hourIndex * hour + minuteIndex * quarterHour)
                    val endTime = Date(startTime.time + quarterHour)
                    return SleepRecordData(startTime = startTime, endTime = endTime, sleepType = typeString, sleepTimeCnt = 15)
                }

                repeat(24) {
                    val hourData = data[it + 9].convertInt()
                    val type0 = (hourData shr 6) and 3
                    val type1 = (hourData shr 4) and 3
                    val type2 = (hourData shr 2) and 3
                    val type3 = hourData and 3
                    arrayOf(type0, type1, type2, type3).forEachIndexed {
                        index, sleepType ->
                        val d = generateSleepRecordData(sleepType, it, index)
                        if (d != null) {
                            sleepDatas.add(d)
                        }
                    }
                }
                sendResult(WristBleService.CMD_GET_SLEEP_RECORD_DATA, sleepDatas)
            }
            0x17 -> {
                val sportDatas = ArrayList<SportRecordData>()
                val date = curCmdGroup.cmdDate

                repeat(24) {
                    val step = data[it * 2 + 9].convertInt(data[it * 2 + 10])
                    if (step == 0) {
                        return@repeat
                    }
                    val hour = 1000 * 60 * 60
                    val startTime = Date(date.time + it * hour)
                    val endTime = Date(startTime.time + hour)
                    val sportRecordData = SportRecordData(startTime = startTime, endTime = endTime, step = step)
                    sportDatas.add(sportRecordData)
                }
                sendResult(WristBleService.CMD_GET_SPORT_RECORD_DATA, sportDatas)
            }

        }
        if (curCmdGroup.isLastCmd && curCmdGroup.type == WristBleService.GROUP_TYPE_SYN_DATA) {
            sendBroadcast(WristBleService.CMD_SYN_DATA_END, curCmdGroup.data as ArrayList<WristSportData>)
        }
        nextCmd()
    }

    override fun <T : Any> mergeGroupData(cmd: String, itemData: T) {
        when (curCmdGroup!!.type) {
            WristBleService.GROUP_TYPE_SYN_DATA -> {
                val data = curCmdGroup!!.data as ArrayList<WristSportData>
                when (cmd) {
                    WristBleService.CMD_GET_SPORT_DATA -> {
                        data.add(itemData as WristSportData)
                    }
                    WristBleService.CMD_GET_SLEEP_RECORD_DATA -> {
                        val sleepRecords = itemData as ArrayList<SleepRecordData>
                        if (sleepRecords.isEmpty()) {
                            return
                        }
                        val itemDateString = sleepRecords[0].startTime.format("MM-dd")
                        data.forEach {
                            val dayString = it.date.format("MM-dd")
                            if (dayString == itemDateString) {
                                (it.sleepRecords as ArrayList).addAll(sleepRecords)
                                return@forEach
                            }
                        }
                    }
                    WristBleService.CMD_GET_SPORT_RECORD_DATA -> {

                        val sportRecords = itemData as ArrayList<SportRecordData>
                        if (sportRecords.isEmpty()) {
                            return
                        }
                        val itemDateString = sportRecords[0].startTime.format("MM-dd")
                        data.forEach {
                            val dayString = it.date.format("MM-dd")
                            if (dayString == itemDateString) {
                                (it.sportRecords as ArrayList).addAll(sportRecords)
                                return@forEach
                            }
                        }
                    }
                }
            }
        }

    }


    override fun doSynData() {
        //先清除命令
        this.clearCmds()
        sendBroadcast(WristBleService.CMD_SYN_DATA_START)

        val cmdGroup = CmdGroup<List<WristSportData>>(WristBleService.GROUP_TYPE_SYN_DATA)
        cmdGroup.data = ArrayList<WristSportData>()
        //添加同步时间的
        cmdGroup.addCmd(synTimeCmd)
        repeat(1) {
            //获取睡眠数据
            cmdGroup.addCmd(ProtocolHelper.merge(byteArrayOf(0xC7.toByte(), 0x0A.toByte()), byteArrayOf(it.toByte())))
            cmdGroup.addCmd(ProtocolHelper.merge(byteArrayOf(0xC7.toByte(), 0x17.toByte()), byteArrayOf(it.toByte())))
            cmdGroup.addCmd(ProtocolHelper.merge(byteArrayOf(0xC7.toByte(), 0x0B.toByte()), byteArrayOf(it.toByte())))

        }
        sendCmd(cmdGroup)
    }

    override fun doPair() {
        val cmd = ProtocolHelper.merge(byteArrayOf(0xFC.toByte(), 0xC2.toByte()), byteArrayOf(0x09.toByte(), 0x03.toByte(), 0x04.toByte()));
        sendCmd(cmd)
    }

    override fun getDeviceBattery() {
        val cmd = ProtocolHelper.merge(byteArrayOf(0xC7.toByte(), 0x05.toByte()));
        sendCmd(cmd)
    }

    override fun getSportData(dayIndex: Int) {
        val cmd = ProtocolHelper.merge(byteArrayOf(0xC7.toByte(), 0x0A.toByte()), byteArrayOf(dayIndex.toByte()));
        sendCmd(cmd)
    }

    override fun getSportRecordData(dayIndex: Int) {
        val cmd = ProtocolHelper.merge(byteArrayOf(0xC7.toByte(), 0x11.toByte()), byteArrayOf(dayIndex.toByte()));
        sendCmd(cmd)
    }

    override fun getSleepRecordData(dayIndex: Int) {
        val cmd = ProtocolHelper.merge(byteArrayOf(0xC7.toByte(), 0x0B.toByte()), byteArrayOf(dayIndex.toByte()));
        sendCmd(cmd)
    }

    override fun prepareOTA() {
        val cmd = ProtocolHelper.merge(byteArrayOf(0xC7.toByte(), 0x14.toByte()));
        sendCmd(cmd)
    }

    override fun synTime() {
        sendCmd(synTimeCmd)
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

            return ProtocolHelper.merge(intArrayOf(0xC7, 0x03), intArrayOf(year shr 8, year and 0xFF, month, day, hour, minute, second));
        }
}

