package com.lcb.one.map;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.lcb.one.R;
import com.lcb.one.base.BaseActivity;
import com.lcb.one.listener.GeoListener;
import com.lcb.one.util.Logs;
import com.lcb.one.util.TimeUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class MarkerActivity extends BaseActivity implements View.OnClickListener {
    private MapView mapwiew;
    private BaiduMap mMap;
    private LocationClient mLocationClient;
    String tag = "MarkerActivity";
    public static final float MAP_ZOOM_DEFAULT = 20;//设置默认的缩放级别
    double longitude;//当前定位的精度
    double latitude;//当前定位的纬度
    boolean isFirstLocate;
    private Marker marker;
    String lon = "longitude";
    String lat = "latitude";
    String time = "time";
    BitmapDescriptor bitmapDescriptor;
    GeoCoder mSearch = null; // 搜索模块，也可去掉地图模块独立使用
    GeoListener mGeoListener;
    private TextView infoTv;
    private String InfoStr = "";


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            infoTv.setText(InfoStr);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker);
        initView();
        initmapView();
        startLocating();
        /*初始化搜索*/
        // 初始化搜索模块，注册事件监听
        mSearch = GeoCoder.newInstance();
        mSearch.setOnGetGeoCodeResultListener(geoCoderResultListener);
    }

    /**
     * 初始化定位
     */
    private void startLocating() {
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(locationlistener);
        LocationClientOption option = new LocationClientOption();
        option.setIsNeedAddress(true);// 设置是否需要地址信息
        option.setOpenGps(true);//设置是否打开gps进行定位
        /*设置坐标类型 返回国测局经纬度坐标系:gcj02 返回百度墨卡托坐标系:bd09 返回百度经纬度坐标系:bd09ll*/
        option.setCoorType("bd09ll");
        option.setScanSpan(9000);//设置扫描间隔，单位是毫秒 当<1000(1s)时，定时定位无效
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setIgnoreKillProcess(false);//设置是否退出定位进程 true:不退出进程； false:退出进程，默认为true
        mLocationClient.setLocOption(option);
        mLocationClient.start();
        mLocationClient.requestLocation();
    }

    private void initmapView() {
        mMap = mapwiew.getMap();
        /*设置是否允许定位图层*/
        mMap.setMyLocationEnabled(true);
        mMap.setOnMapClickListener(clicklistener);
        mMap.setOnMarkerClickListener(markerClickListener);
    }

    private void initView() {
        findViewById(R.id.back_marker).setOnClickListener(this);
        findViewById(R.id.marker_locatemap).setOnClickListener(this);
        findViewById(R.id.marker_clean).setOnClickListener(this);
        mapwiew = (MapView) findViewById(R.id.marker_map);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_marker:
                finish();
                break;
            case R.id.marker_locatemap:
                setLocation();
                break;
            case R.id.marker_clean:
                mMap.clear();
                break;
        }
    }


    /**
     * 设置地图单击事件监听者
     */
    BaiduMap.OnMapClickListener clicklistener = new BaiduMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {
            addMarker(latLng.latitude, latLng.longitude);
        }

        @Override
        public boolean onMapPoiClick(MapPoi mapPoi) {
            return false;
        }
    };

    /**
     * 向地图上添加小图标
     */
    private void addMarker(double latitude, double longitude) {
        LatLng latLng = new LatLng(latitude, longitude);
        Bundle bundle = new Bundle();
        String timeStr = TimeUtil.getSystem();
        bundle.putString(time, timeStr);
        bundle.putString(lat, latitude + "");
        bundle.putString(lon, longitude + "");

        MarkerOptions markerOptions = new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_marker_poi))
                .extraInfo(bundle)
                .position(latLng)
                //设置 marker 覆盖物的 zIndex
                .zIndex(Integer.MAX_VALUE)
                //设置 marker 覆盖物的可见性
                .draggable(true)// 设置手势拖拽
                .visible(true);
        marker = (Marker) mMap.addOverlay(markerOptions);
        //设置当前marker在最上面
        marker.setToTop();
    }


    /**
     * 地图 Marker 覆盖物点击事件监听接口
     */
    BaiduMap.OnMarkerClickListener markerClickListener = new BaiduMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            final Bundle info = marker.getExtraInfo();
            Logs.e(tag + " 170  " + info.getString(time) + "   " + info.getString(lon) + "  " + info.getString(lat));

            /*生成一个TextView用户在地图中显示InfoWindow*/
//            TextView location = new TextView(getApplicationContext());
//            location.setTextColor(ContextCompat.getColor(MarkerActivity.this, R.color.blueSky));
//            location.setText(info.getString(time) + "\n" + info.getString(lon) + "\n" + info.getString(lat));
//            bitmapDescriptor = BitmapDescriptorFactory.fromView(location);


            /*将marker所在的经纬度的信息转化成屏幕上的坐标*/
//            Point p = mMap.getProjection().toScreenLocation(ll);
//            Logs.e("坐标" + p.x + " , " + p.y);
//            p.y -=47;
//            LatLng llInfo = mMap.getProjection().fromScreenLocation(p);

            final LatLng ll = marker.getPosition();


            getdata(ll, new GeoListener() {
                @Override
                public void noting() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            InfoStr = "抱歉，未能找到结果";
//                            mHandler.obtainMessage().sendToTarget();
                            showWindow(info, ll, InfoStr);
                        }
                    });

                }

                @Override
                public void data(JSONObject obj) {
                    InfoStr = "解析错误";
                    try {
                        InfoStr = obj.getString(GeoListener.address);
                        showWindow(info, ll, InfoStr);
//                        mHandler.obtainMessage().sendToTarget();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Logs.d(tag + " 205 " + e);
                    }
                }
            });


            //设置详细信息布局为可见
//            mMarkerInfoLy.setVisibility(View.VISIBLE);
            //根据商家信息为详细信息布局设置信息
//            popupInfo(mMarkerInfoLy, info);
            return true;
        }
    };


            /*自定义layout在地图中显示InfoWindow*/
    public void showWindow(Bundle info, LatLng ll, String infoStr) {
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        View view = inflater.inflate(R.layout.window_locate, null);
        TextView timeStr = (TextView) view.findViewById(R.id.window_time);
        infoTv = (TextView) view.findViewById(R.id.window_info);
        infoTv.setText(infoStr);
        timeStr.setText(info.getString(time));
        bitmapDescriptor = BitmapDescriptorFactory.fromView(view);


        //为弹出的InfoWindow添加点击事件
        InfoWindow mInfoWindow = new InfoWindow(bitmapDescriptor, ll, -127, new InfoWindow.OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick() {
                //隐藏InfoWindow
                mMap.hideInfoWindow();
            }
        });
//         new InfoWindow(location, llInfo, 0);
        //显示InfoWindow
        mMap.showInfoWindow(mInfoWindow);

    }


    /**
     * 定位监听者
     */
    BDLocationListener locationlistener = new BDLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            longitude = bdLocation.getLongitude();
            latitude = bdLocation.getLatitude();
            if (!isFirstLocate) {
                setLocation();
                isFirstLocate = true;
                /*能让默认的定位图标有方向*/
                MyLocationConfiguration myLocationConfiguration = new MyLocationConfiguration(
                        MyLocationConfiguration.LocationMode.NORMAL, true, null);
                mMap.setMyLocationConfiguration(myLocationConfiguration);
            }

          /*只有设置了setMyLocationData数据setMyLocationConfiguration定位的图标才会显示出来*/

            // map view 销毁后不在处理新接收的位置
            if (bdLocation == null || mMap == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(bdLocation.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(bdLocation.getLatitude())
                    .longitude(bdLocation.getLongitude()).build();
            mMap.setMyLocationData(locData);

        }
    };


    /**
     * 设置当前位置哦
     */
    private void setLocation() {
        LatLng latLng = new LatLng(latitude, longitude);
            /*将视图中心移动到定位点*/
//        MapStatus mapStatus = new MapStatus.Builder().target(latLng)
//                .zoom(18)
//                .build();
//        MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mapStatus);
        //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
        MapStatusUpdate mMapStatusUpdate =
                MapStatusUpdateFactory.newLatLngZoom(latLng, MAP_ZOOM_DEFAULT);
        //以动画方式更新地图状态，动画耗时 300 ms
        mMap.animateMapStatus(mMapStatusUpdate);
    }


    /**
     * 得到数据
     */
    public void getdata(LatLng latLng, GeoListener listener) {
        mGeoListener = listener;
        mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(latLng));
    }


    /**
     * 设置编码搜索监听
     */
    OnGetGeoCoderResultListener geoCoderResultListener = new OnGetGeoCoderResultListener() {
        @Override
        public void onGetGeoCodeResult(GeoCodeResult result) {
            Logs.d(" 297 " + result.getAddress() + "   " + result.getLocation().longitude
                    + "   " + result.getLocation().latitude + "  " + result.error);
        }

        @Override
        public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
            Logs.d(" 312 " + result.getAddress() + "   " + result.getLocation().longitude
                    + "   " + result.getLocation().latitude + "  " + result.error);
            if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                mGeoListener.noting();
                return;
            }
            JSONObject jb = null;
            try {
                jb = new JSONObject().put(GeoListener.address, result.getAddress());
            } catch (JSONException e) {
                e.printStackTrace();
                Logs.d(tag + " 324 " + e);
            }
            mGeoListener.data(jb);

        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocationClient != null) {
            mLocationClient.stop();
            mLocationClient.unRegisterLocationListener(locationlistener);
        }

        mapwiew.onDestroy();
        mapwiew = null;
        mMap.setMyLocationEnabled(false);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mapwiew.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        mapwiew.onPause();
    }


}
