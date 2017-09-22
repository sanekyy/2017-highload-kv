package ru.mail.polis.sanekyy;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.NoSuchElementException;

public class MyFileDao implements MyDAO {
    @NotNull
    private final File dir;

    public MyFileDao(@NotNull final File dir) {
        this.dir = dir;
    }

    @NotNull
    private File getFile(@NotNull final String key){
        return new File(dir, key);
    }


    @NotNull
    @Override
    public byte[] get(@NotNull String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        final File file = getFile(key);

        if(!file.exists()){
            throw new NoSuchElementException("File doesn't exist");
        }

        final byte[] value = new byte[(int) file.length()];

        try (InputStream is = new FileInputStream(file)) {
            if (is.read(value) != file.length()) {
                throw new IOException("Can't read file in one go");
            }
        }
        return value;
    }

    @Override
    public void upsert(
            @NotNull final String key,
            @NotNull final byte[] value
    ) throws IllegalArgumentException, IOException {
        try(OutputStream os = new FileOutputStream(getFile(key))){
           os.write(value);
        }
    }

    @Override
    public void delete(@NotNull final String key) throws IllegalArgumentException, IOException {
        try {
            getFile(key).delete();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
