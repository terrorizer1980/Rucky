package com.mayank.rucky.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.mayank.rucky.R;
import com.mayank.rucky.activity.EditorActivity;
import com.mayank.rucky.activity.SplashActivity;
import com.mayank.rucky.receiver.NetSocketReceiver;
import com.mayank.rucky.utils.Config;
import com.mayank.rucky.utils.Constants;

import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;


public class SocketHeartbeatService extends Service {

    Config config;
    final NetSocketReceiver receiver = new NetSocketReceiver();
    Intent i;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        config = new Config(getApplicationContext());
        i = new Intent();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent sIntent = new Intent(getApplicationContext(), SplashActivity.class);
        sIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent sPendingIntent;
        sPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, sIntent, PendingIntent.FLAG_IMMUTABLE);
        startForeground(1, new NotificationCompat.Builder(getApplicationContext(), Constants.SCHANNEL_ID)
                .setContentTitle(getApplicationContext().getResources().getString(R.string.config_status_net_off))
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setContentIntent(sPendingIntent)
                .setAutoCancel(false).build());
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.mayank.rucky.netSocketConnected");
        filter.addAction("com.mayank.rucky.netSocketDisconnected");
        registerReceiver(receiver, filter);
        heartbeatTask();
        EditorActivity.updateNotification(getApplicationContext());
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        stopSelf();
        stopForeground(true);
        super.onDestroy();
    }

    void heartbeatTask() {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (config.getHIDMode() == 1) {
                    checkHeartbeat();
                    sendBroadcast(i);
                }
            }
        };
        timer.schedule(task,0,5000);
    }

    void checkHeartbeat() {
        String ip = config.getNetworkAddress().substring(0,config.getNetworkAddress().indexOf(":"));
        int port = Integer.parseInt(config.getNetworkAddress().substring(config.getNetworkAddress().indexOf(":")+1));
        try {
            Socket socket = new Socket(ip, port);
            if (socket.isConnected())
                i.setAction("com.mayank.rucky.netSocketConnected");
            else
                i.setAction("com.mayank.rucky.netSocketDisconnected");
            socket.close();
        } catch (Exception e) {
            i.setAction("com.mayank.rucky.netSocketDisconnected");
        }
    }
}