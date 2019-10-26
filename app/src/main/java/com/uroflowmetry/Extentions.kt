package com.uroflowmetry

import android.view.View
import android.animation.ObjectAnimator
import android.content.Context
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


fun View.invisiable(){
    this.visibility = View.INVISIBLE
}

fun View.gone(){
    this.visibility = View.GONE
}

fun View.visiable(){
    this.visibility = View.VISIBLE
}

fun String.showLongToast(context: Context){
    Toast.makeText(context, this, Toast.LENGTH_LONG).show()
}

fun String.showShortToast(context: Context){
    Toast.makeText(context, this, Toast.LENGTH_SHORT).show()
}

fun RecyclerView.verticalize(context: Context){
    this.layoutManager = LinearLayoutManager(
        context, RecyclerView.VERTICAL,
        false
    )
}