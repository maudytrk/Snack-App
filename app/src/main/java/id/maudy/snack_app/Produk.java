package id.maudy.snack_app;


public class Produk {
    private String idProduk;
    private String nama;
    private String harga;
    private String deskripsi;
    private String kategori;
    private String gambarBase64;
    private String idPenjual;
    private String status; // "Tersedia" atau "Sold Out"

    // Constructor kosong wajib untuk Firebase
    public Produk() {
    }

    public Produk(String idProduk, String nama, String harga, String deskripsi, String kategori, String gambarBase64, String idPenjual, String status) {
        this.idProduk = idProduk;
        this.nama = nama;
        this.harga = harga;
        this.deskripsi = deskripsi;
        this.kategori = kategori;
        this.gambarBase64 = gambarBase64;
        this.idPenjual = idPenjual;
        this.status = status;
    }

    // Getter dan Setter
    public String getIdProduk() { return idProduk; }
    public void setIdProduk(String idProduk) { this.idProduk = idProduk; }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }

    public String getHarga() { return harga; }
    public void setHarga(String harga) { this.harga = harga; }

    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }

    public String getGambarBase64() { return gambarBase64; }
    public void setGambarBase64(String gambarBase64) { this.gambarBase64 = gambarBase64; }

    public String getIdPenjual() { return idPenjual; }
    public void setIdPenjual(String idPenjual) { this.idPenjual = idPenjual; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}