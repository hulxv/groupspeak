package org.openjfx.model;

import java.util.ArrayList;
import java.util.List;

public class Conversation {
    private String conversationId;
    private String name;
    private boolean isGroup;
    private List<User> participants;
    private List<Message> messages;

    public Conversation(String conversationId, String name, boolean isGroup) {
        this.conversationId = conversationId;
        this.name = name;
        this.isGroup = isGroup;
        this.participants = new ArrayList<>();
        this.messages = new ArrayList<>();
    }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isGroup() { return isGroup; }
    public void setGroup(boolean group) { isGroup = group; }

    public List<User> getParticipants() { return participants; }
    public void setParticipants(List<User> participants) { this.participants = participants; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }

    public void addMessage(Message message) {
        messages.add(message);
    }

    @Override
    public String toString() {
        return name;
    }
}
