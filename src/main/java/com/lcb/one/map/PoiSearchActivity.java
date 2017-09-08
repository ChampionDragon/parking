package com.lcb.one.map;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.GroundOverlayOptions;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.CityInfo;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiBoundSearchOption;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.poi.PoiSortType;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.lcb.one.R;
import com.lcb.one.base.BaseActivity;
import com.lcb.one.listener.OrientationListener;

import java.util.ArrayList;
import java.util.List;


public class PoiSearchActivity extends BaseActivity implements View.OnClickListener {
    AutoCompleteTextView tv;
    private MapView mapwiew;
    private BaiduMap mMap;
    private LocationClient mLocationClient;
    String tag = "PoiSearchActivity";
    public static final float MAP_ZOOM_DEFAULT = 19;//设置默认的缩放级别
    double longitude;//当前定位的精度
    double latitude;//当前定位的纬度
    boolean isFirstLocate;
    /*方向传感器的监听器*/
    private OrientationListener orientationListener;
    /*方向传感器X方向的值*/
    private int mXDirection;
    private String city;
    private ArrayAdapter<String> sugAdapter;//提示列表的适配器
    private PoiSearch mPoiSearch;//搜索模块
    private SuggestionSearch mSuggestionSearch;//建议搜索模块
    private List<String> suggest;//返回的数据结果清单
    int searchType;  // 搜索的类型，在显示时区分
    LatLngBounds searchbound;//设置中心区域,地理范围数据结构
    int radius = 999;//设置搜素区域圆的半径
    LatLng center;//设置中心点
    private LinearLayout mLinearLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poi_search);
        initView();
        initAutoCompleteTextView();
        initmapView();
        startLocating();
        initOritationListener();
        // 初始化搜索模块，注册搜索事件监听
        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(poiListener);
        // 初始化建议搜索模块，注册建议搜索事件监听
        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(suggestionListener);
    }

    /*建议查询结果监听器*/
    OnGetSuggestionResultListener suggestionListener = new OnGetSuggestionResultListener() {
        @Override
        public void onGetSuggestionResult(SuggestionResult res) {
            if (res == null || res.getAllSuggestions() == null) {
                return;
            }
            suggest = new ArrayList<>();
            for (SuggestionResult.SuggestionInfo info : res.getAllSuggestions()) {
                if (info.key != null) {
                    suggest.add(info.key);
                }
            }
            sugAdapter = new ArrayAdapter<>(PoiSearchActivity.this, android.R.layout.simple_list_item_1, suggest);
            tv.setAdapter(sugAdapter);
            sugAdapter.notifyDataSetChanged();
        }
    };


    private class MyPoiOverlay extends PoiOverlay {
        public MyPoiOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public boolean onPoiClick(int index) {
            super.onPoiClick(index);
            /*这里才是点击单个poi出来详细信息，实际上他又单独查询了你点的poi*/
            PoiInfo poi = getPoiResult().getAllPoi().get(index);
            mPoiSearch.searchPoiDetail((new PoiDetailSearchOption()).poiUid(poi.uid));
            return true;
        }

    }

    /*poi检索结果回调*/

    OnGetPoiSearchResultListener poiListener = new OnGetPoiSearchResultListener() {
        /*获取POI搜索结果，包括searchInCity，searchNearby，searchInBound返回的搜索结果*/
        @Override
        public void onGetPoiResult(PoiResult result) {
            if (result == null || result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                Toast.makeText(PoiSearchActivity.this, "未找到结果", Toast.LENGTH_LONG)
                        .show();
                return;
            }
            if (result.error == SearchResult.ERRORNO.NO_ERROR) {
                mMap.clear();
                PoiOverlay overlay = new MyPoiOverlay(mMap);
                mMap.setOnMarkerClickListener(overlay);
                overlay.setData(result);
                overlay.addToMap();
                overlay.zoomToSpan();

                switch (searchType) {
                    case 2:
                        showNearbyArea(center, radius);
                        break;
                    case 3:
                        showBound(searchbound);
                        break;
                    default:
                        break;
                }

                return;
            }
            if (result.error == SearchResult.ERRORNO.AMBIGUOUS_KEYWORD) {

                // 当输入关键字在本市没有找到，但在其他城市找到时，返回包含该关键字信息的城市列表
                String strInfo = "在";
                for (CityInfo cityInfo : result.getSuggestCityList()) {
                    strInfo += cityInfo.city;
                    strInfo += ",";
                }
                strInfo += "找到结果";
                Toast.makeText(PoiSearchActivity.this, strInfo, Toast.LENGTH_LONG)
                        .show();
            }
        }

        /* 获取POI详情搜索结果，得到searchPoiDetail返回的搜索结果*/
        @Override
        public void onGetPoiDetailResult(PoiDetailResult result) {
            if (result.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(PoiSearchActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
                        .show();
            } else {
                showWindow(result.getName(), result.getLocation(), result.getAddress());
            }
        }

        /*poi 室内检索结果回调*/
        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {
        }
    };


    /*自定义layout在地图中显示InfoWindow*/
    public void showWindow(String name, LatLng ll, String address) {
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        View view = inflater.inflate(R.layout.window_locate, null);
        TextView timeStr = (TextView) view.findViewById(R.id.window_time);
        TextView infoTv = (TextView) view.findViewById(R.id.window_info);
        infoTv.setText(address);
        timeStr.setText(name);
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromView(view);
        //为弹出的InfoWindow添加点击事件
        InfoWindow mInfoWindow = new InfoWindow(bitmapDescriptor, ll, -107, new InfoWindow.OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick() {
                //隐藏InfoWindow
                mMap.hideInfoWindow();
            }
        });
        //显示InfoWindow
        mMap.showInfoWindow(mInfoWindow);
    }


    /**
     * 初始化自动提示
     */
    private void initAutoCompleteTextView() {
//        sugAdapter = new ArrayAdapter<>(this,
//                android.R.layout.simple_list_item_1);
//        tv.setAdapter(sugAdapter);
        tv.setThreshold(1);//设置几个字开始有提示
        /*当输入关键字变化时，动态更新建议列表*/
        tv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                SuggestionSearchOption option = new SuggestionSearchOption();
                option = option.city(city).keyword(s.toString());
                mSuggestionSearch.requestSuggestion(option);
                if (s.length() == 0) {
                    mLinearLayout.setVisibility(View.GONE);
                } else {
                    mLinearLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }


    /**
     * 定位监听者
     */
    BDLocationListener locationlistener = new BDLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            longitude = bdLocation.getLongitude();
            latitude = bdLocation.getLatitude();
            city = bdLocation.getCity();//获取定位点所在的城市
            center = new LatLng(latitude, longitude);
            if (!isFirstLocate) {
                setLocation();
                isFirstLocate = true;
                MyLocationConfiguration myLocationConfiguration = new MyLocationConfiguration
                        (MyLocationConfiguration.LocationMode.NORMAL, true, null);
                mMap.setMyLocationConfiguration(myLocationConfiguration);
            }
          /*只有设置了setMyLocationData数据setMyLocationConfiguration定位的图标才会显示出来*/
            // map view 销毁后不在处理新接收的位置
            if (bdLocation == null || mMap == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(44)//定位点环圈的半径
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
        //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
        MapStatusUpdate mMapStatusUpdate =
                MapStatusUpdateFactory.newLatLngZoom(center, MAP_ZOOM_DEFAULT);
        //改变地图状态
        mMap.animateMapStatus(mMapStatusUpdate);
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
        /*如果不设置这个默认为3D地图,MAP_TYPE_NORMAL为2D;MAP_TYPE_SATELLITE为卫星图;MAP_TYPE_NONE:空白的地图什么都没有*/
        mMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        /*设置是否打开交通图层*/
        mMap.setTrafficEnabled(true);
        /*是否允许楼块效果*/
        mMap.setBuildingsEnabled(true);
    }


    /*初始化传感器*/
    private void initOritationListener() {
        orientationListener = new OrientationListener(this);
        orientationListener.setOnOrientationListener(new OrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mXDirection = (int) x;
            }
        });
        // 开启方向传感器
        orientationListener.start();

    }


    private void initView() {
        mapwiew = (MapView) findViewById(R.id.poi_map);
        tv = (AutoCompleteTextView) findViewById(R.id.poi_tv);
        findViewById(R.id.back_poi).setOnClickListener(this);
        findViewById(R.id.poi_city).setOnClickListener(this);
        findViewById(R.id.poi_Bound).setOnClickListener(this);
        findViewById(R.id.poi_Nearby).setOnClickListener(this);
        findViewById(R.id.poi_locatemap).setOnClickListener(this);
        /*搜索框内的图标初始化*/
        mLinearLayout = (LinearLayout) findViewById(R.id.poi_ll_ll);
        findViewById(R.id.poi_iv).setOnClickListener(this);
        findViewById(R.id.poi_search).setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.poi_search:
                city();
                break;
            case R.id.poi_iv:
                tv.setText("");
                break;
            case R.id.back_poi:
                finish();
                break;
            case R.id.poi_city:
                city();
                break;
            case R.id.poi_Bound:
                bound();
                break;
            case R.id.poi_Nearby:
                nearBy();
                break;
            case R.id.poi_locatemap:
                setLocation();
                break;
        }
    }

    /*响应区域搜索按钮点击事件*/
    private void bound() {
        searchType = 3;
        LatLng southwest = new LatLng(latitude - 0.02, longitude - 0.02);
        LatLng northeast = new LatLng(latitude + 0.02, longitude + 0.02);
        //地理范围数据结构，由西南以及东北坐标点确认
        searchbound = new LatLngBounds.Builder().include(southwest).include(northeast)
                .include(northeast).build();
        mPoiSearch.searchInBound(new PoiBoundSearchOption().bound(searchbound).keyword(tv.getText().toString()).pageNum(0));
    }


    /**
     * 响应周边搜索按钮点击事件
     */
    private void nearBy() {
        searchType = 2;// comprehensive：安综合排序 distance：按距离排序
        PoiNearbySearchOption option = new PoiNearbySearchOption().radius(radius)//范围半径
                .keyword(tv.getText().toString()).sortType(PoiSortType.distance_from_near_to_far)//搜索结果排序规则
                .location(center).pageNum(0);
        mPoiSearch.searchNearby(option);
    }

    /*通过城市的方法查询*/
    private void city() {
        searchType = 1;
        String key = tv.getText().toString();
        mPoiSearch.searchInCity(new PoiCitySearchOption().city(city).keyword(key).pageNum(0));
    }

    /*对周边检索的范围进行绘制*/
    public void showNearbyArea(LatLng center, int radius) {
        /*设置范围的中心点*/
//        BitmapDescriptor centerBitmap = BitmapDescriptorFactory
//                .fromResource(R.mipmap.ic_marker_poi);
//        MarkerOptions ooMarker = new MarkerOptions().position(center).icon(centerBitmap);
//        mMap.addOverlay(ooMarker);
        /*创建圆*/
        OverlayOptions ooCircle = new CircleOptions().fillColor(ContextCompat.getColor(this, R.color.trans_bg_color))
                .center(center).stroke(new Stroke(6, ContextCompat.getColor(this, R.color.blueSky)))//边框的宽度,默认为5.边框的颜色
                .radius(radius);
        mMap.addOverlay(ooCircle);
        setLocation();
    }


    /*对区域检索的范围进行绘制*/
    public void showBound(LatLngBounds bounds) {
        BitmapDescriptor bdGround = BitmapDescriptorFactory
                .fromResource(R.mipmap.ground_overlay);
        /*构造 ground 覆盖物的选项类*/
        OverlayOptions ooGround = new GroundOverlayOptions()
                .positionFromBounds(bounds).image(bdGround).transparency(0.8f);
        mMap.addOverlay(ooGround);
        /*将镜头移动到这个覆盖物的中心点*/
        MapStatusUpdate u = MapStatusUpdateFactory
                .newLatLng(bounds.getCenter());
        mMap.animateMapStatus(u);
        bdGround.recycle();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocationClient != null) {
            mLocationClient.stop();
            mLocationClient.unRegisterLocationListener(locationlistener);
        }
        mPoiSearch.destroy();
        mSuggestionSearch.destroy();
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
