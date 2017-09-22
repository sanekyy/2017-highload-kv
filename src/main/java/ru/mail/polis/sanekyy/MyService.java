package ru.mail.polis.sanekyy;

import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVService;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MyService implements KVService{

    private static final String PREFIX = "id=";


    @NotNull
    private final HttpServer server;
    @NotNull
    private final MyDAO dao;



    public MyService(
            int port,
            MyDAO dao
    ) throws IOException {
        this.server = HttpServer.create(
                new InetSocketAddress(port),
                0
        );

        this.dao = dao;

        this.server.createContext(
                "/v0/status",
                new HttpHandlerErrorWrapper(httpExchange -> {
                    final String response = "ONLINE";
                    httpExchange.sendResponseHeaders(200, response.length());
                    httpExchange.getResponseBody().write(response.getBytes());
                })
        );

        this.server.createContext(
                "/v0/entity", //
                new HttpHandlerErrorWrapper(httpExchange -> {
                    final String id = extractId(httpExchange.getRequestURI().getQuery());


                    switch (httpExchange.getRequestMethod()) {
                        case "GET":
                            final byte[] getValue = dao.get(id);

                            httpExchange.sendResponseHeaders(200, getValue.length);
                            httpExchange.getResponseBody().write(getValue);
                            break;

                        case "DELETE":
                            dao.delete(id);
                            httpExchange.sendResponseHeaders(202, 0);
                            break;

                        case "PUT":
                            final int contentLength = Integer.valueOf(
                                    httpExchange.getRequestHeaders().getFirst("Content-Length")
                            );

                            final byte[] putValue = new byte[contentLength];

                            if(contentLength != 0) {
                                if (httpExchange.getRequestBody().read(putValue) != putValue.length) {
                                    throw new IOException("Can't read at one go");
                                }
                            }

                            dao.upsert(id, putValue);

                            httpExchange.sendResponseHeaders(201, 0);
                            break;

                        default:
                            httpExchange.sendResponseHeaders(405, 0);
                    }
                })
        );
    }

    @NotNull
    private static String extractId(@NotNull final String query){
        if(!query.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Shitty string");
        }

        if(query.length()<=PREFIX.length()){
            throw new IllegalArgumentException();
        }

        return query.substring(PREFIX.length());
    }


    @Override
    public void start() {
        this.server.start();
    }

    @Override
    public void stop() {
        this.server.stop(0);
    }
}