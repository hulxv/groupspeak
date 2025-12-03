package org.openjfx;

// Don't need to import ProtocolHandler.java since it is in the same package (org.openjfx).
// However, we import the static members so we can call methods like buildPingRequest() directly
// without typing ProtocolHandler.buildPingRequest() every time.
import static org.openjfx.ProtocolHandler.*;

import org.openjfx.model.Conversation;
import org.openjfx.model.Framing;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatHandler {
    private Socket socket;
    private Framing framing;

    // Connecting to Server
    public void connectToServer(String host, int port) throws IOException {
        // Create the connection
        this.socket = new Socket(host, port);
        this.framing = new Framing(socket.getInputStream(), socket.getOutputStream());
    }

    // Disconnecting from Server
    public void disconnectFromServer() {
        if(framing != null) framing.close();
        try {
            if(socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error Closing Socket: " + e.getMessage());
        }
    }

    private String sendAndReceive(String json) throws IOException {
        if(framing == null) throw new IOException("Not Connected to Server");
        framing.writeFrame(json);
        return framing.readFrame();
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

        String pattern = "\"id\":\"(.*?)\",\\s*\"name\":\"(.*?)\",\\s*\"isGroup\":(true|false)";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(resp.conversations);

        while (m.find()) {
            String id = m.group(1);
            String name = m.group(2);
            boolean isGroup = Boolean.parseBoolean(m.group(3));
            
            // Create the Model Object
            list.add(new Conversation(id, name, isGroup));
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

    public Response sendDM(String conversationID, String senderID, String content, String recipientId) throws IOException {
        String request = buildSendDmRequest(conversationID, senderID, content, recipientId);
        String response = sendAndReceive(request);

        return ProtocolHandler.parseMessageResponse(response);
    }

    public Response sendToGroup(String conversationID, String senderID, String content) throws IOException {
        String request = buildSendGroupRequest(conversationID, senderID, content);
        String response = sendAndReceive(request);

        return parseMessageResponse(response);
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