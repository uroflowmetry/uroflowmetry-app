package com.uroflowmetry.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.uroflowmetry.R
import com.uroflowmetry.base.BaseActivity
import com.uroflowmetry.models.AppStorage
import com.uroflowmetry.verticalize
import kotlinx.android.synthetic.main.activity_volume.*
import kotlinx.android.synthetic.main.item_volume.view.*

class VolumeActivity : BaseActivity() {
    override fun getResID(): Int {
        return R.layout.activity_volume
    }

    private lateinit var volumeAdapter: VolumeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        volumeList.verticalize(this)
        volumeAdapter = VolumeAdapter()
        volumeList.adapter = volumeAdapter

        btnAddVolume.setOnClickListener {
            val intent = Intent(this, AddVolumeActivity::class.java)
            startActivityForResult(intent, 1001)
        }
    }

    private inner class VolumeAdapter : RecyclerView.Adapter<VolumeAdapter.CustomViewHolder>(){

        private var selectedItemId = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
            val view = LayoutInflater.from(this@VolumeActivity).inflate(R.layout.item_volume, parent, false)
            return CustomViewHolder(view)
        }

        override fun getItemCount(): Int {
            return AppStorage.build(this@VolumeActivity).getCustomVolumes().size
        }

        override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
            val volume = AppStorage.build(this@VolumeActivity).getCustomVolumes()[position]

            holder.volumeViewer.text = volume.getVolumeRateString()

            selectedItemId = getCurrentBottleID()

            if(selectedItemId == position){
                holder.volumeViewer.setTextColor(resources.getColor(R.color.dark_green))
                holder.volumeViewer.background = resources.getDrawable(R.drawable.background_rounded_outline_green)
            }else{
                holder.volumeViewer.setTextColor(resources.getColor(R.color.gray))
                holder.volumeViewer.background = resources.getDrawable(R.drawable.background_rounded_outline)
            }

            holder.volumeViewer.setOnClickListener {
                AppStorage.build(this@VolumeActivity).setCurrentBottleModel(volume)
                AppStorage.build(this@VolumeActivity).update()
                notifyDataSetChanged()
            }
        }

        inner class CustomViewHolder(view : View) : RecyclerView.ViewHolder(view){
            val volumeViewer = view.volumeViewer
        }

        private fun getCurrentBottleID() : Int{
            var currentBottleId = 0
            AppStorage.build(this@VolumeActivity).getCustomVolumes().forEachIndexed { index, bottleModel ->
                if(bottleModel == AppStorage.build(this@VolumeActivity).getCurrentBottleModel()){
                    currentBottleId = index
                }
            }
            return  currentBottleId
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 1001){
            volumeAdapter.notifyDataSetChanged()
        }
    }
}
