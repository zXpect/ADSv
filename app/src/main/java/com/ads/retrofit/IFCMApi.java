package com.ads.retrofit;

import com.ads.models.FCMBody;
import com.ads.models.FCMResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMApi {
    @Headers({
            "Content-Type:application/json"
    })
    @POST("v1/projects/adsv-d87e1/messages:send")
    Call<FCMResponse> send(@Body FCMBody body);
}