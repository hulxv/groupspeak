# Chat Server Protocol Cheatsheet

This document outlines all available protocol commands for the chat server, including required parameters and example usage.

## Authentication Commands

### register
Register a new user account.

**Parameters:**
- `username` (required): Unique username
- `password` (required): User password
- `displayName` (required): Display name
- `email` (optional): Email address

**Example Request:**
```json
{"type":"register","username":"user1","password":"pass1","displayName":"User One","email":"user1@example.com"}
```

**Success Response:**
```json
{"type":"register_response","success":true,"userId":"f3b3bc3c-d970-4607-9e55-4bfa18c356af"}
```

**Error Response:**
```json
{"type":"register_response","success":false,"message":"User already exists"}
```

### login
Authenticate and start a session.

**Parameters:**
- `username` (required): Username
- `password` (required): Password
- `device` (optional): Device identifier

**Example Request:**
```json
{"type":"login","username":"user1","password":"pass1","device":"mobile"}
```

**Success Response:**
```json
{"type":"login_response","success":true,"userId":"f3b3bc3c-d970-4607-9e55-4bfa18c356af","sessionToken":"dZem4qvLOwuifWV6wqtKPMSY0qWNCd8yLCloArg2TAY"}
```

**Error Response:**
```json
{"type":"login_response","success":false,"message":"Invalid credentials"}
```

### logout
End the current session.

**Parameters:**
- `username` (required): Username to logout

**Example Request:**
```json
{"type":"logout","username":"user1"}
```

**Response:**
```json
{"type":"logout_response","success":true}
```

## Conversation Management

### get_conversations
Retrieve all conversations for the authenticated user.

**Parameters:** None (requires authentication)

**Example Request:**
```json
{"type":"get_conversations"}
```

**Success Response:**
```json
{"type":"conversations_response","conversations":[{"id":"d6c0b33c-ab21-4492-ae10-9d7f15a9b55b","name":"User One & User Two","isGroup":false},{"id":"e7d1c44d-bc32-5503-bf21-0e8g26b0c66c","name":"Group Chat","isGroup":true}]}
```

**Error Response (not authenticated):**
```json
{"type":"error","code":"not_authenticated","message":"Must be logged in to get conversations"}
```

### create_conversation
Create a new conversation (1-on-1 or group).

**Parameters (1-on-1):**
- `otherUsername` (required): Username of the other participant

**Parameters (group):**
- `name` (required): Group name
- `participants` (required): Comma-separated list of usernames

**Example 1-on-1 Request:**
```json
{"type":"create_conversation","otherUsername":"user2"}
```

**Example Group Request:**
```json
{"type":"create_conversation","name":"Project Team","participants":"user2,user3"}
```

**Success Response:**
```json
{"type":"create_conversation_response","success":true,"conversationId":"d6c0b33c-ab21-4492-ae10-9d7f15a9b55b"}
```

**Error Response:**
```json
{"type":"error","code":"invalid_args","message":"Provide 'otherUsername' for 1-on-1 or 'name' and 'participants' for group"}
```

### add_participant
Add a participant to a group conversation.

**Parameters:**
- `conversationId` (required): Conversation ID
- `participantId` (required): User ID to add

**Example Request:**
```json
{"type":"add_participant","conversationId":"d6c0b33c-ab21-4492-ae10-9d7f15a9b55b","userID":"03455d57-ccbc-4a3e-81a5-9f5fdeb5c129"}
```

**Success Response:**
```json
{"type":"add_participant_response","success":true}
```

### remove_participant
Remove a participant from a group conversation.

**Parameters:**
- `conversationId` (required): Conversation ID
- `participantId` (required): User ID to remove

**Example Request:**
```json
{"type":"remove_participant","conversationId":"d6c0b33c-ab21-4492-ae10-9d7f15a9b55b","userId":"03455d57-ccbc-4a3e-81a5-9f5fdeb5c129"}
```

**Success Response:**
```json
{"type":"remove_participant_response","success":true}
```

## Messaging

### send_dm
Send a direct message to another user.

**Parameters:**
- `conversationId` (required): Conversation ID
- `senderId` (required): Sender's user ID
- `content` (required): Message content
- `recipientId` (required): Recipient's user ID

**Example Request:**
```json
{"type":"send_dm","conversationId":"d6c0b33c-ab21-4492-ae10-9d7f15a9b55b","senderId":"f3b3bc3c-d970-4607-9e55-4bfa18c356af","content":"Hello!","recipientId":"03455d57-ccbc-4a3e-81a5-9f5fdeb5c129"}
```

**Success Response:**
```json
{"type":"message_response","success":true}
```

**Error Response:**
```json
{"type":"error","code":"invalid_args","message":"'conversationId', 'senderId', 'content' and 'recipientId' required"}
```

### send_group
Send a message to all participants in a group conversation.

**Parameters:**
- `conversationId` (required): Group conversation ID
- `senderId` (required): Sender's user ID
- `content` (required): Message content

**Example Request:**
```json
{"type":"send_group","conversationId":"e7d1c44d-bc32-5503-bf21-0e8g26b0c66c","senderId":"f3b3bc3c-d970-4607-9e55-4bfa18c356af","content":"Meeting at 3 PM"}
```

**Success Response:**
```json
{"type":"message_response","success":true}
```

## Utility Commands

### ping
Test server connectivity.

**Parameters:** None

**Example Request:**
```json
{"type":"7ekey"}
```

**Response:**
```json
{"type":"mekey"}
```

## Error Responses

All commands can return error responses in the following format:

```json
{"type":"error","code":"error_code","message":"Error description"}
```

Common error codes:
- `invalid_protocol`: Missing 'type' field
- `invalid_args`: Missing or invalid parameters
- `not_authenticated`: Authentication required
- `unknown_command`: Unknown command type
- `server_error`: Internal server error

## Notes

- All commands requiring authentication must be sent after a successful login
- User IDs are UUIDs returned during registration/login
- Conversation IDs are UUIDs returned when creating conversations
- Messages are delivered asynchronously to online recipients
- Group messages exclude the sender from recipients
