package com.make.it.kit.librarymanager;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.algolia.search.saas.Client;
import com.algolia.search.saas.Index;
import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.viewHolder>
{
    static final String CURRENT_BOOK = "com.make.it.kit.currentBook";
    private final Context mContext;
    private List<Book> mData;
    private final Dialog popup;
    //Algolia
    private Index index;

    RecyclerViewAdapter(@NonNull Context mContext, List<Book> books)
    {
        this.mContext = mContext;
        this.mData = books;
        index = new Client(mContext.getString(R.string.algolia_application_id),
                MainActivity.ALGOLIA_API_KEY).getIndex(mContext.getString(R.string.algolia_index));
        popup = new Dialog(mContext, R.style.Dialog_FrameLess);
        popup.setContentView(R.layout.book_popup);
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

            final TextView[] textViews = {
                    popup.findViewById(R.id.book_popup_name),
                    popup.findViewById(R.id.book_popup_author),
                    popup.findViewById(R.id.book_popup_price),
                    popup.findViewById(R.id.book_popup_category)};
            final ImageView cover = popup.findViewById(R.id.book_popup_cover_photo);
            final MaterialButton edit = popup.findViewById(R.id.book_popup_edit);
            final MaterialButton delete = popup.findViewById(R.id.book_popup_delete);

            cBook.toScreen(textViews, null, mContext);

            cover.setImageDrawable(holder.img.getDrawable());
            edit.setOnClickListener((view) ->
            {
                Intent intent = new Intent(mContext, EditWindow.class);
                intent.putExtra(CURRENT_BOOK, cBook.getSelfRef().getPath());
                mContext.startActivity(intent);
                popup.dismiss();
            });
            delete.setOnClickListener((view) ->
                    new AlertDialog.Builder(mContext)
                            .setMessage(R.string.delete_prompt)
                            .setPositiveButton("Yes", (dialogInterface, index) ->
                                    cBook.getSelfRef().delete()
                                            .addOnSuccessListener(Null ->
                                                    this.index.deleteObjectAsync(cBook.getSelfRef().getPath(),
                                                            (jsonObject, algoliaException) ->
                                                            {
                                                                if (algoliaException != null)
                                                                    Utils.alert(R.string.failed_algolia_delete, mContext);
                                                                else
                                                                    Utils.showToast(R.string.book_delete_success, mContext);
                                                                dialogInterface.dismiss();
                                                            }))
                                            .addOnFailureListener((error) ->
                                                    Utils.alert(R.string.book_delete_failed
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
