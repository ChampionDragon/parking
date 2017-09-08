package com.lcb.one.listener;

import org.json.JSONObject;

/**
 * Description: 地理编码搜索的接口回调
 * AUTHOR: Champion Dragon
 * created at 2017/8/24
 **/
public interface GeoListener {
    public static String address="address";
    //没搜索到结果
    void noting();

    //搜索到的数据
    void data(JSONObject obj);


}
