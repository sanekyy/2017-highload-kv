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
import java.util.Set;
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
        final int ack = extractAck(httpExchange.getRequestURI().getQuery());
        final int from = extractFrom(httpExchange.getRequestURI().getQuery());

        if (!(ack <= from && from <= service.getTopologyManager().getNodesCount() && ack >= 1)) {
            sendResponse(httpExchange, Code.BAD_REQUEST, Body.BAD_REQUEST);
            return;
        }

        if (httpExchange.getRequestHeaders().containsKey(Header.INTERNAL_CALL)) {
            handleInternalGet(httpExchange, id);
        } else {
            handleExternalGet(httpExchange, id, ack, from);
        }
    }

    private void handleExternalGet(
            @NotNull final HttpExchange httpExchange,
            @NotNull final String id,
            final int ack,
            final int from) throws IOException {
        AtomicInteger goodAnswer = new AtomicInteger(0),
                badAnswer = new AtomicInteger(0),
                errorAnswer = new AtomicInteger(0);


        List<byte[]> values = new CopyOnWriteArrayList<>();

        Set<String> addrsToGet = service.getTopologyManager().getAddrsForId(id, service.getTopologyManager().getNodesCount());

        if (addrsToGet.remove(service.getAddr())) {
            try {
                values.add(service.getDao().get(id));
                goodAnswer.incrementAndGet();
            } catch (NoSuchElementException e) {
                badAnswer.incrementAndGet();
            } catch (Exception e) {
                errorAnswer.incrementAndGet();
            }

            if (from == 1) {
                if (goodAnswer.get() == 1) {
                    sendResponse(httpExchange, Code.OK, values.get(0));
                } else {
                    sendResponse(httpExchange, Code.NOT_FOUND, Body.NOT_FOUND);
                }
                return;
            }
        }

        service.getBroadcastManager().getEntity(addrsToGet, id, new Callback<ResponseBody>() {
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
                        } catch (Exception ignored){

                        }
                        break;
                    default:
                        errorAnswer.incrementAndGet();
                        System.out.println(String.valueOf(call.request().url()) + " wtf?");
                }

                if (goodAnswer.get() + badAnswer.get() + errorAnswer.get() == service.getTopologyManager().getNodesCount()) {
                    if (goodAnswer.get() >= ack) {
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
                    } else if (badAnswer.get() >= ack) {
                        sendResponse(httpExchange, Code.NOT_FOUND, Body.NOT_FOUND);
                    } else {
                        sendResponse(httpExchange, Code.NOT_ENOUGH_REPLICAS, Body.NOT_ENOUGH_REPLICAS);
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                errorAnswer.incrementAndGet();
                //t.printStackTrace();
                if (goodAnswer.get() >= ack) {
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
                } else if (badAnswer.get() >= ack) {
                    sendResponse(httpExchange, Code.NOT_FOUND, Body.NOT_FOUND);
                } else if (badAnswer.get() + goodAnswer.get() + errorAnswer.get() == service.getTopologyManager().getNodesCount()) {
                    sendResponse(httpExchange, Code.NOT_ENOUGH_REPLICAS, Body.NOT_ENOUGH_REPLICAS);
                }
            }
        });
    }

    private void handleInternalGet(
            @NotNull final HttpExchange httpExchange,
            @NotNull final String id) throws IOException {
        final byte[] getValue = service.getDao().get(id);
        sendResponse(httpExchange, Code.OK, getValue);
    }

    private void handleDelete(@NotNull final HttpExchange httpExchange) throws IOException {
        final String id = extractId(httpExchange.getRequestURI().getQuery());
        final int ack = extractAck(httpExchange.getRequestURI().getQuery());
        final int from = extractFrom(httpExchange.getRequestURI().getQuery());

        if (!(ack <= from && from <= service.getTopologyManager().getNodesCount() && ack >= 1)) {
            sendResponse(httpExchange, Code.BAD_REQUEST, Body.BAD_REQUEST);
            return;
        }

        if (httpExchange.getRequestHeaders().containsKey(Header.INTERNAL_CALL)) {
            handleInternalDelete(httpExchange, id);
        } else {
            handleExternalDelete(httpExchange, id, ack, from);
        }
    }

    private void handleExternalDelete(
            @NotNull final HttpExchange httpExchange,
            @NotNull final String id,
            final int ack,
            final int from) throws IOException {
        AtomicInteger goodAnswer = new AtomicInteger(0),
                errorAnswer = new AtomicInteger(0);

        Set<String> addrsToRemove = service.getTopologyManager().getAddrsForId(id, from);

        if (addrsToRemove.remove(service.getAddr())) {
            service.getDao().delete(id);
            goodAnswer.incrementAndGet();

            if (from == 1) {
                sendResponse(httpExchange, Code.ACCEPTED, Body.ACCEPTED);
                return;
            }
        }

        service.getBroadcastManager().deleteEntity(addrsToRemove, id, new Callback<ResponseBody>() {
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

                if (goodAnswer.get() + errorAnswer.get() == service.getTopologyManager().getNodesCount()) {
                    if (goodAnswer.get() >= ack) {
                        sendResponse(httpExchange, Code.ACCEPTED, Body.ACCEPTED);
                    } else {
                        sendResponse(httpExchange, Code.NOT_ENOUGH_REPLICAS, Body.NOT_ENOUGH_REPLICAS);
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                errorAnswer.incrementAndGet();
                //t.printStackTrace();
                if (goodAnswer.get() >= ack) {
                    sendResponse(httpExchange, Code.ACCEPTED, Body.ACCEPTED);
                } else if (goodAnswer.get() + errorAnswer.get() == service.getTopologyManager().getNodesCount()) {
                    sendResponse(httpExchange, Code.NOT_ENOUGH_REPLICAS, Body.NOT_ENOUGH_REPLICAS);
                }
            }
        });
    }

    private void handleInternalDelete(
            @NotNull final HttpExchange httpExchange,
            @NotNull final String id) throws IOException {
        service.getDao().delete(id);
        sendResponse(httpExchange, Code.ACCEPTED, Body.ACCEPTED);
    }

    private void handlePut(@NotNull final HttpExchange httpExchange) throws IOException {
        final String id = extractId(httpExchange.getRequestURI().getQuery());
        final int ack = extractAck(httpExchange.getRequestURI().getQuery());
        final int from = extractFrom(httpExchange.getRequestURI().getQuery());

        final byte[] value = extractValue(httpExchange);

        if (!(ack <= from && from <= service.getTopologyManager().getNodesCount() && ack >= 1)) {
            sendResponse(httpExchange, Code.BAD_REQUEST, Body.BAD_REQUEST);
            return;
        }

        if (httpExchange.getRequestHeaders().containsKey(Header.INTERNAL_CALL)) {
            handleInternalPut(httpExchange, id, value);
        } else {
            handleExternalPut(httpExchange, id, value, ack, from);
        }
    }

    private void handleExternalPut(
            @NotNull final HttpExchange httpExchange,
            @NotNull final String id,
            @NotNull final byte[] value,
            final int ack,
            final int from) {
        AtomicInteger goodAnswer = new AtomicInteger(0),
                errorAnswer = new AtomicInteger(0);


        Set<String> addrsToPut = service.getTopologyManager().getAddrsForId(id, from);

        if (addrsToPut.remove(service.getAddr())) {
            try {
                service.getDao().upsert(id, value);
                goodAnswer.incrementAndGet();
            } catch (Exception e) {
                errorAnswer.incrementAndGet();
                //e.printStackTrace();
            }

            if (from == 1) {
                if (goodAnswer.get() == 1) {
                    sendResponse(httpExchange, Code.CREATED, Body.CREATED);
                } else {
                    sendResponse(httpExchange, Code.NOT_ENOUGH_REPLICAS, Body.NOT_ENOUGH_REPLICAS);
                }
                return;
            }

        }

        service.getBroadcastManager().putEntity(addrsToPut, id, value, new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                switch (response.code()) {
                    case Code.CREATED:
                        goodAnswer.incrementAndGet();
                        //System.out.println("puted");
                        break;
                    default:
                        errorAnswer.incrementAndGet();
                        System.out.println(String.valueOf(call.request().url()) + " wtf?");
                }

                if (goodAnswer.get() + errorAnswer.get() == from) {
                    if (goodAnswer.get() >= ack) {
                        sendResponse(httpExchange, Code.CREATED, Body.CREATED);
                    } else {
                        sendResponse(httpExchange, Code.NOT_ENOUGH_REPLICAS, Body.NOT_ENOUGH_REPLICAS);
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                errorAnswer.incrementAndGet();
                //t.printStackTrace();

                if (goodAnswer.get() >= ack) {
                    sendResponse(httpExchange, Code.CREATED, Body.CREATED);
                } else if (goodAnswer.get() + errorAnswer.get() == from) {
                    sendResponse(httpExchange, Code.NOT_ENOUGH_REPLICAS, Body.NOT_ENOUGH_REPLICAS);
                }
            }
        });
    }

    private void handleInternalPut(
            @NotNull final HttpExchange httpExchange,
            @NotNull final String id,
            @NotNull byte[] value) throws IOException {
        service.getDao().upsert(id, value);
        sendResponse(httpExchange, Code.CREATED, Body.CREATED);
    }
}
