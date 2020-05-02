package com.blurryrobot.socialdistance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.blurryrobot.socialdistance.service.BeaconResult;
import com.blurryrobot.socialdistance.service.SocialDistanceService;
import com.blurryrobot.socialdistance.utility.BluetoothStatusIntentReceiver;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import org.joda.time.DateTime;
import org.joda.time.Days;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SocialDistanceService.ServiceListener {

    private final String TAG = "MainActivity";
    private final int BLUETOOTH_PERMISSION_REQUEST = 100, LOCATION_PERMISSION_REQUEST = 101, BLUETOOTH_ON_REQUEST = 102;

    private BluetoothManager bluetoothManager;
    private SwitchMaterial serviceSwitch;
    private LineChart weeklyChart;
    private LineChart monthlyChart;
    private RecyclerView recyclerView;

    private TextView dailyCount;
    private TextView weeklyCount;
    private TextView monthlyCount;

    private AdView mAdView;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore firestore;
    private CollectionReference userContacts;

    private SocialDistanceService socialDistanceService;
    private FirestoreRecyclerAdapter<ContactModel, ContactViewHolder> adapter;


    private BluetoothStatusIntentReceiver bluetoothStatusReceiver;
    private boolean serviceRunning = false;


    private BluetoothStatusIntentReceiver.BluetoothStatusListener bluetoothStatusListener = new BluetoothStatusIntentReceiver.BluetoothStatusListener() {
        @Override
        public void onStatusChanged() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isBluetoothOn() && serviceRunning)
                        stopService();
                }
            });
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, final IBinder service) {
            Log.d(TAG, "onServiceConnected");
            final SocialDistanceService.LocalBinder localBinder = (SocialDistanceService.LocalBinder) service;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    serviceRunning = true;
                    serviceSwitch.setChecked(true);
                    socialDistanceService = localBinder.getService();
                    boolean started = socialDistanceService.start(MainActivity.this, UUID.randomUUID().toString());
                    if (!started){
                        showToast(getString(R.string.failed_to_start));
                        stopService();
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    serviceRunning = false;
                    serviceSwitch.setChecked(false);
                }
            });
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();
        userContacts = firestore.collection("users").document(firebaseUser.getUid()).collection("contacts");
        serviceSwitch = findViewById(R.id.switch1);
        weeklyChart = findViewById(R.id.weekly_chart);
        weeklyChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,LineChartActivity.class);
                startActivity(intent);
            }
        });
        monthlyChart = findViewById(R.id.monthly_chart);
        dailyCount = findViewById(R.id.daily_count);
        weeklyCount = findViewById(R.id.weeklyCount);
        monthlyCount = findViewById(R.id.monthlyCount);
        recyclerView = findViewById(R.id.contact_reclyerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        Query query = userContacts.orderBy("timestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<ContactModel> options = new FirestoreRecyclerOptions.Builder<ContactModel>()
                .setQuery(query, ContactModel.class)
                .build();
        weeklyChart.setTouchEnabled(true);
        monthlyChart.setTouchEnabled(false);
        weeklyChart.setDragEnabled(false);
        monthlyChart.setScaleEnabled(false);
        weeklyChart.setPinchZoom(false);
        monthlyChart.setPinchZoom(false);
        bluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(BLUETOOTH_SERVICE);
        bluetoothStatusReceiver = new BluetoothStatusIntentReceiver(bluetoothStatusListener);
        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        Toolbar toolbar = findViewById(R.id.toolbar2);
        if(toolbar != null)
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if(item.getItemId() == R.id.signOut){
                        firebaseAuth.signOut();
                        Intent intent = new Intent(getApplicationContext(),SplashActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        return true;
                    }else if(item.getItemId() == R.id.about){
                        Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                        startActivity(intent);
                        return true;
                    }
                    return false;
                }
            });
        registerReceiver(bluetoothStatusReceiver, intentFilter);
        serviceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "onCheckedChanged: " + isChecked);
                if (isChecked) {
                    if (!checkHasBluetoothFeature()) {
                        Log.d(TAG, "Bluetooth is not supported on this device.");
                        showToast(getString(R.string.bluetooth_unsupported));
                        //Todo show alert dialog.
                        serviceSwitch.setChecked(false);
                    } else {
                        if (!hasBluetoothPermission()) {
                            requestBluetoothPermission();
                            serviceSwitch.setChecked(false);
                        } else if (!hasLocationPermission()) {
                            requestLocationPermission();
                            serviceSwitch.setChecked(false);
                        } else if (!isBluetoothOn()){
                            requestBluetoothOn();
                        }else {
                            startService();
                        }
                    }
                } else if(serviceRunning)
                    stopService();
            }
        });
        registerAdapter(options);
    }


    void registerAdapter(FirestoreRecyclerOptions<ContactModel> options){
       adapter = new FirestoreRecyclerAdapter<ContactModel, ContactViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ContactViewHolder contactViewHolder, int position, @NonNull ContactModel contactModel) {
                String dist = String.format("%.2f", contactModel.getDistance());
                String distance_str = getString(R.string.contact_recorded_at) + " " + dist + "m";
                contactViewHolder.setDistance(distance_str);
                String dateFormat = "";
                if(contactModel.getTimestamp() != null) {
                    Date date = contactModel.getTimestamp().toDate();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM yy hh:mm:ss");
                    dateFormat = simpleDateFormat.format(date);
                }
                contactViewHolder.setTime(dateFormat);
            }

            @NonNull
            @Override
            public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_item, parent, false);
                return new ContactViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
    }
    void setWeeklyChart(Map<Integer,Integer> week){
       XAxis xAxis = weeklyChart.getXAxis();
        YAxis yAxis = weeklyChart.getAxisLeft();
        weeklyChart.getAxisRight().setEnabled(false);
        weeklyChart.getAxisLeft().setEnabled(false);
        weeklyChart.getXAxis().setEnabled(false);
        Description description = new Description();
        description.setText("");
        weeklyChart.setDescription(description);
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        yAxis.enableGridDashedLine(10f, 10f, 0f);
        ArrayList<Entry> values = new ArrayList<>();
        for(int i = 6; i > -1; i--){
            int value = 0;
            if(week.containsKey(i))
                value = week.get(i);
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
        weeklyChart.setData(linedata);
        weeklyChart.getLegend().setEnabled(false);
        weeklyChart.invalidate();
    }

    void setMonthlyChart(Map<Integer,Integer> month){
        XAxis xAxis = monthlyChart.getXAxis();
        YAxis yAxis = monthlyChart.getAxisLeft();
        monthlyChart.getAxisRight().setEnabled(false);
        monthlyChart.getAxisLeft().setEnabled(false);
        monthlyChart.getXAxis().setEnabled(false);
        Description description = new Description();
        description.setText("");
        monthlyChart.setDescription(description);
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        yAxis.enableGridDashedLine(10f, 10f, 0f);
        ArrayList<Entry> values = new ArrayList<>();
        for(int i = 30; i > -1; i--){
            int value = 0;
            if(month.containsKey(i))
                value = month.get(i);
            values.add(new Entry( 30 - i ,value));
        }
        LineDataSet lineDataSet = new LineDataSet(values, getString(R.string.monthly_contacts));
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
        monthlyChart.setData(linedata);
        monthlyChart.getLegend().setEnabled(false);
        monthlyChart.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isBluetoothOn() && serviceRunning)
            stopService();
        final Timestamp now = Timestamp.now();
        Date yesterdayDate = new DateTime(now.toDate()).minusDays(1).toDate();
        Date lastweekDate = new DateTime(now.toDate()).minusDays(7).toDate();
        Date lastMonthDate = new DateTime(now.toDate()).minusDays(30).toDate();
        Timestamp yesterday = new Timestamp(yesterdayDate);
        Timestamp lastweek = new Timestamp(lastweekDate);
        Timestamp lastMonth = new Timestamp(lastMonthDate);

        userContacts.whereGreaterThan("timestamp",yesterday).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                int count = 0;
                if (queryDocumentSnapshots != null)
                   count = queryDocumentSnapshots.size();
                dailyCount.setText("" + count);
            }
        });
        userContacts.whereGreaterThan("timestamp",lastweek).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                int count = 0;
                Map<Integer,Integer> lastWeekCount = new HashMap<>();
                if (queryDocumentSnapshots != null) {
                    count = queryDocumentSnapshots.size();
                    for(int i = 0; i < queryDocumentSnapshots.size(); i++) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(i);
                        Timestamp contactTimestamp = document.getTimestamp("timestamp", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                        Date contactDate = contactTimestamp.toDate();
                        DateTime contactDateTime = new DateTime(contactDate);
                        DateTime nowDateTime = new DateTime(now.toDate());
                        int days = Days.daysBetween(contactDateTime, nowDateTime).getDays();
                        if (lastWeekCount.containsKey(days)){
                            int currentCount = lastWeekCount.get(days);
                            currentCount++;
                            lastWeekCount.put(days,currentCount);
                        }else{
                            lastWeekCount.put(days,1);
                        }
                    }
                    setWeeklyChart(lastWeekCount);
                }
                weeklyCount.setText("" + count);
            }
        });
        userContacts.whereGreaterThan("timestamp",lastMonth).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                int count = 0;
                if (queryDocumentSnapshots != null)
                    count = queryDocumentSnapshots.size();
                Map<Integer,Integer> lastMonthCount = new HashMap<>();
                if (queryDocumentSnapshots != null) {
                    count = queryDocumentSnapshots.size();
                    for(int i = 0; i < queryDocumentSnapshots.size(); i++) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(i);
                        Timestamp contactTimestamp = document.getTimestamp("timestamp", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                        Date contactDate = contactTimestamp.toDate();
                        DateTime contactDateTime = new DateTime(contactDate);
                        DateTime nowDateTime = new DateTime(now.toDate());
                        int days = Days.daysBetween(contactDateTime, nowDateTime).getDays();
                        if (lastMonthCount.containsKey(days)){
                            int currentCount = lastMonthCount.get(days);
                            currentCount++;
                            lastMonthCount.put(days,currentCount);
                        }else{
                            lastMonthCount.put(days,1);
                        }
                    }
                    setMonthlyChart(lastMonthCount);
                }
                monthlyCount.setText("" + count);
            }
        });
    }

    private void startService() {
        Log.d(TAG, "startService");
        Intent intent = new Intent(this, SocialDistanceService.class);
        ContextCompat.startForegroundService(this, intent);
        bindService(new Intent(this, SocialDistanceService.class),serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void stopService() {
        Log.d(TAG, "stopService");
        Intent serviceIntent = new Intent(this, SocialDistanceService.class);
        stopService(serviceIntent);
        unbindService(serviceConnection);
        serviceRunning = false;
        serviceSwitch.setChecked(false);
    }

    private boolean hasBluetoothPermission() {
        boolean bluetoothPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH)
                == PackageManager.PERMISSION_GRANTED;
        boolean bluetoothAdminPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_ADMIN)
                == PackageManager.PERMISSION_GRANTED;
        return bluetoothPermission && bluetoothAdminPermission;
    }

    private boolean hasLocationPermission() {
        boolean fineLocation = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean backgroundLocation = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            backgroundLocation = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        } else
            backgroundLocation = true;
        return fineLocation && backgroundLocation;
    }


    private void requestBluetoothPermission() {
        Log.d(TAG, "requestBluetoothPermission");
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                Manifest.permission.BLUETOOTH) || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                Manifest.permission.BLUETOOTH_ADMIN)) {
            showBluetoothAlertDialog();
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
        } else {
            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                    BLUETOOTH_PERMISSION_REQUEST);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }
    }

    private void requestLocationPermission() {
        Log.d(TAG, "requestLocationPermission");
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            showLocationAlertDialog();
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
        } else {
            // No explanation needed; request the permission
            String[] permissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                permissions = new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION};
            else
                permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                ActivityCompat.requestPermissions(MainActivity.this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST);
            }

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }
    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult");
        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(this);
        switch (requestCode) {
            case BLUETOOTH_PERMISSION_REQUEST:
                Log.d(TAG, "Bluetooth Permission Request");
                for (int i = 0; i < permissions.length; i++) {
                    int result = grantResults[i];
                    boolean granted = result == PackageManager.PERMISSION_GRANTED;

                    Log.d(TAG, "Permission: " + permissions[i] + " Granted? " + granted);
                    if (!granted) {
                        alertDialogBuilder.setTitle(getString(R.string.limited_functionality));
                        alertDialogBuilder.setMessage(getString(R.string.bluetooth_limited));
                        alertDialogBuilder.setPositiveButton(android.R.string.ok, null);
                        alertDialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                            @Override
                            public void onDismiss(DialogInterface dialog) {
                            }

                        });
                        alertDialogBuilder.show();
                        return;
                    }

                }
                break;
            case LOCATION_PERMISSION_REQUEST:
                Log.d(TAG, "Location Permission Request");
                for (int i = 0; i < permissions.length; i++) {
                    int result = grantResults[i];
                    boolean granted = result == PackageManager.PERMISSION_GRANTED;

                    Log.d(TAG, "Permission: " + permissions[i] + " Granted? " + granted);
                    if (!granted) {
                        alertDialogBuilder.setTitle(getString(R.string.limited_functionality));
                        alertDialogBuilder.setMessage(getString(R.string.location_limited));
                        alertDialogBuilder.setPositiveButton(android.R.string.ok, null);
                        alertDialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                            @Override
                            public void onDismiss(DialogInterface dialog) {
                            }

                        });
                        alertDialogBuilder.show();
                        return;
                    }
                }
                break;
        }
        if (hasBluetoothPermission() && hasLocationPermission()){
            if(isBluetoothOn())
                startService();
            else
                requestBluetoothOn();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");
        if(requestCode == BLUETOOTH_ON_REQUEST){
            if(resultCode == RESULT_OK && isBluetoothOn())
                startService();
            else
                showToast(getString(R.string.bluetooth_must_be_enabled));
        }
    }

    private void showBluetoothAlertDialog() {
        Log.d(TAG, "showBluetoothAlertDialog");
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.bluetooth_permission_request))
                .setMessage(getString(R.string.bluetooth_permission_request_desc))
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                                BLUETOOTH_PERMISSION_REQUEST);
                    }
                });
        builder.show();
    }

    private void showLocationAlertDialog() {
        Log.d(TAG, "showLocationAlertDialog");
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.location_permission_request))
                .setMessage(getString(R.string.location_permission_request_desc))
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                LOCATION_PERMISSION_REQUEST);
                    }
                });
        builder.show();
    }

    private void requestBluetoothOn(){
        Log.d(TAG, "requestBluetoothOn");

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.enable_bluetooth));
        builder.setMessage(getString(R.string.enable_bluetooth_reason));
        builder.setPositiveButton(android.R.string.ok,null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent,BLUETOOTH_ON_REQUEST);
            }
        });
        builder.show();
    }

    private boolean isBluetoothOn() {
        boolean isEnabled = bluetoothManager != null &&  bluetoothManager.getAdapter() != null && bluetoothManager.getAdapter().isEnabled();
        return  isEnabled;
    }

    private boolean checkHasBluetoothFeature() {
        boolean isBluetoothSupported = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        return isBluetoothSupported;
    }

    private void showToast(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }



    @Override
    public void onResult(final BeaconResult result) {
        Log.d(TAG, "onResult");
        final UUID uuid = UUID.fromString(result.getBeaconId());
        final double distance = result.getDistance();
        Timestamp now = Timestamp.now();
        Date yesterdayDate = new DateTime(now.toDate()).minusDays(1).toDate();
        Timestamp yesterday = new Timestamp(yesterdayDate);
        userContacts.whereEqualTo("beaconId",uuid.toString()).whereLessThan("timestamp",now).whereGreaterThan("timestamp",yesterday).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if(e != null){
                    Log.e(TAG, "Error querying.", e);
                    return;
                }
                Log.d(TAG, "Is Empty?" + queryDocumentSnapshots.isEmpty());
                if(queryDocumentSnapshots.isEmpty()){
                    Log.d(TAG, "adding:" + uuid.toString());
                    Map<String, Object> map = new HashMap<>();
                    map.put("beaconId", uuid.toString());
                    map.put("timestamp", FieldValue.serverTimestamp());
                    map.put("distance",distance);
                    userContacts.add(map);
                }
            }
        });
    }
}
