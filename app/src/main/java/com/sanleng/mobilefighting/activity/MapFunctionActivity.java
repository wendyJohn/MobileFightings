package com.sanleng.mobilefighting.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMap.OnMarkerClickListener;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.loopj.android.http.RequestParams;
import com.sanleng.mobilefighting.R;
import com.sanleng.mobilefighting.bean.FireAlarmBean;
import com.sanleng.mobilefighting.net.NetCallBack;
import com.sanleng.mobilefighting.net.RequestUtils;
import com.sanleng.mobilefighting.net.URLs;
import com.sanleng.mobilefighting.util.E_StationDialog;
import com.sanleng.mobilefighting.util.FireFormationDialog;
import com.sanleng.mobilefighting.util.PreferenceUtils;
import com.sanleng.mobilefighting.video.activity.MonitorVideoActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 地图开发
 *
 * @author qiaoshi
 */
public class MapFunctionActivity extends Activity implements OnClickListener {
    // 定位对象
    private LocationClient mLocationClient = null;
    // 定位监听
    private BDLocationListener myListener = new MyLocationListener();
    // 纬度
    private double mylatitude;
    // 经度
    private double mylongitude;
    private LocationManager locationManager;
    // 地图应用
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private LatLng latLng;
    private boolean isFirstLoc = true; // 是否首次定位
    // 初始化全局 bitmap 信息，不用时及时 recycle
    BitmapDescriptor bdA = BitmapDescriptorFactory.fromResource(R.drawable.ic_marks);
    private List<OverlayOptions> list;
    private FireFormationDialog fireFormationDialog;
    private String AlarmTime;
    private String AlarmEquipment;
    private String AlarmUnit;
    private String type;
    private int i = 0;// 次数
    private String str;
    private E_StationDialog e_stationDialog;
    private List<String> mylist = new ArrayList<>();
    private RelativeLayout myr_back;
    BitmapDescriptor bdAs = BitmapDescriptorFactory.fromResource(R.drawable.e_station);

    @Override
    protected void onCreate(Bundle arg0) {
        // TODO Auto-generated method stub
        super.onCreate(arg0);
        this.setContentView(R.layout.mapfunction_activity);
        initview();
        initMap();
    }

    private void initMap() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(MapFunctionActivity.this);
            dialog.setTitle("GPS未打开");
            dialog.setMessage("请打开GPS或WIFI，提高定位精度");
            dialog.setCancelable(false);
            dialog.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    Intent setting_Intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(setting_Intent, 0);
                }
            });
            dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                }
            });
            dialog.show();
        }
        //获取地图控件引用
        mBaiduMap = mMapView.getMap();
        // 普通地图
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        View child = mMapView.getChildAt(1);
        if (child != null && (child instanceof ImageView || child instanceof ZoomControls)) {
            child.setVisibility(View.INVISIBLE);
        }
        mBaiduMap.setMyLocationEnabled(true);
        // 开启交通图
        mBaiduMap.setTrafficEnabled(true);
        // 开启热力图
        mBaiduMap.setBaiduHeatMapEnabled(false);
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        mLocationClient = new LocationClient(getApplicationContext());
        // 声明LocationClient类 //配置定位SDK参数
        initLocation();
        mLocationClient.registerLocationListener(myListener);// 注册监听函数
        // 开启定位
        mLocationClient.start();
        // 图片点击事件，回到定位点
        mLocationClient.requestLocation();

        // 测试地图添加标识
        list = new ArrayList<OverlayOptions>();
        FireAlarmBean bean = new FireAlarmBean();
        double latitude = 31.87308;
        double longitude = 118.83488;
        bean.setAlarmTime("2018/11/30 00:00");
        bean.setAlarmEquipment("应急站");
        bean.setAlarmUnit("三棱科技");
        bean.setTaskId("123");
        bean.setType("应急站");

        bean.setLatitude(latitude);
        bean.setLongitude(longitude);
        // 构建MarkerOption，用于在地图上添加Marker
        LatLng llA = new LatLng(latitude, longitude);
        MarkerOptions option = new MarkerOptions().position(llA).icon(bdAs);
        Marker marker = (Marker) mBaiduMap.addOverlay(option);
        // 将信息保存
        Bundle bundle = new Bundle();
        bundle.putSerializable("marker", bean);
        marker.setExtraInfo(bundle);
        mBaiduMap.addOverlays(list);

        mBaiduMap.setOnMarkerClickListener(new OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker arg0) {
                // 获得marker中的数据
                FireAlarmBean bean = (FireAlarmBean) arg0.getExtraInfo().get("marker");
                AlarmTime = bean.getAlarmTime();
                AlarmEquipment = bean.getAlarmEquipment();
                AlarmUnit = bean.getAlarmUnit();
                type = bean.getType();
                // TODO Auto-generated method stub
                // 实例化一个地理编码查询对象
                GeoCoder geoCoder = GeoCoder.newInstance();
                // 设置反地理编码位置坐标
                ReverseGeoCodeOption op = new ReverseGeoCodeOption();
                op.location(arg0.getPosition());
                // 发起反地理编码请求(经纬度->地址信息)
                geoCoder.reverseGeoCode(op);
                geoCoder.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
                    @Override
                    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult arg0) {
                        // 获取点击的坐标地址
                        String address = arg0.getAddress();

                        if (type.equals("火警")) {
                            fireFormationDialog = new FireFormationDialog(MapFunctionActivity.this, clickListener,
                                    AlarmEquipment, AlarmUnit, address, AlarmTime);
                            fireFormationDialog.show();
                        } else {
                            e_stationDialog = new E_StationDialog(MapFunctionActivity.this, clickListener,
                                    AlarmEquipment, AlarmUnit, address, null);
                            e_stationDialog.show();
                        }
                    }

                    @Override
                    public void onGetGeoCodeResult(GeoCodeResult arg0) {
                    }
                });
                return true;

            }

        });
    }

    //配置定位SDK参数
    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");
        //可选，默认gcj02，设置返回的定位结果坐标系
        int span = 1000;
        option.setScanSpan(span);
        //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);
        //可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);
        //可选，默认false,设置是否使用gps
        option.setLocationNotify(true);
        //可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation
        // .getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);
        //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);
        option.setOpenGps(true);// 打开gps
        // 可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，
        // 设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);
        //可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤GPS仿真结果，默认需要
        mLocationClient.setLocOption(option);
    }

    //实现BDLocationListener接口,BDLocationListener为结果监听接口，异步获取定位结果
    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            latLng = new LatLng(location.getLatitude(), location.getLongitude());
            // 构造定位数据
            MyLocationData locData = new MyLocationData.Builder().accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(location.getLatitude()).longitude(location.getLongitude()).build();
            // 设置定位数据
            mBaiduMap.setMyLocationData(locData);
            // 当不需要定位图层时关闭定位图层
            mBaiduMap.setMyLocationEnabled(false);
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                if (location.getLocType() == BDLocation.TypeGpsLocation) {
                    // GPS定位结果
                    Toast.makeText(MapFunctionActivity.this, location.getAddrStr(), Toast.LENGTH_SHORT).show();
                } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {
                    // 网络定位结果
                    Toast.makeText(MapFunctionActivity.this, location.getAddrStr(), Toast.LENGTH_SHORT).show();
                } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {
                    // 离线定位结果
                    Toast.makeText(MapFunctionActivity.this, location.getAddrStr(), Toast.LENGTH_SHORT).show();
                } else if (location.getLocType() == BDLocation.TypeServerError) {
                    Toast.makeText(MapFunctionActivity.this, "服务器错误，请检查", Toast.LENGTH_SHORT).show();
                } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                    Toast.makeText(MapFunctionActivity.this, "网络错误，请检查", Toast.LENGTH_SHORT).show();
                } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                    Toast.makeText(MapFunctionActivity.this, "手机模式错误，请检查是否飞行", Toast.LENGTH_SHORT).show();
                }
            }
        }



    }

    // 初始化数据
    private void initview() {
        mMapView = (MapView) findViewById(R.id.bmapView);
        mylist.add("A");
        mylist.add("B");
        mylist.add("C");
        mylist.add("D");
        myr_back = (RelativeLayout) findViewById(R.id.r_back);
        myr_back.setOnClickListener(MapFunctionActivity.this);

    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        // 当不需要定位图层时关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mLocationClient.stop();
        // 在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        bdA.recycle();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData(1);
        // 在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    // 加载数据
    private void loadData(int page) {
        RequestParams params = new RequestParams();
        params.put("page", page + "");
        params.put("pageSize", "50");
        params.put("unitcode", PreferenceUtils.getString(MapFunctionActivity.this, "unitcode"));
        params.put("username", PreferenceUtils.getString(MapFunctionActivity.this, "MobileFig_username"));
        params.put("platformkey", "app_firecontrol_owner");
        params.put("status", "pending,processed");
        params.put("scope", "oneday");

        RequestUtils.ClientPost(URLs.FireAlarm_URL, params, new NetCallBack() {
            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onMySuccess(String result) {
                if (result == null || result.length() == 0) {
                    return;
                }
                System.out.println("数据请求成功" + result);
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    String msg = jsonObject.getString("msg");
                    if (msg.equals("获取成功")) {
                        String data = jsonObject.getString("data");
                        JSONObject objects = new JSONObject(data);
                        String mylist = objects.getString("list");
                        JSONArray array = new JSONArray(mylist);
                        JSONObject object;
                        for (int i = 0; i < array.length(); i++) {
                            FireAlarmBean bean = new FireAlarmBean();
                            object = (JSONObject) array.get(i);

                            String taskId = object.getString("ids");
                            String receive_time = object.getString("receive_time");
                            String device_name = object.getString("device_name");
                            String position = object.getString("position");
                            String unit_name = object.getString("unit_name");
                            String alarm_type = object.getString("alarm_type");

//							double latitude = jsonObject.getDouble("latitude");
//							double longitude = jsonObject.getDouble("longitude");
                            if (alarm_type.equals("1") || alarm_type.equals("101")) {
                                double latitude = 31.87408;
                                double longitude = 118.83588;

                                bean.setAlarmTime(receive_time);
                                bean.setAlarmEquipment(device_name);
                                bean.setAlarmPosition(position);
                                bean.setAlarmUnit(unit_name);
                                bean.setTaskId(taskId);
                                bean.setLatitude(latitude);
                                bean.setLongitude(longitude);
                                bean.setType("火警");
                                // 构建MarkerOption，用于在地图上添加Marker
                                LatLng llA = new LatLng(latitude, longitude);
                                MarkerOptions option = new MarkerOptions().position(llA).icon(bdA);
                                Marker marker = (Marker) mBaiduMap.addOverlay(option);
                                // 将信息保存
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("marker", bean);
                                marker.setExtraInfo(bundle);
                                mBaiduMap.addOverlays(list);
                            }
                        }

                    } else {
                    }

                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            @Override
            public void onMyFailure(Throwable arg0) {
            }
        });

    }

    private OnClickListener clickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            switch (v.getId()) {
                // 现场监控
                case R.id.monitor:
                    Intent intent_Inspection = new Intent(MapFunctionActivity.this, MonitorVideoActivity.class);
                    startActivity(intent_Inspection);
                    break;
                // 取消
                case R.id.canle:
                    fireFormationDialog.dismiss();
                    break;
                case R.id.dooropening:
                    i = 0;
                    handler.postDelayed(runnable, 2000);// 每两秒执行一次runnable.
                    break;

                case R.id.warehousings:
                    Intent intent_Warehousing = new Intent(MapFunctionActivity.this, MaterialManagementCapture.class);
                    intent_Warehousing.putExtra("mode", "Warehousing");
                    startActivity(intent_Warehousing);
                    break;

                case R.id.outofstock:
                    Intent intent_OutOfStock = new Intent(MapFunctionActivity.this, MaterialManagementCapture.class);
                    intent_OutOfStock.putExtra("mode", "OutOfStock");
                    startActivity(intent_OutOfStock);
                    break;

                case R.id.reportloss:
                    Intent intent_Reportloss = new Intent(MapFunctionActivity.this, MaterialManagementCapture.class);
                    intent_Reportloss.putExtra("mode", "Reportloss");
                    startActivity(intent_Reportloss);
                    break;
                // 取消
                case R.id.canles:
                    e_stationDialog.dismiss();
                    break;
                default:
                    break;
            }
        }
    };

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            if (i == 4) {
                handler.removeCallbacks(runnable);
            } else {
                str = mylist.get(i).toString();
                i++;
                Unlock(str, "54C9DFF77FAB");
                // 要做的事情
                handler.postDelayed(this, 2000);
            }

        }
    };

    // 开锁方式
    private void Unlock(String position, final String mac) {
        RequestUtils.ClientPost(URLs.ORDER_BASE_URL + "/" + position + "/" + mac, null, new NetCallBack() {
            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onMySuccess(String result) {
                if (result == null || result.length() == 0) {
                    return;
                }
                System.out.println("数据请求成功" + result);
            }

            @Override
            public void onMyFailure(Throwable arg0) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.r_back:
                finish();
                break;

            default:
                break;
        }
    }
}
