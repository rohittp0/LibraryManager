package com.make.it.kit.librarymanager;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class Home extends Fragment
{

    private static final float BOOK_SIZE = 130;
    private List<Book> listBooks;
    private View view;
    private RecyclerView recyclerView;
    private RecyclerViewAdapter adapter;

    static Home newInstance(List<Book> books)
    {
        Home home = new Home();
        home.listBooks = books;

        return home;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.fragment_home, container, false);
        recyclerView = view.findViewById(R.id.home_recycler_view);
        if (recyclerView.getAdapter() == null)
            adapter = new RecyclerViewAdapter(getContext(), listBooks);
        recyclerView.setAdapter(adapter);
        if (recyclerView.getLayoutManager() == null)
            recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), getCount(view)));
        return view;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        if (recyclerView != null)
            recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), getCount(view)));
    }

    private int getCount(@NonNull View view)
    {
        int width = getResources().getDisplayMetrics().widthPixels;
        View container = view.findViewById(R.id.home_recycler_view);

        width -= container.getPaddingStart();
        width -= container.getPaddingEnd();

        int cols = width / Math.round(
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, BOOK_SIZE
                        , getResources().getDisplayMetrics()));
        System.out.println("Number of columns = " + cols);

        return cols > 0 ? cols < listBooks.size() ? cols : listBooks.size() : 1;
    }

    void setData(ArrayList<Book> books)
    {
        adapter.setData(books);
    }
}
