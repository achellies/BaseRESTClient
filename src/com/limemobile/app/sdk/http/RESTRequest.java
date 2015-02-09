package com.limemobile.app.sdk.http;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;

public abstract class RESTRequest extends BasicRESTRequest {
    protected Map<String, String> mRequestParams;
    protected BasicJSONResponse mBasicJSONResponse;

    protected RequestParams mApacheRequestParams;
    protected JsonHttpResponseHandler mApacheResponseHandler;

    public RESTRequest(String domain, String host, String path,
            Map<String, String> requestParams, JSONResponseListener listener) {
        super(domain, host, path, listener);

        mRequestParams = requestParams;
    }

    public void setRequestParams(Map<String, String> requestParams) {
        mRequestParams = requestParams;
    }

    public Map<String, String> getRequestParams() {
        if (mRequestParams == null) {
            mRequestParams = new HashMap<String, String>();
        }
        return mRequestParams;
    }

    public void setApacheRequestParams(RequestParams params) {
        mApacheRequestParams = params;
    }

    public RequestParams getApacheRequestParams() {
        if (mApacheRequestParams == null) {
            mApacheRequestParams = new RequestParams();
        }
        Map<String, String> params = this.getRequestParams();
        if (params != null && !params.isEmpty()) {
            Set<Entry<String, String>> entries = params.entrySet();

            Iterator<Entry<String, String>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                Entry<String, String> entry = iterator.next();
                mApacheRequestParams.add(entry.getKey(), entry.getValue());
            }

            mRequestParams.clear();
        }
        return mApacheRequestParams;
    }

    public ResponseHandlerInterface getApacheResponseHanlder() {
        if (mApacheResponseHandler == null) {
            mApacheResponseHandler = new JsonHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers,
                        JSONObject json) {
                    // super.onSuccess(statusCode, headers, json);
                    mBasicJSONResponse = constructJSONResponse(statusCode,
                            headers);
                    mBasicJSONResponse.setResponseJSONObject(json);
                    try {
                        RESTRequest.this.parseResponse(mBasicJSONResponse);
                    } catch (JSONException e) {
                        mBasicJSONResponse
                                .setErrorCode(BasicJSONResponse.FAILED);
                        mBasicJSONResponse.setErrorMessage(e.toString());

                        if (!TextUtils.isEmpty(getReadableErrorMessage())) {
                            mBasicJSONResponse
                                    .setErrorMessage(getReadableErrorMessage());
                        }
                    }
                    if (mListener != null) {
                        mListener.onResponse(mBasicJSONResponse);
                    }
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers,
                        JSONArray jsonArray) {
                    // super.onSuccess(statusCode, headers, jsonArray);
                    mBasicJSONResponse = constructJSONResponse(statusCode,
                            headers);
                    mBasicJSONResponse.setErrorCode(BasicJSONResponse.FAILED);
                    mBasicJSONResponse.setErrorMessage(jsonArray.toString());

                    // if (!TextUtils.isEmpty(getReadableErrorMessage())) {
                    // mBasicJSONResponse
                    // .setErrorMessage(getReadableErrorMessage());
                    // }
                    if (mListener != null) {
                        mListener.onResponse(mBasicJSONResponse);
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers,
                        Throwable throwable, JSONObject errorResponse) {
                    // super.onFailure(statusCode, headers, throwable,
                    // errorResponse);
                    mBasicJSONResponse = constructJSONResponse(statusCode,
                            headers);
                    mBasicJSONResponse.setErrorCode(BasicJSONResponse.FAILED);
                    if (throwable != null) {
                        mBasicJSONResponse
                                .setErrorMessage(throwable.toString());
                    } else if (errorResponse != null) {
                        mBasicJSONResponse.setErrorMessage(errorResponse
                                .toString());
                    }

                    if (!TextUtils.isEmpty(getReadableErrorMessage())) {
                        mBasicJSONResponse
                                .setErrorMessage(getReadableErrorMessage());
                    }
                    if (mListener != null) {
                        mListener.onResponse(mBasicJSONResponse);
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers,
                        Throwable throwable, JSONArray errorResponse) {
                    // super.onFailure(statusCode, headers, throwable,
                    // errorResponse);
                    mBasicJSONResponse = constructJSONResponse(statusCode,
                            headers);
                    mBasicJSONResponse.setErrorCode(BasicJSONResponse.FAILED);
                    if (throwable != null) {
                        mBasicJSONResponse
                                .setErrorMessage(throwable.toString());
                    } else if (errorResponse != null) {
                        mBasicJSONResponse.setErrorMessage(errorResponse
                                .toString());
                    }

                    if (!TextUtils.isEmpty(getReadableErrorMessage())) {
                        mBasicJSONResponse
                                .setErrorMessage(getReadableErrorMessage());
                    }
                    if (mListener != null) {
                        mListener.onResponse(mBasicJSONResponse);
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers,
                        String responseString, Throwable throwable) {
                    // super.onFailure(statusCode, headers, responseString,
                    // throwable);
                    mBasicJSONResponse = constructJSONResponse(statusCode,
                            headers);
                    mBasicJSONResponse.setErrorCode(BasicJSONResponse.FAILED);
                    if (throwable != null) {
                        mBasicJSONResponse
                                .setErrorMessage(throwable.toString());
                    } else if (!TextUtils.isEmpty(responseString)) {
                        mBasicJSONResponse.setErrorMessage(responseString);
                    }

                    if (!TextUtils.isEmpty(getReadableErrorMessage())) {
                        mBasicJSONResponse
                                .setErrorMessage(getReadableErrorMessage());
                    }
                    if (mListener != null) {
                        mListener.onResponse(mBasicJSONResponse);
                    }
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers,
                        String responseString) {
                    // super.onSuccess(statusCode, headers, responseString);
                    mBasicJSONResponse = constructJSONResponse(statusCode,
                            headers);
                    mBasicJSONResponse.setErrorCode(BasicJSONResponse.FAILED);
                    mBasicJSONResponse.setErrorMessage(responseString);

                    // if (!TextUtils.isEmpty(getReadableErrorMessage())) {
                    // mBasicJSONResponse
                    // .setErrorMessage(getReadableErrorMessage());
                    // }
                    if (mListener != null) {
                        mListener.onResponse(mBasicJSONResponse);
                    }
                }

            };
        }
        return mApacheResponseHandler;
    }

    public void setBasicJSONResponse(BasicJSONResponse response) {
        mBasicJSONResponse = response;
    }

    public BasicJSONResponse getBasicJSONResponse() {
        return mBasicJSONResponse;
    }

    protected BasicJSONResponse constructJSONResponse(int statusCode,
            Header[] headers) {
        BasicJSONResponse response = new BasicJSONResponse(statusCode, headers);
        return response;
    }
}
