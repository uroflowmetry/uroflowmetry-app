package com.uroflowmetry.base

import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.uroflowmetry.showShortToast
import java.util.HashMap

abstract class PermissionCheckActivity : BaseActivity() {

    private val PERMISSION_REQUEST_CODE = 1001

    private var actionAfterPermissionCheck : (() -> Unit)? = null

    protected fun permissionCheck(action : (() -> Unit)? = null): Boolean {
        actionAfterPermissionCheck = action
        return if (Build.VERSION.SDK_INT >= 23) {
            if (!isGuaranteedPermissions()) {
                ActivityCompat.requestPermissions(this, requiredPermissions(), PERMISSION_REQUEST_CODE)
                false
            } else {
                actionAfterPermissionCheck?.invoke()
                true
            }
        } else {
            actionAfterPermissionCheck?.invoke()
            true
        }
    }

    private fun isGuaranteedPermissions() : Boolean{
        requiredPermissions().forEach {
            if(Build.VERSION.SDK_INT >= 23){
                if(checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED) return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                val perms = HashMap<String, Int>()
                requiredPermissions().forEach {
                    perms[it] = PackageManager.PERMISSION_GRANTED
                }

                for (i in permissions.indices) {
                    perms[permissions[i]] = grantResults[i]
                }

                if (checkPermissionsResult(perms)) {
                    // All Permissions Granted
                    actionAfterPermissionCheck?.invoke()
                } else {
                    // Permission Denied
                    "Permission was denied".showShortToast(this)
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun checkPermissionsResult(perms : HashMap<String, Int>) : Boolean{
        requiredPermissions().forEach {
            if(perms[it] != PackageManager.PERMISSION_GRANTED) return false
        }
        return true
    }

    abstract fun requiredPermissions() : Array<String>

/*
/// Replace these permission parmas in the activity that exetned this activity
override fun requiredPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
*/
}