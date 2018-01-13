package ru.mail.polis.sanekyy.data;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class MyFileDao implements MyDAO {

    @NotNull
    private final Map<String, byte[]> cache;

    @NotNull
    private final File dir;

    public MyFileDao(@NotNull final File dir) {
        this.dir = dir;
        cache = new HashMap<>(1000);
    }

    @NotNull
    private File getFile(@NotNull final String key) {
        return new File(dir, key);
    }


    /*@NotNull
    @Override
    public byte[] get(@NotNull String key) throws NoSuchElementException, IllegalArgumentException, IOException {

        final File file = getFile(key);

        if (!file.exists()) {
            throw new NoSuchElementException("File doesn't exist");
        }

        byte[] value = new byte[(int) file.length()];

        try (InputStream is = new FileInputStream(file)) {
            if (is.read(value) != file.length()) {
                throw new IOException("Can't read file in one go");
            }
        }
        return value;
    }*/

    @NotNull
    @Override
    public byte[] get(@NotNull String key) throws NoSuchElementException, IllegalArgumentException, IOException {

        byte[] value = cache.get(key);

        if(value == null) {

            final File file = getFile(key);

            if (!file.exists()) {
                throw new NoSuchElementException("File doesn't exist");
            }

            value = new byte[(int) file.length()];

            try (InputStream is = new FileInputStream(file)) {
                if (is.read(value) != file.length()) {
                    throw new IOException("Can't read file in one go");
                }
            }

            if(cache.size() == 1000){
                cache.remove(cache.keySet().iterator().next());
            }

            cache.put(key, value);
        }

        return value;
    }

    @Override
    public void upsert(
            @NotNull final String key,
            @NotNull final byte[] value
    ) throws IllegalArgumentException, IOException {
        try (OutputStream os = new FileOutputStream(getFile(key))) {
            os.write(value);
        }
    }

    @Override
    public void delete(@NotNull final String key) throws IllegalArgumentException, IOException {
        try {
            getFile(key).delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
