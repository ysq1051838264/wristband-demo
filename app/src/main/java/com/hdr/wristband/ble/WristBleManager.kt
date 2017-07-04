package com.hdr.wristband.ble

import android.content.Context
import no.nordicsemi.android.nrftoolbox.profile.BleManager
import no.nordicsemi.android.nrftoolbox.profile.BleManagerCallbacks
import java.util.*

/**
 * Created by hdr on 16/7/4.
 */
abstract class WristBleManager(context: Context) : BleManager<WristBleManager.WristBleCallback>(context), CommandSender {


    interface WristBleCallback : BleManagerCallbacks {

        fun onReceiveData(uuid: UUID, value: ByteArray)
    }
}