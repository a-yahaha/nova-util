package com.xss.common.nova.util;

import com.xss.common.nova.exception.RetrofitException;
import jodd.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import retrofit2.*;
import retrofit2.converter.jackson.JacksonConverterFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BaseRetrofitUtil2 {
    public static Builder newBuilder(String baseUrl) {
        return new Builder(baseUrl);
    }

    public static class Builder {
        private String baseUrl;
        private Map<String, String> headers = new HashMap<>();
        private List<Interceptor> interceptors = new ArrayList<>();
        private Integer connectTimeout = 5000;//ms
        private Integer writeTimeout = 5000;//ms
        private Integer readTimeout = 10000;//ms
        private Integer retryTimes = 0;
        private Integer timeBetweenRetry = 0;//ms
        private Proxy proxy;
        private List<Converter.Factory> factories = new ArrayList<>(0);

        private Builder(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        private static OkHttpClient.Builder defaultClientBuilder() {
            SSLContext sslContext = null;
            try {
                sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, new TrustManager[]{new MyTrustManager()}, new SecureRandom());
            } catch (Throwable e) {
                log.error("初始化SSLContext异常", e);
            }

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
            if (sslContext != null) {
                clientBuilder.sslSocketFactory(sslContext.getSocketFactory());
            }
            clientBuilder.hostnameVerifier(new NoopHostnameVerifier());
            clientBuilder.connectionPool(new ConnectionPool());
            return clientBuilder;
        }

        public Builder headers(Map<String, String> headers) {
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }

        public Builder addInterceptors(Interceptor... interceptors) {
            if (interceptors != null) {
                for (Interceptor interceptor : interceptors) {
                    this.interceptors.add(interceptor);
                }
            }
            return this;
        }

        public Builder connectTimeout(Integer connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder writeTimeout(Integer writeTimeout) {
            this.writeTimeout = writeTimeout;
            return this;
        }

        public Builder readTimeout(Integer readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder retryWhenTimeout(Integer retryTimes) {
            this.retryTimes = retryTimes;
            return this;
        }

        public Builder timeBetweenRetry(Integer timeBetweenRetry) {
            this.timeBetweenRetry = timeBetweenRetry;
            return this;
        }

        public Builder proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder addConverterFactory(Converter.Factory... factories) {
            if (BaseCollectionUtil.isNotEmpty(factories)) {
                this.factories.addAll(Arrays.asList(factories));
            }
            return this;
        }

        public Retrofit build() {
            //添加用户自定义header
            OkHttpClient.Builder clientBuilder = defaultClientBuilder();
            if (headers != null && !headers.isEmpty()) {
                clientBuilder.addInterceptor((chain) -> {
                    Request.Builder requestBuilder = chain.request().newBuilder();
                    headers.entrySet().forEach(entry -> requestBuilder.addHeader(entry.getKey(), entry.getValue()));
                    return chain.proceed(requestBuilder.build());
                });
            }

            //添加用户自定义interceptor
            if (CollectionUtils.isNotEmpty(interceptors)) {
                interceptors.forEach(interceptor -> clientBuilder.addInterceptor(interceptor));
            }

            //添加基本interceptor对非200响应及异常进行处理
            clientBuilder.addInterceptor(chain -> {
                Request request = chain.request();
                okhttp3.Response response = null;
                IOException ex = null;

                //执行http请求, 超时时重试
                int tryCount = 0;
                boolean isTimeout = true;
                while (isTimeout && tryCount <= retryTimes) {
                    try {
                        response = chain.proceed(request);
                        isTimeout = false;
                        ex = null;
                    } catch (IOException e) {
                        ex = e;
                        if (!(e instanceof SocketTimeoutException)) {
                            isTimeout = false;
                        } else if (tryCount < retryTimes) {
                            log.info("超时,第{}次重试...", (tryCount + 1));
                            request = request.newBuilder().build();
                            if (timeBetweenRetry > 0) {
                                ThreadUtil.sleep(timeBetweenRetry);
                            }
                        }
                    } finally {
                        tryCount++;
                    }
                }

                //异常处理
                if (ex != null) {
                    if (ex instanceof SocketTimeoutException) {
                        log.warn("调用第三方接口[{}]超时", request.url().toString());
                        throw ex;
                    }

                    log.error("调用第三方接口[{}]异常:{}", request.url().toString(), ExceptionUtils.getStackTrace(ex));
                    throw new RetrofitException(500, ex.getMessage());
                }

                if (response == null || response.body() == null || response.body().contentLength() == 0) {
                    String errorMsg = String.format("调用第三方接口[%s]响应为空", request.url().toString());
                    throw new RetrofitException(500, errorMsg);
                }

                if (response.code() < 200 || response.code() >= 300) {
                    String body = response.body().string();
                    log.warn("请求第三方api[{}]响应异常:{}", request.url().toString(), body);
                    IOUtils.closeQuietly(response);
                    throw new RetrofitException(response.code(), body);
                }
                return response;
            });

            //设置http代理
            if (proxy != null) {
                clientBuilder.proxy(proxy);
            }

            //设置http超时时间
            clientBuilder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                    .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                    .readTimeout(readTimeout, TimeUnit.MILLISECONDS);

            //返回
            Retrofit.Builder builder = new Retrofit.Builder().baseUrl(baseUrl);
            if (BaseCollectionUtil.isNotEmpty(factories)) {
                factories.forEach(factory -> builder.addConverterFactory(factory));
            }

            return builder//.addConverterFactory(SimpleXmlConverterFactory.create())
                    .addConverterFactory(JacksonConverterFactory.create())
                    .addCallAdapterFactory(SynchronousCallAdapterFactory.create())
                    .client(clientBuilder.build()).build();
        }
    }

    static class MyTrustManager extends X509ExtendedTrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s, Socket socket)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s, Socket socket)
                throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine)
                throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    static class SynchronousCallAdapterFactory extends CallAdapter.Factory {
        public static CallAdapter.Factory create() {
            return new SynchronousCallAdapterFactory();
        }

        @Override
        public CallAdapter<Object> get(final Type returnType, Annotation[] annotations, Retrofit retrofit) {
            if (returnType.getTypeName().contains("retrofit2.Call")) {
                return null;
            }

            return new CallAdapter<Object>() {
                @Override
                public Type responseType() {
                    return returnType;
                }

                @Override
                public <R> Object adapt(Call<R> call) {
                    try {
                        Response response = call.execute();
                        if (returnType.getTypeName().contains("retrofit2.Response")) {
                            return response;
                        }

                        if (response.body() != null) {
                            return response.body();
                        } else {
                            return BaseJsonUtil.readValue(response.errorBody().string(), Map.class);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
    }
}
