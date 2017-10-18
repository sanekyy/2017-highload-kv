package ru.mail.polis.sanekyy.Utils;

import org.jetbrains.annotations.NotNull;

public class StringUtils {

    @NotNull
    public static String getHostnameFromAddr(@NotNull final String addr) {
        String[] args = addr.split("[:/]");
        return args[3];
    }
}
