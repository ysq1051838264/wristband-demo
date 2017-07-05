package com.hdr.wristband.xrz

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.hdr.wristband.ble.WristBleManager
import com.hdr.wristband.utils.StringUtils
import no.nordicsemi.android.nrftoolbox.profile.BleManager
import java.util.*

/**
 * Created by hdr on 16/7/4.
 */
class XrzWristBleManager(context: Context) : WristBleManager(context) {

    override fun getGattCallback() = bleManagerGattCallback

    companion object {

        @JvmStatic val NRF51_UUID_SERVICE = UUID.fromString("00008FFF-1212-EFDE-1523-785FEABCD123")

        @JvmStatic val NRF51_UUID_CHARACTERISTIC_WRITE = UUID.fromString("00008FFA-1212-EFDE-1523-785FEABCD123")

        @JvmStatic val NRF51_UUID_CHARACTERISTIC_NOTIFY = UUID.fromString("00008FFB-1212-EFDE-1523-785FEABCD123")
    }

    var writeBgc: BluetoothGattCharacteristic? = null
    var notifyBgc: BluetoothGattCharacteristic? = null

    protected val bleManagerGattCallback = object : BleManager<WristBleManager.WristBleCallback>.BleManagerGattCallback() {

        override fun initGatt(gatt: BluetoothGatt?): Queue<Request> {
            val queue = LinkedList<Request>()
            queue.push(Request.newEnableNotificationsRequest(notifyBgc))
            return queue
        }

        override fun onDeviceDisconnected() {
            writeBgc = null
            notifyBgc = null
        }

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service = gatt.getService(NRF51_UUID_SERVICE) ?: return false

            writeBgc = service.getCharacteristic(NRF51_UUID_CHARACTERISTIC_WRITE)
            notifyBgc = service.getCharacteristic(NRF51_UUID_CHARACTERISTIC_NOTIFY)
            return writeBgc != null && notifyBgc != null
        }

        override fun onCharacteristicNotified(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            mCallbacks.onReceiveData(characteristic.uuid, characteristic.value)
        }

    }

    override fun send(uuid: UUID?, value: ByteArray) {
        writeBgc?.let {
            val code = StringUtils.format(value)
            Log.i("hdr", "发送数据:$code")
            it.value = value
            writeCharacteristic(it)
        }
    }

}