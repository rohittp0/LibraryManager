package com.make.it.kit.librarymanager;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class EditWindow extends AppCompatActivity
{
    private Book cBook;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_window);
        cBook = (Book) getIntent().getSerializableExtra(RecyclerViewAdapter.CURRENT_BOOK);
    }
}
