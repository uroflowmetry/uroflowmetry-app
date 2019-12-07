package com.uroflowmetry.activities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.uroflowmetry.R
import com.uroflowmetry.base.PermissionCheckActivity
import com.uroflowmetry.invisiable
import com.uroflowmetry.visiable
import ir.sohreco.androidfilechooser.FileChooser
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : PermissionCheckActivity() {
    override fun getResID(): Int {
        return R.layout.activity_main
    }

    override fun requiredPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        btnStart.setOnClickListener {
            permissionCheck {
                val intent = Intent(this, CameraActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        btnGallery.setOnClickListener {
            permissionCheck {
                fragment.visiable()
                val chooser = FileChooser.Builder(FileChooser.ChooserType.FILE_CHOOSER) {
                    if(File(it).exists()){
                        val intent = Intent(this, VideoPlayActivity::class.java)
                        intent.putExtra("filePath", it)
                        startActivity(intent)

                    }else{
                        Toast.makeText(applicationContext,"File Load Failed", Toast.LENGTH_SHORT).show()
                    }

                    fragment.invisiable()
                }
                    .setMultipleFileSelectionEnabled(false)
                    .setFileFormats(arrayOf(".mp4", ".MP4"))
                    .build()

                supportFragmentManager.beginTransaction().add(R.id.fragment, chooser).commit()
            }
        }

        arrayOf(btnData, btnProfile, btnVolume, btnSendToDoctor).forEach {
            it.setOnClickListener { view ->
                when(view.id){
                    R.id.btnData -> startActivity(DataActivity::class.java)
                    R.id.btnProfile -> startActivity(ProfileActivity::class.java)
                    R.id.btnVolume -> startActivity(VolumeActivity::class.java)
                    R.id.btnSendToDoctor -> startActivity(LoginActivity::class.java)
                }
            }
        }
    }

    override fun onBackPressed() {
        if(fragment.visibility == View.VISIBLE){
            fragment.invisiable()
        }else{
            finish()
        }
    }
}
