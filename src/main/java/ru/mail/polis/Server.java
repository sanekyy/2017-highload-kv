package ru.mail.polis;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Starts storage and waits for shutdown
 *
 * @author Vadim Tsesko <mail@incubos.org>
 */
public final class Server {
    private static final int PORT = 8080;

    private Server() {
        // Not instantiable
    }

    public static void main(String[] args) throws IOException {
        // Temporary storage in the file system
        Set<String> topology = new LinkedHashSet<>(Arrays.asList("http://localhost:30001", "http://localhost:30002"));

       /* final File data = Files.createTempDirectory();

        // Start the storage
        final KVService storage =
                KVServiceFactory.create(
                        PORT,
                        data,
                        Collections.singleton("http://localhost:" + PORT));
        storage.start();
        Runtime.getRuntime().addShutdownHook(new Thread(storage::stop));
        */

        final File data1 = Files.createTempDirectory();
        final File data2 = Files.createTempDirectory();

        // Start the storage
        final KVService storage1 =
                KVServiceFactory.create(
                        30001,
                        data1,
                        topology);
        storage1.start();
        Runtime.getRuntime().addShutdownHook(new Thread(storage1::stop));

        final KVService storage2 =
                KVServiceFactory.create(
                        30002,
                        data2,
                        topology);
        storage2.start();
        Runtime.getRuntime().addShutdownHook(new Thread(storage2::stop));

    }
}
