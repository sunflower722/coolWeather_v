package com.example.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import com.example.coolweather.Util.HttpUtil;
import com.example.coolweather.Util.Utility;
import com.example.coolweather.json.Weather;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");

    }

    public int onStartCommand(Intent intent,int flag,int startId){
        //更新对应数据
        updateWeather();
        updateBingPic();

        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 8*60*60*1000;  //8小时毫秒数

        long triggerAtTime = SystemClock.elapsedRealtime()+anHour;
        //使用Intent启动另一个服务 //intent的基本作用，启动活动、启动服务和传递广播，startActivity(intent)会立即执行
        Intent i =new Intent(this,AutoUpdateService.class);
        //对Intent进行封装，使得Intent不会
        PendingIntent pi = PendingIntent.getService(this,0,i,PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
        return super.onStartCommand(intent,flag,startId);
    }

    //更新缓存器中天气信息的内容
    private void updateWeather(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);
        if(weatherString != null){
            Weather oldWeather = Utility.handleWeatherResponse(weatherString);  //将字符串转换成Weather实例
            String oldWeatherId = oldWeather.basic.weatherId;
            //向服务器发送请求
            String weatherUrl = "http://guolin.tech/api/weather?cityid=" + oldWeatherId + "f30591f8a43842c8a327f4f42382b3b1";
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String reponseText = response.body().string();
                    Weather newWeather = Utility.handleWeatherResponse(reponseText);
                    //更新缓存器内容
                    if(newWeather != null && "ok".equals(newWeather.status)){
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("weather",reponseText);
                        editor.apply();
                    }

                }
            });
        }
    }
    //更新缓存器中的背景图片
    private void updateBingPic(){
        String requestBingPic="https://cn.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String bingPicResponse = response.body().string();
                String bingPic = Utility.handleBingPicResponse(bingPicResponse);
                //更新缓存器
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
            }
        });
    }
}