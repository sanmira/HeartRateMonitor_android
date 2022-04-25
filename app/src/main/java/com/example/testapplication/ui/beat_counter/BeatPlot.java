package com.example.testapplication.ui.beat_counter;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;

import com.example.testapplication.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;

public class BeatPlot {

    private String TAG = "Plot";

    private Activity m_activity;

    private LineChart lineChart;

    public BeatPlot(Activity activity) {
        m_activity = activity;

        lineChart = m_activity.findViewById(R.id.beat_chart);
        lineChart.getDescription().setEnabled(false);
//    lineChart.getDescription().setText("Heart Beat Plot");
        lineChart.setTouchEnabled(false);
        lineChart.setDragEnabled(false);
        lineChart.setScaleEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setPinchZoom(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setBackgroundColor(Color.WHITE);

        XAxis xl = lineChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(false);
        xl.setDrawLabels(false);
        Log.d(TAG, "Labels: " + xl.getLabelCount());
        xl.setEnabled(true);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMaximum(255f);
        leftAxis.setAxisMinimum(180f);
        leftAxis.setDrawLabels(false);
        leftAxis.setEnabled(true);

        YAxis rightAxis = lineChart.getAxisRight();
        leftAxis.setDrawGridLines(false);
        rightAxis.setEnabled(false);

        LineData lineData = new LineData();
        lineData.setValueTextColor(Color.WHITE);
        lineChart.setData(lineData);
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setDrawValues(false);
        set.setAxisDependency(YAxis.AxisDependency.RIGHT);
        set.setLineWidth(5f);
        set.setColor(Color.RED);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.3f);
        return set;
    }

    public void clearGraph() {
        lineChart.clearValues();
    }

    public void addEntry(double value) {
        LineData data = lineChart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), (float)value), 0);
            if (data.getEntryCount() > 150) {
                set.removeFirst();
                // change Indexes - move to beginning by 1
                for (int i = 1; i < 150; i++) {
                    Entry entry = set.getEntryForIndex(i);
                    entry.setX(entry.getX() - 1);
                }
            }
            data.notifyDataChanged();

            // let the chart know it's data has changed
            lineChart.notifyDataSetChanged();

            // limit the number of visible entries
            lineChart.setVisibleXRangeMaximum(150);
            //lineChart.setVisibleYRangeMaximum(150, YAxis.AxisDependency.LEFT);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            lineChart.moveViewToX(set.getEntryCount());

            lineChart.notifyDataSetChanged();

            lineChart.invalidate();
//            YAxis leftAxis = lineChart.getAxisLeft();
//            leftAxis.setAxisMaximum(set.getYMax());
//            leftAxis.setAxisMinimum(set.getYMin());
        }
    }
}
