package ru.mail.polis.sanekyy.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.sanekyy.MyService;

import java.io.IOException;
import java.util.NoSuchElementException;

public class BaseHandler implements HttpHandler {

    private static final String PREFIX_ID = "id=";
    private static final String PREFIX_REPLICAS = "replicas=";
    private static final String PREFIX_ADDR = "addr=";
    @NotNull
    final MyService service;
    @NotNull
    HttpHandler handler;

    BaseHandler(@NotNull MyService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            handler.handle(httpExchange);
        } catch (NoSuchElementException e) {
            sendResponse(httpExchange, Code.NOT_FOUND, Body.NOT_FOUND);
            //e.printStackTrace();
            httpExchange.close();
        } catch (IllegalArgumentException e) {
            sendResponse(httpExchange, Code.BAD_REQUEST, Body.BAD_REQUEST);
            //e.printStackTrace();
            httpExchange.close();
        } catch (Exception e) {
            //e.printStackTrace();
            httpExchange.close();
        }
    }

    void sendResponse(
            @NotNull final HttpExchange httpExchange,
            final int code,
            @NotNull final byte[] body) {

        try {
            httpExchange.sendResponseHeaders(code, body.length);
            httpExchange.getResponseBody().write(body);
        } catch (IOException e) {
            //e.printStackTrace();
        } finally {
            httpExchange.close();
        }
    }

    @NotNull
    String extractId(@NotNull final String query) {
        String[] subqueries = query.split("&");

        for (String subquery : subqueries) {
            if (subquery.startsWith(PREFIX_ID)) {
                if (query.length() == PREFIX_ID.length()) {
                    throw new IllegalArgumentException();
                }
                return subquery.substring(PREFIX_ID.length());
            }
        }

        throw new IllegalArgumentException("Shitty string");
    }

    @NotNull
    byte[] extractValue(HttpExchange httpExchange) throws IOException {
        final int contentLength = Integer.valueOf(
                httpExchange.getRequestHeaders().getFirst("Content-Length")
        );

        final byte[] value = new byte[contentLength];

        if (contentLength != 0) {
            if (httpExchange.getRequestBody().read(value) != value.length) {
                throw new IOException("Can't read at one go");
            }
        }

        return value;
    }

    int extractAck(String query) {
        String[] subqueries = query.split("&");

        for (String subquery : subqueries) {
            if (subquery.startsWith(PREFIX_REPLICAS)) {
                if (query.length() == PREFIX_REPLICAS.length()) {
                    throw new IllegalArgumentException();
                }

                String[] ackAndFrom = subquery.substring(PREFIX_REPLICAS.length()).split("/");

                if (ackAndFrom.length != 2) {
                    throw new IllegalArgumentException();
                } else {
                    return Integer.valueOf(ackAndFrom[0]);
                }
            }
        }

        return service.getTopologyManager().getQuorum();
    }

    int extractFrom(String query) {
        String[] subqueries = query.split("&");

        for (String subquery : subqueries) {
            if (subquery.startsWith(PREFIX_REPLICAS)) {
                if (query.length() == PREFIX_REPLICAS.length()) {
                    throw new IllegalArgumentException();
                }
                String[] ackAndFrom = subquery.substring(PREFIX_REPLICAS.length()).split("/");

                if (ackAndFrom.length != 2) {
                    throw new IllegalArgumentException();
                } else {
                    return Integer.valueOf(ackAndFrom[1]);
                }
            }
        }

        return service.getTopologyManager().getNodesCount();
    }

    @NotNull
    String extractAddr(String query) {
        String[] subqueries = query.split("&");

        for (String subquery : subqueries) {
            if (subquery.startsWith(PREFIX_ADDR)) {
                if (query.length() == PREFIX_ADDR.length()) {
                    throw new IllegalArgumentException();
                }
                return subquery.substring(PREFIX_ADDR.length());
            }
        }

        throw new IllegalArgumentException();
    }

    static class Code {
        static final int OK = 200;
        static final int CREATED = 201;
        static final int ACCEPTED = 202;
        static final int BAD_REQUEST = 400;
        static final int NOT_FOUND = 404;
        static final int METHOD_NOT_ALLOWED = 405;
        static final int NOT_ENOUGH_REPLICAS = 504;
    }

    static class Header {
        static final String INTERNAL_CALL = "Internal-Call";
    }

    static class Body {
        static final byte[] STATUS_ONLINE = "Online".getBytes();
        static final byte[] CREATED = "Created".getBytes();
        static final byte[] ACCEPTED = "Accepted".getBytes();
        static final byte[] BAD_REQUEST = "Bad request".getBytes();
        static final byte[] NOT_FOUND = "Not found".getBytes();
        static final byte[] METHOD_NOT_ALLOWED = "Method Not Allowed".getBytes();
        static final byte[] NOT_ENOUGH_REPLICAS = "Not Enough Replicas".getBytes();
    }
}
