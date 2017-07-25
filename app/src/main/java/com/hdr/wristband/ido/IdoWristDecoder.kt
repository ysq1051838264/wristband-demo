package com.hdr.wristband.ido

import android.content.Context
import android.util.Log
import com.hdr.wristband.ble.CommandSender
import com.hdr.wristband.ble.IdoWristBleManager
import com.hdr.wristband.ble.WristDecoder
import com.hdr.wristband.convertInt
import com.hdr.wristband.utils.StringUtils
import java.util.*

/**
 * Created by hdr on 17/7/5.
 */

class IdoWristDecoder(context: Context, commandSender: CommandSender) : WristDecoder(context, commandSender) {

    override var TIME_OUT_MILLISECOND = 20000L

    var data: ByteArray? = null

    override fun doSynData() {
//        sendCmd(byteArrayOf(0x08, 0x01, 0x01))
    }

    var stepData = 0
    var timeData = 0
    var calData = 0
    var distanceData = 0

    var lightSleepData = 0
    var deepSleepData = 0
    var sleepData = 0
    var sleepGroup = 0

    var heartRateCount = 0
    var heartRateGroup = 0
    var offset = 0
    var totalValue = 0

    //记录历史数据的天数
    var sportRecordDays = 0
    var sleepRecordDays = 0
    var heartRateRecordDays = 0

    override fun onReceiveData(uuid: UUID, pkgData: ByteArray) {
        gotFeedback()
        this.data = pkgData
        val code = StringUtils.format(data!!)
        Log.i("hdr", "收到数据:$code")
        Log.e("ysq", "收到数据:$code")

        val data = data!!

        when (data[0].convertInt()) {
            0x02 -> {
                when (data[1].convertInt()) {
                    0x01 -> {
                        //获取设备信息
                    }
                    0x04 -> {
                        //获取mac地址
                        Log.e("ysq", "用户获取到了mac地址")
                    }
                }
            }
            0x03 -> {
                when (data[1].convertInt()) {
                    0x01 -> {
                        //设置时间
                        Log.e("ysq", "用户设置了时间")
                    }
                    0x02 -> {
                        //设置闹钟
                    }
                    0x03 -> {
                        //设置运动目标
                    }
                    0x11 -> {
                        //设置单位
                    }
                    0x20 -> {
                        //久坐提醒
                    }
                    0x21 -> {
                        //防丢设置
                    }
                    0x22 -> {
                        //佩戴模式
                    }
                    0x24 -> {
                        //心率区间
                    }
                    0x25 -> {
                        //心率模式
                    }
                    0x26 -> {
                        //寻找手机
                    }
                    0x28 -> {
                        //抬腕识别
                    }
                    0x29 -> {
                        //勿扰模式
                    }
                    0x2c -> {
                        //紧急求救
                    }
                }
            }
            0x04 -> {
                when (data[1].convertInt()) {
                    0x01 -> {
                        //绑定
                    }
                    0x02 -> {
                        //解绑
                    }
                }
            }
            0x05 -> {
                when (data[1].convertInt()) {
                    0x01 -> {
                        //电话提醒
                    }
                    0x03 -> {
                        //智能提醒（短信、qq、微信等）
                        when (data[4].convertInt()) {
                            0x01 -> {
                                //短信

                            }
                            0x06 -> {
                                //faceBook提醒

                            }
                            0x07 -> {
                                //Twitter提醒

                            }
                            0x08 -> {
                                //WhatsApp提醒

                            }
                        }
                    }
                }
            }
            0x06 -> {
                when (data[1].convertInt()) {
                    0x01 -> {
                        //控制音乐开始和结束
                    }
                    0x02 -> {
                        //拍照控制
                    }
                }
            }

            0x08 -> {
                when (data[1].convertInt()) {
                    0x01 -> {
                        sportRecordDays = data[4].convertInt()
                        sleepRecordDays = data[5].convertInt()
                        heartRateRecordDays = data[6].convertInt()

                        Log.e("ysq", "记录历史数据的天数   运动 ${data[4].convertInt()} 睡眠 ${data[5].convertInt()}   心率 ${data[6].convertInt()}  ")

                        TIME_OUT_MILLISECOND = (data[4].convertInt() + data[4].convertInt() + data[6].convertInt()) * 6000L

                        sysHistorySport(0x01)
                    }

                    0x03 -> {
                        if (data.size < 9)
                            return
                        //当天运动数据
                        sysSportData(data)
                    }

                    0x04 -> {
                        if (data.size < 9)
                            return
                        //当天睡眠数据
                        sysSleepData(data)
                    }

                    0x07 -> {
                        //当天心率数据
                        if (data.size < 9)
                            return
                        sysHeartRateData(data)
                    }

                    0x05 -> {
                        //历史运动数据
                        if (data[2].toInt() == 2 && data.size < 9) {
                            sysHistorySleep(0x01)
                        }

                        if (data.size < 9)
                            return
                        sysSportData(data)
                    }

                    0x06 -> {
                        if (data[2].toInt() == 2 && data.size < 9) {
                            sysHistoryHeartRate(0x01)
                        }

                        //历史睡眠数据
                        if (data.size < 9)
                            return
                        sysSleepData(data)
                    }

                    0x08 -> {
                        //历史心率数据
                        if (data.size < 9)
                            return
                        sysHeartRateData(data)
                    }

                    0xEE -> {
                        when (data[2].convertInt()) {
                            0x01 -> {
                                sportRecordDays -= 1
                                Log.e("ysq", "总数据  总步数$stepData  总时间 $timeData   总卡路里$calData  总距离 $distanceData   sportRecordDays的值 $sportRecordDays")
                                sysHistorySport(0x02)
                                if (sportRecordDays > 0)
                                    sysHistorySport(0x01)
                            }

                            0x03 -> {
                                sleepRecordDays -= 1
                                sysHistorySleep(0x02)
                                if (sportRecordDays > 0)
                                    sysHistorySleep(0x01)

                            }

                            0x05 -> {
                                sleepRecordDays -= 1
                                sysHistoryHeartRate(0x02)
                                if (sportRecordDays > 0)
                                    sysHistoryHeartRate(0x01)
                            }

                        }
                    }

                }
            }
        }

        nextCmd()
    }


    fun sysSleepData(data: ByteArray) {
        if (data[2].toInt() == 1) {
            lightSleepData = 0
            deepSleepData = 0
            sleepData = 0
            Log.e("ysq", "当天睡眠数据:${data[5].convertInt(data[4])} 年 ${data[6].toInt()} 月 ${data[7].toInt()} 日  总睡眠时长${data[11].convertInt(data[10])}")
            sleepGroup = data[13].toInt()
        }

        if (data[2].toInt() == 2) {
            Log.e("ysq", "当天睡眠数据: 浅睡眠 ${data[8].convertInt(data[7])} 深睡 ${data[10].convertInt(data[9])}  ")
        }

        //第三包开始就是详细数据
        if (data[2].toInt() > 2) {
            val length = data[3].convertInt()
            var model = 0

            repeat(length) {
                //等于0就是模式，后面就是值
                val flag: Int = (it + 4) % 2

                val basis = 4 + it

                if (flag == 0) {
                    model = data[basis].toInt()
                } else {
                    Log.e("ysq", "睡眠模式 $model  睡眠时间 ${data[basis].toInt()} ")
                    //1是清醒 2是浅睡 3是深睡
                    when (model) {
                        1 -> {
                            sleepData += data[basis].toInt()
                        }

                        2 -> {
                            lightSleepData += data[basis].toInt()
                        }

                        3 -> {
                            deepSleepData += data[basis].toInt()
                        }
                    }
                }
            }

            if (data[2].toInt() == sleepGroup) {
                Log.e("ysq", "总数据  浅睡$lightSleepData  深睡 $deepSleepData   清醒$sleepData ")
            }
        }
    }


    fun sysSportData(data: ByteArray) {
        if (data[2].toInt() == 1) {
            stepData = 0
            timeData = 0
            calData = 0
            distanceData = 0
            Log.e("ysq", "当天运动数据:${data[5].convertInt(data[4])} 年 ${data[6].toInt()} 月 ${data[7].toInt()} 日 ${data[12].toInt()} 条数据")
        }

        if (data[2].toInt() == 2) {
            Log.e("ysq", "当天运动数据: 总步数 ${data[5].convertInt(data[4])} 卡路里 ${data[9].convertInt(data[8])}  总距离 ${data[13].convertInt(data[12])}  时间 ${data[17].convertInt(data[16])}  ")
        }

        //第三包开始就是详细数据
        if (data[2].toInt() > 2) {
            val group = (data.size - 4) / 5

            repeat(group) {
                val basis = 5 * it + 4

                //代表运动模式
                var mode = 0x3 and data[basis].convertInt()
                //代表总步数
                var step = (0xFC and data[basis].convertInt() shr 2) + (0x3F and data[basis + 1].convertInt() shl 6)
                //代表运动时间
                var time = (0xC0 and data[basis + 1].convertInt() shr 6) + (0x3 and data[basis + 2].convertInt() shl 2)

                // 代表卡路里；
                var cal = (0xFC and data[basis + 2].convertInt() shr 2) + (0xF and data[basis + 3].convertInt() shl 6)

                //代表距离。
                var distance = (0xF0 and data[basis + 3].convertInt() shr 4) + (0xFF and data[basis + 4].convertInt() shl 4)

                Log.e("ysq", "第 ${data[2].toInt()} 组的第$it 分组数据，模式 $mode  步数 $step  时间 $time   卡路里 $cal  距离 $distance ")

                stepData += step
                timeData += time
                calData += cal
                distanceData += distance
            }

            if (data[2].toInt() == 34) {
                Log.e("ysq", "总数据  总步数$stepData  总时间 $timeData   总卡路里$calData  总距离 $distanceData")
            }
        }
    }


    fun sysHeartRateData(data: ByteArray) {

        if (data[2].toInt() == 1) {
            offset = 0
            totalValue = 0
            heartRateCount = 0
            Log.e("ysq", "当天心率数据:${data[5].convertInt(data[4])} 年 ${data[6].toInt()} 月 ${data[7].toInt()} 日  平稳心率${data[10].toInt()}")
            heartRateGroup = data[13].toInt()
        }

        if (data[2].toInt() == 2) {
            Log.e("ysq", "当天心率数据: 脂肪燃烧 ${data[4].convertInt()} 有氧锻炼 ${data[5].convertInt()}  极限锻炼 ${data[6].convertInt()} ")
        }

        //第三包开始就是详细数据
        if (data[2].toInt() in 3..heartRateGroup) {
            val length = data[3].convertInt()

            repeat(length) {
                //等于0就是 间隔，后面就是值
                val flag = (it + 4) % 2

                val basis = 4 + it

                if (flag == 0) {
                    offset += data[basis].convertInt()
                } else {
                    Log.e("ysq", "偏移$offset  心率 ${data[basis].convertInt()} ")
                    totalValue += data[basis].convertInt()
                    if (data[basis].convertInt() > 0) {
                        heartRateCount++
                    }
                }
            }

            if (data[2].toInt() == heartRateGroup) {
                Log.e("ysq", "总的心率是 $totalValue 有效数组个数是  $heartRateCount  平均心率是  ${totalValue / heartRateCount}")
            }
        }
    }

    override fun doPair() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun synTime() {
        sendCmd(synTimeCmd)
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

    override fun getSleepRecordData(dayIndex: Int) {
        sendCmd(byteArrayOf(0x08, 0x04, 0x01))
    }

    override fun getSportData(dayIndex: Int) {
        sendCmd(byteArrayOf(0x08, 0x03, 0x01))
    }

    override fun getHeartRate() {
        sendCmd(byteArrayOf(0x08, 0x07, 0x01))
    }

    override fun getDeviceInfo() {
        sendCmd(byteArrayOf(0x02, 0x01))
    }

    override fun callPhone() {
//        sendCmd(byteArrayOf(0x05,0x01,0x01,0x01,0x0b,0x02,0x31,0x33,0x32,0x35,0x32,0x32,0x37,0x30,0x33,0x34,0x35,0x78,0x74,0x00))
        sendCmd(byteArrayOf(0x05, 0x01, 0x01, 0x01))
    }

    override fun sendSms() {
        sendCmd(byteArrayOf(0x05, 0x03, 0x02, 0x01, 0x01, 0x06, 0x0b, 0x02, 0x31, 0x33, 0x32, 0x30, 0x32, 0x32, 0x37, 0x30, 0x33, 0x34, 0x35, 0x78))
        sendCmd(byteArrayOf(0x05, 0x03, 0x02, 0x02, 0x74, 0xe4.toByte(), 0xbd.toByte(), 0xa0.toByte(), 0xe5.toByte(), 0xa5.toByte(), 0xbd.toByte(), 0x52, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
    }

    override fun sysHistorySleep(code: Int) {
        sendCmd(byteArrayOf(0x08, 0x06, code.toByte()))
    }

    override fun sysHistoryHeartRate(code: Int) {
        sendCmd(byteArrayOf(0x08, 0x08, code.toByte()))
    }

    override fun sysHistoryData() {
        sendCmd(byteArrayOf(0x08, 0x01, 0x01))
    }

    override fun sysHistorySport(code: Int) {
        sendCmd(byteArrayOf(0x08, 0x05, code.toByte()))
    }

    override fun findPhone(code: Int) {
        sendCmd(byteArrayOf(0x03, 0x26, code.toByte(), 0x00, 0x00))
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
