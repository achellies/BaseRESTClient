package com.limemobile.app.sdk.orm.internal;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.alibaba.fastjson.JSON;
import com.limemobile.app.sdk.orm.AbstractJSONParser;

public abstract class FastJSONModel<T> implements AbstractJSONParser<T> {
    protected final Class<T> mClazz;

    public FastJSONModel(Class<T> clazz) {
        super();

        mClazz = clazz;
    }

    public List<T> parseObjects(JSONObject json) {
        ArrayList<T> entities = new ArrayList<T>();
        // TOOD 需要子类来实现
        return entities;
    }

    public T parseObject(JSONObject json) {
        T entity = null;
        try {
            entity = JSON.parseObject(json.toString(), mClazz);
            updateCacheExpiryDate(entity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entity;
    }

    public List<T> parseObjects(JSONArray jsonArray) {
        List<T> entities = JSON.parseArray(jsonArray.toString(), mClazz);
        if (entities != null && !entities.isEmpty()) {
            for (T entity : entities) {
                updateCacheExpiryDate(entity);
            }
        }
        return entities;
    }

    protected abstract void updateCacheExpiryDate(T entity);

    public abstract boolean isCacheExpired(T entity);

    public abstract boolean isCacheExpired(List<T> entities);
}
