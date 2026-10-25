package id.maudy.snack_app;

public class ChatRoom {
    private String chatRoomId;
    private String lawanId;
    private String namaLawan;
    private String lastMessage;
    private long timestamp;

    public ChatRoom() {}

    public ChatRoom(String chatRoomId, String lawanId, String namaLawan, String lastMessage, long timestamp) {
        this.chatRoomId = chatRoomId;
        this.lawanId = lawanId;
        this.namaLawan = namaLawan;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }

    public String getChatRoomId() { return chatRoomId; }
    public String getLawanId() { return lawanId; }
    public String getNamaLawan() { return namaLawan; }
    public String getLastMessage() { return lastMessage; }
    public long getTimestamp() { return timestamp; }
}