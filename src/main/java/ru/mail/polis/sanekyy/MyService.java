package ru.mail.polis.sanekyy;

import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVService;
import ru.mail.polis.sanekyy.data.MyDAO;
import ru.mail.polis.sanekyy.handlers.v0_checkMissedCalls;
import ru.mail.polis.sanekyy.handlers.v0_entity;
import ru.mail.polis.sanekyy.handlers.v0_status;
import ru.mail.polis.sanekyy.internalServices.TopologyManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;

@Getter
public class MyService implements KVService {

    private final Mode mode = Mode.sync;
    @NotNull
    private final HttpServer server;
    @NotNull
    private final MyDAO dao;
    private final int quorum;
    private final int nodesCount;
    @NotNull
    private final TopologyManager topologyManager;

    public MyService(
            final int port,
            @NotNull final MyDAO dao,
            @NotNull final Set<String> topology) throws IOException {

        this.dao = dao;
        quorum = topology.size() / 2 + 1;
        nodesCount = topology.size();

        topologyManager = new TopologyManager(port, mode, topology);

        this.server = HttpServer.create(
                new InetSocketAddress(topologyManager.getHostName(), topologyManager.getPort()),
                0
        );

        this.server.createContext(
                "/v0/status",
                new v0_status(this)
        );

        this.server.createContext(
                "/v0/entity", //
                new v0_entity(this)
        );

        this.server.createContext(
                "/v0/checkMissedCalls",
                new v0_checkMissedCalls(this)
        );
    }

    @Override
    public void start() {
        this.server.start();
        topologyManager.checkMissedCalls();
    }

    @Override
    public void stop() {
        this.server.stop(0);
    }

    public enum Mode {
        async,
        sync
    }
}
