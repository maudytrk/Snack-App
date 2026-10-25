package id.maudy.snack_app;

public class Message {
    private String messageId;
    private String senderId;
    private String text;
    private long timestamp;

    public Message() {}

    public Message(String messageId, String senderId, String text, long timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getMessageId() { return messageId; }
    public String getSenderId() { return senderId; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }
}