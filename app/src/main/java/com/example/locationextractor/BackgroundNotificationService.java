package com.example.locationextractor;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class BackgroundNotificationService extends IntentService {

    public BackgroundNotificationService() {
        super("Service");
    }

    public static NotificationCompat.Builder notificationBuilder;
    public static NotificationManager notificationManager;
    FileUtil excelUtil;
    public  static Context context;

    @Override
    protected void onHandleIntent(Intent intent) {

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        context=this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("id", "an", NotificationManager.IMPORTANCE_LOW);

            notificationChannel.setDescription("no sound");
            notificationChannel.setSound(null, null);
            notificationChannel.enableLights(false);
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.enableVibration(false);
            notificationManager.createNotificationChannel(notificationChannel);

        }

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder = new NotificationCompat.Builder(this, "id")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Importing excel")
                .setContentText("")
                .setDefaults(0)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        Notification notification=notificationBuilder.build();
        //notificationManager.notify(0, notification);
        startForeground(0,notification);


    }


    public static void updateNotification(int currentProgress) {


        notificationBuilder.setProgress(100, currentProgress, false);
        notificationBuilder.setContentText(currentProgress + "%");
        notificationManager.notify(0, notificationBuilder.build());
    }


    public static void sendProgressUpdate(boolean downloadComplete) {

        Intent intent = new Intent(MainActivity.PROGRESS_UPDATE);
        intent.putExtra("downloadComplete", downloadComplete);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void onDownloadComplete(boolean downloadComplete) {
        sendProgressUpdate(downloadComplete);
        //NotificationManagerCompat.from(context).cancel(0);
        notificationManager.cancel(0);
        notificationBuilder.setProgress(0, 0, false);
        notificationBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        notificationBuilder.setContentTitle("Excel Imported Successfully");
        notificationManager.notify(0, notificationBuilder.build());

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        NotificationManagerCompat.from(context).cancel(0);
    }

}