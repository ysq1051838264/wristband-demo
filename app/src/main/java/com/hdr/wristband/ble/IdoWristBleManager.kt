package com.hdr.wristband.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.hdr.wristband.utils.StringUtils
import no.nordicsemi.android.nrftoolbox.profile.BleManager
import java.util.*

/**
 * Created by hdr on 17/7/5.
 */

class IdoWristBleManager(context: Context) : WristBleManager(context) {
    override fun getGattCallback() = bleManagerGattCallback

    companion object {

        @JvmStatic val UUID_SERVICE = UUID.fromString("00000af0-0000-1000-8000-00805f9b34fb")

        @JvmStatic val UUID_CHARACTERISTIC_NORMAL_WRITE = UUID.fromString("00000af6-0000-1000-8000-00805f9b34fb")
        @JvmStatic val UUID_CHARACTERISTIC_HEALTH_WRITE = UUID.fromString("00000af1-0000-1000-8000-00805f9b34fb")

        @JvmStatic val UUID_CHARACTERISTIC_NORMAL_NOTIFY = UUID.fromString("00000af7-0000-1000-8000-00805f9b34fb")
        @JvmStatic val UUID_CHARACTERISTIC_HEALTH_NOTIFY = UUID.fromString("00000af2-0000-1000-8000-00805f9b34fb")
    }

    var writeNormalBgc: BluetoothGattCharacteristic? = null
    var writeHealthBgc: BluetoothGattCharacteristic? = null
    var notifyNormalBgc: BluetoothGattCharacteristic? = null
    var notifyHealthBgc: BluetoothGattCharacteristic? = null

    protected val bleManagerGattCallback = object : BleManager<WristBleCallback>.BleManagerGattCallback() {

        override fun initGatt(gatt: BluetoothGatt?): Queue<Request> {
            val queue = LinkedList<Request>()
            queue.push(Request.newEnableNotificationsRequest(notifyNormalBgc))
            queue.push(Request.newEnableNotificationsRequest(notifyHealthBgc))
            return queue
        }

        override fun onDeviceDisconnected() {
            writeNormalBgc = null
            notifyNormalBgc = null
        }

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service = gatt.getService(UUID_SERVICE) ?: return false

            writeNormalBgc = service.getCharacteristic(UUID_CHARACTERISTIC_NORMAL_WRITE)
            writeHealthBgc = service.getCharacteristic(UUID_CHARACTERISTIC_HEALTH_WRITE)
            notifyNormalBgc = service.getCharacteristic(UUID_CHARACTERISTIC_NORMAL_NOTIFY)
            notifyHealthBgc = service.getCharacteristic(UUID_CHARACTERISTIC_HEALTH_NOTIFY)
            return writeNormalBgc != null && notifyNormalBgc != null && writeHealthBgc != null && notifyHealthBgc != null
        }

        override fun onCharacteristicNotified(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            mCallbacks.onReceiveData(characteristic.uuid, characteristic.value)
        }

    }

    override fun send(uuid: UUID?, value: ByteArray) {
        var target: BluetoothGattCharacteristic? = null

        if (writeHealthBgc != null && writeHealthBgc!!.uuid == uuid) {
            target = writeHealthBgc
        } else if (writeNormalBgc != null && writeNormalBgc!!.uuid == uuid) {
            target = writeNormalBgc
        }
        target?.let {
            val code = StringUtils.format(value)
            Log.i("hdr", "发送数据:$code")
            it.value = value
            writeCharacteristic(it)
        }
    }


}
