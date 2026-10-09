package com.quran.kiblat.salat.ui.ayat;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.quran.kiblat.salat.R;
import com.quran.kiblat.salat.databinding.TulisanArtiBinding;

/**
 * Terjemahan satu ayat, dari menu "Terjemahan" di {@link AdapterAyat}.
 * Selebar layar, berlatar warna daluang, dan hanya ditutup lewat tombolnya.
 */
public class DialogTerjemahan extends DialogFragment {

    static final String TAG = "terjemahan";

    private static final String ARG_TERJEMAHAN = "latin";
    private static final String ARG_SURAT = "surat";
    private static final String ARG_AYAT = "ayatke";

    public static DialogTerjemahan untuk(Ayat ayat) {
        DialogTerjemahan dialog = new DialogTerjemahan();
        Bundle args = new Bundle();
        args.putString(ARG_TERJEMAHAN, ayat.latin());
        args.putString(ARG_SURAT, ayat.judul());
        args.putInt(ARG_AYAT, ayat.ayatke());
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);
        Bundle args = requireArguments();
        TulisanArtiBinding ikatan = TulisanArtiBinding.inflate(getLayoutInflater());
        ikatan.tulisan.setText(args.getString(ARG_TERJEMAHAN, ""));

        return new MaterialAlertDialogBuilder(requireActivity())
                .setTitle("Terjemahan surat " + args.getString(ARG_SURAT) + " ayat ke " + args.getInt(ARG_AYAT))
                .setView(ikatan.getRoot())
                .setNeutralButton("Ok 👌", null)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Window jendela = requireDialog().getWindow();
        if (jendela != null) {
            jendela.setBackgroundDrawable(new ColorDrawable(
                    ContextCompat.getColor(requireContext(), R.color.daluang)));
            jendela.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
