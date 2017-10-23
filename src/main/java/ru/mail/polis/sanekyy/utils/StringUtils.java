package ru.mail.polis.sanekyy.utils;

import org.jetbrains.annotations.NotNull;

public class StringUtils {

    @NotNull
    public static String getHostnameFromAddr(@NotNull final String addr) throws IllegalArgumentException {
        String[] args = addr.split("[:/]");
        if(args.length < 4) {
            throw new IllegalArgumentException("Bad address");
        } else {
            return args[3];
        }
    }
}
