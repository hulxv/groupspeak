package org.openjfx.model;

import java.time.LocalDateTime;

public class Message {
    private String messageId;
    private String conversationId;
    private String senderId;
    private String content;
    private String createdAt;
    private User sender;

    public Message(String messageId, String conversationId, String senderId, String content, String createdAt) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Message(String conversationId, String senderId, String content) {
        this.messageId = java.util.UUID.randomUUID().toString();
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = LocalDateTime.now().toString();
    }


    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    @Override
    public String toString() {
        return content;
    }
}
