package com.uroflowmetry.models

import android.content.Context
import com.uroflowmetry.library.utils.TinyDB

class AppStorage private constructor(context: Context) {

    companion object {
        private val TAG = "AppStorage"
        private var mDB: TinyDB? = null
        private var mInstance : AppStorage? = null
        fun build(context : Context) : AppStorage{
            mDB = TinyDB(context)
            try {
                if(mInstance == null) {
                    mInstance = mDB?.getObject(TAG, AppStorage::class.java) as AppStorage
                }

                return  mInstance!!
            } catch (e: NullPointerException) {

                mInstance = AppStorage(context)
                return mInstance!!
            }

        }
    }

    private var customVolumes = ArrayList<BottleModel>()

    private var currentBottleModel: BottleModel? = null
    fun getCurrentBottleModel() : BottleModel{
        if(currentBottleModel == null){
            currentBottleModel = getCustomVolumes()[0]
            update()
        }

        return currentBottleModel!!
    }

    fun setCurrentBottleModel(bottleModel: BottleModel){
        currentBottleModel = bottleModel
        update()
    }

    private val savedData = ArrayList<DataModel>()

    fun saveDataModel(dataModel: DataModel){
        savedData.add(dataModel)
        update()
    }

    fun getSavedDataModel() : ArrayList<DataModel>{
        return savedData
    }

    var profile = ProfileModel()

    fun getCustomVolumes() : ArrayList<BottleModel>{

        if(customVolumes.isEmpty()){
            var bottleModel = BottleModel()
            bottleModel.volume = 500
            customVolumes.add(bottleModel)

            bottleModel = BottleModel()
            bottleModel.volume = 1000
            customVolumes.add(bottleModel)

            bottleModel = BottleModel()
            bottleModel.volume = 1500
            customVolumes.add(bottleModel)

            bottleModel = BottleModel()
            bottleModel.volume = 2000
            customVolumes.add(bottleModel)

            update()
        }
        return customVolumes
    }

    fun addCustomVolume(volume : BottleModel){
        customVolumes.add(volume)
        update()
    }

    fun update() {
        mDB?.putObject(TAG, this)
    }

    init {
        mDB = TinyDB(context)
    }
}