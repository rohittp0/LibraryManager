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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;

public class Home extends Fragment implements SwipeRefreshLayout.OnRefreshListener
{

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final float BOOK_SIZE = 130;

    private View view;
    private RecyclerView recyclerView;
    private RecyclerViewAdapter adapter;
    private SwipeRefreshLayout swipe;

    @NonNull
    @Contract(" -> new")
    static Home newInstance()
    {
        return new Home();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.fragment_home, container, false);
        recyclerView = view.findViewById(R.id.home_recycler_view);
        swipe = view.findViewById(R.id.home_swipe_refresh);
        swipe.setOnRefreshListener(this);

        if (recyclerView.getLayoutManager() == null)
            recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), getCount(view)));

        if (recyclerView.getAdapter() == null) swipe.post(this::onRefresh);
        else recyclerView.setAdapter(adapter);

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

        return cols > 0 ? cols : 1;
    }

    void setData(ArrayList<Book> books)
    {
        adapter.setData(books);
    }

    /**
     * Called when a swipe gesture triggers a refresh.
     */
    @Override
    public void onRefresh()
    {
        swipe.setRefreshing(true);
        CollectionReference bookRef = db.collection("Books");
        // Create a query against the collection.
        bookRef.orderBy("SavedOn", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20).get()
                .addOnCompleteListener((task) ->
                {
                    if (task.isSuccessful() && task.getResult() != null)
                    {
                        try
                        {
                            final List<Book> books = task.getResult().toObjects(Book.class);
                            adapter = new RecyclerViewAdapter(getContext(), books);
                            recyclerView.setAdapter(adapter);
                        } catch (Exception error)
                        {
                            firebaseError(error);
                        }
                    } else firebaseError(task.getException());
                    swipe.setRefreshing(false);
                });
    }

    private void firebaseError(@Nullable Exception error)
    {
        if(error != null) error.printStackTrace(); //TODO add crashlytics
    }
}
