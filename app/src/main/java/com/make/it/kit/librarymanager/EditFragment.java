package com.make.it.kit.librarymanager;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;

import java.util.Objects;

public class EditFragment extends Add
{
    private DocumentReference bookRef;
    private String deleteRef = null;
    private EditWindow This;

    @NonNull
    static EditFragment newInstance(@NonNull EditWindow This)
    {
        final EditFragment editFragment = new EditFragment();
        editFragment.This = This;
        return editFragment;
    }

    @Override
    void addBook(@NonNull EditText[] textViews)
    {
        toggleAddingDialog(true);
        This.addBook(textViews, bookRef, bytes, deleteRef, this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        bookRef = db.document(This.bookPath);
        bookRef.get().addOnCompleteListener((snapshotTask) ->
        {
            final Book book = Objects.requireNonNull(snapshotTask.getResult()).toObject(Book.class);
            if (book == null || !snapshotTask.getResult().exists())
                throw new NullPointerException("No such book exists.");
            deleteRef = book.getPhotoRef();
            TextView[] textViews =
                    {
                            view.findViewById(R.id.add_book_name),
                            view.findViewById(R.id.add_book_author),
                            view.findViewById(R.id.add_book_price),
                            view.findViewById(R.id.add_book_category)
                    };
            book.toScreen(textViews, view.findViewById(R.id.add_book_cover_photo),
                    Objects.requireNonNull(getContext()));
            textViews[2].setText(textViews[2].getText().subSequence(1, textViews[2].getText().length()));
        })
                .addOnFailureListener(this);
        MaterialButton submit = view.findViewById(R.id.add_book_submit_button);
        submit.setText(R.string.edit_button_text);
    }
}
