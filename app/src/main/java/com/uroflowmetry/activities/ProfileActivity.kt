package com.uroflowmetry.activities

import android.content.Intent
import android.os.Bundle
import com.uroflowmetry.R
import com.uroflowmetry.base.BaseActivity
import com.uroflowmetry.models.AppStorage
import com.uroflowmetry.showShortToast
import kotlinx.android.synthetic.main.activity_profile.*

class ProfileActivity : BaseActivity() {
    override fun getResID(): Int {
        return R.layout.activity_profile
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        etMyEmail.setText(AppStorage.build(this).profile.myEmail)
        etDoctorEmail.setText(AppStorage.build(this).profile.doctorEmail)
        volumeViewer.text = AppStorage.build(this).getCurrentBottleModel().getVolumeRateString()

        arrayOf(btnSaveMyEmail, btnSaveDoctorEmail, btnEditVolume).forEach {
            it.setOnClickListener { view ->
                when(view.id){
                    R.id.btnSaveMyEmail ->{
                        val out = Out<String>()
                        if(!checkEmail(etMyEmail, out)) return@setOnClickListener

                        AppStorage.build(this).profile.myEmail = out.get()!!
                        AppStorage.build(this).update()
                        "Your email was saved successfully".showShortToast(this)
                    }
                    R.id.btnSaveDoctorEmail -> {
                        val out = Out<String>()
                        if(!checkEmail(etDoctorEmail, out)) return@setOnClickListener

                        AppStorage.build(this).profile.doctorEmail = out.get()!!
                        AppStorage.build(this).update()
                        "Doctor email was saved successfully".showShortToast(this)
                    }
                    R.id.btnEditVolume -> {
                        val intent = Intent(this, VolumeActivity::class.java)
                        startActivityForResult(intent, 1001)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        volumeViewer.text = AppStorage.build(this).getCurrentBottleModel().getVolumeRateString()
    }
}
