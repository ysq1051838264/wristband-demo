package com.hdr.wristband.ble

import java.util.*

import android.os.Parcel
import android.os.Parcelable


/**
 * 手环的数据回调
 * Created by hdr on 16/7/11.
 */
data class WristSportData(

        /**
         * 数据对应的日期
         */
        val date: Date,
        /**
         * 总步数
         */
        val steps: Int,
        /**
         * 总距离
         */
        val distance: Int,
        /**
         * 总卡路里
         */
        val calories: Int,
        /**
         * 睡眠时间
         */
        val sleepTimeCnt: Int,

        /**
         * 运动的详细数据
         */
        val sportRecords:List<SportRecordData> = ArrayList<SportRecordData>(),

        /**
         * 睡眠的详细数据
         */
        val sleepRecords:List<SleepRecordData> = ArrayList<SleepRecordData>()


) : Parcelable{
    constructor(source: Parcel): this(source.readSerializable() as Date, source.readInt(), source.readInt(), source.readInt(), source.readInt(), source.createTypedArrayList(SportRecordData.CREATOR), source.createTypedArrayList(SleepRecordData.CREATOR))

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeSerializable(date)
        dest?.writeInt(steps)
        dest?.writeInt(distance)
        dest?.writeInt(calories)
        dest?.writeInt(sleepTimeCnt)
        dest?.writeTypedList(sportRecords)
        dest?.writeTypedList(sleepRecords)
    }

    companion object {
        @JvmField final val CREATOR: Parcelable.Creator<WristSportData> = object : Parcelable.Creator<WristSportData> {
            override fun createFromParcel(source: Parcel): WristSportData {
                return WristSportData(source)
            }

            override fun newArray(size: Int): Array<WristSportData?> {
                return arrayOfNulls(size)
            }
        }
    }
}

data class SportRecordData(
        //开始时间
        val startTime:Date,
        //结束时间
        val endTime:Date,
        //步数
        val step:Int
) : Parcelable {
    constructor(source: Parcel): this(source.readSerializable() as Date, source.readSerializable() as Date, source.readInt())

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeSerializable(startTime)
        dest?.writeSerializable(endTime)
        dest?.writeInt(step)
    }

    companion object {
        @JvmField final val CREATOR: Parcelable.Creator<SportRecordData> = object : Parcelable.Creator<SportRecordData> {
            override fun createFromParcel(source: Parcel): SportRecordData {
                return SportRecordData(source)
            }

            override fun newArray(size: Int): Array<SportRecordData?> {
                return arrayOfNulls(size)
            }
        }
    }
}


data class SleepRecordData(
        //开始时间
        val startTime:Date,
        //结束时间
        val endTime:Date,
        //睡眠类型
        val sleepType:String,
        //这个时间段包含的分钟数
        val sleepTimeCnt: Int
) : Parcelable {
    constructor(source: Parcel): this(source.readSerializable() as Date, source.readSerializable() as Date, source.readString(), source.readInt())

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeSerializable(startTime)
        dest?.writeSerializable(endTime)
        dest?.writeString(sleepType)
        dest?.writeInt(sleepTimeCnt)
    }

    companion object {
        @JvmField final val CREATOR: Parcelable.Creator<SleepRecordData> = object : Parcelable.Creator<SleepRecordData> {
            override fun createFromParcel(source: Parcel): SleepRecordData {
                return SleepRecordData(source)
            }

            override fun newArray(size: Int): Array<SleepRecordData?> {
                return arrayOfNulls(size)
            }
        }
    }
}