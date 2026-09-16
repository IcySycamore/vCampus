package edu.seu.vcampus.client.user;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.common.message.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared protocol fixture for UserService tests. */
final class UserServiceTestFixture {
    private UserServiceTestFixture() {
    }

    static Message response(String status, Object data) {
        Message message = new Message(0, data);
        message.setStatusCode(status);
        return message;
    }

    /** Records requests and returns responses configured by command. */
    static final class FakeDispatcher extends ClientMessageDispatcher {
        final List<Message> sent = new ArrayList<Message>();
        private final Map<Integer, Message> replies = new HashMap<Integer, Message>();

        void reply(int command, Message response) {
            replies.put(Integer.valueOf(command), response);
        }

        @Override
        public void send(Message message) {
            sent.add(message);
        }

        @Override
        public Message request(Message request, long timeoutMillis) {
            sent.add(request);
            return replies.get(Integer.valueOf(request.getCommand()));
        }
    }
}
