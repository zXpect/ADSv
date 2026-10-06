package com.project.ads.retrofit;

import com.project.ads.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cliente HTTP del backend Django de ADS. La URL base viene de
 * BuildConfig.ADS_API_BASE_URL (ver app/build.gradle.kts).
 */
public class AdsApiClient {

    private static IAdsApi sApi;

    private AdsApiClient() {
    }

    public static synchronized IAdsApi getApi() {
        if (sApi == null) {
            FirebaseIdTokenInterceptor auth = new FirebaseIdTokenInterceptor();

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .addInterceptor(auth)
                    .authenticator(auth)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS);

            if (BuildConfig.DEBUG) {
                // BASIC: método, URL y código; nunca headers ni cuerpos (token y datos personales)
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
                builder.addInterceptor(logging);
            }

            sApi = new Retrofit.Builder()
                    .baseUrl(BuildConfig.ADS_API_BASE_URL)
                    .client(builder.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(IAdsApi.class);
        }
        return sApi;
    }
}
