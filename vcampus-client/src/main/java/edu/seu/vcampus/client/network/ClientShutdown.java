package edu.seu.vcampus.client.network;

import java.io.IOException;
import java.net.Socket;

/** 按宽限期停止客户端后台线程并关闭 Socket。 */
final class ClientShutdown {

    private ClientShutdown() {
    }

    static void close(Socket socket, MessageReceiver receiver, Thread receiverThread,
            Thread reconnectThread, long graceMillis) throws IOException {
        if (reconnectThread != null) {
            reconnectThread.interrupt();
        }
        IOException failure = shutdownOutput(socket);
        join(receiverThread, graceMillis);
        if (receiver != null) {
            receiver.stop();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException exception) {
            failure = failure == null ? exception : failure;
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static IOException shutdownOutput(Socket socket) {
        try {
            if (socket != null && !socket.isOutputShutdown()) {
                socket.shutdownOutput();
            }
            return null;
        } catch (IOException exception) {
            return exception;
        }
    }

    private static void join(Thread thread, long millis) {
        if (thread == null || thread == Thread.currentThread()) {
            return;
        }
        try {
            thread.join(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
