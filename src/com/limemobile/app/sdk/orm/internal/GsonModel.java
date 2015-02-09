package com.limemobile.app.sdk.orm.internal;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.limemobile.app.sdk.orm.AbstractJSONParser;

public abstract class GsonModel<T> implements AbstractJSONParser<T> {
    protected final Class<T> mClazz;
    protected final Gson mGson;

    public GsonModel(Class<T> clazz) {
        super();

        mClazz = clazz;

        GsonBuilder gsonBuilder = new GsonBuilder();
        mGson = gsonBuilder.create();
    }

    public List<T> parseObjects(JSONObject json) {
        ArrayList<T> entities = new ArrayList<T>();
        // TOOD 需要子类来实现
        return entities;
    }

    public T parseObject(JSONObject json) {
        T entity = null;
        try {
            entity = mGson.fromJson(json.toString(), mClazz);
            updateCacheExpiryDate(entity);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return entity;
    }

    public List<T> parseObjects(JSONArray jsonArray) {
        ArrayList<T> entities = new ArrayList<T>();
        if (jsonArray != null && jsonArray.length() > 0) {
            int count = jsonArray.length();
            for (int index = 0; index < count; ++index) {
                JSONObject json = jsonArray.optJSONObject(index);
                if (json == null) {
                    continue;
                }
                T entity = parseObject(json);
                if (entity == null) {
                    continue;
                }
                entities.add(entity);
            }
        }
        return entities;
    }

    protected abstract void updateCacheExpiryDate(T entity);

    public abstract boolean isCacheExpired(T entity);

    public abstract boolean isCacheExpired(List<T> entities);
}
