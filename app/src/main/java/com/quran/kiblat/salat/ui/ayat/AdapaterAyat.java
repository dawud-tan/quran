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
import com.quran.kiblat.salat.CustomDialogFragment;
import com.quran.kiblat.salat.R;

import java.text.NumberFormat;
import java.util.List;

public class AdapaterAyat extends RecyclerView.Adapter<AdapaterAyat.PemegangView> {

    private final List<Ayat> daftarAyat;
    private final NumberFormat numberFormat;
    private final SharedPreferences sharedPref;
    private final boolean nightModeFlags;

    public AdapaterAyat(final List<Ayat> daftarAyat, NumberFormat numberFormat, Context konteks, boolean nightModeFlags) {
        this.daftarAyat = daftarAyat;
        this.numberFormat = numberFormat;
        sharedPref = konteks.getSharedPreferences("pref", Context.MODE_PRIVATE);
        this.nightModeFlags = nightModeFlags;
    }

    @NonNull
    @Override
    public PemegangView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.satuan_ayat, parent, false);
        return new PemegangView(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PemegangView holder, int position) {

        SharedPreferences.Editor editor = sharedPref.edit();
        Ayat ayat = daftarAyat.get(position);
        int nomorAyat = ayat.ayatke();
        holder.ayatke.setText((nomorAyat > 0 ? numberFormat.format(nomorAyat) : ""));
        holder.arab.setText(ayat.arab());

        Context wrapper = new ContextThemeWrapper(holder.itemView.getContext(), R.style.Theme_Quran);
        PopupMenu popup = new PopupMenu(wrapper, holder.textViewOptions);
        popup.inflate(R.menu.aksi_ayat);

        if (nightModeFlags)
            holder.textViewOptions.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black)
            ));
        else {
            holder.textViewOptions.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.daluang)
            ));
        }

        holder.textViewOptions.setOnClickListener(view -> {
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.terjemahan) {
                    CustomDialogFragment.newInstance(ayat)
                            .show(((FragmentActivity) holder.itemView.getContext()).getSupportFragmentManager(), "1");
                    return true;
                } else if (item.getItemId() == R.id.terakhirDibaca) {
                    editor.putInt("bindingAdapterPosition", holder.getBindingAdapterPosition());
                    editor.putInt("juzke", ayat.juzke());
                    editor.putInt("suratke", ayat.suratke());
                    editor.putString("judul", ayat.judul());
                    editor.apply();
                    return true;
                } else {
                    return false;
                }
            });
            popup.show();

        });
    }

    @Override
    public int getItemCount() {
        return daftarAyat.size();
    }

    @Override
    public void onViewRecycled(AdapaterAyat.PemegangView holder) {
        holder.itemView.setOnLongClickListener(null);
        super.onViewRecycled(holder);
    }

    public static final class PemegangView extends RecyclerView.ViewHolder {
        final public MaterialTextView arab, ayatke;
        final public MaterialButton textViewOptions;

        public PemegangView(@NonNull View itemView) {
            super(itemView);
            arab = itemView.findViewById(R.id.arab);
            ayatke = itemView.findViewById(R.id.ayatke);
            textViewOptions = itemView.findViewById(R.id.textViewOptions);
        }
    }

}