package com.make.it.kit.librarymanager;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.viewHolder>
{
    private final Context mContext;
    private List<Book> mData;
    private final Dialog popup;
    private final Dialog editWindow;

    RecyclerViewAdapter(Context mContext, List<Book> books)
    {
        this.mContext = mContext;
        this.mData = books;
        popup = new Dialog(mContext, R.style.Dialog_FrameLess);
        popup.setContentView(R.layout.book_popup);
        editWindow = new Dialog(mContext, R.style.Dialog_FrameLess);
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i)
    {
        return new viewHolder(LayoutInflater.from(mContext).inflate(R.layout.book,
                parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final viewHolder holder, final int position)
    {
        final Book cBook = mData.get(position);
        holder.tv_name.setText(cBook.getName());
        if (cBook.getPhoto() != null && cBook.getPhoto().length() > 0)
            Picasso.get().load(cBook.getPhoto()).into(holder.img);
        else holder.img.setImageResource(cBook.getPhotoID());
        holder.cardView.setOnClickListener(v ->
        {

            final TextView author = popup.findViewById(R.id.book_popup_author);
            final TextView category = popup.findViewById(R.id.book_popup_category);
            final TextView price = popup.findViewById(R.id.book_popup_price);
            final TextView name = popup.findViewById(R.id.book_popup_name);
            final ImageView cover = popup.findViewById(R.id.book_popup_cover_photo);
            final MaterialButton edit = popup.findViewById(R.id.book_popup_edit);
            final MaterialButton delete = popup.findViewById(R.id.book_popup_delete);

            NumberFormat formatter = NumberFormat.getNumberInstance();
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);

            name.setText(cBook.getName());
            author.setText(cBook.getAuthor());
            category.setText(cBook.getCategory());
            price.setText(mContext.getString(R.string.book_popup_rupee_sign,
                    formatter.format(cBook.getPrice())));
            cover.setImageDrawable(holder.img.getDrawable());

            edit.setOnClickListener((view) ->
            {

            });
            delete.setOnClickListener((view) ->
                    new AlertDialog.Builder(mContext)
                            .setMessage(R.string.delete_prompt)
                            .setPositiveButton("Yes", (dialogInterface, index) ->
                                    cBook.getSelfRef().delete()
                                            .addOnSuccessListener(aVoid ->
                                                    Utils.showToast("Book successfully deleted!"
                                                            , mContext))
                                            .addOnFailureListener((error) ->
                                                    Utils.alert("Unable to delete Book."
                                                            , mContext)))
                            .setNegativeButton("No", (dialogInterface, index) ->
                                    dialogInterface.dismiss())
                            .create().show());

            popup.show();
        });

    }

    @Override
    public int getItemCount()
    {
        return mData == null ? 0 : mData.size();
    }

    void setData(ArrayList<Book> books)
    {
        mData = books;
        notifyDataSetChanged();
    }

    static class viewHolder extends RecyclerView.ViewHolder
    {

        private final TextView tv_name;
        private final ImageView img;
        private final CardView cardView;

        viewHolder(@NonNull View itemView)
        {
            super(itemView);
            tv_name = itemView.findViewById(R.id.preview_book_name);
            img = itemView.findViewById(R.id.preview_book_cover_photo);
            cardView = itemView.findViewById(R.id.book);
        }
    }

}
