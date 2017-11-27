package com.kdoctor.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kdoctor.R;
import com.kdoctor.fragments.vaccines.adapters.RecyclerViewAdapterVaccine;
import com.kdoctor.main.view.MainActivity;
import com.kdoctor.models.Vaccine;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by INI\huy.trinh on 30/10/2017.
 */

public class VaccineService extends Service {

    public static Date getBirthDay() {
        return birthDay;
    }

    public static void setBirthDay(Date birthDay) {
        VaccineService.birthDay = birthDay;
    }

    private static Date birthDay;

    public static boolean isRunning() {
        return isRunning;
    }

    public static void setIsRunning(boolean isRunning) {
        VaccineService.isRunning = isRunning;
    }

    private static boolean isRunning  = false;

    public static List<Vaccine> getVaccines() {
        return vaccines;
    }

    public static void setVaccines(List<Vaccine> vaccines) {
        VaccineService.vaccines = vaccines;
    }

    private static List<Vaccine> vaccines = new ArrayList<Vaccine>();

    private static List<Vaccine> hasReadElements = new ArrayList<Vaccine>();

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    /*
                    Vaccine vaccine = new Vaccine();
                    vaccine.setActivity("Vaccine A");
                    vaccine.setStartMonth(12);
                    vaccine.setEndMonth(24);
                    */
                    Calendar calendar = Calendar.getInstance();
                    for (Vaccine vaccine :
                            vaccines) {
                        boolean hasRead = false;
                        for (Vaccine hasReadElement :
                                hasReadElements) {
                            if (hasReadElement.getId() == vaccine.getId()){
                                hasRead = true;
                                break;
                            }
                        }
                        if (!hasRead && birthDay != null && daysBetween(birthDay, calendar.getTime())/30 >= vaccine.getStartMonth()
                                && daysBetween(birthDay, calendar.getTime())/30 <= vaccine.getEndMonth()
                                && vaccine.isSelected()){
                            showNotification(vaccine);
                            try{
                                Thread.sleep(5000);
                            }
                            catch (Exception e){
                            }
                        }
                    }

                    try{
                        Thread.sleep(5000);
                    }
                    catch (Exception e){
                    }
                }
                stopSelf();
            }
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    public void showNotification(Vaccine vaccine){
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
        Notification n  = new Notification.Builder(this)
                .setContentTitle(vaccine.getActivity())
                .setContentText(RecyclerViewAdapterVaccine.getTimeStringByMonths(vaccine.getStartMonth(), RecyclerViewAdapterVaccine.IS_START_MONTH)+
                        RecyclerViewAdapterVaccine.getTimeStringByMonths(vaccine.getEndMonth(), RecyclerViewAdapterVaccine.IS_END_MONTH)
                + ((vaccine.getNote() == null || vaccine.getNote().equals("")) ? "" : " - Ghi chú: "+vaccine.getNote()))
                .setSmallIcon(R.drawable.kdoctor_icon)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.kdoctor_icon, "Xác nhận", pIntent).build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(0, n);

        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
            ringtone.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(2000);

        hasReadElements.add(vaccine);
    }

    public int daysBetween(Date d1, Date d2){
        return (int)( (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
    }
}
