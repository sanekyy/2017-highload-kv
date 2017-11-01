package ru.mail.polis.sanekyy;

import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVService;
import ru.mail.polis.sanekyy.data.MyDAO;
import ru.mail.polis.sanekyy.handlers.v0_checkMissedCalls;
import ru.mail.polis.sanekyy.handlers.v0_entity;
import ru.mail.polis.sanekyy.handlers.v0_status;
import ru.mail.polis.sanekyy.internalServices.BroadcastManager;
import ru.mail.polis.sanekyy.internalServices.ITopologyManager;
import ru.mail.polis.sanekyy.internalServices.TopologyManager;
import ru.mail.polis.sanekyy.utils.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

@Getter
public class MyService implements KVService {

    @NotNull
    private final HttpServer server;
    @NotNull
    private final MyDAO dao;
    @NotNull
    private final BroadcastManager broadcastManager;
    @NotNull
    private final String addr;
    @NotNull
    private final ITopologyManager topologyManager;

    public MyService(
            final int port,
            @NotNull final MyDAO dao,
            @NotNull final Set<String> topology) throws IOException {

        this.dao = dao;
        addr = getAddrFromTopologyForPort(port, topology);
        topologyManager = new TopologyManager(topology);

        Set<String> neighborsAdds = new HashSet<>(topology);
        neighborsAdds.remove(addr);
        broadcastManager = new BroadcastManager(neighborsAdds);

        this.server = HttpServer.create(
                new InetSocketAddress(StringUtils.getHostnameFromAddr(addr), port),
                0
        );

        this.server.createContext(
                "/v0/status",
                new v0_status(this)
        );

        this.server.createContext(
                "/v0/entity",
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
        // polling all nodes to get missed requests/calls
        broadcastManager.checkMissedCalls(addr);
    }

    @Override
    public void stop() {
        this.server.stop(0);
    }

    @NotNull
    private String getAddrFromTopologyForPort(
            final int port,
            Set<String> topology) throws IllegalArgumentException {
        for (String addr : topology) {
            String currPort = addr.substring(addr.lastIndexOf(":") + 1);

            if (currPort.equals(String.valueOf(port))) {
                return addr;
            }
        }

        throw new IllegalArgumentException("Bad topology");
    }

    public enum Mode {
        async,
        sync
    }
}
