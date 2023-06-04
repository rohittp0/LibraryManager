package com.make.it.kit.librarymanager;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class EditFragment extends Add
{
    private Book book;
    private EditWindow This;
    private DocumentReference bookRef;

    @NonNull
    static EditFragment newInstance(@NonNull EditWindow This)
    {
        final EditFragment editFragment = new EditFragment();
        editFragment.This = This;
        return editFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        bookRef = FirebaseFirestore.getInstance().document(
                String.valueOf(This.getIntent()
                        .getSerializableExtra(RecyclerViewAdapter.CURRENT_BOOK)));
    }

    @Override
    void addBook(@NonNull EditText[] textViews)
    {
        toggleAddingDialog(true);
        This.addBook(textViews, bookRef, bytes, book.getPhotoRef(), this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        TextView[] textViews =
                {
                        view.findViewById(R.id.add_book_name),
                        view.findViewById(R.id.add_book_author),
                        view.findViewById(R.id.add_book_price),
                        view.findViewById(R.id.add_book_category)
                };
        bookRef.get().addOnSuccessListener(bookSnapshot ->
        {
            book = bookSnapshot.toObject(Book.class);
            assert book != null;
            book.toScreen(textViews, view.findViewById(R.id.add_book_cover_photo),
                    Objects.requireNonNull(getContext()));
        })
                .addOnFailureListener(this);
        MaterialButton submit = view.findViewById(R.id.add_book_submit_button);
        submit.setText(R.string.edit_button_text);

    }
}
