package com.limemobile.app.sdk.http.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.os.Looper;

public class MyApacheHttpClient {
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_RANGE = "Content-Range";
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String ENCODING_GZIP = "gzip";

    public static final int DEFAULT_MAX_CONNECTIONS = 10;
    public static final int DEFAULT_SOCKET_TIMEOUT = 30 * 1000;
    public static final int DEFAULT_MAX_RETRIES = 5;
    public static final int DEFAULT_RETRY_SLEEP_TIME_MILLIS = 1500;
    public static final int DEFAULT_SOCKET_BUFFER_SIZE = 8192;

    private static int maxConnections = DEFAULT_MAX_CONNECTIONS;
    private static int connectTimeout = DEFAULT_SOCKET_TIMEOUT;
    private static int responseTimeout = DEFAULT_SOCKET_TIMEOUT;

    private static boolean enableRelativeRedirects = true;
    private static boolean enableCircularRedirects = true;
    private static boolean enableRedirects = true;

    public static DefaultHttpClient newDefaultHttpClient(String userAgent,
            Context context, CookieStore cookieStore) {
        DefaultHttpClient httpClient = null;
        BasicHttpParams httpParams = new BasicHttpParams();

        ConnManagerParams.setTimeout(httpParams, connectTimeout);
        ConnManagerParams.setMaxConnectionsPerRoute(httpParams,
                new ConnPerRouteBean(maxConnections));
        ConnManagerParams.setMaxTotalConnections(httpParams,
                DEFAULT_MAX_CONNECTIONS);

        HttpConnectionParams.setSoTimeout(httpParams, responseTimeout);
        HttpConnectionParams.setConnectionTimeout(httpParams, connectTimeout);
        HttpConnectionParams.setTcpNoDelay(httpParams, true);
        HttpConnectionParams.setSocketBufferSize(httpParams,
                DEFAULT_SOCKET_BUFFER_SIZE);

        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setUserAgent(httpParams, userAgent);

        int httpPort = 80;
        int httpsPort = 443;

        // Fix to SSL flaw in API < ICS
        // See https://code.google.com/p/android/issues/detail?id=13117
        SSLSocketFactory sslSocketFactory;
        // if (fixNoHttpResponseException) {
        sslSocketFactory = MySSLSocketFactory.getFixedSocketFactory();
        // } else {
        // sslSocketFactory = SSLSocketFactory.getSocketFactory();
        // }

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory
                .getSocketFactory(), httpPort));
        schemeRegistry
                .register(new Scheme("https", sslSocketFactory, httpsPort));
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(
                httpParams, schemeRegistry);

        httpClient = new DefaultHttpClient(cm, httpParams);

        HttpParams params = httpClient.getParams();
        HttpClientParams.setCookiePolicy(params, CookiePolicy.BROWSER_COMPATIBILITY);
        HttpClientParams.setRedirecting(params, true);

        params.setBooleanParameter(ClientPNames.REJECT_RELATIVE_REDIRECT,
                !enableRelativeRedirects);
        params.setBooleanParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS,
                enableCircularRedirects);
        httpClient.setRedirectHandler(new MyRedirectHandler(enableRedirects));

        httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
            @Override
            public void process(HttpRequest request, HttpContext context) {
                if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
                    request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
                }
            }
        });

        httpClient.addResponseInterceptor(new HttpResponseInterceptor() {
            @Override
            public void process(HttpResponse response, HttpContext context) {
                final HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return;
                }
                final Header encoding = entity.getContentEncoding();
                if (encoding != null) {
                    for (HeaderElement element : encoding.getElements()) {
                        if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
                            response.setEntity(new InflatingEntity(entity));
                            break;
                        }
                    }
                }
            }
        });

        httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
            @Override
            public void process(final HttpRequest request,
                    final HttpContext context) throws HttpException,
                    IOException {
                AuthState authState = (AuthState) context
                        .getAttribute(ClientContext.TARGET_AUTH_STATE);
                CredentialsProvider credsProvider = (CredentialsProvider) context
                        .getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context
                        .getAttribute(ExecutionContext.HTTP_TARGET_HOST);

                if (authState.getAuthScheme() == null) {
                    AuthScope authScope = new AuthScope(targetHost
                            .getHostName(), targetHost.getPort());
                    Credentials creds = credsProvider.getCredentials(authScope);
                    if (creds != null) {
                        authState.setAuthScheme(new BasicScheme());
                        authState.setCredentials(creds);
                    }
                }
            }
        }, 0);

        httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
            public void process(HttpRequest request, HttpContext context) {
                // Prevent the HttpRequest from being sent on the main thread
                if (Looper.myLooper() != null
                        && Looper.myLooper() == Looper.getMainLooper()) {
                    throw new RuntimeException(
                            "This thread forbids HTTP requests");
                }
            }
        });

        httpClient.setHttpRequestRetryHandler(new RetryHandler(
                DEFAULT_MAX_RETRIES, DEFAULT_RETRY_SLEEP_TIME_MILLIS));
        httpClient.setCookieStore(cookieStore);

        return httpClient;
    }

    /**
     * Enclosing entity to hold stream of gzip decoded data for accessing
     * HttpEntity contents
     */
    private static class InflatingEntity extends HttpEntityWrapper {
        public InflatingEntity(HttpEntity wrapped) {
            super(wrapped);
        }

        InputStream wrappedStream;
        PushbackInputStream pushbackStream;
        GZIPInputStream gzippedStream;

        @Override
        public InputStream getContent() throws IOException {
            wrappedStream = wrappedEntity.getContent();
            pushbackStream = new PushbackInputStream(wrappedStream, 2);
            if (isInputStreamGZIPCompressed(pushbackStream)) {
                gzippedStream = new GZIPInputStream(pushbackStream);
                return gzippedStream;
            } else {
                return pushbackStream;
            }
        }

        @Override
        public long getContentLength() {
            return -1;
        }

        @Override
        public void consumeContent() throws IOException {
            silentCloseInputStream(wrappedStream);
            silentCloseInputStream(pushbackStream);
            silentCloseInputStream(gzippedStream);
            super.consumeContent();
        }
    }

    /**
     * Checks the InputStream if it contains GZIP compressed data
     * 
     * @param inputStream
     *            InputStream to be checked
     * @return true or false if the stream contains GZIP compressed data
     * @throws java.io.IOException
     */
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

    /**
     * A utility function to close an input stream without raising an exception.
     * 
     * @param is
     *            input stream to close safely
     */
    public static void silentCloseInputStream(InputStream is) {
        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            // Log.w(LOG_TAG, "Cannot close input stream", e);
        }
    }

    /**
     * A utility function to close an output stream without raising an
     * exception.
     * 
     * @param os
     *            output stream to close safely
     */
    public static void silentCloseOutputStream(OutputStream os) {
        try {
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            // Log.w(LOG_TAG, "Cannot close output stream", e);
        }
    }
}
