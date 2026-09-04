package edu.seu.vcampus.client.network;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

/** 建立带超时和指数退避重试的客户端对象流连接。 */
final class ClientConnectionFactory {

    private final String host;
    private final int port;
    private final ClientNetworkConfig config;
    private volatile boolean stopped;
    private volatile Socket pendingSocket;

    ClientConnectionFactory(String host, int port, ClientNetworkConfig config) {
        this.host = host;
        this.port = port;
        this.config = config;
    }

    synchronized Connection openWithRetry() throws IOException {
        IOException failure = null;
        for (int attempt = 0; attempt <= config.getMaxRetries(); attempt++) {
            ensureRunning();
            try {
                return openOnce();
            } catch (IOException exception) {
                failure = exception;
                if (attempt == config.getMaxRetries()) {
                    break;
                }
                waitBeforeRetry(attempt);
            }
        }
        throw failure;
    }

    void stop() {
        stopped = true;
        closeQuietly(pendingSocket);
    }

    private Connection openOnce() throws IOException {
        Socket socket = new Socket();
        pendingSocket = socket;
        try {
            socket.connect(new InetSocketAddress(host, port),
                    config.getConnectTimeoutMillis());
            socket.setSoTimeout(config.getReadTimeoutMillis());
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            ensureRunning();
            return new Connection(socket, output, input);
        } catch (IOException exception) {
            closeQuietly(socket);
            throw exception;
        } finally {
            pendingSocket = null;
        }
    }

    private void waitBeforeRetry(int attempt) throws IOException {
        long delay = config.getInitialBackoffMillis();
        for (int index = 0; index < attempt && delay < config.getMaxBackoffMillis(); index++) {
            if (delay > config.getMaxBackoffMillis() / 2L) {
                delay = config.getMaxBackoffMillis();
            } else {
                delay *= 2L;
            }
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            InterruptedIOException interrupted = new InterruptedIOException("retry interrupted");
            interrupted.initCause(exception);
            throw interrupted;
        }
    }

    private void ensureRunning() throws SocketException {
        if (stopped) {
            throw new SocketException("client is closed");
        }
    }

    static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // 保留触发清理的原始异常。
            }
        }
    }

    /** 一次成功建立的 Socket 及其对象流。 */
    static final class Connection {
        final Socket socket;
        final ObjectOutputStream output;
        final ObjectInputStream input;

        Connection(Socket socket, ObjectOutputStream output, ObjectInputStream input) {
            this.socket = socket;
            this.output = output;
            this.input = input;
        }
    }
}
