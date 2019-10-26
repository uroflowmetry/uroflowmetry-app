package com.uroflowmetry.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.uroflowmetry.R

class EnterMeasuredVolumeDialog(context: Context) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_enter_measured_volume)


    }
}