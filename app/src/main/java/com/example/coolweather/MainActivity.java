package com.example.coolweather;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.example.c1.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //缓冲器中获取数据
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //判断是否有天气数据，若有则直接显示天气界面
        if(prefs.getString("weather",null)!=null){
            Intent intent = new Intent(this,WeatherActivity.class);  //重新打开另一个服务
            startActivity(intent);
            finish();

//            //天气界面显示测试代码
//            SharedPreferences.Editor editor=prefs.edit();
//            editor.clear();
//            editor.commit();
        }
    }
}