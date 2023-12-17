package com.example.coolweather.Util;

import android.text.TextUtils;

import com.example.coolweather.db.City;
import com.example.coolweather.db.County;
import com.example.coolweather.db.Province;
import com.example.coolweather.json.Weather;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utility {
    //处理省级数据
    public static boolean handleProvinceResponse(String response){
        if(!TextUtils.isEmpty(response)){
            try{
                JSONArray allProvinces=new JSONArray(response);
                //遍历所有省
                for(int i=0;i<allProvinces.length();i++){
                    JSONObject provinceObject=allProvinces.getJSONObject(i);
                    Province province=new Province();
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.setProvinceName(provinceObject.getString("name"));
                    province.save();
                }

            }catch (JSONException e){
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
    //处理市级数据
    public static boolean handleCityResponse(String response,int provinceCode){
        if(!TextUtils.isEmpty(response)){
            try{
                JSONArray allCities=new JSONArray(response);
                //遍历所有省
                for(int i=0;i<allCities.length();i++){
                    JSONObject cityObject=allCities.getJSONObject(i);
                    City city =new City();
                    city.setCityCode(cityObject.getInt("id"));
                    city.setCityName(cityObject.getString("name"));
                    city.setProvinceCode(provinceCode);
                    city.save();
                }

            }catch (JSONException e){
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
    public static boolean handleCountyResponse(String response,int CityCode){
        if(!TextUtils.isEmpty(response)){
            try{
                JSONArray allCounties=new JSONArray(response);
                //遍历所有省
                for(int i=0;i<allCounties.length();i++){
                    JSONObject countiesObject=allCounties.getJSONObject(i);
                    County county =new County();
                    county.setWeather_id(countiesObject.getString("weather_id"));
                    county.setCountyName(countiesObject.getString("name"));
                    county.setCityCode(CityCode);
                    county.save();
                }

            }catch (JSONException e){
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    //处理某县某日天气信息
    public static Weather handleWeatherResponse(String response){
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather");
            String weatherContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherContent,Weather.class);

        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //处理返回的JSON数据，获取背景图片
    public static String handleBingPicResponse(String response){
        //try-catch用于捕捉异常
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("images");
            JSONObject jsonObject1 = jsonArray.getJSONObject(0);
            String url = jsonObject1.getString("url");
            String bingPic = "http://cn.bing.com"+url;
            return bingPic;
        }catch (JSONException e){
            throw new RuntimeException(e);

        }
    }
}
