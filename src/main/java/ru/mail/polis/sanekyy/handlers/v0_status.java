package ru.mail.polis.sanekyy.handlers;

import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.sanekyy.MyService;

public class v0_status extends BaseHandler {

    @NotNull
    private final HttpHandler handler = httpExchange -> sendResponse(httpExchange, Code.OK, Body.STATUS_ONLINE);

    public v0_status(@NotNull final MyService service) {
        super(service);
        super.handler = handler;
    }
}
