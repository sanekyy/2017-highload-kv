package ru.mail.polis.sanekyy.internalServices;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.http.*;

public interface NodeService {

    @Headers("Internal-Call: true")
    @GET("v0/status")
    Call<ResponseBody> status();

    @Headers("Internal-Call: true")
    @GET("v0/entity")
    Call<ResponseBody> getEntity(@Query("id") @NotNull final String id);

    @Headers("Internal-Call: true")
    @PUT("v0/entity")
    Call<ResponseBody> putEntity(@Query("id") @NotNull final String id, @Body @NotNull final RequestBody body);

    @Headers("Internal-Call: true")
    @DELETE("v0/entity")
    Call<ResponseBody> deleteEntity(@Query("id") @NotNull final String id);

    @GET("v0/checkMissedCalls")
    Call<ResponseBody> checkMissedCall(@Query("addr") @NotNull final String addr);
}
