package com.quran.kiblat.salat;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.quran.kiblat.salat.databinding.TulisanArtiBinding;
import com.quran.kiblat.salat.ui.ayat.Ayat;

import java.util.Objects;

public class CustomDialogFragment extends DialogFragment {

    private View theDialogView;
    private TulisanArtiBinding ikatan;

    public static CustomDialogFragment newInstance(Ayat pesan) {
        CustomDialogFragment frag = new CustomDialogFragment();
        Bundle args = new Bundle();
        args.putString("latin", pesan.latin());
        args.putString("surat", pesan.judul());
        args.putInt("ayatke", pesan.ayatke());
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = requireArguments();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity())
                .setTitle("Terjemahan surat " + args.getString("surat") + " ayat ke " + args.getInt("ayatke"))
                .setNeutralButton("Ok \uD83D\uDC4C", null);
        ikatan = TulisanArtiBinding.inflate(getLayoutInflater());
        String latin = args.getString("latin", "latin");
        ikatan.tulisan.setText(latin);
        theDialogView = ikatan.getRoot();
        builder.setView(theDialogView);
        return builder.create();
    }

    @Override
    public View getView() {
        return theDialogView;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setCancelable(false);
        View view = inflater.inflate(R.layout.tulisan_arti, container);
        Objects.requireNonNull(requireDialog().getWindow()).setBackgroundDrawable(
                new ColorDrawable(ContextCompat.getColor(requireContext(), R.color.daluang)));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Objects.requireNonNull(requireDialog().getWindow()).setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ikatan = null;
    }

}