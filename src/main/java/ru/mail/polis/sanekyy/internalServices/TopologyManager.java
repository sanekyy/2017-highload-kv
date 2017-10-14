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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Getter
public class TopologyManager {

    private final int port;
    @NotNull
    private final Set<String> topology;
    @NotNull
    private final List<NodeService> nodeServices;
    @NotNull
    private final MissedCalls missedCalls;
    @NotNull
    private final Mode mode;
    @NotNull
    private String hostName;

    public TopologyManager(final int port, @NotNull final Mode mode, @NotNull final Set<String> topology) {
        this.port = port;
        this.topology = topology;
        this.mode = mode;

        nodeServices = new ArrayList<>();
        missedCalls = new MissedCalls(this);


        boolean isHostNameFound = false;

        for (String addr : topology) {
            String[] args = addr.split("[:/]");

            if (args[4].equals(String.valueOf(port))) {
                if (!isHostNameFound) {
                    hostName = args[3];
                    isHostNameFound = true;
                }
            } else {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(addr + "/")
                        .build();
                nodeServices.add(retrofit.create(NodeService.class));
            }
        }

        if (!isHostNameFound) {
            hostName = "localhost";
        }
    }


    public void getEntity(@NotNull final String id, @NotNull final Callback<ResponseBody> callBack) {
        if (mode == Mode.sync) {
            nodeServices.forEach(service -> {
                Call<ResponseBody> call = service.getEntity(id);
                executeSyncCall(call, callBack);
            });
        } else if (mode == Mode.async) {
            nodeServices.forEach(service -> service.getEntity(id).enqueue(callBack));
        } else {
            System.out.println("Unknown mode");
        }
    }

    public void deleteEntity(@NotNull final String id, @NotNull final Callback<ResponseBody> callBack) {
        if (mode == Mode.sync) {
            nodeServices.forEach(service -> {
                Call<ResponseBody> call = service.deleteEntity(id);
                executeSyncCall(call, callBack);
            });
        } else if (mode == Mode.async) {
            nodeServices.forEach(service -> service.deleteEntity(id).enqueue(callBack));
        } else {
            System.out.println("Unknown mode");
        }
    }

    public void putEntity(@NotNull final String id, @NotNull final byte[] body, @NotNull final Callback<ResponseBody> callBack) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain"), body);

        if (mode == Mode.sync) {
            nodeServices.forEach(service -> {
                Callback<ResponseBody> callbackWrapper = new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                        callBack.onResponse(call, response);
                    }

                    @Override
                    public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                        missedCalls.addMissedCall(service.putEntity(id, requestBody));
                        callBack.onFailure(call, t);
                    }
                };

                Call<ResponseBody> call = service.putEntity(id, requestBody);
                executeSyncCall(call, callbackWrapper);
            });
        } else if (mode == Mode.async) {
            nodeServices.forEach(service -> service.putEntity(id, requestBody).enqueue(callBack));
        } else {
            System.out.println("Unknown mode");
        }
    }

    public void checkMissedCalls() {
        Callback<ResponseBody> callBack = new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                System.out.println(hostName + ":" + port + " missedChecked");
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                //t.printStackTrace();
            }
        };

        if (mode == Mode.sync) {
            nodeServices.forEach(service -> {
                Call<ResponseBody> call = service.checkMissedCall(hostName, port);
                executeSyncCall(call, callBack);
            });
        } else if (mode == Mode.async) {
            nodeServices.forEach(service -> service.checkMissedCall(hostName, port).enqueue(callBack));
        } else {
            System.out.println("Unknown mode");
        }
    }

    private void executeSyncCall(@NotNull final Call<ResponseBody> call, @NotNull final Callback<ResponseBody> callBack) {
        try {
            Response<ResponseBody> response = call.execute();
            callBack.onResponse(call, response);
        } catch (IOException e) {
            callBack.onFailure(call, e);
        }
    }
}
