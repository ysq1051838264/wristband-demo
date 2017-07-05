package com.hdr.wristband

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.TextView
import com.hdr.wristband.ble.SleepRecordData
import com.hdr.wristband.ble.WristBleService
import com.hdr.wristband.ble.WristSportData
import com.hdr.wristband.model.BleDevice
import com.hdr.wristband.model.LogModel
import com.hdr.wristband.xrz.ProtocolHelper
import com.kingnew.health.base.adapter.AmazingAdapter
import com.kingnew.health.base.adapter.HolderConverter
import com.kingnew.health.base.adapter.ListAdapter
import com.kingnew.health.base.adapter.MultiViewAdapter
import no.nordicsemi.android.nrftoolbox.profile.BleProfileService
import org.jetbrains.anko.*
import org.jetbrains.anko.recyclerview.v7.recyclerView
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec

class MainActivity : Activity() {


    lateinit var menuRecyclerView: RecyclerView
    lateinit var logRecyclerView: RecyclerView


    val menuAdapter = ListAdapter(MyMenu.menus, {
        object : HolderConverter<String>() {
            lateinit var menuTv: TextView
            override fun initData(data: String, index: Int) {
                menuTv.text = data
            }

            override fun createView(context: Context) = context.relativeLayout {
                minimumHeight = dip(45)
                menuTv = textView {
                    textSize = 12f
                }.lparams {
                    centerInParent()
                }
            }

            override fun onClick(data: String, index: Int) {
                onMenuClick(data)
            }
        }
    })

    var isOta = false

    val logAdapter = MultiViewAdapter(
            holderConverterFactory = {
                when (it) {
                    0 -> {
                        object : HolderConverter<BleDevice>() {
                            lateinit var indexTv: TextView
                            lateinit var nameTv: TextView
                            lateinit var addressTv: TextView
                            lateinit var rssiTv: TextView

                            override fun initData(data: BleDevice, index: Int) {
                                indexTv.text = (index + 1).toString()
                                nameTv.text = data.device.name
                                addressTv.text = data.device.address
                                rssiTv.text = data.rssi.toString()
                            }

                            override fun createView(context: Context) = context.linearLayout {
                                padding = dip(10)
                                indexTv = textView {
                                    textSize = 12f
                                }.lparams(dip(20))
                                nameTv = textView {
                                    textSize = 12f
                                }.lparams(dip(100))
                                addressTv = textView {
                                    textSize = 12f
                                }.lparams {
                                    leftMargin = dip(10)
                                }
                                rssiTv = textView {
                                    textSize = 12f
                                }.lparams {
                                    leftMargin = dip(10)
                                }
                            }

                            override fun onClick(data: BleDevice, index: Int) {
                                connect(data.device)
                            }
                        }


                    }
                    else -> {
                        object : HolderConverter<LogModel>() {
                            lateinit var contentTV: TextView
                            val sdf = SimpleDateFormat("hh:mm:ss")

                            override fun initData(data: LogModel, index: Int) {
                                contentTV.text = "${sdf.format(Date(data.time))}: ${data.content}"
                            }

                            override fun createView(context: Context) = context.relativeLayout {
                                padding = dip(10)
                                contentTV = textView {
                                    textSize = 14f
                                }.lparams {

                                }
                            }
                        }
                    }
                }
            },
            viewTypeFactory = {
                data, position ->
                when (data) {
                    is BleDevice -> 0
                    else -> 1
                }
            }
    )
    val wristBleListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent == null) {
                return
            }
            when (intent.action) {
                BleProfileService.BROADCAST_CONNECTION_STATE -> {
                    val state = intent.getIntExtra(BleProfileService.EXTRA_CONNECTION_STATE, BleProfileService.STATE_DISCONNECTED)
                    when (state) {
                        BleProfileService.STATE_DISCONNECTED, BleProfileService.STATE_LINK_LOSS -> {
                            log("已断开")
                            if (isOta) {
                                startActivity<NordicFwUpdateActivity>()
                            }
                        }
                        BleProfileService.STATE_CONNECTING -> {
                            log("正在连接")
                            isOta = false
                        }
                        BleProfileService.STATE_CONNECTED -> {
                            log("已连接")
                        }
                        BleProfileService.STATE_DISCONNECTING -> {
                            log("正在断开")
                        }
                        WristBleService.STATE_SCANNING -> {
                            log("正在寻找手环")
                        }
                        WristBleService.STATE_CLOSED -> {
                            log("蓝牙已关闭")
                        }
                    }
                }
                WristBleService.CMD_GET_SPORT_DATA -> {
                    val sportData: WristSportData = intent.getParcelableExtra(WristBleService.KEY_DATA)
                    onGetSportData(sportData)
                }
                WristBleService.CMD_GET_DEVICE_BATTERY -> {
                    onGetBattery(intent.getIntExtra(WristBleService.KEY_DATA, 0))
                }
                WristBleService.CMD_GET_SLEEP_RECORD_DATA -> {
                    val sleepDatas: List<SleepRecordData> = intent.getParcelableArrayListExtra(WristBleService.KEY_DATA)
                    onGetSleepData(sleepDatas)
                }
                WristBleService.CMD_SYN_DATA_START -> {
                    onSynDataStart()
                }
                WristBleService.CMD_SYN_TIME->{
                    log("同步时间成功")
                }
                WristBleService.CMD_SYN_DATA_END -> {
                    val sportData: List<WristSportData> = intent.getParcelableArrayListExtra(WristBleService.KEY_DATA)
                    onSynDataEnd(sportData)
                }
            }
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        relativeLayout {
            menuRecyclerView = recyclerView {
                id = viewId()
                layoutManager = LinearLayoutManager(context)
                adapter = menuAdapter
                addItemDecoration(menuAdapter.LinearDivider { any, i, view, recyclerView -> AmazingAdapter.Divider(size = dip(1), color = Color.LTGRAY) })
            }.lparams(dip(100), matchParent) {
                alignParentEnd()
            }
            logRecyclerView = recyclerView {
                id = viewId()
                layoutManager = LinearLayoutManager(context)
                adapter = logAdapter
                addItemDecoration(menuAdapter.LinearDivider { any, i, view, recyclerView -> AmazingAdapter.Divider(size = dip(1), color = Color.LTGRAY, marginStart = dip(10), marginEnd = dip(10)) })
            }.lparams(matchParent, matchParent) {
                leftOf(menuRecyclerView)
            }
            view {
                backgroundColor = Color.LTGRAY
            }.lparams(dip(1), matchParent) {
                leftOf(menuRecyclerView)
            }
        }

//        val datas = ProtocolHelper.decryptData(0xfc.toByte(), 0xc2.toByte(), 0xc0.toByte(), 0x09.toByte(), 0x03.toByte(), 0x04.toByte())
//        val sb = StringBuilder()
//        datas.forEach {
//            sb.append(String.format("%02X ", it))
//        }
//        Log.i("解密数据", sb.toString())

        LocalBroadcastManager.getInstance(this).registerReceiver(wristBleListener, makeWristIntentFilter())

        wristConnect()
    }

    fun makeWristIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BleProfileService.BROADCAST_CONNECTION_STATE)
        intentFilter.addAction(WristBleService.CMD_GET_DEVICE_BATTERY)
        intentFilter.addAction(WristBleService.CMD_GET_SPORT_DATA)
        intentFilter.addAction(WristBleService.CMD_GET_SLEEP_RECORD_DATA)
        intentFilter.addAction(WristBleService.CMD_SYN_DATA_START)
        intentFilter.addAction(WristBleService.CMD_SYN_DATA_END)

        return intentFilter
    }

    override fun onDestroy() {
        super.onDestroy()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(wristBleListener)
    }

    fun onGetBattery(battery: Int) {
        log("电池电量是:$battery")
    }

    fun onGetSportData(sportData: WristSportData) {
        log("运动数据:$sportData")
    }

    fun onGetSleepData(sleepDatas: List<SleepRecordData>) {
        log("睡眠数据:$sleepDatas")
    }

    fun onSynDataStart() {
        log("开始同步数据")
    }

    fun onSynDataEnd(data: List<WristSportData>) {
        log("同步数据结束$data")
    }


    fun connect(device: BluetoothDevice) {
        logAdapter.reset()
    }

    fun onMenuClick(menu: String) {
        when (menu) {
            MyMenu.CLEAR_LOG -> {
                logAdapter.reset(emptyList())
            }
            MyMenu.BIND_DEVICE -> {
                wristBind(WristBleService.targetAddress)
            }
            MyMenu.UNBIND_DEVICE -> {
                wristUnbind()
            }
            MyMenu.CONNECT_DEVICE -> {
                wristConnect()
            }
            MyMenu.DISCONNECT -> {
                wristDisconnect()
            }
            MyMenu.DECRYPT_FIRMWARE -> {
                decryptFirmware()
            }
            MyMenu.OTA -> {
                isOta = true
                wristSendCmd(WristBleService.CMD_PREPARE_OTA)
            }
            MyMenu.SYN_TIME -> {
                wristSendCmd(WristBleService.CMD_SYN_TIME)
            }
            MyMenu.GET_DEVICE_BATTERY -> {
                wristSendCmd(WristBleService.CMD_GET_DEVICE_BATTERY)
            }
            MyMenu.GET_DEVICE_INFO -> {
                wristSendCmd(WristBleService.CMD_GET_DEVICE_INFO)
//                startActivity<NordicFwUpdateActivity>()
            }
            MyMenu.GET_SPORT_DATA -> {
                wristSendCmd(WristBleService.CMD_GET_SPORT_DATA, 0)
            }
        }
    }

    private val mPrivateKey = byteArrayOf(0x78, 0x72, 0x7A, 0x78, 0x6A, 0x75, 0x61, 0x64)

    fun decryptFirmware() {
        val sourceFilePath = Environment.getExternalStorageDirectory().absolutePath + "/btlinker_w/xju.zip"
        val destFilePath = Environment.getExternalStorageDirectory().absolutePath + "/btlinker_w/hdr.zip"
        try {
            val inputStream: InputStream = FileInputStream(sourceFilePath)
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()

            val sr = SecureRandom()
            val key = mPrivateKey
            val desKeySpec = DESKeySpec(key)
            val secretKey = SecretKeyFactory.getInstance("DES").generateSecret(desKeySpec)
            val cipher = Cipher.getInstance("DES")
            cipher.init(2, secretKey, sr)
            val fileData = cipher.doFinal(buffer)

            val outputStream = FileOutputStream(destFilePath)
            outputStream.write(fileData)
            outputStream.flush()
            outputStream.close()
            toast("破解成功")
        } catch (var12: Exception) {
            var12.printStackTrace()
            toast("破解失败")
        }

    }

    fun log(text: String) {
        logAdapter.addSection(LogModel(text))
        logRecyclerView.postDelayed({
            logRecyclerView.layoutManager.scrollToPosition(logAdapter.itemCount)
        }, 100)
    }
}

