package ru.mail.polis.sanekyy.internalServices;

import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.mail.polis.sanekyy.MyService.Mode;
import ru.mail.polis.sanekyy.Utils.Config;

import java.io.IOException;
import java.util.*;

public class MissedCalls {

    @NotNull
    private final Map<String, List<Call<ResponseBody>>> missedCalls;

    MissedCalls(@NotNull final Set<String> addrs) {
        missedCalls = new HashMap<>();

        for (String addr : addrs) {
            missedCalls.put(addr, new ArrayList<>());
        }
    }

    void addMissedCall(Call<ResponseBody> call) {
        String addr = call.request().url().toString().split("/v\\d/")[0];
        missedCalls.get(addr).add(call);
    }

    public void checkMissedRequests(@NotNull final String addr) {
        Callback<ResponseBody> callBack = new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                //t.printStackTrace();
            }
        };

        if (Config.mode == Mode.sync) {
            missedCalls.get(addr).forEach(call -> executeSyncCall(call, callBack));
        } else if (Config.mode == Mode.async) {
            missedCalls.get(addr).forEach(call -> call.enqueue(callBack));
        } else {
            System.out.println("Unknown mode");
        }
    }

    private void executeSyncCall(
            @NotNull final Call<ResponseBody> call,
            @NotNull final Callback<ResponseBody> callBack) {
        if (!call.isExecuted()) {
            try {
                Response<ResponseBody> response = call.execute();
                callBack.onResponse(call, response);
            } catch (IOException e) {
                callBack.onFailure(call, e);
            }
        }
    }
}
