package com.limemobile.app.sdk.http.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.text.TextUtils;

import com.limemobile.app.sdk.http.BasicJSONResponse;
import com.limemobile.app.sdk.http.RESTRequest;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

public class VolleyJSONRequest extends Request<JSONObject> implements
        RetryPolicy {
    private static final String UTF8_BOM = "\uFEFF";
    // private static final String SET_COOKIE_KEY = "Set-Cookie";
    // private static final String COOKIE_KEY = "Cookie";
    private static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
    // private static final String HEADER_CONTENT_DISPOSITION =
    // "Content-Disposition";
    private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    private static final String ENCODING_GZIP = "gzip";

    protected final Context mContext;
    protected final Map<String, String> mHeaders;
    protected final RESTRequest mBasicRequest;

    protected String mRedirectUrl;

    /** The current timeout in milliseconds. */
    private int mCurrentTimeoutMs;

    /** The current retry count. */
    private int mCurrentRetryCount;

    /** The maximum number of attempts. */
    private final int mMaxNumRetries;

    /** The backoff multiplier for for the policy. */
    private final float mBackoffMultiplier;

    /** The default socket timeout in milliseconds */
    public static final int DEFAULT_TIMEOUT_MS = 2500;

    /** The default number of retries */
    public static final int DEFAULT_MAX_RETRIES = 1;

    /** The default backoff multiplier */
    public static final float DEFAULT_BACKOFF_MULT = 1f;

    public VolleyJSONRequest(Context context, int method,
            Map<String, String> headers, RESTRequest request) {
        super(method, request.getUrl(), new ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        this.mContext = context;
        this.mBasicRequest = request;
        this.mHeaders = headers;

        this.mCurrentTimeoutMs = mBasicRequest.getTimeoutMs();
        this.mMaxNumRetries = mBasicRequest.getRetryCount();
        this.mBackoffMultiplier = DEFAULT_BACKOFF_MULT;

        this.setShouldCache(this.mBasicRequest.shouldCache());
        this.setRetryPolicy(this);
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return mBasicRequest.getRequestParams();
    }

    @Override
    public String getUrl() {
        if (!TextUtils.isEmpty(mRedirectUrl)) {
            return mRedirectUrl;
        }
        String url = mBasicRequest.getUrl();
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(url);
        Map<String, String> requestParams = mBasicRequest.getRequestParams();
        if ((Request.Method.GET == getMethod() || Request.Method.DELETE == getMethod())
                && requestParams != null && !requestParams.isEmpty()) {
            if (url.contains("?")) {
                if (!url.endsWith("&")) {
                    urlBuilder.append("&");
                }
            } else {
                urlBuilder.append("?");
            }
            Set<Map.Entry<String, String>> set = requestParams.entrySet();
            Iterator<Map.Entry<String, String>> iterator = set.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                urlBuilder.append(entry.getKey());
                urlBuilder.append("=");
                urlBuilder.append(entry.getValue());
                if (iterator.hasNext()) {
                    urlBuilder.append("&");
                }
            }
        }
        return urlBuilder.toString();
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = mHeaders != null ? mHeaders
                : new HashMap<String, String>();

        headers.put(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
        return headers;
    }

    @Override
    protected void deliverResponse(JSONObject response) {
        if (mBasicRequest != null
                && mBasicRequest.getJSONResponseListener() != null
                && mBasicRequest.getBasicJSONResponse() != null) {
            mBasicRequest.getJSONResponseListener().onResponse(
                    mBasicRequest.getBasicJSONResponse());
        }
    }

    @Override
    public void deliverError(VolleyError error) {
        if (mBasicRequest.getBasicJSONResponse() == null && error != null
                && error.networkResponse == null) {
            BasicJSONResponse response = new BasicJSONResponse(
                    BasicJSONResponse.FAILED, (Header[]) null);
            mBasicRequest.setBasicJSONResponse(response);
            response.setErrorCode(BasicJSONResponse.FAILED);

            String msg = error.getMessage();
            // if (PayApplication.getInstance() != null) {
            // msg = PayApplication.getInstance().getContext()
            // .getString(R.string.errcode_network_response_timeout);
            // }
            response.setErrorMessage(msg);
        }
        if (mBasicRequest != null
                && mBasicRequest.getJSONResponseListener() != null
                && mBasicRequest.getBasicJSONResponse() != null) {
            mBasicRequest.getJSONResponseListener().onResponse(
                    mBasicRequest.getBasicJSONResponse());
        }
    }

    @Override
    public Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        int statusCode = response.statusCode;
        Map<String, String> headers = response.headers;
        BasicJSONResponse jsonResponse = new BasicJSONResponse(statusCode,
                headers);
        if (mBasicRequest != null) {
            mBasicRequest.setBasicJSONResponse(jsonResponse);
        }

        JSONObject jsonObject = null;
        try {
            boolean gzip = false;
            if (headers != null) {
                Set<Entry<String, String>> entries = headers.entrySet();
                Iterator<Entry<String, String>> iterator = entries.iterator();

                while (iterator.hasNext()) {
                    Entry<String, String> entry = iterator.next();

                    if (HEADER_CONTENT_ENCODING.equals(entry.getKey())
                            && ENCODING_GZIP.equals(entry.getValue())) {
                        gzip = true;
                        break;
                    }
                }
            }
            String jsonString = null;
            if (gzip) {
                ByteArrayInputStream bais = new ByteArrayInputStream(
                        response.data);

                PushbackInputStream pushbackStream = new PushbackInputStream(
                        bais, 2);
                InputStream is = null;
                try {
                    if (isInputStreamGZIPCompressed(pushbackStream)) {
                        is = new GZIPInputStream(pushbackStream);
                    } else {
                        is = pushbackStream;
                    }
                } catch (IOException e1) {
                }
                try {
                    jsonString = readToString(is,
                            HttpHeaderParser.parseCharset(response.headers));
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        is = null;
                    } catch (IOException e) {
                    }
                }
            } else {
                jsonString = new String(response.data,
                        HttpHeaderParser.parseCharset(response.headers));
            }

            Object result = null;
            if (!TextUtils.isEmpty(jsonString)) {
                jsonString = jsonString.trim();
                if (jsonString.startsWith(UTF8_BOM)) {
                    jsonString = jsonString.substring(1);
                }
                if (jsonString.startsWith("{") || jsonString.startsWith("[")) {
                    try {
                        result = new JSONTokener(jsonString).nextValue();
                    } catch (JSONException e) {
                        jsonResponse.setErrorCode(BasicJSONResponse.FAILED);
                        jsonResponse.setErrorMessage(e.toString());

                        // if (!TextUtils.isEmpty(mBasicRequest
                        // .getReadableErrorMessage())) {
                        // jsonResponse.setErrorMessage(mBasicRequest
                        // .getReadableErrorMessage());
                        // }
                    }
                }
            }
            if (result != null) {
                if (result instanceof JSONObject) {
                    jsonObject = (JSONObject) result;
                    jsonResponse.setResponseJSONObject(jsonObject);
                    if (mBasicRequest != null) {
                        try {
                            mBasicRequest.parseResponse(jsonResponse);
                        } catch (JSONException e) {
                            jsonResponse.setErrorCode(BasicJSONResponse.FAILED);
                            jsonResponse.setErrorMessage(e.toString());

                            if (!TextUtils.isEmpty(mBasicRequest
                                    .getReadableErrorMessage())) {
                                jsonResponse.setErrorMessage(mBasicRequest
                                        .getReadableErrorMessage());
                            }
                            return Response.error(new ParseError(response));
                        }
                        return Response.success(jsonObject,
                                HttpHeaderParser.parseCacheHeaders(response));
                    } else {
                        return Response.success(jsonObject,
                                HttpHeaderParser.parseCacheHeaders(response));
                    }
                } else if (result instanceof JSONArray) {
                    jsonResponse.setErrorCode(BasicJSONResponse.FAILED);
                    jsonResponse.setErrorMessage(((JSONArray) result)
                            .toString());

                    // if (!TextUtils.isEmpty(mBasicRequest
                    // .getReadableErrorMessage())) {
                    // jsonResponse.setErrorMessage(mBasicRequest
                    // .getReadableErrorMessage());
                    // }
                    return Response.error(new ParseError(response));
                } else if (result instanceof String) {
                    jsonResponse.setErrorCode(BasicJSONResponse.FAILED);
                    jsonResponse.setErrorMessage(((String) result));

                    // if (!TextUtils.isEmpty(mBasicRequest
                    // .getReadableErrorMessage())) {
                    // jsonResponse.setErrorMessage(mBasicRequest
                    // .getReadableErrorMessage());
                    // }
                    return Response.error(new ParseError(response));
                } else {
                    jsonResponse.setErrorCode(BasicJSONResponse.FAILED);
                    jsonResponse.setErrorMessage(result.toString());

                    // if (!TextUtils.isEmpty(mBasicRequest
                    // .getReadableErrorMessage())) {
                    // jsonResponse.setErrorMessage(mBasicRequest
                    // .getReadableErrorMessage());
                    // }
                    return Response.error(new ParseError(response));
                }
            } else {
                jsonResponse.setErrorCode(BasicJSONResponse.FAILED);
                jsonResponse.setErrorMessage(jsonString);

                // if
                // (!TextUtils.isEmpty(mBasicRequest.getReadableErrorMessage()))
                // {
                // jsonResponse.setErrorMessage(mBasicRequest
                // .getReadableErrorMessage());
                // }
                return Response.error(new ParseError(response));
            }

        } catch (UnsupportedEncodingException e) {
            jsonResponse.setErrorCode(BasicJSONResponse.FAILED);
            jsonResponse.setErrorMessage(e.toString());

            if (!TextUtils.isEmpty(mBasicRequest.getReadableErrorMessage())) {
                jsonResponse.setErrorMessage(mBasicRequest
                        .getReadableErrorMessage());
            }
            return Response.error(new ParseError(e));
        }
    }

    @Override
    public VolleyError parseNetworkError(VolleyError volleyError) {
        if (volleyError.networkResponse == null) {
            BasicJSONResponse response = new BasicJSONResponse(
                    BasicJSONResponse.FAILED, new HashMap<String, String>());
            if (mBasicRequest != null) {
                mBasicRequest.setBasicJSONResponse(response);
            }

            String msg = volleyError.toString();
            response.setErrorMessage(msg);

            if (!TextUtils.isEmpty(mBasicRequest.getReadableErrorMessage())) {
                response.setErrorMessage(mBasicRequest
                        .getReadableErrorMessage());
            }
        } else {
            String responseString = null;

            boolean gzip = false;
            if (volleyError.networkResponse != null
                    && volleyError.networkResponse.data != null
                    && volleyError.networkResponse.headers != null) {
                Set<Entry<String, String>> entries = volleyError.networkResponse.headers
                        .entrySet();
                Iterator<Entry<String, String>> iterator = entries.iterator();

                while (iterator.hasNext()) {
                    Entry<String, String> entry = iterator.next();

                    if (HEADER_CONTENT_ENCODING.equals(entry.getKey())
                            && ENCODING_GZIP.equals(entry.getValue())) {
                        gzip = true;
                        break;
                    }
                }
            }
            if (gzip) {
                ByteArrayInputStream bais = new ByteArrayInputStream(
                        volleyError.networkResponse.data);

                PushbackInputStream pushbackStream = new PushbackInputStream(
                        bais, 2);
                InputStream is = null;
                try {
                    if (isInputStreamGZIPCompressed(pushbackStream)) {
                        is = new GZIPInputStream(pushbackStream);
                    } else {
                        is = pushbackStream;
                    }
                } catch (IOException e1) {
                }
                try {
                    responseString = readToString(
                            is,
                            HttpHeaderParser
                                    .parseCharset(volleyError.networkResponse.headers));
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        is = null;
                    } catch (IOException e) {
                    }
                }
            } else {
                try {
                    responseString = new String(
                            volleyError.networkResponse.data,
                            HttpHeaderParser
                                    .parseCharset(volleyError.networkResponse.headers));
                } catch (UnsupportedEncodingException e) {
                }
            }
            BasicJSONResponse response = new BasicJSONResponse(
                    volleyError.networkResponse.statusCode,
                    volleyError.networkResponse.headers);
            if (mBasicRequest != null) {
                mBasicRequest.setBasicJSONResponse(response);
            }
            response.setErrorCode(BasicJSONResponse.FAILED);
            Object result = null;
            if (!TextUtils.isEmpty(responseString)) {
                responseString = responseString.trim();
                if (responseString.startsWith(UTF8_BOM)) {
                    responseString = responseString.substring(1);
                }
                if (responseString.startsWith("{")
                        || responseString.startsWith("[")) {
                    try {
                        result = new JSONTokener(responseString).nextValue();
                    } catch (JSONException e) {
                        response.setErrorCode(BasicJSONResponse.FAILED);
                        response.setErrorMessage(e.toString());

                        if (!TextUtils.isEmpty(mBasicRequest
                                .getReadableErrorMessage())) {
                            response.setErrorMessage(mBasicRequest
                                    .getReadableErrorMessage());
                        }
                    }
                }
            }

            if (result != null) {
                JSONObject jsonObject = null;
                if (result instanceof JSONObject) {
                    jsonObject = (JSONObject) result;
                    response.setResponseJSONObject(jsonObject);
                    if (mBasicRequest != null) {
                        try {
                            mBasicRequest.parseResponse(response);
                        } catch (JSONException e) {
                            response.setErrorCode(BasicJSONResponse.FAILED);
                            response.setErrorMessage(e.toString());
                        }
                    }
                } else if (result instanceof JSONArray) {
                    response.setErrorCode(BasicJSONResponse.FAILED);
                    response.setErrorMessage(((JSONArray) result).toString());

                    // if (!TextUtils.isEmpty(mBasicRequest
                    // .getReadableErrorMessage())) {
                    // response.setErrorMessage(mBasicRequest
                    // .getReadableErrorMessage());
                    // }
                } else if (result instanceof String) {
                    response.setErrorCode(BasicJSONResponse.FAILED);
                    response.setErrorMessage(((String) result));

                    // if (!TextUtils.isEmpty(mBasicRequest
                    // .getReadableErrorMessage())) {
                    // response.setErrorMessage(mBasicRequest
                    // .getReadableErrorMessage());
                    // }
                } else {
                    response.setErrorCode(BasicJSONResponse.FAILED);
                    response.setErrorMessage(result.toString());

                    // if (!TextUtils.isEmpty(mBasicRequest
                    // .getReadableErrorMessage())) {
                    // response.setErrorMessage(mBasicRequest
                    // .getReadableErrorMessage());
                    // }
                }
            } else {
                response.setErrorMessage(String.format(
                        "statusCode = %d, response = %s",
                        volleyError.networkResponse.statusCode, responseString));

                // if
                // (!TextUtils.isEmpty(mBasicRequest.getReadableErrorMessage()))
                // {
                // response.setErrorMessage(mBasicRequest
                // .getReadableErrorMessage());
                // }
            }
        }
        return volleyError;
    }

    @Override
    public int getCurrentTimeout() {
        return mCurrentTimeoutMs;
    }

    @Override
    public int getCurrentRetryCount() {
        return mCurrentRetryCount;
    }

    @Override
    public void retry(VolleyError error) throws VolleyError {
        mCurrentRetryCount++;
        mCurrentTimeoutMs += (mCurrentTimeoutMs * mBackoffMultiplier);
        if (!hasAttemptRemaining()) {
            throw error;
        }
        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            if (statusCode == HttpStatus.SC_UNAUTHORIZED
                    || statusCode == HttpStatus.SC_FORBIDDEN) {
                throw error;
            }

            if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY
                    || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                String url = error.networkResponse.headers.get("location");
                if (!TextUtils.isEmpty(url)) {
                    mRedirectUrl = url;
                }
            }
        }
    }

    public BasicJSONResponse getBasicJSONResponse() {
        return (mBasicRequest != null) ? mBasicRequest.getBasicJSONResponse()
                : null;
    }

    protected boolean hasAttemptRemaining() {
        return mCurrentRetryCount <= mMaxNumRetries;
    }

    protected BasicJSONResponse parseJSONResponse(int statusCode,
            Header[] headers) {
        BasicJSONResponse response = new BasicJSONResponse(statusCode, headers);
        return response;
    }

    @SuppressWarnings("unused")
    private BasicClientCookie parseRawCookie(String rawCookie) {
        String[] rawCookieParams = rawCookie.split(";");

        String[] rawCookieNameAndValue = rawCookieParams[0].split("=");
        if (rawCookieNameAndValue.length != 2) {
            // throw new Exception("Invalid cookie: missing name and value.");
            return null;
        }

        String cookieName = rawCookieNameAndValue[0].trim();
        String cookieValue = rawCookieNameAndValue[1].trim();
        BasicClientCookie cookie = new BasicClientCookie(cookieName,
                cookieValue);
        for (int i = 1; i < rawCookieParams.length; i++) {
            String rawCookieParamNameAndValue[] = rawCookieParams[i].trim()
                    .split("=");

            String paramName = rawCookieParamNameAndValue[0].trim();

            if (paramName.equalsIgnoreCase("secure")) {
                cookie.setSecure(true);
            } else {
                if (rawCookieParamNameAndValue.length != 2) {
                    // throw new Exception(
                    // "Invalid cookie: attribute not a flag or missing value.");
                    return null;
                }

                String paramValue = rawCookieParamNameAndValue[1].trim();

                if (paramName.equalsIgnoreCase("expires")) {
                    Date expiryDate = null;
                    try {
                        expiryDate = DateFormat.getDateTimeInstance(
                                DateFormat.FULL, DateFormat.FULL).parse(
                                paramValue);
                        cookie.setExpiryDate(expiryDate);
                    } catch (ParseException e) {
                    }
                } else if (paramName.equalsIgnoreCase("max-age")) {
                    long maxAge = Long.parseLong(paramValue);
                    Date expiryDate = new Date(System.currentTimeMillis()
                            + maxAge);
                    cookie.setExpiryDate(expiryDate);
                } else if (paramName.equalsIgnoreCase("domain")) {
                    cookie.setDomain(paramValue);
                } else if (paramName.equalsIgnoreCase("path")) {
                    cookie.setPath(paramValue);
                } else if (paramName.equalsIgnoreCase("comment")) {
                    cookie.setComment(paramValue);
                } else {
                    // throw new
                    // Exception("Invalid cookie: invalid attribute name.");
                }
            }
        }

        return cookie;
    }

    public static String readToString(InputStream is, String charsetName)
            throws IOException {
        byte[] data = readToByteArray(is);
        return new String(data, charsetName);
    }

    public static byte[] readToByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        readToStream(is, baos);
        return baos.toByteArray();
    }

    public static void readToStream(InputStream is, OutputStream os)
            throws IOException {
        byte[] buffer = new byte[8 * 1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }

    public static boolean isInputStreamGZIPCompressed(
            final PushbackInputStream inputStream) throws IOException {
        if (inputStream == null)
            return false;

        byte[] signature = new byte[2];
        int readStatus = inputStream.read(signature);
        inputStream.unread(signature);
        int streamHeader = ((int) signature[0] & 0xff)
                | ((signature[1] << 8) & 0xff00);
        return readStatus == 2 && GZIPInputStream.GZIP_MAGIC == streamHeader;
    }
}
