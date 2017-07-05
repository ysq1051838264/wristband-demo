package com.hdr.wristband.ble

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.hdr.wristband.ido.IdoWristDecoder
import com.hdr.wristband.xrz.XrzWristBleManager
import com.hdr.wristband.xrz.XrzWristDecoder
import no.nordicsemi.android.nrftoolbox.profile.BleProfileService
import no.nordicsemi.android.support.v18.scanner.*
import org.jetbrains.anko.toast
import java.util.*

/**
 * Created by hdr on 16/7/4.
 */
class WristBleService : BleProfileService(), WristBleManager.WristBleCallback {

    companion object {

        @JvmStatic val STATE_SCANNING = 4
        @JvmStatic val STATE_CLOSED = 5

        @JvmStatic val targetAddress = "C4:38:4C:2B:DF:D5"

        @JvmStatic val GROUP_TYPE_SYN_DATA = "group_type_syn_data"

        @JvmStatic val KEY_DATA = "key_data"

        @JvmStatic val KEY_CMD = "key_cmd"

        @JvmStatic val KEY_RESULT = "key_result"

        @JvmStatic val KEY_ERROR_MSG = "key_error_msg"
        @JvmStatic val KEY_TIME_OUT_CUR_CMD_GROUP = "key_time_out_cur_cmd_group"

        @JvmStatic val ACTION_DISCONNECT = "action_disconnect"
        @JvmStatic val ACTION_CONNECT = "action_connect"

        @JvmStatic val ACTION_SEND_CMD = "action_send_cmd"

        @JvmStatic val ACTION_BIND = "action_bind"
        @JvmStatic val ACTION_UNBIND = "action_unbind"

        @JvmStatic val CMD_SYN_TIME = "cmd_syn_time"

        @JvmStatic val CMD_PREPARE_OTA = "cmd_prepare_ota"

        @JvmStatic val CMD_SYN_DATA = "cmd_syn_data"
        @JvmStatic val CMD_SYN_DATA_START = "cmd_syn_data_start"
        @JvmStatic val CMD_SYN_DATA_END = "cmd_syn_data_end"

        @JvmStatic val CMD_GET_DEVICE_BATTERY = "cmd_get_device_battery"

        @JvmStatic val CMD_GET_DEVICE_INFO = "cmd_get_device_info"

        @JvmStatic val CMD_GET_SPORT_DATA = "cmd_get_sport_data"

        @JvmStatic val CMD_GET_SLEEP_RECORD_DATA = "cmd_get_sleep_record_data"

        @JvmStatic val CMD_GET_SPORT_RECORD_DATA = "cmd_get_sport_record_data"

        @JvmStatic val CMD_TIME_OUT = "cmd_time_out"

    }

    var currentAddress: String = targetAddress

    val wristBleManager by lazy { IdoWristBleManager(this) }

    var wristDecoder: WristDecoder? = null
        private set

    /**
     * 决定蓝牙断开后,是否重新启动扫描,如果不启动则销毁Service ,true则销毁,false则重启扫描
     */
    var disconnectFlag = false

    override fun initializeManager() = wristBleManager

    val isBleEnable: Boolean
        get() = BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled

    var bleState: Int = BleProfileService.STATE_DISCONNECTED

    var scanHit = false
    //已经扫描到了设备
    var hasScan = false
    val scanner by lazy { BluetoothLeScannerCompat.getScanner() }
    val scanSettings by lazy { ScanSettings.Builder().setMatchMode(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT).build() }
    val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.i("wrist", result.toString())
            val sc = this
            mHandler.post {
                if (scanHit) {
                    return@post
                }
                hasScan = true
                scanHit = true
                scanner.stopScan(sc)

                wristBleManager.connect(result.device)
            }
        }
    }

    val bleConnectStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) {
                return
            }
            bleState = intent.getIntExtra(BleProfileService.EXTRA_CONNECTION_STATE, BleProfileService.STATE_DISCONNECTED)
        }

    }

    val bleEnableStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isBleEnable) {
                postState(STATE_DISCONNECTED)
                mHandler.postDelayed({
                    doScan()
                }, 2000)
            } else {
                scanner.stopScan(scanCallback)
                postState(STATE_CLOSED)
            }
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return Service.START_REDELIVER_INTENT
        }
        when (intent.action) {
            ACTION_DISCONNECT -> {
                disconnect()
            }
            ACTION_UNBIND -> {
                //这里简单的置空,项目中要发在SP文件中吧MAC地址清除
                currentAddress = ""
                disconnect()
            }

            ACTION_CONNECT -> {
                //这里是判断有没有绑定设备,如果有则连接
                doScan()
            }
            ACTION_BIND -> {
                //这里模拟把数据放到SP中,
                currentAddress = intent.getStringExtra(KEY_DATA) ?: ""
                doScan()
            }
            ACTION_SEND_CMD -> {
                val cmd = intent.getStringExtra(KEY_CMD)
                val wristDecoder = this@WristBleService.wristDecoder
                if (wristDecoder == null) {
                    toast("手环还未就绪,请稍等")
                    return Service.START_REDELIVER_INTENT
                }
                when (cmd) {
                    CMD_SYN_DATA -> {
                        wristDecoder.doSynData()
                    }
                    CMD_SYN_TIME -> {
                        wristDecoder.synTime()
                    }
                    CMD_GET_SPORT_DATA -> {
                        val dayIndex = intent.getIntExtra(KEY_DATA, 0)
                        wristDecoder.getSportData(dayIndex)
                    }
                    CMD_PREPARE_OTA -> {
                        this.disconnectFlag = true
                        wristDecoder.prepareOTA()
                    }
                    CMD_GET_DEVICE_BATTERY -> {
                        wristDecoder.getDeviceBattery()
                    }
                    CMD_GET_DEVICE_INFO -> {
                        wristDecoder.getDeviceInfo()
                    }
                }
            }
        }

        return Service.START_REDELIVER_INTENT
    }

    override fun onCreate() {
        super.onCreate()

        registerReceiver(bleEnableStatusReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        val intentFilter = IntentFilter()
        intentFilter.addAction(BleProfileService.BROADCAST_CONNECTION_STATE)
        LocalBroadcastManager.getInstance(this).registerReceiver(this.bleConnectStateReceiver, intentFilter)

        disconnectFlag = false

    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()

        unregisterReceiver(bleEnableStatusReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.bleConnectStateReceiver)
    }

    override fun stopWhenDisconnected() = disconnectFlag

    override fun onReceiveData(uuid: UUID, value: ByteArray) {
        if (value[0].toInt() == 0) {
            return
        }
        wristDecoder?.onReceiveData(uuid, value)
    }

    override fun onDeviceReady() {
        super.onDeviceReady()
        wristDecoder = IdoWristDecoder(this, wristBleManager)

        wristDecoder?.doSynData()
    }


    override fun onDeviceDisconnected() {
        super.onDeviceDisconnected()
        wristDecoder = null
        if (disconnectFlag) {
            //如果是设置了这个标识
            return
        }
        doScan()
    }

    private fun postState(state: Int) {
        val broadcast = Intent(BROADCAST_CONNECTION_STATE)
        broadcast.putExtra(EXTRA_CONNECTION_STATE, state)
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, this.currentAddress)
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast)
    }


    fun doScan() {
        if (!isBleEnable || currentAddress.isEmpty()) {
            stopSelf()
            return
        }
        if (bleState == STATE_CONNECTING || bleState == STATE_CONNECTED || bleState == STATE_SCANNING) {
            //已经处理连接状态
            return
        }
        postState(STATE_SCANNING)

        scanHit = false
        if (needScanBeforeConnect()) {
            scanner.startScan(listOf(ScanFilter.Builder().setDeviceAddress(currentAddress).build()), scanSettings, scanCallback)
        } else {
            wristBleManager.connect(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(targetAddress))
        }

    }

    fun disconnect() {
        disconnectFlag = true
        if (bleState != STATE_DISCONNECTED && bleState != STATE_DISCONNECTING && bleState != STATE_LINK_LOSS) {
            wristBleManager.disconnect()
        }

    }

    fun needScanBeforeConnect(): Boolean {
        if (hasScan) {
            return false
        }
        val manufacturer = Build.MANUFACTURER
        var needScanBeforeConnect = false


        if ((manufacturer.equals("huawei", true)) || (manufacturer.equals("samsung", false))) {
            needScanBeforeConnect = true
        }
        return needScanBeforeConnect;

    }
}
