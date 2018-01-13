package ru.mail.polis.sanekyy.internalServices;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ITopologyManager {
    @NotNull
    List<String> getAddrsForId(@NotNull final String id);

    int getNodesCount();

    int getQuorum();
}
