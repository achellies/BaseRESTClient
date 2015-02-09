package com.limemobile.app.sdk.http;

import java.util.List;
import java.util.Map;

import org.apache.http.cookie.Cookie;

import android.content.Context;

public abstract class BaseRESTClient {
    public abstract RequestHandleWrapper get(Context context,
            RESTRequest request);

    public abstract RequestHandleWrapper get(Context context,
            RESTRequest request, Map<String, String> headers);

    public abstract RequestHandleWrapper post(Context context,
            RESTRequest request);

    public abstract RequestHandleWrapper post(Context context,
            RESTRequest request, Map<String, String> headers);

    public abstract RequestHandleWrapper put(Context context,
            RESTRequest request);

    public abstract RequestHandleWrapper put(Context context,
            RESTRequest request, Map<String, String> headers);

    public abstract RequestHandleWrapper delete(Context context,
            RESTRequest request);

    public abstract RequestHandleWrapper delete(Context context,
            RESTRequest request, Map<String, String> headers);

    public abstract BasicJSONResponse getSync(Context context,
            RESTRequest request);

    public abstract BasicJSONResponse getSync(Context context,
            RESTRequest request, Map<String, String> headers);

    public abstract BasicJSONResponse postSync(Context context,
            RESTRequest request);

    public abstract BasicJSONResponse postSync(Context context,
            RESTRequest request, Map<String, String> headers);

    public abstract BasicJSONResponse putSync(Context context,
            RESTRequest request);

    public abstract BasicJSONResponse putSync(Context context,
            RESTRequest request, Map<String, String> headers);

    public abstract BasicJSONResponse deleteSync(Context context,
            RESTRequest request);

    public abstract BasicJSONResponse deleteSync(Context context,
            RESTRequest request, Map<String, String> headers);

    public abstract void clearCookies(String domain);

    public abstract List<Cookie> getCookies(String domain);

    public abstract Cookie getCookie(String domain, String name);

    public abstract void cancelRequests(final Context context, final String tag);
}
