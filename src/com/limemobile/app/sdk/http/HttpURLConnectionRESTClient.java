package com.limemobile.app.sdk.http;

import java.io.File;
import java.net.CookieHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.apache.http.cookie.Cookie;
import org.json.JSONObject;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.android.volley.Network;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RequestQueue.RequestFilter;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.limemobile.app.sdk.http.internal.FakeX509TrustManager;
import com.limemobile.app.sdk.http.internal.HttpHeaders;
import com.limemobile.app.sdk.http.internal.HttpUtils;
import com.limemobile.app.sdk.http.internal.MyApacheHttpClient;
import com.limemobile.app.sdk.http.internal.MyCookieStore;
import com.limemobile.app.sdk.http.internal.VolleyJSONRequest;

public class HttpURLConnectionRESTClient extends BaseRESTClient {
    protected final Context mContext;
    protected final RequestQueue mRequestQueue;
    protected final String mUserAgent;
    protected final MyCookieStore mCookieStore;

    /*
     * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
     * 
     * Volley doesn't actually make HTTP requests itself, and thus doesn't
     * manage Cookies directly. It instead uses an instance of HttpStack to do
     * this. There are two main implementations:
     * 
     * HurlStack: Uses HttpUrlConnection under the hood HttpClientStack: uses
     * Apache HttpClient under the hood Cookie management is the responsibility
     * of those HttpStacks. And they each handle Cookies differently.
     * 
     * If you need to support < 2.3, then you should use the HttpClientStack:
     * 
     * Configure an HttpClient instance, and pass that to Volley for it to use
     * under the hood:
     * 
     * // If you need to directly manipulate cookies later on, hold onto this
     * client // object as it gives you access to the Cookie Store
     * DefaultHttpClient httpclient = new DefaultHttpClient();
     * 
     * CookieStore cookieStore = new BasicCookieStore();
     * httpclient.setCookieStore( cookieStore );
     * 
     * HttpStack httpStack = new HttpClientStack( httpclient ); RequestQueue
     * requestQueue = Volley.newRequestQueue( context, httpStack ); The
     * advantage with this vs manually inserting cookies into the headers is
     * that you get actual cookie management. Cookies in your store will
     * properly respond to HTTP controls that expire or update them.
     */
    /**
     * @param context
     */
    public HttpURLConnectionRESTClient(Context context) {
        super();
        mContext = context.getApplicationContext();
        mUserAgent = HttpUtils.createUserAgentString(mContext);

        mCookieStore = new MyCookieStore(mContext);

        File cacheDir = new File(context.getCacheDir(), "restclient");

        Network network = buildNetwork(context);

        mRequestQueue = new RequestQueue(new DiskBasedCache(cacheDir), network);
        mRequestQueue.start();
    }

    @Override
    public List<Cookie> getCookies(String domain) {
        List<Cookie> cookies = new ArrayList<Cookie>();

        List<Cookie> allCookies = mCookieStore.getCookies();
        for (Cookie cookie : allCookies) {
            if (TextUtils.isEmpty(domain) || cookie.getDomain().equals(domain)) {
                cookies.add(cookie);
            }
        }

        return cookies;
    }

    @Override
    public Cookie getCookie(String domain, String name) {
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException();
        }
        List<Cookie> cookies = getCookies(domain);
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())
                        && (TextUtils.isEmpty(domain) || domain.equals(cookie
                                .getDomain()))) {
                    return cookie;
                }
            }
        }
        return null;
    }

    @Override
    public void clearCookies(String domain) {
        List<Cookie> cookies = mCookieStore.getCookies();
        for (Cookie cookie : cookies) {
            if ((TextUtils.isEmpty(domain) || domain.equals(cookie.getDomain()))) {
                mCookieStore.deleteCookie(cookie);
            }
        }
    }

    public void cancelAllRequests(String tag) {
        mRequestQueue.cancelAll(tag);
    }

    @Override
    public void cancelRequests(Context context, String tag) {
        if (!TextUtils.isEmpty(tag)) {
            mRequestQueue.cancelAll(tag);
        } else {
            mRequestQueue.cancelAll(new RequestFilter() {

                @Override
                public boolean apply(Request<?> request) {
                    return true;
                }

            });
        }
    }

    @Override
    public RequestHandleWrapper get(Context context, RESTRequest request) {
        return get(context, request, null);
    }

    @Override
    public RequestHandleWrapper get(Context context, RESTRequest request,
            Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        headers.put(HttpHeaders.USER_AGENT, mUserAgent);
        Request<JSONObject> requestHandle = mRequestQueue
                .add(new VolleyJSONRequest(mContext, Request.Method.GET,
                        headers, request));

        return new RequestHandleWrapper(requestHandle, request);
    }

    @Override
    public RequestHandleWrapper post(Context context, RESTRequest request) {
        return post(context, request, null);
    }

    @Override
    public RequestHandleWrapper post(Context context, RESTRequest request,
            Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        headers.put(HttpHeaders.USER_AGENT, mUserAgent);
        Request<JSONObject> requestHandle = mRequestQueue
                .add(new VolleyJSONRequest(mContext, Request.Method.POST,
                        headers, request));
        return new RequestHandleWrapper(requestHandle, request);
    }

    @Override
    public RequestHandleWrapper put(Context context, RESTRequest request) {
        return put(context, request, null);
    }

    @Override
    public RequestHandleWrapper put(Context context, RESTRequest request,
            Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        headers.put(HttpHeaders.USER_AGENT, mUserAgent);
        Request<JSONObject> requestHandle = mRequestQueue
                .add(new VolleyJSONRequest(mContext, Request.Method.PUT,
                        headers, request));

        return new RequestHandleWrapper(requestHandle, request);
    }

    @Override
    public RequestHandleWrapper delete(Context context, RESTRequest request) {
        return delete(context, request, null);
    }

    @Override
    public RequestHandleWrapper delete(Context context, RESTRequest request,
            Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        headers.put(HttpHeaders.USER_AGENT, mUserAgent);
        Request<JSONObject> requestHandle = mRequestQueue
                .add(new VolleyJSONRequest(mContext, Request.Method.DELETE,
                        headers, request));

        return new RequestHandleWrapper(requestHandle, request);
    }

    @Override
    public BasicJSONResponse getSync(Context context, RESTRequest request) {
        return getSync(context, request, null);
    }

    @Override
    public BasicJSONResponse getSync(Context context, RESTRequest request,
            Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        headers.put(HttpHeaders.USER_AGENT, mUserAgent);
        return excuteSync(context, new VolleyJSONRequest(mContext,
                Request.Method.GET, headers, request));
    }

    @Override
    public BasicJSONResponse postSync(Context context, RESTRequest request) {
        return postSync(context, request, null);
    }

    @Override
    public BasicJSONResponse postSync(Context context, RESTRequest request,
            Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        headers.put(HttpHeaders.USER_AGENT, mUserAgent);
        return excuteSync(context, new VolleyJSONRequest(mContext,
                Request.Method.POST, headers, request));
    }

    @Override
    public BasicJSONResponse putSync(Context context, RESTRequest request) {
        return putSync(context, request, null);
    }

    @Override
    public BasicJSONResponse putSync(Context context, RESTRequest request,
            Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        headers.put(HttpHeaders.USER_AGENT, mUserAgent);
        return excuteSync(context, new VolleyJSONRequest(mContext,
                Request.Method.PUT, headers, request));
    }

    @Override
    public BasicJSONResponse deleteSync(Context context, RESTRequest request) {
        return deleteSync(context, request, null);
    }

    @Override
    public BasicJSONResponse deleteSync(Context context, RESTRequest request,
            Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        headers.put(HttpHeaders.USER_AGENT, mUserAgent);
        return excuteSync(context, new VolleyJSONRequest(mContext,
                Request.Method.DELETE, headers, request));
    }

    public final RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    private BasicJSONResponse excuteSync(Context context,
            VolleyJSONRequest request) {

        Network network = buildNetwork(context);

        try {
            request.addMarker("network-queue-take");

            // Perform the network request.
            NetworkResponse networkResponse = network.performRequest(request);
            request.addMarker("network-http-complete");

            // Parse the response here on the worker thread.
            @SuppressWarnings("unused")
            Response<?> response = request
                    .parseNetworkResponse(networkResponse);
            request.addMarker("network-parse-complete");

            // Post the response back.
            request.markDelivered();
        } catch (VolleyError volleyError) {
            request.parseNetworkError(volleyError);
        } catch (Exception e) {
            request.parseNetworkError(new VolleyError(e));
        }

        return request.getBasicJSONResponse();
    }

    private Network buildNetwork(Context context) {
        HttpStack stack;
        if (Build.VERSION.SDK_INT >= 10) {
            stack = new HurlStack();
            CookieHandler.setDefault(mCookieStore);
            FakeX509TrustManager.allowAllSSL();
        } else {
            // Prior to Gingerbread, HttpUrlConnection was unreliable.
            // See:
            // http://android-developers.blogspot.com/2011/09/androids-http-clients.html
            HttpClient client = MyApacheHttpClient.newDefaultHttpClient(
                    mUserAgent, mContext, mCookieStore); // AndroidHttpClient.newInstance(mUserAgent);
            stack = new HttpClientStack(client);
        }

        Network network = new BasicNetwork(stack);
        return network;
    }
}
