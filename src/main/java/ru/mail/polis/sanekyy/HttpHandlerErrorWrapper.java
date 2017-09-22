package ru.mail.polis.sanekyy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.NoSuchElementException;

public class HttpHandlerErrorWrapper implements HttpHandler {
    private final HttpHandler httpHandler;

    HttpHandlerErrorWrapper(HttpHandler httpHandler) {
        this.httpHandler = httpHandler;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            httpHandler.handle(httpExchange);
        } catch (NoSuchElementException e){
            httpExchange.sendResponseHeaders(404, 0);
            httpExchange.close();
        } catch (IllegalArgumentException e) {
            httpExchange.sendResponseHeaders(400, 0);
        } finally {
            httpExchange.close();
        }
    }
}
