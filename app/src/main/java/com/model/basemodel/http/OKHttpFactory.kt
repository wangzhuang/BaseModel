package com.model.basemodel.http


import com.model.basemodel.app.MyApplication
import okhttp3.*
import java.util.concurrent.TimeUnit
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Cookie


/**
 * BaseModel
 * Created by wangchong on 2017/7/14.
 */
object OKHttpFactory {

    val okHttpClient: OkHttpClient


    init {
        val TIMEOUT_READ = 25
        val TIMEOUT_WRITE = 25
        val TIMEOUT_CONNECTION = 25
        // Log信息
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC

        val cache = Cache(MyApplication.instance().cacheDir, 10 * 1024 * 1024)

        okHttpClient = OkHttpClient.Builder()
                //打印请求log
                .addInterceptor(loggingInterceptor)
                //stetho,可以在chrome中查看请求
                .addNetworkInterceptor { chain ->
                    val request = chain.request()
                    val response = chain.proceed(request)
                    /*String cacheControl = request.header("Cache-Control");
                                          if (TextUtils.isEmpty(cacheControl)) {
                                              cacheControl = "public, max-age=60";
                                          }*/
                    //60秒缓存
                    val maxAge = 60
                    return@addNetworkInterceptor response.newBuilder()
                            .removeHeader("Pragma")// 清除头信息，因为服务器如果不支持，会返回一些干扰信息，不清除下面无法生效
                            .removeHeader("Cache-Control")
                            .header("Cache-Control", "public, max-age=" + maxAge)
                            .build()
                }

                //添加header
                .addInterceptor(Interceptor { chain ->
                    val request = chain.request()
                            .newBuilder()
                            .addHeader("Accept", "application/json")
                            .addHeader("app-version", "")
                            .addHeader("app-key", "")
                            .build()
                    return@Interceptor chain.proceed(request)
                })

                //必须是设置Cache目录
                .cache(cache)

                .cookieJar(object : CookieJar {
                    private val cookieStore = mutableMapOf<String?, MutableList<Cookie>?>()
                    override fun saveFromResponse(url: HttpUrl?, cookies: MutableList<Cookie>?) {
                        cookieStore.put(url?.host(), cookies)
                    }

                    override fun loadForRequest(url: HttpUrl?): MutableList<Cookie>? {
                        val cookies = cookieStore[url?.host()]
                        return cookies ?: ArrayList<Cookie>()
                    }
                })

                //失败重连
                .retryOnConnectionFailure(true)

                //time out
                .readTimeout(TIMEOUT_READ.toLong(), TimeUnit.SECONDS)
                .connectTimeout(TIMEOUT_CONNECTION.toLong(), TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_WRITE.toLong(), TimeUnit.SECONDS)
                .build()
    }
}
