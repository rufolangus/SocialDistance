package com.blurryrobot.socialdistance;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.graphics.Canvas;
import android.os.Bundle;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.github.mikephil.charting.utils.MPPointF;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LineChartActivity extends AppCompatActivity {

    LineChart lineChart;
    TextView countText;
    TextView title;

    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    FirebaseFirestore firestore;
    CollectionReference userContacts;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_chart);
        lineChart = findViewById(R.id.chart);
        countText = findViewById(R.id.count);
        title = findViewById(R.id.chart_title);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();
        userContacts = firestore.collection("users").document(firebaseUser.getUid()).collection("contacts");
        setupFirestoreQuery();
    }

    void setupFirestoreQuery(){
        final Timestamp now = Timestamp.now();
        final Date lastweekDate = new DateTime(now.toDate()).minusDays(7).toDate();
        Date lastMonthDate = new DateTime(now.toDate()).minusDays(30).toDate();
        Timestamp lastweek = new Timestamp(lastweekDate);
        Timestamp lastMonth = new Timestamp(lastMonthDate);

        userContacts.whereGreaterThan("timestamp",lastweek).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                int count = 0;
                Map<Integer,Data> lastWeekCount = new HashMap<>();
                if (queryDocumentSnapshots != null) {
                    count = queryDocumentSnapshots.size();
                    for(int i = 0; i < queryDocumentSnapshots.size(); i++) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(i);
                        Timestamp contactTimestamp = document.getTimestamp("timestamp", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                        Date contactDate = contactTimestamp.toDate();
                        DateTime contactDateTime = new DateTime(contactDate);
                        DateTime nowDateTime = new DateTime(now.toDate());
                        SimpleDateFormat simpleDateFormat = new  SimpleDateFormat("MMM DD");
                        String date = simpleDateFormat.format(contactDate);
                        int days = Days.daysBetween(contactDateTime, nowDateTime).getDays();
                        if (lastWeekCount.containsKey(days)){
                            Data data = lastWeekCount.get(days);
                            int currentCount = data.count;
                            currentCount++;
                            data.count = currentCount;
                            lastWeekCount.put(days,data);
                        }else{
                            Data data = new Data();
                            data.date = date;
                            data.count = 1;
                            lastWeekCount.put(days,data);
                        }
                    }
                    setWeeklyChart(lastWeekCount);
                }
                countText.setText("" + count);
            }
        });
    }
private class Data{
        public int count;
        public String date;
}
    void setWeeklyChart(Map<Integer,Data> week){
        XAxis xAxis = lineChart.getXAxis();
        YAxis yAxis = lineChart.getAxisLeft();
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setEnabled(false);
        lineChart.getXAxis().setEnabled(false);
        Description description = new Description();
        description.setText("");
        lineChart.setDescription(description);
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        yAxis.enableGridDashedLine(10f, 10f, 0f);
        ArrayList<Entry> values = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for(int i = 6; i > -1; i--){
            if (week.containsKey(i))
                labels.add(week.get(i).date);
        }
        final ArrayList<String> lbls = labels;
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return lbls.get((int) value);
            }
        });
        for(int i = 6; i > -1; i--){
            int value = 0;
            if(week.containsKey(i))
                value = week.get(i).count;
            values.add(new Entry( 6 - i ,value));
        }
        Collections.sort(values, new EntryXComparator());
        LineDataSet lineDataSet = new LineDataSet(values, getString(R.string.weekly_contacts));
        int color = ContextCompat.getColor(getApplicationContext(), R.color.secondaryDarkColor);
        int textColor = ContextCompat.getColor(getApplicationContext(), R.color.white);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        lineDataSet.setCircleRadius(0);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setColor(color);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setValueTextColor(textColor);
        lineDataSet.setDrawValues(false);
        dataSets.add(lineDataSet);
        LineData linedata = new LineData(lineDataSet);
        lineChart.setData(linedata);
        lineChart.getLegend().setEnabled(false);
        lineChart.invalidate();
    }
}
