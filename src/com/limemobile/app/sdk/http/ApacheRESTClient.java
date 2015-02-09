package com.limemobile.app.sdk.http;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;

import org.apache.http.Header;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicHeader;

import android.content.Context;
import android.text.TextUtils;

import com.limemobile.app.sdk.http.internal.HttpUtils;
import com.limemobile.app.sdk.http.internal.MyCookieStore;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.SyncHttpClient;

public class ApacheRESTClient extends BaseRESTClient {
    public static final int DEFAULT_MAX_CONNECTIONS = 10;
    public static final int DEFAULT_CONNECT_TIMEOUT = 15 * 1000;
    public static final int DEFAULT_RESPONSE_TIMEOUT = 15 * 1000;
    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final int DEFAULT_RETRY_SLEEP_TIME_MILLIS = 2000;

    protected final Context mContext;
    protected AsyncHttpClient mAsyncClient;
    protected AsyncHttpClient mSyncClient;
    protected final String mUserAgent;

    public ApacheRESTClient(Context context) {
        super();
        mContext = context.getApplicationContext();
        mUserAgent = HttpUtils.createUserAgentString(mContext);

        mSyncClient = new SyncHttpClient(true, 80, 443);
        mSyncClient.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        mSyncClient.setResponseTimeout(DEFAULT_RESPONSE_TIMEOUT);
        mSyncClient.setMaxConnections(DEFAULT_MAX_CONNECTIONS);
        mSyncClient.setMaxRetriesAndTimeout(DEFAULT_MAX_RETRIES,
                DEFAULT_RETRY_SLEEP_TIME_MILLIS);
        mSyncClient.setThreadPool(Executors.newCachedThreadPool());
        mSyncClient.setUserAgent(mUserAgent);

        mAsyncClient = new AsyncHttpClient(true, 80, 443);
        mAsyncClient.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        mAsyncClient.setResponseTimeout(DEFAULT_RESPONSE_TIMEOUT);
        mAsyncClient.setMaxConnections(DEFAULT_MAX_CONNECTIONS);
        mAsyncClient.setMaxRetriesAndTimeout(DEFAULT_MAX_RETRIES,
                DEFAULT_RETRY_SLEEP_TIME_MILLIS);
        mAsyncClient.setThreadPool(Executors.newCachedThreadPool());
        mAsyncClient.setUserAgent(mUserAgent);

        MyCookieStore cookieStore = new MyCookieStore(mContext);
        mSyncClient.setCookieStore(cookieStore);
        mAsyncClient.setCookieStore(cookieStore);
    }

    @Override
    public List<Cookie> getCookies(String domain) {
        List<Cookie> cookies = new ArrayList<Cookie>();
        MyCookieStore cookieStore = getCookieStore();
        if (cookieStore != null) {
            List<Cookie> allCookies = cookieStore.getCookies();
            for (Cookie cookie : allCookies) {
                if (TextUtils.isEmpty(domain)
                        || cookie.getDomain().equals(domain)) {
                    cookies.add(cookie);
                }
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
        MyCookieStore cookieStore = getCookieStore();
        if (cookieStore == null) {
            return;
        }
        List<Cookie> cookies = cookieStore.getCookies();
        for (Cookie cookie : cookies) {
            if ((TextUtils.isEmpty(domain) || domain.equals(cookie.getDomain()))) {
                cookieStore.deleteCookie(cookie);
            }
        }
    }

    public void setConnectTimeout(int value) {
        mAsyncClient.setConnectTimeout(value);
        mSyncClient.setConnectTimeout(value);
    }

    public void setResponseTimeout(int value) {
        mAsyncClient.setResponseTimeout(value);
        mSyncClient.setResponseTimeout(value);
    }

    public void setMaxConnections(int maxConnections) {
        mAsyncClient.setMaxConnections(maxConnections);
        mSyncClient.setMaxConnections(maxConnections);
    }

    public void setMaxRetriesAndTimeout(int retries, int timeout) {
        mAsyncClient.setMaxRetriesAndTimeout(retries, timeout);
        mSyncClient.setMaxRetriesAndTimeout(retries, timeout);
    }

    @Override
    public void cancelRequests(Context context, String tag) {
        if (context == null) {
            cancelAllRequests(true);
        } else {
            cancelRequests(context, true);
        }
    }

    public void cancelRequests(final Context context,
            final boolean mayInterruptIfRunning) {
        mAsyncClient.cancelRequests(context, mayInterruptIfRunning);
        mSyncClient.cancelRequests(context, mayInterruptIfRunning);
    }

    public void cancelAllRequests(boolean mayInterruptIfRunning) {
        mAsyncClient.cancelAllRequests(mayInterruptIfRunning);
        mSyncClient.cancelAllRequests(mayInterruptIfRunning);
    }

    public void setProxy(String hostname, int port) {
        mAsyncClient.setProxy(hostname, port);
        mSyncClient.setProxy(hostname, port);
    }

    public void setProxy(String hostname, int port, String username,
            String password) {
        mAsyncClient.setProxy(hostname, port, username, password);
        mSyncClient.setProxy(hostname, port, username, password);
    }

    @Override
    public RequestHandleWrapper get(Context context, RESTRequest request) {
        return get(context, request, null);
    }

    @Override
    public RequestHandleWrapper get(Context context, RESTRequest request,
            Map<String, String> headers) {
        RequestHandle requestHandle = mAsyncClient.get(context,
                request.getUrl(), convertHeaders(headers),
                request.getApacheRequestParams(),
                request.getApacheResponseHanlder());

        return new RequestHandleWrapper(requestHandle, request);
    }

    @Override
    public RequestHandleWrapper post(Context context, RESTRequest request) {
        return post(context, request, null);
    }

    @Override
    public RequestHandleWrapper post(Context context, RESTRequest request,
            Map<String, String> headers) {
        RequestHandle requestHandle = mAsyncClient.post(context,
                request.getUrl(), convertHeaders(headers),
                request.getApacheRequestParams(), null,
                request.getApacheResponseHanlder());
        return new RequestHandleWrapper(requestHandle, request);
    }

    @Override
    public RequestHandleWrapper put(Context context, RESTRequest request) {
        return put(context, request, null);
    }

    @Override
    public RequestHandleWrapper put(Context context, RESTRequest request,
            Map<String, String> headers) {
        RequestHandle requestHandle = mAsyncClient.put(context,
                request.getUrl(), convertHeaders(headers),
                request.getApacheRequestParams(), null,
                request.getApacheResponseHanlder());
        return new RequestHandleWrapper(requestHandle, request);
    }

    @Override
    public RequestHandleWrapper delete(Context context, RESTRequest request) {
        return delete(context, request, null);
    }

    @Override
    public RequestHandleWrapper delete(Context context, RESTRequest request,
            Map<String, String> headers) {
        RequestHandle requestHandle = mAsyncClient.delete(context,
                request.getUrl(), convertHeaders(headers),
                request.getApacheRequestParams(),
                request.getApacheResponseHanlder());
        return new RequestHandleWrapper(requestHandle, request);
    }

    @Override
    public BasicJSONResponse getSync(Context context, RESTRequest request) {
        return getSync(context, request, null);
    }

    @Override
    public BasicJSONResponse getSync(Context context, RESTRequest request,
            Map<String, String> headers) {
        mSyncClient.get(context, request.getUrl(), convertHeaders(headers),
                request.getApacheRequestParams(),
                request.getApacheResponseHanlder());
        return request.getBasicJSONResponse();
    }

    @Override
    public BasicJSONResponse postSync(Context context, RESTRequest request) {
        return postSync(context, request, null);
    }

    @Override
    public BasicJSONResponse postSync(Context context, RESTRequest request,
            Map<String, String> headers) {
        mSyncClient.post(context, request.getUrl(), convertHeaders(headers),
                request.getApacheRequestParams(), null,
                request.getApacheResponseHanlder());
        return request.getBasicJSONResponse();
    }

    @Override
    public BasicJSONResponse putSync(Context context, RESTRequest request) {
        return putSync(context, request, null);
    }

    @Override
    public BasicJSONResponse putSync(Context context, RESTRequest request,
            Map<String, String> headers) {
        mSyncClient.put(context, request.getUrl(), convertHeaders(headers),
                request.getApacheRequestParams(), null,
                request.getApacheResponseHanlder());
        return request.getBasicJSONResponse();
    }

    @Override
    public BasicJSONResponse deleteSync(Context context, RESTRequest request) {
        return deleteSync(context, request, null);
    }

    @Override
    public BasicJSONResponse deleteSync(Context context, RESTRequest request,
            Map<String, String> headers) {
        mSyncClient.delete(context, request.getUrl(), convertHeaders(headers),
                request.getApacheRequestParams(),
                request.getApacheResponseHanlder());
        return request.getBasicJSONResponse();
    }

    private MyCookieStore getCookieStore() {
        return (MyCookieStore) mAsyncClient.getHttpContext().getAttribute(
                ClientContext.COOKIE_STORE);
    }

    /**
     * Converts Headers[] to Map<String, String>.
     */
    private static Header[] convertHeaders(Map<String, String> headerParams) {
        if (headerParams == null || headerParams.isEmpty()) {
            return null;
        }
        Header[] headers = new Header[headerParams.size()];
        Set<Entry<String, String>> entries = headerParams.entrySet();
        Iterator<Entry<String, String>> iterator = entries.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            Entry<String, String> entry = iterator.next();
            BasicHeader header = new BasicHeader(entry.getKey(),
                    entry.getValue());
            headers[index++] = header;
        }
        return headers;
    }
}
