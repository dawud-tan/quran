package com.quran.kiblat.salat.ui.ayat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.quran.kiblat.salat.R;

import java.text.NumberFormat;
import java.util.List;

/**
 * Satu baris per ayat: nomor ayat (angka Arab), teks Arab, dan tombol menu
 * (terjemahan, tandai terakhir dibaca).
 */
public class AdapterAyat extends RecyclerView.Adapter<AdapterAyat.PemegangView> {

    private final List<Ayat> daftarAyat;
    private final NumberFormat angkaArab;
    private final SharedPreferences sharedPref;
    private final boolean malam;

    public AdapterAyat(List<Ayat> daftarAyat, NumberFormat angkaArab, SharedPreferences sharedPref, boolean malam) {
        this.daftarAyat = daftarAyat;
        this.angkaArab = angkaArab;
        this.sharedPref = sharedPref;
        this.malam = malam;
    }

    @NonNull
    @Override
    public PemegangView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.satuan_ayat, parent, false);
        return new PemegangView(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PemegangView holder, int position) {
        Ayat ayat = daftarAyat.get(position);
        int nomorAyat = ayat.ayatke();
        // basmalah sisipan bernomor 0 dan tampil tanpa nomor
        holder.ayatke.setText(nomorAyat > 0 ? angkaArab.format(nomorAyat) : "");
        holder.arab.setText(ayat.arab());

        Context konteks = holder.itemView.getContext();
        holder.tombolMenu.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(konteks, malam ? android.R.color.black : R.color.daluang)));

        PopupMenu popup = new PopupMenu(new ContextThemeWrapper(konteks, R.style.Theme_Quran), holder.tombolMenu);
        popup.inflate(R.menu.aksi_ayat);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.terjemahan) {
                DialogTerjemahan.untuk(ayat)
                        .show(((FragmentActivity) konteks).getSupportFragmentManager(), DialogTerjemahan.TAG);
                return true;
            } else if (item.getItemId() == R.id.terakhirDibaca) {
                PosisiBaca.tandai(sharedPref, holder.getBindingAdapterPosition(), ayat);
                return true;
            }
            return false;
        });
        holder.tombolMenu.setOnClickListener(view -> popup.show());
    }

    @Override
    public int getItemCount() {
        return daftarAyat.size();
    }

    public static final class PemegangView extends RecyclerView.ViewHolder {
        final MaterialTextView arab, ayatke;
        final MaterialButton tombolMenu;

        public PemegangView(@NonNull View itemView) {
            super(itemView);
            arab = itemView.findViewById(R.id.arab);
            ayatke = itemView.findViewById(R.id.ayatke);
            tombolMenu = itemView.findViewById(R.id.textViewOptions);
        }
    }

}
