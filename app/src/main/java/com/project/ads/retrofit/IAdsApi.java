package com.project.ads.retrofit;

import com.project.ads.models.Pqr;
import com.project.ads.models.api.ApiBodies;
import com.project.ads.models.api.ApiResponse;
import com.project.ads.models.api.CatalogData;
import com.project.ads.models.api.NotificationResult;
import com.project.ads.models.api.PaymentData;
import com.project.ads.models.api.QuoteData;
import com.project.ads.models.api.QuoteLimits;
import com.project.ads.models.api.QuoteReference;
import com.project.ads.models.api.RatingData;
import com.project.ads.models.api.RequestActionData;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Endpoints del backend Django usados por la app (ver README del backend,
 * sección "Pagos, cotización y API de la app móvil").
 */
public interface IAdsApi {

    // ---------- Catálogo y solicitudes ----------

    @GET("api/catalog/")
    Call<ApiResponse<CatalogData>> getCatalog();

    @POST("api/requests/estimate/")
    Call<ApiResponse<QuoteReference>> estimate(@Body ApiBodies.EstimateBody body);

    @POST("api/requests/")
    Call<ApiResponse<RequestActionData>> createRequest(@Body ApiBodies.CreateRequestBody body);

    @POST("api/requests/{requestId}/schedule/")
    Call<ApiResponse<RequestActionData>> reschedule(@Path("requestId") String requestId,
                                                    @Body ApiBodies.ScheduleBody body);

    @POST("api/requests/{requestId}/cancel/")
    Call<ApiResponse<RequestActionData>> cancelRequest(@Path("requestId") String requestId,
                                                       @Body ApiBodies.CancelBody body);

    // ---------- Cotización ----------

    @GET("api/requests/quote-limits/")
    Call<ApiResponse<QuoteLimits>> getQuoteLimits(@Query("service_type") String serviceType);

    @POST("api/requests/{requestId}/quote/")
    Call<ApiResponse<QuoteData>> submitQuote(@Path("requestId") String requestId,
                                             @Body ApiBodies.QuoteBody body);

    @POST("api/requests/{requestId}/quote/accept/")
    Call<ApiResponse<QuoteData>> acceptQuote(@Path("requestId") String requestId);

    @POST("api/requests/{requestId}/quote/reject/")
    Call<ApiResponse<QuoteData>> rejectQuote(@Path("requestId") String requestId,
                                             @Body ApiBodies.RejectQuoteBody body);

    // ---------- Calificación ----------

    @POST("api/requests/{requestId}/rating/")
    Call<ApiResponse<RatingData>> rateService(@Path("requestId") String requestId,
                                              @Body ApiBodies.RatingBody body);

    // ---------- Pagos ----------

    @POST("api/payments/")
    Call<ApiResponse<PaymentData>> createPayment(@Body ApiBodies.CreatePaymentBody body);

    @GET("api/payments/{reference}/")
    Call<ApiResponse<PaymentData>> getPayment(@Path("reference") String reference);

    // ---------- Notificaciones ----------

    // ---------- PQR ----------

    @GET("api/pqr/")
    Call<ApiResponse<java.util.List<Pqr>>> getPqrs();

    @POST("api/pqr/")
    Call<ApiResponse<Pqr>> createPqr(@Body ApiBodies.PqrBody body);

    @GET("api/pqr/{pqrId}/")
    Call<ApiResponse<Pqr>> getPqr(@Path("pqrId") String pqrId);

    @POST("api/notifications/")
    Call<ApiResponse<NotificationResult>> notify(@Body ApiBodies.NotificationBody body);
}
