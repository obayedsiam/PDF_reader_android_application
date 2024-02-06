package com.example.iqra.Activity;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iqra.R;

public class MainViewHolder extends RecyclerView.ViewHolder {
    public TextView txtName;

    public TextView lastAccessDate;

    public ImageView pdfImageView;

    public TextView pdfSize;

    public CardView cardView;

    public MainViewHolder(@NonNull View itemView) {
        super(itemView);

        txtName = itemView.findViewById(R.id.pdf_textName);
//        lastAccessDate = itemView.findViewById(R.id.lastAccessDate);
        pdfSize = itemView.findViewById(R.id.pdfSize);
        cardView = itemView.findViewById(R.id.pdf_cardView);
        pdfImageView = itemView.findViewById(R.id.pdf_imageView);
    }
}
