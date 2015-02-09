package com.limemobile.app.sdk.orm;

import java.util.Collection;

import com.limemobile.app.sdk.http.BasicJSONResponse;

public interface DataModelFetchListener<T> {
    public static final int SUCCESS_WITH_EXPIRED_DATA = 1;
    public static final int SUCCESS = 0;
    public static final int FAILED = -1;
    public static final int FAILED_WITH_NETOWRK_NOT_AVALIABLE = -2;

    /**
     * 当网络可用并且数据过期的情况下resultCode才等于SUCCESS_WITH_EXPIRED_DATA(这是后台会自动调用api来获取数据)
     * 
     * @param resultCode
     * @param collection
     * @param response
     */
    public void onQueryFinish(int resultCode, Collection<T> collection,
            BasicJSONResponse response);

    /**
     * querymore不判断数据是否过期，只当没有数据并且网络可用的情况下才会调用api获取数据
     * @param resultCode
     * @param collection
     * @param response
     */
    public void onQueryMoreFinish(int resultCode, Collection<T> collection,
            BasicJSONResponse response);

    public void onUpdateFinish(int resultCode, Collection<T> collection,
            BasicJSONResponse response);

}
