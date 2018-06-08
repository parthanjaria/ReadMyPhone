package com.app.readmyphone.Activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.app.readmyphone.R;
import com.app.readmyphone.Adapter.ReportAdapter;
import com.app.readmyphone.Utils.Util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ScanDetailsActivity extends AppCompatActivity {

    @BindView(R.id.scrollView)
    NestedScrollView scrollView;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.total_files)
    TextView total_files;
    @BindView(R.id.total_files_size)
    TextView total_files_size;
    @BindView(R.id.file_exe)
    TextView file_exe;
    @BindView(R.id.average)
    TextView average;
    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.file_size)
    TextView file_size;
    @BindView(R.id.layout)
    LinearLayout layout;
    @BindView(R.id.lets_share)
    Button lets_share;



    LinearLayoutManager linearLayoutManager;
    ReportAdapter reportAdapter;
    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.overridePendingTransition(R.anim.slidein, R.anim.slideout);
        setContentView(R.layout.activity_scan_details);
        ButterKnife.bind(this);
        name.setText("File Name");
        name.setText("File Size");
         linearLayoutManager = new LinearLayoutManager(this){
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setNestedScrollingEnabled(false);
        ArrayList<File> files = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(Util.getPref(context).getString(getResources().getString(R.string.my_pref_names),"[]"));
            for(int i=0;i<jsonArray.length();i++)
            {
                File file = new File(jsonArray.getString(i));
                files.add(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        reportAdapter = new ReportAdapter(files);
        recyclerView.setAdapter(reportAdapter);

        long totalFiles = Util.getPref(context).getLong(getResources().getString(R.string.my_pref_total_files),0);
        double totalFilesSize = Util.getPref(context).getFloat(getResources().getString(R.string.my_pref_total_files_size),0);
        try {
            JSONObject fileExe = new JSONObject(Util.getPref(context).getString(getResources().getString(R.string.my_pref_file_exe),"{}"));
            Iterator<String> iterator = fileExe.keys();
            StringBuilder msg = new StringBuilder();
            while (iterator.hasNext())
            {
                String exe = iterator.next();
                msg.append(exe+" : "+fileExe.getInt(exe)+"\n");
            }
            file_exe.setText(msg.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        DecimalFormat form = new DecimalFormat("0.00");
        average.setText(form.format(totalFilesSize/totalFiles)+" MB");
        total_files.setText(totalFiles+"");
        total_files_size.setText(totalFilesSize+"");
        scrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_UP);
            }
        },1100);
    }

    @OnClick(R.id.lets_share)
    public void onLetsShare()
    {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        Bitmap returnedBitmap = Bitmap.createBitmap(layout.getWidth(), layout.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable =layout.getBackground();
        if (bgDrawable!=null)
            bgDrawable.draw(canvas);
        else
            canvas.drawColor(Color.WHITE);
        layout.draw(canvas);

        Uri bmpUri = null;
        try {
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "my_scan_details_" + Calendar.getInstance().getTimeInMillis() + ".png");
            FileOutputStream out = new FileOutputStream(file);
            returnedBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
            bmpUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".core.my.provider", file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        shareIntent.putExtra(Intent.EXTRA_TEXT, "Checkout my scanning details");
        shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
        shareIntent.setType("image/*");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "send"));

    }

}
