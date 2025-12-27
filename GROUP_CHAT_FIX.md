# Group Chat Fix - Simplified Polling Approach

## Problem

The original implementation attempted to maintain persistent server connections for real-time group message routing, which caused the error:

```
Lỗi không xác định: invalid type code: AC
```

This error occurred because:

1. Multiple ObjectOutputStream instances were created for the same socket
2. The stream header (0xACED - "AC" in the error) was written multiple times
3. ObjectInputStream couldn't properly deserialize the malformed stream

## Solution

Simplified the architecture to use **database polling** instead of real-time server routing.

### Architecture Change

**Before (Broken)**:

```
Client A ──→ Server (maintains persistent connection)
                ↓ (routes messages)
Client B ←──────┘
```

**After (Working)**:

```
Client A ──→ Database ──→ Client B (polls every 2 seconds)
```

### Key Changes

#### 1. Removed Server-Side Routing

**File**: `ServerCore.java`

- Removed `clientStreams` map
- Removed `handleGroupMessage()` method
- Removed `startClientListener()` method
- Restored original simple request-response pattern

#### 2. Simplified Client Connection

**File**: `Client.java`

- Removed `serverSocket` and `serverOut` fields
- Removed `getServerOutputStream()` method
- No persistent server connection needed

#### 3. Changed GroupChatFrame to Polling

**File**: `GroupChatFrame.java`

**Added**:

```java
private int lastMessageId = 0; // Track last seen message
private javax.swing.Timer pollTimer; // Poll every 2 seconds

private void startMessagePolling() {
    pollTimer = new javax.swing.Timer(2000, e -> checkForNewMessages());
    pollTimer.start();
}

private void checkForNewMessages() {
    // Query database for messages with messageId > lastMessageId
    // Display new messages in UI
}
```

**Message Flow**:

1. User types message
2. Save to database via `GroupDAO.saveGroupMessage()`
3. Display locally (blue bubble)
4. Other clients poll database every 2 seconds
5. New messages appear automatically (white bubble)

#### 4. Updated MainFrame

**File**: `MainFrame.java`

- Changed `GroupChatFrame` constructor call
- Pass `Client` object instead of `ObjectOutputStream`

### How It Works Now

#### Sending a Message

```java
private void sendGroupMessage() {
    String content = txtMessage.getText().trim();

    // 1. Display locally (immediate feedback)
    addMessageBubble(currentUser, content, "right", ...);

    // 2. Save to database
    GroupDAO.saveGroupMessage(groupId, currentUser, content);

    // 3. Other clients will see it within 2 seconds via polling
}
```

#### Receiving Messages

```java
private void checkForNewMessages() {
    // 1. Load all messages from database
    List<GroupMessage> allMessages = GroupDAO.loadGroupMessages(groupId);

    // 2. Filter for new messages (messageId > lastMessageId)
    List<GroupMessage> newMessages = ...;

    // 3. Display new messages
    for (GroupMessage msg : newMessages) {
        addMessageBubbleWithTime(msg.sender(), msg.content(), ...);
        lastMessageId = msg.messageId(); // Update tracker
    }
}
```

### Advantages of Polling Approach

✅ **Simple**: No complex server routing logic  
✅ **Reliable**: Database is source of truth  
✅ **No Serialization Issues**: Standard request-response pattern  
✅ **Works Offline**: Messages persist even if recipient is offline  
✅ **Scales**: Database handles concurrent access

### Disadvantages

⚠️ **Latency**: 2-second delay before messages appear  
⚠️ **Database Load**: Queries every 2 seconds per group  
⚠️ **Not Real-Time**: Not suitable for instant messaging expectations

### Configuration

You can adjust the polling interval in `GroupChatFrame.java`:

```java
// Poll every 2 seconds (default)
pollTimer = new javax.swing.Timer(2000, e -> checkForNewMessages());

// For faster updates (more database load):
pollTimer = new javax.swing.Timer(1000, e -> checkForNewMessages());

// For lower load (slower updates):
pollTimer = new javax.swing.Timer(5000, e -> checkForNewMessages());
```

### Future Improvements

If you need real-time messaging, consider:

1. **WebSocket Protocol**: Replace ObjectStreams with WebSocket for bidirectional communication
2. **Message Queue**: Use RabbitMQ or Kafka for message routing
3. **Push Notifications**: Server pushes updates instead of client polling
4. **Long Polling**: Client keeps connection open until new messages arrive

### Testing

To verify the fix works:

1. Start the server
2. Login with User A
3. Create a group and add User B
4. Login with User B (separate client)
5. User B should see the group in "My Groups"
6. User A sends message in group
7. Within 2 seconds, User B sees the message (no "AC" error!)

### Error Resolution

The original error is now fixed because:

- ✅ No persistent server connections
- ✅ No multiple ObjectOutputStream instances
- ✅ No stream header conflicts
- ✅ Simple database read/write operations
- ✅ Standard ObjectInputStream/ObjectOutputStream usage for server communication

The application now uses the proven request-response pattern that already works for user registration and peer discovery.
