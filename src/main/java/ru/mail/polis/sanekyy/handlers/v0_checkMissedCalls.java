package ru.mail.polis.sanekyy.handlers;

import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.sanekyy.MyService;

public class v0_checkMissedCalls extends BaseHandler {

    @NotNull
    private final HttpHandler handler = httpExchange -> {
        String hostname = extractHostname(httpExchange.getRequestURI().getQuery());
        int port = extractPort(httpExchange.getRequestURI().getQuery());

        service.getTopologyManager().getMissedCalls().checkMissedRequests(hostname, port);
        sendResponse(httpExchange, Code.OK, Body.STATUS_ONLINE);
    };

    public v0_checkMissedCalls(@NotNull final MyService service) {
        super(service);
        super.handler = handler;
    }
}
