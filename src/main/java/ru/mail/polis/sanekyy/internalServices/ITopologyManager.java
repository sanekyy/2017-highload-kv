package ru.mail.polis.sanekyy.internalServices;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface ITopologyManager {
    @NotNull
    Set<String> getAddrsForId(@NotNull final String id, final int nodesCount);

    int getNodesCount();

    int getQuorum();
}
