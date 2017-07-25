package com.hdr.wristband.ble

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.hdr.wristband.put
import java.util.*


/**
 * 手环解码器类,用以返回手环的相关命令
 * Created by hdr on 16/7/11.
 */
abstract class WristDecoder(val context: Context, val commandSender: CommandSender) {

    abstract val TIME_OUT_MILLISECOND: Long

    val localBroadcastManager by lazy { LocalBroadcastManager.getInstance(context) }

    /**
     * 命令组队列
     */
    val cmdQueue: Queue<CmdGroup<*>> = LinkedList<CmdGroup<*>>()

    /**
     * 当前正在执行的命令
     */
    var curCmdGroup: CmdGroup<*>? = null

    val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    var timeoutAction = Runnable {

        sendBroadcast(WristBleService.CMD_TIME_OUT)
        Log.i("hdr", "手环命令超时")
        nextCmd()
    }

    //命令组,
    class CmdGroup<T : Any>(var type: String = "") {

        constructor(cmd: ByteArray) : this(cmd, null)

        constructor(cmd: ByteArray, uuid: UUID?) : this() {
            this.uuid = uuid
            this.addCmd(cmd)
        }

        var uuid: UUID? = null

        val cmdQueue: Queue<ByteArray> = LinkedList<ByteArray>()

        var curCmd: ByteArray? = null

        var data: T? = null

        fun addCmd(cmd: ByteArray) {
            cmdQueue.add(cmd)
        }

        fun next(): ByteArray? {
            this.curCmd = cmdQueue.poll()
            return this.curCmd
        }

        val isLastCmd: Boolean
            get() = cmdQueue.isEmpty()

        /**
         * 是简单的命令,并非组合命令
         */
        val isSimpleCmd: Boolean
            get() = type.isEmpty()

    }

    /**
     * 清空前面所下发的所有命令
     */
    fun clearCmds() {
        cmdQueue.clear()
    }


    fun gotFeedback() {
        mainHandler.removeCallbacks(timeoutAction)
    }

    open fun sendCmd(value: ByteArray) {
        sendCmd(CmdGroup<Any>(value))
    }

    fun sendCmd(cmdGroup: CmdGroup<*>) {
        cmdQueue.add(cmdGroup)
        if (curCmdGroup == null) {
            nextCmd()
        }
    }

    //执行同步数据操作
    abstract fun doSynData()

    protected fun nextCmd() {
        curCmdGroup?.let {
            val cmd = it.next()
            if (cmd != null) {
                commandSender.send(it.uuid, cmd)
                mainHandler.postDelayed(timeoutAction, 1000)
                return
            }
        }
        curCmdGroup = cmdQueue.poll()
        curCmdGroup?.let {
            val cmd = it.next()
            if (cmd == null) {
                nextCmd()
            } else {
                commandSender.send(it.uuid, cmd)
                mainHandler.postDelayed(timeoutAction, TIME_OUT_MILLISECOND)
            }
        }
    }


    fun <T : Any> sendResult(cmd: String, data: T) {
        curCmdGroup?.let {
            if (it.isSimpleCmd) {
                sendBroadcast(cmd, data)
            } else {
                mergeGroupData(cmd, data)
            }
        }
    }

    open fun <T : Any> mergeGroupData(cmd: String, data: T) {
    }

    fun <T : Any> sendBroadcast(cmd: String, data: T) {
        val intent = Intent(cmd)
        intent.put(WristBleService.KEY_DATA to data)
        intent.put(WristBleService.KEY_RESULT to 1)
        localBroadcastManager.sendBroadcast(intent)
    }

    fun sendBroadcast(cmd: String) {
        val intent = Intent(cmd)
        intent.put(WristBleService.KEY_RESULT to 1)
        localBroadcastManager.sendBroadcast(intent)
    }


    abstract fun onReceiveData(uuid: UUID, pkgData: ByteArray)

    /**
     * 发送配对请求
     */
    abstract fun doPair()

    /**
     * 同步时间
     */
    abstract fun synTime()

    abstract fun getSleepRecordData(dayIndex: Int)

    abstract fun getSportRecordData(dayIndex: Int)

    abstract fun prepareOTA()

    /**
     * 获取电池电量,返回电池电量
     */
    abstract fun getDeviceBattery()

    abstract fun getDeviceInfo()

    abstract fun getHeartRate()

    abstract fun callPhone()

    abstract fun sendSms()

    abstract fun sysHistorySport(code: Int)

    abstract fun sysHistorySleep(code: Int)

    abstract fun sysHistoryHeartRate(code: Int)

    abstract fun sysHistoryData()

    abstract fun findPhone(code: Int)

    abstract fun getSportData(dayIndex: Int)
}