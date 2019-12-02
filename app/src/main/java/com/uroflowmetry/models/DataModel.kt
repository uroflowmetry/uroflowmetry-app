package com.uroflowmetry.models

import com.uroflowmetry.library.utils.DateTimeUtils
import java.lang.Exception

class DataModel {
    var voidedVolume = 0 // milliliter
    var mStartedTime = "" //TimeStamp
    var mEndedTime = "" //TimeStamp
    var values = ArrayList<Float>()

    fun getVoidedTime() : Long{
        try {
            return DateTimeUtils.getDifferentMilliSec(mStartedTime, mEndedTime)
        }catch (e : Exception){
            return 0
        }
    }


    fun getStartedTimeString() : String {
        return DateTimeUtils.convertFormat(DateTimeUtils.FMT_TIMESTAMP, "yyyy/MM/dd HH:mm:ss", mStartedTime)
    }


    fun getEndTimeString() : String {
        return DateTimeUtils.convertFormat(DateTimeUtils.FMT_TIMESTAMP, "yyyy/MM/dd HH:mm:ss", mEndedTime)
    }


    fun getFlowRateF() : Float {

        try {
            val voidedTime = getVoidedTime()
            return if(voidedTime > 0){
                voidedVolume.toFloat() / (voidedTime / 1000)
            }else {
                0f
            }
        }catch (e : Exception){
            return 0f
        }

    }

    fun getFlowRateS() : String{
        return String.format("%.2f ml/s", getFlowRateF())
    }

    fun getMeasuredTimeSeconds() : Int{
        return (getVoidedTime()/1000).toInt()
    }
}