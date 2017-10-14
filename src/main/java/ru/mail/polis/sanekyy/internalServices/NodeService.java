package ru.mail.polis.sanekyy.internalServices;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface NodeService {

    @Headers("Internal-Call: true")
    @GET("v0/status")
    Call<ResponseBody> status();

    @Headers("Internal-Call: true")
    @GET("v0/entity")
    Call<ResponseBody> getEntity(@Query("id") String id);

    @Headers("Internal-Call: true")
    @PUT("v0/entity")
    Call<ResponseBody> putEntity(@Query("id") String id, @Body RequestBody body);

    @Headers("Internal-Call: true")
    @DELETE("v0/entity")
    Call<ResponseBody> deleteEntity(@Query("id") String id);

    @GET("v0/checkMissedCalls")
    Call<ResponseBody> checkMissedCall(@Query("hostname") String hostname, @Query("port") int port);
}
