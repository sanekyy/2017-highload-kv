package ru.mail.polis.sanekyy.internalServices;

import lombok.Getter;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import ru.mail.polis.sanekyy.MyService.Mode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Getter
public class BroadcastManager {
    @NotNull
    private final MissedCalls missedCalls;
    @NotNull
    private final Map<String, NodeService> nodeServices;

    public BroadcastManager(Set<String> addrs) {
        missedCalls = new MissedCalls(addrs);
        nodeServices = new HashMap<>();

        for (String addr : addrs) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(addr + "/")
                    .build();
            nodeServices.put(addr, retrofit.create(NodeService.class));
        }
    }

    public void getEntity(
            @NotNull final Mode mode,
            @NotNull final Iterable<String> addrs,
            @NotNull final String id,
            @NotNull final Callback<ResponseBody> callback) {
        for (String addr : addrs) {
            NodeService service = nodeServices.get(addr);
            Call<ResponseBody> call = service.getEntity(id);
            executeCall(call, callback, mode);
        }
    }

    public void deleteEntity(
            @NotNull final Mode mode,
            @NotNull final Iterable<String> addrs,
            @NotNull final String id,
            @NotNull final Callback<ResponseBody> callback) {
        for (String addr : addrs) {
            NodeService service = nodeServices.get(addr);
            Call<ResponseBody> call = service.deleteEntity(id);
            Callback<ResponseBody> wrappedCallback = new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NotNull final Call<ResponseBody> call, @NotNull final Response<ResponseBody> response) {
                    callback.onResponse(call, response);
                }

                @Override
                public void onFailure(@NotNull final Call<ResponseBody> call, @NotNull final Throwable t) {
                    missedCalls.addMissedCall(service.deleteEntity(id));
                    callback.onFailure(call, t);
                }
            };
            executeCall(call, wrappedCallback, mode);
        }
    }

    public void putEntity(
            @NotNull final Mode mode,
            @NotNull final Iterable<String> addrs,
            @NotNull final String id,
            @NotNull final byte[] body,
            @NotNull final Callback<ResponseBody> callback) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain"), body);

        for (String addr : addrs) {
            NodeService service = nodeServices.get(addr);
            Call<ResponseBody> call = service.putEntity(id, requestBody);
            Callback<ResponseBody> wrappedCallback = new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NotNull final Call<ResponseBody> call, @NotNull final Response<ResponseBody> response) {
                    callback.onResponse(call, response);
                }

                @Override
                public void onFailure(@NotNull final Call<ResponseBody> call, @NotNull final Throwable t) {
                    missedCalls.addMissedCall(service.putEntity(id, requestBody));
                    callback.onFailure(call, t);
                }
            };
            executeCall(call, wrappedCallback, mode);
        }
    }

    public void checkMissedCalls(@NotNull final String addr) {
        Callback<ResponseBody> callBack = new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
            }
        };

        nodeServices.values().forEach(service -> {
            Call<ResponseBody> call = service.checkMissedCall(addr);
            executeCall(call, callBack, Mode.sync);
        });
    }

    private void executeCall(
            @NotNull final Call<ResponseBody> call,
            @NotNull final Callback<ResponseBody> callback, Mode mode) {
        if (mode == Mode.sync) {
            try {
                Response<ResponseBody> response = call.execute();
                callback.onResponse(call, response);
            } catch (IOException e) {
                callback.onFailure(call, e);
            }
        } else if (mode == Mode.async) {
            call.enqueue(callback);
        } else {
            System.out.println("Unknown mode");
        }
    }

}
