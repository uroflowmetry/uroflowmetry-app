package com.uroflowmetry.models

class BottleModel {
    var volume = 0
    var rateOfWH = 0.75f

    fun getVolumeRateString() :String{
        return "${volume}ML (W/H: $rateOfWH)"
    }
}