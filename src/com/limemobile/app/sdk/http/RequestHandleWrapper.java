package com.limemobile.app.sdk.http;

import org.json.JSONObject;

import com.android.volley.Request;
import com.loopj.android.http.RequestHandle;

public class RequestHandleWrapper {
    private final Request<JSONObject> mHttpUrlConnectionRequestHandle;
    private final String mHttpUrlConnectionRequestHandleTag;

    private final RequestHandle mApacheRequestHandle;

    public RequestHandleWrapper(
            Request<JSONObject> httpUrlConnectionRequestHandle,
            RESTRequest request) {
        super();
        mHttpUrlConnectionRequestHandle = httpUrlConnectionRequestHandle;
        mHttpUrlConnectionRequestHandleTag = request.getTag();
        mHttpUrlConnectionRequestHandle
                .setTag(mHttpUrlConnectionRequestHandleTag);

        mApacheRequestHandle = null;
    }

    public RequestHandleWrapper(RequestHandle apacheRequestHandle,
            RESTRequest request) {
        super();
        mHttpUrlConnectionRequestHandle = null;
        mHttpUrlConnectionRequestHandleTag = null;

        mApacheRequestHandle = apacheRequestHandle;
    }

    public void cancel() {
        if (mHttpUrlConnectionRequestHandle != null) {
            mHttpUrlConnectionRequestHandle.cancel();
        } else if (mApacheRequestHandle != null) {
            mApacheRequestHandle.cancel(true);
        }
    }

    public boolean isCanceled() {
        if (mHttpUrlConnectionRequestHandle != null) {
            return mHttpUrlConnectionRequestHandle.isCanceled();
        } else if (mApacheRequestHandle != null) {
            return mApacheRequestHandle.isCancelled();
        }
        return true;
    }

    public boolean isFinished() {
        if (mHttpUrlConnectionRequestHandle != null) {
            return mHttpUrlConnectionRequestHandle.hasHadResponseDelivered();
        } else if (mApacheRequestHandle != null) {
            return mApacheRequestHandle.isFinished();
        }
        return true;
    }
}
