package com.lcb.one.map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
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
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.CityInfo;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.baidu.navisdk.adapter.BNCommonSettingParam;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNaviSettingManager;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.lcb.one.R;
import com.lcb.one.base.BaseActivity;
import com.lcb.one.listener.OrientationListener;
import com.lcb.one.util.Logs;
import com.lcb.one.util.ToastUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class NaviActivity extends BaseActivity implements View.OnClickListener {
    AutoCompleteTextView tv;
    private MapView mapwiew;
    private BaiduMap mMap;
    private LocationClient mLocationClient;
    String tag = "NaviActivity";
    public static final float MAP_ZOOM_DEFAULT = 19;//设置默认的缩放级别
    boolean isFirstLocate;
    private OrientationListener orientationListener;//方向传感器的监听器
    private int mXDirection;//方向传感器X方向的值
    private ArrayAdapter<String> sugAdapter;//提示列表的适配器
    private PoiSearch mPoiSearch;//搜索模块
    private SuggestionSearch mSuggestionSearch;//建议搜索模块
    private List<String> suggest;//返回的数据结果清单
    LatLng center;//设置中心点
    private LinearLayout mLinearLayout;
    private Button goButton;//开始导航按钮
    /*导航*/
    private final String TTS_API_KEY = "10088247"; //语音播报api_key,一定要在官网申请不然用不了。申请的ID:10088247
    private boolean initSuccess = false; //导航是否初始化成功的标志位
    private BDLocation currentLocation, endLocation;//起始位置，目标位置
    private static final String APP_FOLDER_NAME = "百度地图的缓存音频";//缓存音频的文件夹
    private String mSDCardPath = null;
    public static List<Activity> activityList = new LinkedList<>();
    public static final String ROUTE_PLAN_NODE = "routePlanNode";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navi);
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
            sugAdapter = new ArrayAdapter<>(NaviActivity.this, android.R.layout.simple_list_item_1, suggest);
            tv.setAdapter(sugAdapter);
            sugAdapter.notifyDataSetChanged();
        }
    };
    /*poi检索结果回调*/

    OnGetPoiSearchResultListener poiListener = new OnGetPoiSearchResultListener() {
        /*获取POI搜索结果，包括searchInCity，searchNearby，searchInBound返回的搜索结果*/
        @Override
        public void onGetPoiResult(PoiResult result) {
            if (result == null || result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                Toast.makeText(NaviActivity.this, "未找到结果", Toast.LENGTH_LONG)
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
                Toast.makeText(NaviActivity.this, strInfo, Toast.LENGTH_LONG)
                        .show();
            }
        }

        /* 获取POI详情搜索结果，得到searchPoiDetail返回的搜索结果*/
        @Override
        public void onGetPoiDetailResult(PoiDetailResult result) {
            if (result.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(NaviActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
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

//        boolean isdir = SmallUtil.setFile(APP_FOLDER_NAME);
        /**
         * 首先进行授权
         */
        if (!initSuccess && initDirs())
            initNavi();

        /*创建目的地址：endLocation*/
        endLocation = new BDLocation();
        endLocation.setLatitude(ll.latitude);
        endLocation.setLongitude(ll.longitude);
        endLocation.setAddrStr(address);

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

    /*初始化自动提示*/
    private void initAutoCompleteTextView() {
        tv.setThreshold(1);//设置几个字开始有提示
        /*当输入关键字变化时，动态更新建议列表*/
        tv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                SuggestionSearchOption option = new SuggestionSearchOption();
                if (currentLocation.getCity() != null) {
                    option = option.city(currentLocation.getCity()).keyword(s.toString());
                    mSuggestionSearch.requestSuggestion(option);
                    if (s.length() == 0) {
                        mLinearLayout.setVisibility(View.GONE);
                    } else {
                        mLinearLayout.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void initView() {
        mapwiew = (MapView) findViewById(R.id.navi_map);
        tv = (AutoCompleteTextView) findViewById(R.id.navi_tv);
        findViewById(R.id.back_navi).setOnClickListener(this);
        findViewById(R.id.navi_locatemap).setOnClickListener(this);
        /*搜索框内的图标初始化*/
        mLinearLayout = (LinearLayout) findViewById(R.id.navi_ll_ll);
        findViewById(R.id.navi_iv).setOnClickListener(this);
        findViewById(R.id.navi_search).setOnClickListener(this);
        goButton = (Button) findViewById(R.id.navi_btn);
        goButton.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_navi:
                finish();
                break;
            case R.id.navi_locatemap:
                setLocation();
                break;
            case R.id.navi_iv:
                tv.setText("");
                break;
            case R.id.navi_search:
                if (currentLocation.getCity() != null) {
                    city();
                }
                break;
            case R.id.navi_btn:
                Navi();
                break;
        }
    }


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


    /*通过城市的方法查询*/
    private void city() {
        String key = tv.getText().toString();
        mPoiSearch.searchInCity(new PoiCitySearchOption().city(currentLocation.getCity()).keyword(key).pageNum(0));
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
            currentLocation = bdLocation;
            center = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
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

    /*初始化SD卡，在SD卡路径下新建文件夹：App目录名，文件中包含了很多东西，比如log、cache等等*/
    private boolean initDirs() {
        mSDCardPath = getSdcardDir();
        if (mSDCardPath == null) {
            return false;
        }
        File f = new File(mSDCardPath, APP_FOLDER_NAME);
        if (!f.exists()) {
            try {
                f.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /* 得到根目录的路径*/
    private String getSdcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }


    /* +++++++++++++++++++++++++++++    导航部分   ++++++++++++++++++++++++++++++++*/
    private void Navi() {

        /**
         * 判断是否已经授权
         */
//        Logs.d("433  "+BaiduNaviManager.isNaviInited());
        if (!BaiduNaviManager.isNaviInited()) {
            ToastUtil.showShort("授权失败咯");
            return;
        }
        /**
         * 获取起始的算路节点
         */
        BNRoutePlanNode sNode = null;
        BNRoutePlanNode eNode = null;
        Logs.i(tag + "471  开始获取起点和终点");
        sNode = new BNRoutePlanNode(
                currentLocation.getLongitude(),          //经度
                currentLocation.getLatitude(),           //纬度
                currentLocation.getBuildingName(),       //算路节点名
                null,                                   //算路节点地址描述
                BNRoutePlanNode.CoordinateType.BD09LL
        ); //坐标类型
        eNode = new BNRoutePlanNode(
                endLocation.getLongitude(), endLocation.getLatitude(), endLocation.getAddrStr(),
                null,
                BNRoutePlanNode.CoordinateType.BD09LL);

//        Logs.d("454   "+sNode+"   "+eNode);
        if (sNode != null && eNode != null) {
            List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
            list.add(sNode);
            list.add(eNode);
            /**
             * 发起算路操作并在算路成功后通过回调监听器进入导航过程,返回是否执行成功
             */
            BaiduNaviManager.getInstance()
                    .launchNavigator(
                            NaviActivity.this,               //建议是应用的主Activity
                            list,                            //传入的算路节点，顺序是起点、途经点、终点，其中途经点最多三个
                            1,                               //算路偏好 1:推荐 8:少收费 2:高速优先 4:少走高速 16:躲避拥堵
                            true,                            //true表示真实GPS导航，false表示模拟导航
                            new DemoRoutePlanListener(sNode)//开始导航回调监听器，在该监听器里一般是进入导航过程页面
                    );
        }
    }


    /**
     * 导航回调监听器
     */
    public class DemoRoutePlanListener implements BaiduNaviManager.RoutePlanListener {

        private BNRoutePlanNode mBNRoutePlanNode = null;

        public DemoRoutePlanListener(BNRoutePlanNode node) {
            mBNRoutePlanNode = node;
        }

        @Override
        public void onJumpToNavigator() {
            /*
             * 设置途径点以及resetEndNode会回调该接口
             */

            for (Activity ac : activityList) {
                if (ac.getClass().getName().endsWith("GuideActivity")) {
                    return;
                }
            }
            /**
             * 导航activity
             */
            Intent intent = new Intent(NaviActivity.this, GuideActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(ROUTE_PLAN_NODE, mBNRoutePlanNode);
            intent.putExtras(bundle);
            startActivity(intent);
        }

        @Override
        public void onRoutePlanFailed() {
            Toast.makeText(NaviActivity.this, "算路失败", Toast.LENGTH_SHORT).show();
        }
    }


    String authinfo = null;//钥匙的验证信息

    /**
     * 使用SDK前，先进行百度服务授权和引擎初始化
     */
    private void initNavi() {
        BaiduNaviManager.getInstance().init(this, mSDCardPath, APP_FOLDER_NAME, new BaiduNaviManager.NaviInitListener() {
            @Override
            public void onAuthResult(int status, String msg) {
                if (0 == status) {
                    authinfo = "key校验成功!";
                } else {
                    authinfo = "key校验失败, " + msg;
                }
                NaviActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(NaviActivity.this, authinfo, Toast.LENGTH_LONG).show();
                    }
                });
            }

            public void initSuccess() {
                initSuccess = true;
                initSetting();
                goButton.setText("授权成功,点击进入导航");
                goButton.setEnabled(true);
            }

            public void initStart() {
                Logs.i("446   百度导航引擎初始化开始");
            }

            public void initFailed() {
                Logs.i("449   百度导航引擎初始化失败");
            }
        }, null, ttsHandler, ttsPlayStateListener);

    }


    /**
     * 内部TTS播报状态回调接口
     */
    private BaiduNaviManager.TTSPlayStateListener ttsPlayStateListener = new BaiduNaviManager.TTSPlayStateListener() {

        @Override
        public void playEnd() {
            Logs.d(tag + "593 TTS play end");
        }

        @Override
        public void playStart() {
            Logs.d(tag + "597 TTS play start");
        }
    };


    /**
     * 内部TTS播报状态回传handler
     */
    private Handler ttsHandler = new Handler() {
        public void handleMessage(Message msg) {
            int type = msg.what;
            switch (type) {
                case BaiduNaviManager.TTSPlayMsgType.PLAY_START_MSG: {
                    Logs.w(tag + "609 Handler_TTS play start");
                    break;
                }
                case BaiduNaviManager.TTSPlayMsgType.PLAY_END_MSG: {
                    Logs.v(tag + "613 Handler_TTS play end");
                    break;
                }
                default:
                    break;
            }
        }
    };

    /**
     * 导航设置管理器
     */
    private void initSetting() {
        /**
         * 日夜模式 1：自动模式 2：白天模式 3：夜间模式
         */
        BNaviSettingManager.setDayNightMode(BNaviSettingManager.DayNightMode.DAY_NIGHT_MODE_DAY);
        /**
         * 设置预览路况条 ROAD_CONDITION_BAR_SHOW_OFF 关闭显示
         */
        BNaviSettingManager.setShowTotalRoadConditionBar(BNaviSettingManager.PreViewRoadCondition.ROAD_CONDITION_BAR_SHOW_ON);
        /**
         * 设置语音播报模式 Novice新手模式  Quite静音 Veteran老手模式
         */
        BNaviSettingManager.setVoiceMode(BNaviSettingManager.VoiceMode.Veteran);
        /**
         * 设置实时路况条 NAVI_ITS_OFF 关闭显示
         */
        BNaviSettingManager.setRealRoadCondition(BNaviSettingManager.RealRoadCondition.NAVI_ITS_ON);

        Bundle bundle = new Bundle();
        // 必须设置APPID，否则会静音
        bundle.putString(BNCommonSettingParam.TTS_APP_ID, TTS_API_KEY);
        BNaviSettingManager.setNaviSdkParam(bundle);

    }


}
