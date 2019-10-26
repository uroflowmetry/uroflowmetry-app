package com.uroflowmetry.activities

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import com.uroflowmetry.R
import com.uroflowmetry.base.BaseActivity
import com.uroflowmetry.models.AppStorage
import com.uroflowmetry.models.DataModel
import com.uroflowmetry.verticalize
import kotlinx.android.synthetic.main.activity_data.*
import kotlinx.android.synthetic.main.item_data.view.*
import java.util.*
import kotlin.collections.ArrayList

class DataActivity : BaseActivity() {

    private val models = ArrayList<DataModel>()
    private val values = ArrayList<Float>()
    protected lateinit var mChart: LineChart
    private var currentID = 0

    override fun getResID(): Int {
        return R.layout.activity_data
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        models.addAll(AppStorage.build(this@DataActivity).getSavedDataModel())

        dataList.verticalize(this)
        dataList.adapter = DataAdapter()
        chartSetting()
        drawChart()
    }

    private fun drawChart(){
        if(!models.isNullOrEmpty()){
            values.clear()
            values.addAll(models[currentID].values)

            mChart.clear()
            chartSetting()
            //mChart.axisLeft.axisMaximum = getMaxValue()

            Handler().postDelayed({
                values.forEach {
                    feedMultiple(it)
                }
            }, 1000)
        }
    }

    private fun chartSetting(){

        mChart = findViewById(R.id.dataChart)

        mChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {

            override fun onNothingSelected() {

            }

            override fun onValueSelected(e: Entry?, h: Highlight?) {

            }

        })

        // enable description text
        mChart.description.isEnabled = false

        // enable touch gestures
        mChart.setTouchEnabled(true)

        // enable scaling and dragging
        mChart.isDragEnabled = true
        mChart.setScaleEnabled(true)
        mChart.setDrawGridBackground(false)

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true)

        // set an alternative background color
        //mChart.setBackgroundColor(Color.BLACK)

        val data = LineData()
        data.setValueTextColor(Color.WHITE)

        // add empty data
        mChart.data = data

        // get the legend (only possible after setting data)
        val l = mChart.legend

        // modify the legend ...
//        l.form = Legend.LegendForm.LINE
//        l.textColor = Color.WHITE
        l.isEnabled = false

        val xl = mChart.xAxis
        xl.textColor = Color.WHITE
        xl.setDrawGridLines(false)
        xl.setAvoidFirstLastClipping(true)
        xl.isEnabled = true
        xl.position = XAxis.XAxisPosition.BOTTOM

        val leftAxis = mChart.axisLeft
        leftAxis.textColor = Color.WHITE
        leftAxis.axisMaximum = getMaxValue()
        leftAxis.axisMinimum = 0f
        leftAxis.setDrawGridLines(true)

        val rightAxis = mChart.axisRight
        rightAxis.isEnabled = false
    }

    private fun getMaxValue() : Float{
        if(values.isNotEmpty()){
            val sortedValue = ArrayList<Float>()
            sortedValue.addAll(values)
            sortedValue.sort()
            return sortedValue[values.size -1]
        }else{
            return 100f
        }

    }

    //private var thread: Thread? = null
    protected fun feedMultiple(value : Float) {

        //thread?.interrupt()

        val runnable = Runnable {
            //val estimatedTime = getTestEstimatedTime()
            addEntry(value)
        }

        // Don't generate garbage runnables inside the loop.
        runOnUiThread(runnable)
    }

    protected fun addEntry(value : Float) {

        val data = mChart.data

        if (data != null) {

            var set: ILineDataSet? = data.getDataSetByIndex(0)
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet()
                data.addDataSet(set)
            }

            if(set == null) return

            data.addEntry(Entry(set.entryCount.toFloat(), value), 0)
            data.notifyDataChanged()

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged()

            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(10f)
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            mChart.moveViewToX(data.entryCount.toFloat())
            mChart.animateX(200)
            // mChart.moveViewToX(seconds)

            // this automatically refreshes the chart (calls invalidate())
//             mChart.moveViewTo(data.getXValCount()-7, 55f,
//             YAxis.AxisDependency.LEFT)
        }
    }

    private fun createSet(): LineDataSet?{

        val set = LineDataSet(null, "")
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.color = Color.BLUE
        set.setCircleColor(Color.RED)
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.lineWidth = 2f
        set.circleRadius = 4f
        set.fillAlpha = 65
        set.fillColor = Color.BLUE
        set.highLightColor = Color.rgb(244, 117, 117)
        set.valueTextColor = Color.WHITE
        set.valueTextSize = 9f
        set.setDrawValues(false)

        return set

    }

    private inner class DataAdapter : RecyclerView.Adapter<DataAdapter.CustomViewHolder>(){

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {

            val view = LayoutInflater.from(this@DataActivity).inflate(R.layout.item_data, parent, false)

            return CustomViewHolder(view)
        }

        override fun getItemCount(): Int {
            return models.size
        }

        override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
            val model = models[position]

            with(holder){
                if(currentID == position){
                    itemBackground.setBackgroundColor(Color.LTGRAY)
                }else{
                    itemBackground.setBackgroundColor(Color.WHITE)
                }
                timeViewer.text = model.getStartedTimeString()
                mDurationViewer.text = model.getMeasuredTimeSeconds().toString()
                voidedVolumeViewer.text = model.voidedVolume.toString()
                flowRateViewer.text = model.getFlowRateS()

                itemBackground.setOnClickListener {
                    currentID = position
                    drawChart()
                    notifyDataSetChanged()
                }
            }
        }

        private inner class CustomViewHolder(view : View) : RecyclerView.ViewHolder(view){
            val timeViewer = view.timeViewer
            val mDurationViewer = view.mDurationViewer
            val flowRateViewer = view.flowRateViewer
            val voidedVolumeViewer = view.voidedVolumeViewer
            val itemBackground = view.itemBackground
        }
    }
}
