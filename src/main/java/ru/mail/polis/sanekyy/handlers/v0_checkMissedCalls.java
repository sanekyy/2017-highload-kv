package ru.mail.polis.sanekyy.handlers;

import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.sanekyy.MyService;

public class v0_checkMissedCalls extends BaseHandler {

    @NotNull
    private final HttpHandler handler = httpExchange -> {
        String addr = extractAddr(httpExchange.getRequestURI().getQuery());

        service.getBroadcastManager().getMissedCalls().checkMissedRequests(addr);
        sendResponse(httpExchange, Code.OK, Body.STATUS_ONLINE);
    };

    public v0_checkMissedCalls(@NotNull final MyService service) {
        super(service);
        super.handler = handler;
    }
}
