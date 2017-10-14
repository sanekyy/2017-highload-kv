package ru.mail.polis.sanekyy.internalServices;

import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.mail.polis.sanekyy.MyService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MissedCalls {

    @NotNull
    private final TopologyManager topologyManager;
    @NotNull
    private final Map<String, List<Call<ResponseBody>>> missedCalls;

    public MissedCalls(@NotNull TopologyManager topologyManager) {
        this.topologyManager = topologyManager;

        missedCalls = new HashMap<>();

        for (String addr : topologyManager.getTopology()) {
            missedCalls.put(addr, new ArrayList<>());
        }
    }

    public void addMissedCall(Call<ResponseBody> call) {
        String addr = call.request().url().toString().split("/v\\d/")[0];
        missedCalls.get(addr).add(call);
    }

    public void checkMissedRequests(@NotNull final String hostname, final int port) {
        final String addr = "http://" + hostname + ":" + port;

        Callback<ResponseBody> callBack = new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                t.printStackTrace();
            }
        };

        if (topologyManager.getMode() == MyService.Mode.sync) {
            missedCalls.get(addr).forEach(call -> executeSyncCall(call, callBack));
        } else if (topologyManager.getMode() == MyService.Mode.async) {
            missedCalls.get(addr).forEach(call -> call.enqueue(callBack));
        } else {
            System.out.println("Unknown mode");
        }
    }

    private void executeSyncCall(@NotNull final Call<ResponseBody> call, @NotNull final Callback<ResponseBody> callBack) {
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
