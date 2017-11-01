package ru.mail.polis.sanekyy.internalServices;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class TopologyManager implements ITopologyManager {

    @NotNull
    private final List<String> topology;

    public TopologyManager(@NotNull final Collection<String> topology) {
        this.topology = new ArrayList<>(topology);
        this.topology.sort(String::compareTo);
    }

    @NotNull
    @Override
    public List<String> getAddrsForId(@NotNull final String id) {
        List<String> result = new ArrayList<>();

        for (int i = 0; i < topology.size(); i++) {
            result.add(topology.get((getNodeIndexForIdAndOffset(id, i))));
        }

        return result;
    }

    private int getNodeIndexForIdAndOffset(@NotNull final String id, int offset) {
        int sum = 0;

        for (int i = 0; i < id.length(); i++) {
            sum += id.charAt(i);
        }

        return ((sum % topology.size()) + offset) % topology.size();
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
