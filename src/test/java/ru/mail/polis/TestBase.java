package ru.mail.polis;

import org.apache.http.HttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Contains utility methods for unit tests
 *
 * @author Vadim Tsesko <mail@incubos.org>
 */
abstract class TestBase {
    private static final int VALUE_LENGTH = 1024;

    private static Set<Integer> ports;

    static int randomPort() throws Exception {
        if(ports == null){
            ports = new HashSet<>();
            for(int i = 10000; i < 60000; i++){
                ports.add(i);
            }
        }

        for (int port : ports) {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                serverSocket.close();
                ports.remove(port);
                return port;
            } catch (IOException ignored) {
                ports.remove(port);
            }
        }

        throw new Exception("Can't allocate port");
    }

    @NotNull
    static String randomKey() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong());
    }

    @NotNull
    static byte[] randomValue() {
        final byte[] result = new byte[VALUE_LENGTH];
        ThreadLocalRandom.current().nextBytes(result);
        return result;
    }

    @NotNull
    static byte[] payloadOf(@NotNull final HttpResponse response) throws IOException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        response.getEntity().writeTo(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    @NotNull
    static String endpoint(final int port) {
        return "http://localhost:" + port;
    }
}
