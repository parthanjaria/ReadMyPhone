package com.app.readmyphone.Activity;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.app.readmyphone.R;
import com.app.readmyphone.Service.ReadMyFile;
import com.app.readmyphone.Utils.Util;
import com.daimajia.numberprogressbar.NumberProgressBar;

import org.json.JSONArray;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class HomeActivity extends AppCompatActivity {

    final String SCANFINISH = "scan_finished";
    @BindView(R.id.progress_files_scans)
    NumberProgressBar progressFilesScans;
    @BindView(R.id.file_names)
    TextView file_names;
    @BindView(R.id.start_stop_btn)
    Button start_stop_btn;
    @BindView(R.id.report_btn)
    Button report_btn;

    public final static long MEGA = 1048576;

    BroadcastReceiver broadcast_receiver;
    ArrayList<String> fileNamesStringArrayList;

    String fileName;

    boolean scanFinished = false;
    double totalFileSize;
    long totalFiles;
    long sizeStorageUsed;

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast_receiver, new IntentFilter(ReadMyFile.BROADCAST_FILTER));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcast_receiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.overridePendingTransition(R.anim.slidein, R.anim.slideout);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long total_size, available_size, usedSize;
        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            total_size = stat.getBlockCountLong()
                    * stat.getBlockSizeLong();
            available_size = stat.getAvailableBlocksLong()
                    * stat.getBlockSizeLong();
            usedSize = total_size - available_size;
        } else {
            total_size = (long) stat.getBlockCount()
                    * (long) stat.getBlockSize();
            available_size = (long) stat.getAvailableBlocks()
                    * (long) stat.getBlockSize();
            usedSize = total_size - available_size;
        }


        sizeStorageUsed = usedSize / MEGA;

        broadcast_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                totalFiles = intent.getLongExtra(ReadMyFile.TOTAL_FILES, 0);
                totalFileSize = intent.getDoubleExtra(ReadMyFile.TOTAL_FILE_SIZE, 0);


                if (intent.hasExtra(ReadMyFile.FILENAME)) {
                    fileName = intent.getStringExtra(ReadMyFile.FILENAME);
                    file_names.setText("Scanning File : " + fileName);
                    int per = (int) ((totalFileSize / sizeStorageUsed) * 100);
                    progressFilesScans.setProgress(per);
                } else if (intent.hasExtra(ReadMyFile.FINISHED)) {
                    scanFinished = true;
                    fileNamesStringArrayList = intent.getStringArrayListExtra(ReadMyFile.FINISHED);
                    JSONArray jsonArray = new JSONArray(fileNamesStringArrayList);

                    SharedPreferences.Editor editor = Util.getPref(context).edit();
                    editor.putString(getResources().getString(R.string.my_pref_names), jsonArray.toString());
                    editor.putLong(getResources().getString(R.string.my_pref_total_files), totalFiles);
                    editor.putFloat(getResources().getString(R.string.my_pref_total_files_size), (float) totalFileSize);
                    editor.putString(getResources().getString(R.string.my_pref_file_exe), intent.getStringExtra(ReadMyFile.FILEEXE));
                    editor.apply();
                    start_stop_btn.setText(HomeActivity.this.getResources().getString(R.string.scan));
                    progressFilesScans.setProgress(100);
                    if (intent.getBooleanExtra(ReadMyFile.STOPPED, true))
                        Toast.makeText(HomeActivity.this, getResources().getString(R.string.complete_scan), Toast.LENGTH_SHORT).show();
                }
            }
        };

    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ReadMyFile.FILENAME, fileName);
        outState.putLong(ReadMyFile.TOTAL_FILES, totalFiles);
        outState.putBoolean(SCANFINISH, scanFinished);
        outState.putDouble(ReadMyFile.TOTAL_FILE_SIZE, totalFileSize);
        outState.putStringArrayList(ReadMyFile.FINISHED, fileNamesStringArrayList);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        fileName = savedInstanceState.getString(ReadMyFile.FILENAME);

        totalFiles = savedInstanceState.getLong(ReadMyFile.TOTAL_FILES, 0);
        scanFinished = savedInstanceState.getBoolean(SCANFINISH);
        int progress = scanFinished?100: (int) ((totalFileSize / sizeStorageUsed) * 100);
        progressFilesScans.setProgress(progress);

        totalFileSize = savedInstanceState.getDouble(ReadMyFile.TOTAL_FILE_SIZE, 0);
        fileNamesStringArrayList = savedInstanceState.getStringArrayList(ReadMyFile.FINISHED);

    }


    private void letsScan() {
        Intent intentService = new Intent(HomeActivity.this, ReadMyFile.class);
        startService(intentService);
        start_stop_btn.setText(getResources().getString(R.string.stop));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            boolean granted = true;
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                granted = false;
            }
            if (granted) {
                letsScan();
            } else {
                Toast.makeText(this, getResources().getString(R.string.no_permission), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @OnClick({R.id.start_stop_btn, R.id.report_btn})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_stop_btn:
                scanFinished = false;
                if (start_stop_btn.getText().toString().equalsIgnoreCase(getResources().getString(R.string.scan))) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        if ((ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {

                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    100);
                        } else {
                            letsScan();
                        }
                    } else {
                        letsScan();
                    }
                } else {
                    Intent intentService = new Intent(HomeActivity.this, ReadMyFile.class);
                    stopService(intentService);
                    NotificationManager mNotificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.cancel(100);
                    start_stop_btn.setText(getResources().getString(R.string.scan));
                }
                break;
            case R.id.report_btn:
                if (scanFinished) {
                    Intent intent = new Intent(HomeActivity.this, ScanDetailsActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, getResources().getString(R.string.scan_yet), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        Intent intentService = new Intent(HomeActivity.this, ReadMyFile.class);
        stopService(intentService);
        finish();
    }
}
