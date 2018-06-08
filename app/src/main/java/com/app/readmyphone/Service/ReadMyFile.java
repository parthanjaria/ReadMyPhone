package com.app.readmyphone.Service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;

import com.app.readmyphone.Activity.HomeActivity;
import com.app.readmyphone.Model.FileExtension;
import com.app.readmyphone.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

public class ReadMyFile extends Service {

    static final public String BROADCAST_FILTER = "BROADCAST_FILTER", FILENAME = "FILENAME",
            TOTAL_FILE_SIZE = "TOTAL_FILE_SIZE", TOTAL_FILES = "TOTAL_FILES", FINISHED = "FINISHED",
            FILEEXE = "FILEEXE", STOPPED = "STOPPED";
    double total_size;
    long total_files;
    Thread thread;
    private boolean alreadyRunning = true;
    ArrayList<File> fileArrayList;
    HashMap<String, Integer> fileExtensions;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;


    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            thread = new Thread() {
                @Override
                public void run() {
                    super.run();
                    letScan();
                }
            };
            thread.start();
        }
    }

    @Override
    public void onCreate() {

        HandlerThread thread = new HandlerThread("ReadMyFileArguments",
                THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        alreadyRunning = false;
        stopSelf();
    }

    protected void letScan() {

        String fileName = Environment.getExternalStorageDirectory().toString();
        total_size = 0;
        total_files = 0;
        fileArrayList = new ArrayList<>();
        fileExtensions = new HashMap<>();
        getFiles(fileName);
        Collections.sort(fileArrayList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.length() > o2.length() ? -1 : o1.length() < o2.length() ? 1 : 0;
            }
        });
        ArrayList<String> stringArrayList = new ArrayList<>();
        int size = fileArrayList.size() > 10 ? 10 : fileArrayList.size();
        for (int i = 0; i < size; i++) {
            stringArrayList.add(fileArrayList.get(i).getAbsolutePath());
        }
        Iterator<String> iterator = fileExtensions.keySet().iterator();
        ArrayList<FileExtension> fileExtensionArrayList = new ArrayList<>();
        while (iterator.hasNext()) {
            FileExtension fileExtension = new FileExtension();
            fileExtension.setExeName(iterator.next());
            fileExtension.setNo_of_time(fileExtensions.get(fileExtension.getExeName()));
            fileExtensionArrayList.add(fileExtension);
        }
        Collections.sort(fileExtensionArrayList, new Comparator<FileExtension>() {
            @Override
            public int compare(FileExtension o1, FileExtension o2) {
                return o1.getNo_of_time() > o2.getNo_of_time() ? -1 : o1.getNo_of_time() < o2.getNo_of_time() ? 1 : 0;
            }
        });
        JSONObject jsonObject = new JSONObject();
        size = fileExtensionArrayList.size() > 5 ? 5 : fileExtensionArrayList.size();
        for (int i = 0; i < size; i++) {
            FileExtension fileExtension = fileExtensionArrayList.get(i);
            try {
                jsonObject.put(fileExtension.getExeName(), fileExtension.getNo_of_time());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        sendMesage(null, total_files, total_size, stringArrayList, jsonObject.toString());
        stopSelf();
    }

    public void getFiles(String DirectoryPath) {
        if (alreadyRunning) {
            File f = new File(DirectoryPath);
            File[] files = f.listFiles();
            f.mkdirs();
            if (files.length > 0 && alreadyRunning) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        getFiles(file.getAbsolutePath());
                    } else {
                        total_files++;
                        total_size += (file.length() / 1048576.0f);
                        fileArrayList.add(file);
                        String fileName_ = file.getName();
                        String exe = "";
                        try {
                            exe = fileName_.substring(fileName_.lastIndexOf(".") + 1).toLowerCase();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (fileExtensions.containsKey(exe)) {
                            fileExtensions.put(exe, fileExtensions.get(exe) + 1);
                        } else {
                            fileExtensions.put(exe, 1);
                        }

                        if (alreadyRunning)
                            sendMesage(file.getAbsolutePath(), total_files, total_size, null, null);
                    }

                }
            }
        }
    }

    public void sendMesage(String fileName, long total_files, double total_size, ArrayList<String> strings, String fileExe) {
        Intent intent = new Intent(BROADCAST_FILTER);
        if (fileName != null) {
            intent.putExtra(FILENAME, fileName);
            sendNotification(total_files, true);
        } else {
            intent.putExtra(FINISHED, strings);
            intent.putExtra(FILEEXE, fileExe);
            sendNotification(total_files, false);
        }
        intent.putExtra(TOTAL_FILES, total_files);
        intent.putExtra(TOTAL_FILE_SIZE, total_size);
        intent.putExtra(STOPPED, alreadyRunning);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public void sendNotification(long total_files, boolean b) {

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (b) {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(getApplicationContext(), "file_notification").setSmallIcon(R.mipmap.ic_launcher).setContentTitle("Scanning in progress").setContentText(total_files + " Files Scanned").setTicker(getResources().getString(R.string.app_name)).setVisibility(1);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
            }
            Intent resultIntent = new Intent(getApplicationContext(), HomeActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
            mNotificationManager.notify(100, mBuilder.build());
        } else {
            mNotificationManager.cancel(100);
        }
    }


}
