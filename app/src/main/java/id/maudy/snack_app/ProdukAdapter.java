package id.maudy.snack_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import id.maudy.snack_app.R;

public class ProdukAdapter extends RecyclerView.Adapter<ProdukAdapter.ProdukViewHolder> {

    private Context context;
    private List<Produk> produkList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Produk produk);
    }

    public ProdukAdapter(Context context, List<Produk> produkList, OnItemClickListener listener) {
        this.context = context;
        this.produkList = produkList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProdukViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_produk, parent, false);
        return new ProdukViewHolder(view);
    }

    // PERBAIKAN: Parameter kedua diganti dari 'int viewType' menjadi 'int position'
    @Override
    public void onBindViewHolder(@NonNull ProdukViewHolder holder, int position) {
        // Mengambil data produk berdasarkan urutan posisi yang benar
        Produk produk = produkList.get(position);

        holder.tvNama.setText(produk.getNama());
        holder.tvKategori.setText(produk.getKategori());
        holder.tvHarga.setText("Rp " + produk.getHarga());

        // Cek status sold out
        if ("Sold Out".equalsIgnoreCase(produk.getStatus())) {
            holder.tvSoldOut.setVisibility(View.VISIBLE);
        } else {
            holder.tvSoldOut.setVisibility(View.GONE);
        }

        // Dekode Base64 String ke Bitmap untuk Gambar
        String base64Image = produk.getGambarBase64();
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.imgProduk.setImageBitmap(bitmap);
            } catch (Exception e) {
                // Jika decode gagal, pasang gambar default report bawaan android
                holder.imgProduk.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        } else {
            holder.imgProduk.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        // Aksi Klik ke Detail
        holder.itemView.setOnClickListener(v -> listener.onItemClick(produk));
    }

    @Override
    public int getItemCount() {
        return produkList.size();
    }

    public static class ProdukViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduk;
        TextView tvNama, tvKategori, tvHarga, tvSoldOut;

        public ProdukViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduk = itemView.findViewById(R.id.imgProduk);
            tvNama = itemView.findViewById(R.id.tvNamaProduk);
            tvKategori = itemView.findViewById(R.id.tvKategoriProduk);
            tvHarga = itemView.findViewById(R.id.tvHargaProduk);
            tvSoldOut = itemView.findViewById(R.id.tvStatusSoldOut);
        }
    }
}