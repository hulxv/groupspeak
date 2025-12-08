package org.openjfx;

// Don't need to import ProtocolHandler.java since it is in the same package (org.openjfx).
// However, we import the static members so we can call methods like buildPingRequest() directly
// without typing ProtocolHandler.buildPingRequest() every time.
import static org.openjfx.ProtocolHandler.*;

import org.openjfx.model.Conversation;
import org.openjfx.model.Message;
import org.openjfx.model.User;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatHandler {

    private synchronized String sendAndReceive(String json) throws IOException {
        Connection conn = ClientState.getInstance().getConnection();
        if(conn == null) throw new IOException("Not Connected to Server");
        
        conn.send(json);
        
        if (ClientState.getInstance().isAsyncMode()) {
            try {
                return ClientState.getInstance().getResponseQueue().take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for response", e);
            }
        } else {
            return conn.receive();
        }
    }

    public RegisterResponse register(String username, String password, String displayName, String email) throws IOException {
        String request = buildRegisterRequest(username, password, displayName, email);
        String response = sendAndReceive(request);
        System.out.println("Register Response: " + response);
        return parseRegisterResponse(response);
    }

    public LoginResponse login(String username, String password) throws IOException {
        String request = buildLoginRequest(username, password, "desktop");
        String response = sendAndReceive(request);
        return parseLoginResponse(response);
    }

    // ==================================================================
    //                     CONVERSATION MANAGEMENT
    // ==================================================================

    public List<Conversation> getConversations() throws IOException {
        String request = buildGetConversationsRequest();
        String json = sendAndReceive(request);

        ConversationsResponse resp = parseConversationsResponse(json);
        List<Conversation> list = new ArrayList<>();
        if(!resp.success || resp.conversations == null) return new ArrayList<>();

        // Regex to match conversation object including participants array
        // {"id":"...","name":"...","isGroup":...,"participants":["...","..."]}
        String pattern = "\"id\":\"(.*?)\",\\s*\"name\":\"(.*?)\",\\s*\"isGroup\":(true|false),\\s*\"participants\":\\[(.*?)\\]";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(resp.conversations);

        while (m.find()) {
            String id = m.group(1);
            String name = m.group(2);
            boolean isGroup = Boolean.parseBoolean(m.group(3));
            String participantsJson = m.group(4);
            
            Conversation conv = new Conversation(id, name, isGroup);
            
            // Parse participants array string: "id1","id2"
            if (participantsJson != null && !participantsJson.isEmpty()) {
                String[] ids = participantsJson.split(",");
                for (String pid : ids) {
                    // Remove quotes
                    String cleanId = pid.trim().replace("\"", "");
                    if (!cleanId.isEmpty()) {
                        conv.participantIds.add(cleanId);
                    }
                }
            }
            
            list.add(conv);
        }
        return list;
    }

    public List<User> getUsers() throws IOException {
        String request = buildGetUsersRequest();
        String json = sendAndReceive(request);
        // System.out.println("getUsers JSON: " + json); // Commented out debug

        UsersResponse resp = parseUsersResponse(json);
        List<User> list = new ArrayList<>();
        if(!resp.success || resp.users == null) return new ArrayList<>();

        // Regex to parse user objects in the array
        // {"id":"...","username":"...","displayName":"...","isOnline":true/false}
        String pattern = "\"id\":\"(.*?)\",\\s*\"username\":\"(.*?)\",\\s*\"displayName\":\"(.*?)\",\\s*\"isOnline\":(true|false)";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(resp.users);

        while (m.find()) {
            String id = m.group(1);
            String username = m.group(2);
            String displayName = m.group(3);
            boolean isOnline = Boolean.parseBoolean(m.group(4));
            
            list.add(new User(id, username, displayName, isOnline));
        }
        return list;
    }

    public List<Message> getMessages(String conversationId) throws IOException {
        String request = buildGetMessagesRequest(conversationId);
        String json = sendAndReceive(request);
        
        MessagesResponse resp = parseMessagesResponse(json);
        List<Message> list = new ArrayList<>();
        if(!resp.success || resp.messages == null) return new ArrayList<>();

        // Regex for Message object
        // {"id":"...","senderId":"...","content":"...","createdAt":"..."}
        String pattern = "\"id\":\"(.*?)\",\\s*\"senderId\":\"(.*?)\",\\s*\"content\":\"(.*?)\",\\s*\"createdAt\":\"(.*?)\"";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(resp.messages);

        while (m.find()) {
            String id = m.group(1);
            String senderId = m.group(2);
            String content = m.group(3);
            String createdAt = m.group(4);
            
            list.add(new Message(id, senderId, content, createdAt));
        }
        return list;
    }

    public Conversation createConversation(String otherUsername, String groupName, String participants) throws IOException {
        String request = buildCreateConversationRequest(otherUsername, groupName, participants);
        String json = sendAndReceive(request);

        CreateConversationResponse resp = parseCreateConversationResponse(json);
        if (resp.success) {
            String conversationName = (groupName != null) ? groupName : otherUsername;
            boolean isGroup = (conversationName == groupName);

            return new Conversation(resp.conversationId, conversationName, isGroup);
        }
        return null;
    }

    public Response addParticipantToGroup(String conversationID, String userID) throws IOException {
        String request = buildAddParticipantRequest(conversationID, userID);
        String response = sendAndReceive(request);

        return parseAddParticipantResponse(response);
    }

    public Response removeParticipantFromGroup(String conversationID, String userID) throws IOException {
        String request = buildRemoveParticipantRequest(conversationID, userID);
        String response = sendAndReceive(request);

        return parseRemoveParticipantResponse(response);
    }

    // ==================================================================
    //                            MESSAGING
    // ==================================================================

    public void sendDM(String conversationID, String senderID, String content, String recipientId) throws IOException {
        String request = buildSendDmRequest(conversationID, senderID, content, recipientId);
        Connection conn = ClientState.getInstance().getConnection();
        if(conn == null) throw new IOException("Not Connected to Server");
        conn.send(request);
    }

    public void sendToGroup(String conversationID, String senderID, String content) throws IOException {
        String request = buildSendGroupRequest(conversationID, senderID, content);
        Connection conn = ClientState.getInstance().getConnection();
        if(conn == null) throw new IOException("Not Connected to Server");
        conn.send(request);
    }

    public void reloadConversationsForUser(String userId) throws IOException {
        String request = buildReloadConversationsRequest(userId);
        Connection conn = ClientState.getInstance().getConnection();
        if (conn == null) throw new IOException("Not Connected to Server");
        conn.send(request);
    }

    // ==================================================================
    //                            UTILITIES
    // ==================================================================

    public Response ping() throws IOException {
        String request = buildPingRequest();
        String response = sendAndReceive(request);

        return parsePingResponse(response);
    }
}

/*
get_conversations()
create_conversation(otherUsername) (1-1)
create_conversation(groupName, participants (Comma-separated list of usernames))
add_participant(conversationId, participantId)
remove_participant(conversationId, participantId)
-------------------------------------------------------------------------------------
send_dm(conversationId, senderId, content (Message content), recipientId)
send_group(conversationId, senderId, content (Message content))
*/