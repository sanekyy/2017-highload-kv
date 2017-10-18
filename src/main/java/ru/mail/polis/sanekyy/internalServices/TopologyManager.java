package ru.mail.polis.sanekyy.internalServices;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TopologyManager implements ITopologyManager {

    @NotNull
    private final List<String> topology;

    public TopologyManager(@NotNull final Set<String> topology) {
        this.topology = new ArrayList<>(topology);
        this.topology.sort(String::compareTo);
    }

    @NotNull
    @Override
    public Set<String> getAddrsForId(@NotNull final String id, final int nodesCount) {
        Set<String> result = new HashSet<>();
        int startNodeIndex = id.charAt(0) % topology.size();

        for (int i = 0; i < nodesCount; i++) {
            result.add(topology.get((startNodeIndex + i) % topology.size()));
        }

        return result;
    }

    @Override
    public int getNodesCount() {
        return topology.size();
    }

    @Override
    public int getQuorum() {
        return topology.size() / 2 + 1;
    }
}
