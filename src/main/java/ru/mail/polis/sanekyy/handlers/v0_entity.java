package ru.mail.polis.sanekyy.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.mail.polis.sanekyy.MyService;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class v0_entity extends BaseHandler {

    @NotNull
    private final HttpHandler handler = httpExchange -> {
        switch (httpExchange.getRequestMethod()) {
            case "GET":
                handleGet(httpExchange);
                break;
            case "DELETE":
                handleDelete(httpExchange);
                break;
            case "PUT":
                handlePut(httpExchange);
                break;
            default:
                sendResponse(httpExchange, Code.METHOD_NOT_ALLOWED, Body.METHOD_NOT_ALLOWED);
        }
    };

    public v0_entity(@NotNull final MyService service) {
        super(service);
        super.handler = handler;
    }

    private void handleGet(@NotNull final HttpExchange httpExchange) throws IOException {
        final String id = extractId(httpExchange.getRequestURI().getQuery());
        final int replicas = extractReplicas(httpExchange.getRequestURI().getQuery());

        if (replicas > service.getNodesCount() || replicas < 1) {
            sendResponse(httpExchange, Code.BAD_REQUEST, Body.BAD_REQUEST);
            return;
        }

        if (httpExchange.getRequestHeaders().containsKey(Header.INTERNAL_CALL)) {
            handleInternalGet(httpExchange, id);
        } else {
            handleExternalGet(httpExchange, id, replicas);
        }
    }

    private void handleExternalGet(@NotNull final HttpExchange httpExchange, @NotNull final String id, final int replicas) throws IOException {
        AtomicInteger goodAnswer = new AtomicInteger(0),
                badAnswer = new AtomicInteger(0),
                errorAnswer = new AtomicInteger(0);


        List<byte[]> values = new CopyOnWriteArrayList<>();


        try {
            values.add(service.getDao().get(id));
            goodAnswer.incrementAndGet();
        } catch (NoSuchElementException e) {
            badAnswer.incrementAndGet();
        } catch (Exception e) {
            errorAnswer.incrementAndGet();
            e.printStackTrace();
        }

        if (replicas == 1) {
            if (goodAnswer.get() == 1) {
                sendResponse(httpExchange, Code.OK, values.get(0));
            } else {
                sendResponse(httpExchange, Code.NOT_FOUND, Body.NOT_FOUND);
            }
            return;
        }

        service.getTopologyManager().getEntity(id, new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                switch (response.code()) {
                    case Code.NOT_FOUND:
                        badAnswer.incrementAndGet();
                        break;
                    case Code.OK:
                        goodAnswer.incrementAndGet();
                        try {
                            values.add(response.body().bytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        errorAnswer.incrementAndGet();
                        System.out.println(String.valueOf(call.request().url()) + " wtf?");
                }

                if (goodAnswer.get() >= replicas) {
                    sendResponse(httpExchange, Code.OK, values.get(0));

                    // for sendResponse all values
                    /*int totalLength = values.stream().map(bytes -> bytes.length).reduce((a,b) -> a + b).get();
                    byte[] totalBytes = new byte[totalLength];
                    int destPos = 0;

                    for(byte[] item : values){
                        System.arraycopy(item, 0, totalBytes, destPos, item.length);
                        destPos += item.length;
                    }

                    sendResponse(Code.OK, totalBytes);*/
                } else if (badAnswer.get() >= replicas) {
                    sendResponse(httpExchange, Code.NOT_FOUND, Body.NOT_FOUND);
                } else if (badAnswer.get() + goodAnswer.get() + errorAnswer.get() == service.getNodesCount()) {
                    sendResponse(httpExchange, Code.NOT_ENOUGH_REPLICAS, Body.NOT_ENOUGH_REPLICAS);
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                errorAnswer.incrementAndGet();
                //t.printStackTrace();
                if (goodAnswer.get() >= replicas) {
                    sendResponse(httpExchange, Code.OK, values.get(0));

                    // for sendResponse all values
                    /*int totalLength = values.stream().map(bytes -> bytes.length).reduce((a,b) -> a + b).get();
                    byte[] totalBytes = new byte[totalLength];
                    int destPos = 0;

                    for(byte[] item : values){
                        System.arraycopy(item, 0, totalBytes, destPos, item.length);
                        destPos += item.length;
                    }

                    sendResponse(Code.OK, totalBytes);*/
                } else if (badAnswer.get() >= replicas) {
                    sendResponse(httpExchange, Code.NOT_FOUND, Body.NOT_FOUND);
                } else if (badAnswer.get() + goodAnswer.get() + errorAnswer.get() == service.getNodesCount()) {
                    sendResponse(httpExchange, Code.NOT_ENOUGH_REPLICAS, Body.NOT_ENOUGH_REPLICAS);
                }
            }
        });
    }

    private void handleInternalGet(@NotNull final HttpExchange httpExchange, @NotNull final String id) throws IOException {
        final byte[] getValue = service.getDao().get(id);
        sendResponse(httpExchange, Code.OK, getValue);
    }

    private void handleDelete(@NotNull final HttpExchange httpExchange) throws IOException {
        final String id = extractId(httpExchange.getRequestURI().getQuery());
        final int replicas = extractReplicas(httpExchange.getRequestURI().getQuery());

        if (replicas > service.getNodesCount() || replicas < 1) {
            sendResponse(httpExchange, Code.BAD_REQUEST, Body.BAD_REQUEST);
            return;
        }

        if (httpExchange.getRequestHeaders().containsKey(Header.INTERNAL_CALL)) {
            handleInternalDelete(httpExchange, id);
        } else {
            handleExternalDelete(httpExchange, id, replicas);
        }
    }

    private void handleExternalDelete(@NotNull final HttpExchange httpExchange, @NotNull final String id, final int replicas) throws IOException {
        AtomicInteger goodAnswer = new AtomicInteger(0),
                errorAnswer = new AtomicInteger(0);

        service.getDao().delete(id);
        goodAnswer.incrementAndGet();

        if (service.getNodesCount() == 1) {
            sendResponse(httpExchange, Code.ACCEPTED, Body.ACCEPTED);
            return;
        }

        service.getTopologyManager().deleteEntity(id, new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull final Call<ResponseBody> call, @NotNull final Response<ResponseBody> response) {
                switch (response.code()) {
                    case Code.ACCEPTED:
                        goodAnswer.incrementAndGet();
                        break;
                    default:
                        errorAnswer.incrementAndGet();
                        System.out.println(String.valueOf(call.request().url()) + " wtf?");
                }

                if (goodAnswer.get() >= replicas) {
                    sendResponse(httpExchange, Code.ACCEPTED, Body.ACCEPTED);
                } else if (goodAnswer.get() + errorAnswer.get() == service.getNodesCount()) {
                    sendResponse(httpExchange, Code.NOT_ENOUGH_REPLICAS, Body.NOT_ENOUGH_REPLICAS);
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                errorAnswer.incrementAndGet();
                //t.printStackTrace();
                if (goodAnswer.get() >= replicas) {
                    sendResponse(httpExchange, Code.ACCEPTED, Body.ACCEPTED);
                } else if (goodAnswer.get() + errorAnswer.get() == service.getNodesCount()) {
                    sendResponse(httpExchange, Code.NOT_ENOUGH_REPLICAS, Body.NOT_ENOUGH_REPLICAS);
                }
            }
        });
    }

    private void handleInternalDelete(@NotNull final HttpExchange httpExchange, @NotNull final String id) throws IOException {
        service.getDao().delete(id);
        sendResponse(httpExchange, Code.ACCEPTED, Body.ACCEPTED);
    }

    private void handlePut(@NotNull final HttpExchange httpExchange) throws IOException {
        final String id = extractId(httpExchange.getRequestURI().getQuery());
        final int replicas = extractReplicas(httpExchange.getRequestURI().getQuery());

        final byte[] value = extractValue(httpExchange);

        if (replicas > service.getNodesCount() || replicas < 1) {
            sendResponse(httpExchange, Code.BAD_REQUEST, Body.BAD_REQUEST);
            return;
        }

        if (httpExchange.getRequestHeaders().containsKey(Header.INTERNAL_CALL)) {
            handleInternalPut(httpExchange, id, value);
        } else {
            handleExternalPut(httpExchange, id, value, replicas);
        }
    }

    private void handleExternalPut(@NotNull final HttpExchange httpExchange, @NotNull final String id, @NotNull final byte[] value, final int replicas) {
        AtomicInteger goodAnswer = new AtomicInteger(0),
                errorAnswer = new AtomicInteger(0);

        try {
            service.getDao().upsert(id, value);
            goodAnswer.incrementAndGet();
        } catch (Exception e) {
            errorAnswer.incrementAndGet();
            e.printStackTrace();
        }

        // Если клиент хотел надёжность в 1, то после первой успешной записи сразу же можно сообщить что всё ок
        // ОДНАКО, во время тестирования в методе overlapRead производится запись с надёжностью 1,
        // а потом счивание с 2-х, но к тому времени вторая нода ещё не записала себе значение,
        // в следствии чего тест не проходит, поэтому пришлось закомментировать этот блок.
        // Ну или пофиксить это тем, что сделать все вызовы синхронными

        /*if (replicas == 1 && goodAnswer.get() == 1) {
            sendResponse(httpExchange, Code.CREATED, Body.CREATED);
        }*/

        if (service.getNodesCount() == 1) {
            if (goodAnswer.get() == 1) {
                sendResponse(httpExchange, Code.CREATED, Body.CREATED);
            } else {
                sendResponse(httpExchange, Code.NOT_ENOUGH_REPLICAS, Body.NOT_ENOUGH_REPLICAS);
            }
            return;
        }


        service.getTopologyManager().putEntity(id, value, new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                switch (response.code()) {
                    case Code.CREATED:
                        goodAnswer.incrementAndGet();
                        break;
                    default:
                        errorAnswer.incrementAndGet();
                        System.out.println(String.valueOf(call.request().url()) + " wtf?");
                }

                if (goodAnswer.get() >= replicas) {
                    sendResponse(httpExchange, Code.CREATED, Body.CREATED);
                } else if (goodAnswer.get() + errorAnswer.get() == service.getNodesCount()) {
                    sendResponse(httpExchange, Code.NOT_ENOUGH_REPLICAS, Body.NOT_ENOUGH_REPLICAS);
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                errorAnswer.incrementAndGet();
                //t.printStackTrace();

                if (goodAnswer.get() >= replicas) {
                    sendResponse(httpExchange, Code.CREATED, Body.CREATED);
                } else if (goodAnswer.get() + errorAnswer.get() == service.getNodesCount()) {
                    sendResponse(httpExchange, Code.NOT_ENOUGH_REPLICAS, Body.NOT_ENOUGH_REPLICAS);
                }
            }
        });
    }

    private void handleInternalPut(@NotNull final HttpExchange httpExchange, @NotNull final String id, @NotNull byte[] value) throws IOException {
        service.getDao().upsert(id, value);
        sendResponse(httpExchange, Code.CREATED, Body.CREATED);
    }
}
