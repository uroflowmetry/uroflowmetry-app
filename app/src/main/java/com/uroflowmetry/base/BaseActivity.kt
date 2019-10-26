@file:Suppress("DEPRECATION")

package com.uroflowmetry.base

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.uroflowmetry.R
import com.uroflowmetry.activities.CameraActivity
import com.uroflowmetry.activities.MainActivity
import kotlinx.android.synthetic.main.activity_volume.*


@Suppress("DEPRECATION")
@SuppressLint("Registered")
abstract class BaseActivity : AppCompatActivity() {

    val RL = 0
    val LR = 1
    val TB = 2
    val BT = 3
    val NT = 4

    protected val TAG = javaClass.simpleName

    private var progressDialog: ProgressDialog? = null

    private var titleViewer: TextView? = null

    var activity: Activity? = null

    fun restartActivity(cls: Class<*>) {

        val intent = Intent(this, cls)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
        finish()
    }

    abstract fun getResID() : Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getResID())

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }

    }

    fun restartActivity(cls: Class<*>, direct: Int) {

        val intent = Intent(this, cls)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        when (direct) {
            LR -> overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right)
            RL -> overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left)
            TB -> overridePendingTransition(R.anim.anim_slide_in_top, R.anim.anim_slide_out_bottom)
            BT -> overridePendingTransition(R.anim.anim_slide_in_bottom, R.anim.anim_slide_out_top)
        }
        finish()
    }

    fun restartActivity(intent: Intent) {

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    fun startActivity(cls: Class<*>) {

        val intent = Intent(this, cls)
        startActivity(intent)
    }

    fun checkEditText(edit: EditText?, value: Out<String>): Boolean {
        if (edit == null) return false
        edit.error = null
        val string = edit.text.toString()
        if (TextUtils.isEmpty(string)) {
            edit.error = "This field is required"
            edit.requestFocus()
            return false
        }
        value.set(string)
        return true
    }

    fun checkEmail(edit: EditText, value: Out<String>): Boolean {
        if (!checkEditText(edit, value)) return false
        val string = value.get()
        val res = !TextUtils.isEmpty(string) && android.util.Patterns.EMAIL_ADDRESS.matcher(string).matches()
        if (!res) {
            edit.error = "Invalid email format."
            edit.requestFocus()
            return false
        }
        return true
    }

    fun checkTextView(textView: TextView?, value: Out<String>): Boolean {
        if (textView == null) return false
        textView.error = null
        val string = textView.text.toString()
        if (TextUtils.isEmpty(string)) {
            return false
        }
        value.set(string)
        return true
    }

    fun showKeyboard(view: View) {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInputFromWindow(view.applicationWindowToken, InputMethodManager.SHOW_FORCED, 0)
    }

    fun hideKeyboard(view: View?) {
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }


    class Out<T> {
        var s: T? = null

        fun set(value: T) {
            s = value
        }

        fun get(): T? {
            return s
        }
    }

    fun showDialog(dialog: Dialog) {
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.show()
    }

    @JvmOverloads
    fun showProgressDialog(message: String = "") {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this, ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT)
            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            //progressDialog!!.setTitle("Wait...")
        }
        progressDialog!!.setMessage(message)
        progressDialog!!.show()
    }

    fun hideProgressDialog() {
        if (progressDialog != null) progressDialog!!.dismiss()
    }

    protected open fun serviceStarted() {

    }

    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {
        hideKeyboard(this.currentFocus)

        when {

            this is CameraActivity -> {
                restartActivity(MainActivity::class.java)
            }
            else -> {
                finish()
            }
        }
    }
}
