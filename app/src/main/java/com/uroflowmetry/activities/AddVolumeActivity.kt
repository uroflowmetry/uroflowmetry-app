package com.uroflowmetry.activities

import android.os.Bundle
import com.uroflowmetry.R
import com.uroflowmetry.base.BaseActivity
import com.uroflowmetry.models.AppStorage
import com.uroflowmetry.models.BottleModel
import com.uroflowmetry.showShortToast
import kotlinx.android.synthetic.main.activity_add_volume.*

class AddVolumeActivity : BaseActivity() {
    override fun getResID(): Int {
        return R.layout.activity_add_volume
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        btnSave.setOnClickListener {
            val out = Out<String>()
            if(!checkEditText(etVolume, out)) return@setOnClickListener
            val volume = out.get()!!.toInt()

            if(!checkEditText(etRateOfWH, out)) return@setOnClickListener
            val rateOfWH = out.get()!!.toFloat()

            val bottleModel = BottleModel()
            bottleModel.volume = volume
            bottleModel.rateOfWH = rateOfWH

            AppStorage.build(this).addCustomVolume(bottleModel)
            "Your bottle was saved successfully".showShortToast(this)
        }
    }
}
