package com.hdr.wristband

/**
 * Created by hdr on 16/7/4.
 */
object MyMenu {

    const val CLEAR_LOG = "清除log"
    const val BIND_DEVICE = "绑定设备"
    const val UNBIND_DEVICE = "解绑设备"
    const val CONNECT_DEVICE = "连接设备"
    const val DISCONNECT = "断开连接"
    const val SYN_TIME = "同步时间"
    const val OTA = "OTA升级"
    const val DECRYPT_FIRMWARE = "破解固件"
    const val GET_DEVICE_BATTERY = "获取设备电量"
    const val GET_DEVICE_INFO = "获取设备信息"
    const val GET_SPORT_DATA = "获取运动数据"

    val menus: List<String>
        get() = listOf(CLEAR_LOG, BIND_DEVICE, UNBIND_DEVICE, CONNECT_DEVICE, DISCONNECT, SYN_TIME, DECRYPT_FIRMWARE, OTA, GET_DEVICE_BATTERY, GET_DEVICE_INFO, GET_SPORT_DATA)
}