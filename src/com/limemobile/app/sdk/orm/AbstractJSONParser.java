package com.limemobile.app.sdk.orm;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public interface AbstractJSONParser<T> {
    public T parseObject(JSONObject json);

    public List<T> parseObjects(JSONObject json);

    public List<T> parseObjects(JSONArray jsonArray);

    public boolean isCacheExpired(T entity);

    public boolean isCacheExpired(List<T> entities);
}
