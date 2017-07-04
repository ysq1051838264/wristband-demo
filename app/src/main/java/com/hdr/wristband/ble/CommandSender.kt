package com.hdr.wristband.ble

import java.util.*

/**
 * Created by hdr on 16/7/11.
 */
interface  CommandSender {
    fun send(value: ByteArray)

    fun send(serviceUUID: UUID, characterUUID: UUID, value: ByteArray) {
        send(value)
    }
}