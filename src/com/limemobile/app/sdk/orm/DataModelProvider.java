package com.limemobile.app.sdk.orm;

import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.limemobile.app.sdk.http.BasicJSONResponse;
import com.limemobile.app.sdk.http.HttpURLConnectionRESTClient;
import com.limemobile.app.sdk.http.JSONResponseListener;
import com.limemobile.app.sdk.http.RESTRequest;
import com.limemobile.app.sdk.http.internal.HttpUtils;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.dao.query.WhereCondition;

public class DataModelProvider<T> {
    protected final Context mContext;

    protected final AbstractDao<T, Long> mDao;

    protected final HttpURLConnectionRESTClient mHttpClient;

    protected final AbstractJSONParser<T> mJSONPaserModel;

    protected final DataModelFetchListener<T> mListener;

    public DataModelProvider(Context context, AbstractDao<T, Long> dao,
            Class<T> clazz, AbstractJSONParser<T> parser,
            DataModelFetchListener<T> listener) {
        super();
        mContext = context;

        mDao = dao;

        mHttpClient = new HttpURLConnectionRESTClient(context);

        mJSONPaserModel = parser;

        mListener = listener;
    }

    /*
     * QueryBuilder qb = userDao.queryBuilder();
     * qb.where(Properties.FirstName.eq("Joe"),
     * qb.or(Properties.YearOfBirth.gt(1970),
     * qb.and(Properties.YearOfBirth.eq(1970),
     * Properties.MonthOfBirth.ge(10)))); List youngJoes = qb.list();
     * 
     * 
     * Select * from xxx where xxxx Order by xxxx offset xxx limit xxx
     */
    public void query(Context context, RESTRequest api, int offset, int limit,
            Property orderProperty, String customOrderForProperty,
            WhereCondition cond, WhereCondition... condMore) {
        queryImpl(context, api, false, offset, limit, orderProperty,
                customOrderForProperty, cond, condMore);
    }

    public void queryMore(Context context, RESTRequest api, int offset,
            int limit, Property orderProperty, String customOrderForProperty,
            WhereCondition cond, WhereCondition... condMore) {
        queryImpl(context, api, true, offset, limit, orderProperty,
                customOrderForProperty, cond, condMore);
    }

    private void queryImpl(Context context, RESTRequest api,
            final boolean loadMore, int offset, int limit,
            Property orderProperty, String customOrderForProperty,
            WhereCondition cond, WhereCondition... condMore) {
        final QueryBuilder<T> queryBuilder = mDao.queryBuilder();
        final QueryBuilder<T> allQueryBuilder = mDao.queryBuilder();
        queryBuilder.offset(offset);
        queryBuilder.limit(limit);
        if (cond != null) {
            queryBuilder.where(cond, condMore);
            allQueryBuilder.where(cond, condMore);
        }

        if (orderProperty != null && !TextUtils.isEmpty(customOrderForProperty)) {
            queryBuilder.orderCustom(orderProperty, customOrderForProperty);
        }

        boolean isNetworkAvaliable = HttpUtils.isNetworkAvaliable(mContext);

        List<T> entities = queryBuilder.list();
        if (!loadMore && entities != null && !entities.isEmpty()
                && isNetworkAvaliable
                && mJSONPaserModel.isCacheExpired(entities)) {

            mListener.onQueryFinish(
                    DataModelFetchListener.SUCCESS_WITH_EXPIRED_DATA, entities,
                    null);

            // 有网情况下并且不是LoadMore时，如果缓存过期的话清理缓存
            mDao.deleteInTx(allQueryBuilder.list());
            entities.clear();
        }
        if (entities != null && !entities.isEmpty()) {
            if (loadMore) {
                mListener.onQueryMoreFinish(DataModelFetchListener.SUCCESS,
                        entities, null);
            } else {
                mListener.onQueryFinish(DataModelFetchListener.SUCCESS,
                        entities, null);
            }
            return;
        } else {
            if (!isNetworkAvaliable) {
                if (loadMore) {
                    mListener
                            .onQueryMoreFinish(
                                    DataModelFetchListener.FAILED_WITH_NETOWRK_NOT_AVALIABLE,
                                    entities, null);
                } else {
                    mListener
                            .onQueryFinish(
                                    DataModelFetchListener.FAILED_WITH_NETOWRK_NOT_AVALIABLE,
                                    entities, null);
                }
                return;
            }
            api.setResponseHandler(new JSONResponseListener() {

                @Override
                public void onResponse(BasicJSONResponse response) {
                    if (BasicJSONResponse.SUCCESS == response.getErrorCode()) {
                        List<T> entities = mJSONPaserModel
                                .parseObjects(response.getJSONObject());
                        mDao.insertInTx(entities);
                        entities = queryBuilder.list();

                        if (loadMore) {
                            mListener.onQueryMoreFinish(
                                    DataModelFetchListener.SUCCESS, entities,
                                    response);
                        } else {
                            mListener.onQueryFinish(
                                    DataModelFetchListener.SUCCESS, entities,
                                    response);
                        }
                    } else {
                        if (loadMore) {
                            mListener.onQueryMoreFinish(
                                    DataModelFetchListener.FAILED, null,
                                    response);
                        } else {
                            mListener.onQueryFinish(
                                    DataModelFetchListener.FAILED, null,
                                    response);
                        }
                    }
                }

            });
            mHttpClient.get(mContext, api);
        }
    }

    public void update(Context context, RESTRequest api, int offset, int limit,
            Property orderProperty, String customOrderForProperty,
            WhereCondition cond, WhereCondition... condMore) {
        final QueryBuilder<T> queryBuilder = mDao.queryBuilder();
        final QueryBuilder<T> allQueryBuilder = mDao.queryBuilder();
        queryBuilder.offset(offset);
        queryBuilder.limit(limit);
        if (cond != null) {
            queryBuilder.where(cond, condMore);
            allQueryBuilder.where(cond, condMore);
        }

        if (orderProperty != null && !TextUtils.isEmpty(customOrderForProperty)) {
            queryBuilder.orderCustom(orderProperty, customOrderForProperty);
        }

        if (!HttpUtils.isNetworkAvaliable(mContext)) {
            mListener.onUpdateFinish(
                    DataModelFetchListener.FAILED_WITH_NETOWRK_NOT_AVALIABLE,
                    null, null);
            return;
        }
        api.setResponseHandler(new JSONResponseListener() {

            @Override
            public void onResponse(BasicJSONResponse response) {
                if (BasicJSONResponse.SUCCESS == response.getErrorCode()) {
                    List<T> entities = mJSONPaserModel.parseObjects(response
                            .getJSONObject());
                    mDao.deleteInTx(allQueryBuilder.list());
                    mDao.insertInTx(entities);
                    entities = queryBuilder.list();

                    mListener.onUpdateFinish(DataModelFetchListener.SUCCESS,
                            entities, response);
                } else {
                    mListener.onUpdateFinish(DataModelFetchListener.FAILED,
                            null, response);
                }
            }

        });
        mHttpClient.get(mContext, api);
    }

    public AbstractDao<T, Long> getDao() {
        return mDao;
    }

    /**
     * Avoid leaks by using a non-anonymous handler class.
     */
    @SuppressWarnings("unused")
    private static class ResponderHandler<T> extends Handler {
        private final DataModelProvider<T> mProvider;

        public ResponderHandler(DataModelProvider<T> provider, Looper looper) {
            super(looper);
            mProvider = provider;
        }

        @Override
        public void handleMessage(Message msg) {
        }
    }
}
