package com.lcb.one.base;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;

import com.baidu.mapapi.SDKInitializer;
import com.lcb.one.constant.SpKey;
import com.lcb.one.util.ToastUtil;

import java.util.Stack;

public class BaseApplication extends Application {
    private static BaseApplication mInstance = null;
    public static Context context;
    public static SpUtil sp;
    public static WifiManager.MulticastLock lock;
    public static WifiManager wfm;
    String tag = "BaseApplication";
    private long firsttime;


    public static BaseApplication getInstance() {
        if (mInstance == null) {
            mInstance = new BaseApplication();
        }
        return mInstance;
    }



    @Override
    public void onCreate() {
        super.onCreate();
        /*百度地图SDK初始化*/
        SDKInitializer.initialize(this);
        if (activityStack == null) {
            activityStack = new Stack<>();
        }
        mInstance = this;
        context = getApplicationContext();
        sp = SpUtil.getInstance(SpKey.SP_name, MODE_PRIVATE);
        initUDP();
    }



    private void initUDP() {
        // 有的手机不能直接接收UDP包，可能是手机厂商在定制Rom的时候把这个功能给关掉了。实例化一个WifiManager.MulticastLock
        // 对象lock, 在调用广播发送、接收报文之前先调用lock.acquire()方法；
        // 用完之后及时调用lock.release()释放资源，否决多次调用lock.acquire()方法，程序可能会崩.
        wfm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        lock = wfm.createMulticastLock("wifi lcb");
    }


    /**
     * 退出整个程序
     */
    public void exitApp() {
        long secondtime = System.currentTimeMillis();
        if (secondtime - firsttime > 1000) {
            ToastUtil.showLong("再点一次就退出哦！");
            firsttime = secondtime;
        } else {
            onTerminate();
        }
    }

    @Override
    public void onTerminate() {
        //杀死栈里面的所有Activity
        finishAllActivity();

        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        manager.killBackgroundProcesses(getPackageName());

        System.exit(0);
        super.onTerminate();
    }


    /* ---------------------------------自定义管理栈------------------------------------------------------------- */
    private static Stack<Activity> activityStack;

    /**
     * 添加Activity到堆栈
     */
    public void addActivity(Activity activity) {
        activityStack.add(activity);
    }

    /**
     * 获取当前Activity（堆栈中最后一个压入的）
     */
    public Activity currentActivity() {
        Activity activity = activityStack.lastElement();
        return activity;
    }

    /**
     * 结束当前Activity（堆栈中最后一个压入的）
     */
    public void finishActivity() {
        Activity activity = activityStack.lastElement();
        if (activity != null) {
            activity.finish();
            activity = null;
        }
    }

    /**
     * 结束指定的Activity
     */
    public void finishActivity(Activity activity) {
        if (activity != null) {
            activityStack.remove(activity);
            activity.finish();
            activity = null;
        }
    }

    /**
     * 结束指定类名的Activity
     */
    public void finishActivity(Class<?> cls) {
        for (Activity activity : activityStack) {
            if (activity.getClass().equals(cls)) {
                finishActivity(activity);
            }
        }
    }

    /**
     * 结束所有Activity
     */
    public void finishAllActivity() {
        for (int i = 0, size = activityStack.size(); i < size; i++) {
            if (null != activityStack.get(i)) {
                activityStack.get(i).finish();
            }
        }
        activityStack.clear();
    }


}