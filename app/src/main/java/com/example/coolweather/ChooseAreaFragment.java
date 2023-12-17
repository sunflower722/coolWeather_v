package com.example.coolweather;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telecom.Call;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;


import com.example.c1.R;
import com.example.coolweather.Util.HttpUtil;
import com.example.coolweather.Util.Utility;
import com.example.coolweather.db.City;
import com.example.coolweather.db.County;
import com.example.coolweather.db.Province;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=0;
    public static final int LEVEL_COUNTY=0;
    private int currentLevel;//当前访问的状态，对应于LEVEL_PROVINCE，LEVEL_CITY，LEVEL_COUNTY
    private TextView titleText;
    private Button back_btn;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> datalist = new ArrayList<>();
    //当前的省
    private Province currentProvince;
    //当前的市
    private City currentCity;
    //所有的省
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    private List<City> cities;
    private List<County> counties;
    private ProgressDialog progressDialog;
    @Nullable
    @Override

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText=(TextView) view.findViewById(R.id.title_text);
        back_btn=(Button) view.findViewById(R.id.back_btn);
        listView=(ListView) view.findViewById(R.id.list_view);
        adapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,datalist);
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        requireActivity().getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                if (event.getTargetState() == Lifecycle.State.CREATED) {
                    queryProvince();
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
                            //如果列表显示的是省级数据，则点击某市显示对应的县级数据
                            if (currentLevel == LEVEL_PROVINCE) {
                                currentProvince = provinceList.get(i);
                                queryCity();
                            }
                            //如果列表显示的是市级数据，则点击某市显示对应的县级数据
                            else if (currentLevel == LEVEL_CITY) {
                                currentCity = cityList.get(i);
                                queryCounty();
                            }
                            //如果列表显示的是县级数据，则点击某县显示对应的天气数据
                            else if (currentLevel == LEVEL_COUNTY) {
                                //判断当前状态
                                String weatherId = countyList.get(i).getWeather_id();
                                //若当前在MainActivity中，则点击县级数据时，则切换到WeatherActivity
                                if(getActivity() instanceof MainActivity){

                                    //通过Intent向天气界面传递数据
                                    Intent intent = new Intent(getActivity(), WeatherActivity.class);
                                    intent.putExtra("weather_id", weatherId);
                                    //启动WeatherActivity
                                    startActivity(intent);
                                    //关闭
                                    getActivity().finish();
                                }
                                else if (getActivity() instanceof WeatherActivity){
                                    WeatherActivity weatherActivity = (WeatherActivity) getActivity();
                                    weatherActivity.drawerLayout.closeDrawers();    //关闭滑窗
                                    weatherActivity.swipeRefresh.setRefreshing(true);   //显示刷新进度条
                                    weatherActivity.requestWeather(weatherId);
                                }
                            }

                        }
                    });
                    back_btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //如果当前显示市级数据，则点击返回键返回显示省级数据
                            if (currentLevel == LEVEL_CITY) {
                                queryProvince();
                            } else if (currentLevel == LEVEL_COUNTY) {
                                queryCity();
                            }
                        }
                    });
                    requireActivity().getLifecycle().removeObserver(this);

                }

            }
        });
    }

    public void queryFromService(String address, final String type){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
                    @Override
                    public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //加载失败，关闭加载进度对话框，显示“加载失败”提示信息
                                closeProgressDialog();
                                Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) throws IOException {
                        String responseText=response.body().string();
                        boolean result = false;
                        if("province".equals(type)){
                            result = Utility.handleProvinceResponse(responseText);
                        }else if ("city".equals(type)){
                            result = Utility.handleCityResponse(responseText, currentProvince.getProvinceCode());
                        }else if("country".equals(type)){
                            result = Utility.handleCountyResponse(responseText, currentCity.getCityCode());
                        }
                        if(result){
                            //读取数据库，并写在页面上
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //关闭进度对话框
                                    closeProgressDialog();

                                    //数据库存储成功后，进行页面的展示
                                    if("province".equals(type)){
                                        queryProvince();
                                    }else if ("city".equals(type)){
                                        queryCity();
                                    }else if("country".equals(type)){
                                        queryCounty();
                                    }
                                }
                            });
                        }

                    }
        });
    }

    /*
     * 1.获取所有省信息，并显示在UI上
     * 2.如果数据库有信息，则读取数据库
     * 3.如果数据库没有信息，则先链接服务器读取数据并存储，再读取数据库
     * */
    public void queryProvince(){
        titleText.setText("中国");
        back_btn.setVisibility(View.GONE);
        //读取数据库，判断是否为空
        provinceList = LitePal.findAll(Province.class);
        //访问数据库有数据，直接读取并显示
        if (provinceList.size()>0){
            datalist.clear();
            for (Province province:provinceList){
                datalist.add(province.getProvinceName());
            }
            //页面显示数据
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }
        else{
            String address = "http://quolin.tech/api/china";
            queryFromService(address,"province");

        }
    }
    //获取市级数据
    public void queryCity(){
        titleText.setText(currentProvince.getProvinceName());
        back_btn.setVisibility(View.VISIBLE);//显示市级数据时，显示返回键
        //优先从数据库
        //查找当前选中的省下面的所有市
        //SQL:select * from City Where provinceCode = currentProvince.provinceCode
        cityList=LitePal.where("provinceCode = ?",String.valueOf(currentProvince.getProvinceCode())).find(City.class);
        //判断数据库是否为空
        if(cityList.size()>0){
            datalist.clear();
            for (City city: cityList){
                datalist.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else{
            String address = "http://guolin.tech/api/china"+ currentProvince.getProvinceCode();
            queryFromService(address,"city");

        }

    }
    public void queryCounty(){
        titleText.setText(currentCity.getCityName());
        back_btn.setVisibility(View.VISIBLE);//显示县级数据时，显示返回键
        //优先从数据库中获取
        countyList=LitePal.where("cityCode = ?",String.valueOf(currentCity.getCityCode())).find(County.class);
        if(countyList.size()>0){
            datalist.clear();
            for(County county : countyList){
                datalist.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }else{
            //数据库为空
            String address ="http://guolin.tech/api/china/"+ currentProvince.getProvinceCode()+"/"+currentCity.getCityCode();
            queryFromService(address,"county");
        }
    }
    private void showProgressDialog(){
        if(progressDialog==null){
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载中...");
            progressDialog.setCanceledOnTouchOutside(false);//用户点击对话框外部，对话框无法取消
        }
        progressDialog.show();

    }
    //关闭进度对话框
    private  void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
        }