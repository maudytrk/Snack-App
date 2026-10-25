package id.maudy.snack_app;

public class User {
    public String uid;
    public String nama;
    public String nim;
    public String email;
    public String fotoBase64; // Default kosong, bisa diupdate di profil

    // Constructor kosong diperlukan untuk Firebase Realtime Database
    public User() {
    }

    public User(String uid, String nama, String nim, String email, String fotoBase64) {
        this.uid = uid;
        this.nama = nama;
        this.nim = nim;
        this.email = email;
        this.fotoBase64 = fotoBase64;
    }
}