package com.example.coolweather;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.c1.R;
import com.example.coolweather.Util.HttpUtil;
import com.example.coolweather.Util.Utility;
import com.example.coolweather.json.Forecast;
import com.example.coolweather.json.Weather;
import com.example.coolweather.service.AutoUpdateService;

import java.io.IOException;
import java.util.prefs.Preferences;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ScrollView weatherLayout; //天气数据滚动控件
    public SwipeRefreshLayout swipeRefresh;
    private String mWeatherId; //当前的天气Id
    private Button navButton;   //城市切换按钮
    public DrawerLayout drawerLayout;  //城市切换的滑动菜单
    private ImageView bingPicImg;  //显示背景图片


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);


        //初始化控件（为了获取控件实例，用于数据传递和显示）
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);//用于动态加载天气预测的显示部分
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        navButton=(Button) findViewById(R.id.nav_btn);   //城市切换按钮
        drawerLayout=(DrawerLayout) findViewById(R.id.drawer_layout);    //切换城市滑动菜单
        bingPicImg=(ImageView) findViewById(R.id.bing_pic_img);  //背景图片控件


        //获取数据并显示
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            //缓冲器中数据不为空，则直接读取数据并显示
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            //缓冲器中数据为空，则先服务器请求当前天气数据
            String weatherId = getIntent().getStringExtra("weather_id");
            requestWeather(weatherId);
            weatherLayout.setVisibility(View.VISIBLE);
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });

        //点击切换按钮，出现滑动菜单，实现地点的切换
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);

            }
        });

        //动态加载背景图片,优先从缓存器中获取数据
        String bingPic =prefs.getString("bing_pic",null);  //从服务器中获取
        if(bingPic!=null){
            //缓存不为空，则直接显示
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else {
            //缓存为空，则从服务器中申请
            loadBingPic();
        }


        }
        //从服务器中申请背景图片
    private void loadBingPic(){
        String requestBingPic="https://cn.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String bingPicResponse = response.body().string();
                String bingPic = Utility.handleBingPicResponse(bingPicResponse);  //解析返回的JSON数据
                //存入缓存器
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                //背景图片的显示
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }
    //将天气信息显示在界面上


    //将天气信息显示在界面上
    public void showWeatherInfo(Weather weather) {
        //从weather实例对象中获取相应的数据
        String cityNAme = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];   //只显示24小时具体时间
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        //将响应数据传递给响应控件并显示
        titleCity.setText(cityNAme);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        //天气列表
        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.data_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_info);
            TextView minText = (TextView) view.findViewById(R.id.min_info);
            //传递数据并显示
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            //添加每一天的天气预测
            forecastLayout.addView(view);
        }
        //空气质量
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        //生活建议
        String comfort = "舒适度:" + weather.suggestion.comfort.info;
        String carWash = "洗车指数:" + weather.suggestion.carWash.info;
        String sport = "运动建议:" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
        //激活定时更新服务
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }


    //从服务器申请天气信息
    public void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "f30591f8a43842c8a327f4f42382b3b1";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();//命令行打印异常信息
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气数据失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);      //关闭下拉刷新进度条
                    }
                });

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                //涉及布局，回到主线程
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            //将天气数据存入缓存器
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            mWeatherId=weather.basic.weatherId;   //更新当前天气ID

                            //显示天气数据
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气数据失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);      //关闭下拉刷新进度条
                    }
                });
            }
        });
    }
}