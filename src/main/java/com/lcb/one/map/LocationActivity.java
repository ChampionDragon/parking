package com.lcb.one.map;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.lcb.one.R;
import com.lcb.one.base.BaseActivity;
import com.lcb.one.listener.OrientationListener;
import com.lcb.one.util.Logs;


public class LocationActivity extends BaseActivity implements View.OnClickListener {
    private MapView mapwiew;
    private BaiduMap mMap;
    private LocationClient mLocationClient;
    String tag = "LocationActivity";
    public static final float MAP_ZOOM_DEFAULT = 19;//设置默认的缩放级别
    double longitude;//当前定位的精度
    double latitude;//当前定位的纬度
    boolean isFirstLocate;
    private Button model, marker, state;
    private MyLocationConfiguration.LocationMode mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;//当前图标类型
    BitmapDescriptor mCurrentMarker;//当前图标UI
    boolean ismarker;
    String stateStr="satellite";
    /*方向传感器的监听器*/
    private OrientationListener orientationListener;
    /*方向传感器X方向的值*/
    private int mXDirection;
    ImageButton condition, hot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        initView();
        initmapView();
        startLocating();
        initOritationListener();
    }

    /*初始化传感器*/
    private void initOritationListener() {
        orientationListener = new OrientationListener(this);
        orientationListener.setOnOrientationListener(new OrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                /*让它静态是指向正北*/
                mXDirection = (int) x;
            }
        });
        // 开启方向传感器
        orientationListener.start();

    }

    /**
     * 开启定位服务
     */
    private void startLocating() {
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(locationlistener);
        LocationClientOption option = new LocationClientOption();
        option.setIsNeedAddress(true);// 设置是否需要地址信息
        option.setOpenGps(true);//设置是否打开gps进行定位
        /*设置坐标类型 返回国测局经纬度坐标系:gcj02 返回百度墨卡托坐标系:bd09 返回百度经纬度坐标系:bd09ll*/
        option.setCoorType("bd09ll");
        option.setScanSpan(2000);//设置扫描间隔，单位是毫秒 当<1000(1s)时，定时定位无效
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setIgnoreKillProcess(false);//设置是否退出定位进程 true:不退出进程； false:退出进程，默认为true
        mLocationClient.setLocOption(option);
        mLocationClient.start();
        mLocationClient.requestLocation();

    }


    /**
     * 定位监听者
     */
    BDLocationListener locationlistener = new BDLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            longitude = bdLocation.getLongitude();
            latitude = bdLocation.getLatitude();
            Logs.w("精度:" + longitude + "纬度:" + latitude + "\n" + "国家:" + bdLocation.getCountry()
                    + "省份:" + bdLocation.getProvince() + "城市:" + bdLocation.getCity() + "\n"
                    + "区:" + bdLocation.getDistrict() + "街道:" + bdLocation.getStreet() + "\n"
                    + "详细信息" + bdLocation.getAddrStr());
            if (!isFirstLocate) {
                setLocation();
                isFirstLocate = true;
                MyLocationConfiguration myLocationConfiguration = new MyLocationConfiguration(mCurrentMode,
                        true, null);
                mMap.setMyLocationConfiguration(myLocationConfiguration);
            }

          /*只有设置了setMyLocationData数据setMyLocationConfiguration定位的图标才会显示出来*/


            // map view 销毁后不在处理新接收的位置
            if (bdLocation == null || mMap == null) {
                return;
            }


            Logs.d("11111111111 :" + mXDirection + "   " + bdLocation.getRadius());

            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(44)
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mXDirection).latitude(bdLocation.getLatitude())
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
        MapStatus mapStatus = new MapStatus.Builder().target(latLng)
                .zoom(18)
                .build();
        //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
        MapStatusUpdate mMapStatusUpdate =
                MapStatusUpdateFactory.newLatLngZoom(latLng, MAP_ZOOM_DEFAULT);
//                MapStatusUpdateFactory.newMapStatus(mapStatus);
        //改变地图状态
        mMap.animateMapStatus(mMapStatusUpdate);
    }


    /**
     * 初始化地图
     */
    private void initmapView() {
        /*设置是否显示缩放控件*/
        mapwiew.showZoomControls(true);
        /*设置是否显示比例尺控件*/
        mapwiew.showScaleControl(true);
        mMap = mapwiew.getMap();
        /*设置是否允许定位图层*/
        mMap.setMyLocationEnabled(true);
        /*设置是否打开交通图层*/
//        mMap.setTrafficEnabled(true);
        /*是否允许楼块效果*/
//        mMap.setBuildingsEnabled(true);
        /*设置地图最大以及最小缩放级别，地图支持的最大最小级别分别为[3-21]*/
//        mMap.setMaxAndMinZoomLevel(19.0f, 9.0f);
//        mMarkerManager = MarkerManager.getInstance(mMap, this); mMarkerManager
        mMap.setOnMarkerClickListener(markerclickListener);
        mMap.setOnMapLoadedCallback(loadedCallback);
        mMap.setOnMyLocationClickListener(locationClicklistener);
        mMap.setOnMapClickListener(clicklistener);
        mMap.setOnMapTouchListener(touchlistener);
        mMap.setOnMapStatusChangeListener(stateChangeListener);


    }

    /**
     * 设置显示图标的类型
     */
    private void setMarker() {
                /*配置定位图层显示方式*/
//        mode - 定位图层显示方式, 默认为 LocationMode.NORMAL 普通态
//        enableDirection - 是否允许显示方向信息
//        customMarker - 设置用户自定义定位图标，可以为 null
//        accuracyCircleFillColor - 设置精度圈填充颜色
//        accuracyCircleStrokeColor - 设置精度圈边界颜色
//        COMPASS 罗盘态，显示定位方向圈，保持定位图标在地图中心
//        FOLLOWING  跟随态，保持定位图标在地图中心
//        NORMAL   普通态： 更新定位数据时不对地图做任何操作
        /*设置定位图层配置信息，只有先允许定位图层后设置定位图层配置信息才会生效*/

        if (ismarker) {
            ismarker = false;
            marker.setText("自定义图标");
            mCurrentMarker = null;
            MyLocationConfiguration myLocationConfiguration = new MyLocationConfiguration(mCurrentMode,
                    true, mCurrentMarker);
            mMap.setMyLocationConfiguration(myLocationConfiguration);
        } else {
            ismarker = true;
            marker.setText("默认图标");
            // 设置“我的位置”自定义图标
            mCurrentMarker = BitmapDescriptorFactory
                    .fromResource(R.mipmap.ic_map_nearby);
            MyLocationConfiguration myLocationConfiguration = new MyLocationConfiguration(mCurrentMode,
                    true, mCurrentMarker);//, R.color.white, R.color.gold
            mMap.setMyLocationConfiguration(myLocationConfiguration);
        }


    }

    /**
     * 设置地位图层的显示方式
     */
    private void setModel() {
        switch (mCurrentMode) {
            case NORMAL:
                model.setText("罗盘");
                mCurrentMode = MyLocationConfiguration.LocationMode.FOLLOWING;
                mMap.setMyLocationConfiguration(new MyLocationConfiguration(
                        mCurrentMode, true, mCurrentMarker));
                break;
            case COMPASS:
                model.setText("跟随");
                mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;
                mMap.setMyLocationConfiguration(new MyLocationConfiguration(
                        mCurrentMode, true, mCurrentMarker));
                break;
            case FOLLOWING:
                model.setText("普通");
                mCurrentMode = MyLocationConfiguration.LocationMode.COMPASS;
                mMap.setMyLocationConfiguration(new MyLocationConfiguration(
                        mCurrentMode, true, mCurrentMarker));
                break;
            default:
                break;
        }
    }


    /*设置地图类型*/
    private void setState() {
        switch (stateStr){
            case "satellite":
                state.setText("普通地图");
                stateStr="normal";
                mMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                break;
            case "normal":
                state.setText("3D地图");
                stateStr="3D";
                mMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                mMap.setBuildingsEnabled(false);
                break;
            case "3D":
                state.setText("卫星地图");
                stateStr="satellite";
                mMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                mMap.setBuildingsEnabled(true);
                break;
        }

    }


    /**
     * 设置地图 Marker 覆盖物点击事件监听者
     */
    BaiduMap.OnMarkerClickListener markerclickListener = new BaiduMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            Logs.v(tag + " 122 覆盖物点击事件监听者");
            return false;
        }
    };


    /**
     * 设置地图加载完成回调
     */
    BaiduMap.OnMapLoadedCallback loadedCallback = new BaiduMap.OnMapLoadedCallback() {
        @Override
        public void onMapLoaded() {
            Logs.v(tag + " 134 哈哈，地图加载完了");
        }
    };


    /**
     * 设置定位图标点击事件监听者
     */
    BaiduMap.OnMyLocationClickListener locationClicklistener = new BaiduMap.OnMyLocationClickListener() {
        @Override
        public boolean onMyLocationClick() {
            Logs.v(tag + " 145 定位图标点击事件监听者");
            return false;
        }
    };


    /**
     * 设置地图单击事件监听者
     */
    BaiduMap.OnMapClickListener clicklistener = new BaiduMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {
            Logs.i(tag + " 156  " + latLng.latitude + "   " + latLng.longitude);
        }

        @Override
        public boolean onMapPoiClick(MapPoi mapPoi) {

//            Logs.e(tag + " 162  " + mapPoi.getUid() + "   " + mapPoi.getName());

            return false;
        }
    };


    /**
     * 地图触摸事件监听者
     */
    BaiduMap.OnMapTouchListener touchlistener = new BaiduMap.OnMapTouchListener() {
        @Override
        public void onTouch(MotionEvent motionEvent) {
//            Logs.v(tag + " 172 定位图标点击事件监听者");
        }
    };


    /**
     * 设置地图状态监听者
     */
    BaiduMap.OnMapStatusChangeListener stateChangeListener = new BaiduMap.OnMapStatusChangeListener() {
        //手势操作地图，设置地图状态等操作导致地图状态开始改变。
        @Override
        public void onMapStatusChangeStart(MapStatus mapStatus) {

        }
        //地图状态变化中

        @Override
        public void onMapStatusChange(MapStatus mapStatus) {

        }

        //地图状态改变结束
        @Override
        public void onMapStatusChangeFinish(MapStatus mapStatus) {

        }
    };


    private void initView() {
        state = (Button) findViewById(R.id.location_state);
        state.setOnClickListener(this);
        hot = (ImageButton) findViewById(R.id.location_hot);
        condition = (ImageButton) findViewById(R.id.location_condition);
        findViewById(R.id.back_location).setOnClickListener(this);
        findViewById(R.id.location_locatemap).setOnClickListener(this);
        hot.setOnClickListener(this);
        condition.setOnClickListener(this);
        mapwiew = (MapView) findViewById(R.id.location_map);
        model = (Button) findViewById(R.id.location_model);
        marker = (Button) findViewById(R.id.location_marker);
        model.setOnClickListener(this);
        marker.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_location:
                finish();
                break;
            case R.id.location_state:
                setState();
                break;
            case R.id.location_locatemap:
                setLocation();
                break;
            case R.id.location_marker:
                setMarker();
                break;
            case R.id.location_model:
                setModel();
                break;
            case R.id.location_hot:
                if (mMap.isBaiduHeatMapEnabled()) {
                    mMap.setBaiduHeatMapEnabled(false);
                    hot.setImageResource(R.mipmap.hot);
                } else {
                    hot.setImageResource(R.mipmap.hot_true);
                    mMap.setBaiduHeatMapEnabled(true);
                }

                break;
            case R.id.location_condition:
                if (mMap.isTrafficEnabled()) {
                    mMap.setTrafficEnabled(false);
                    condition.setImageResource(R.mipmap.main_icon_roadcondition_off);
                } else {
                    mMap.setTrafficEnabled(true);
                    condition.setImageResource(R.mipmap.main_icon_roadcondition_on);
                }
                break;
        }
    }


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
        // 关闭方向传感器
        orientationListener.stop();
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
