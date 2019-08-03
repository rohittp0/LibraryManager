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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;


public class Home extends Fragment implements SwipeRefreshLayout.OnRefreshListener, EventListener<QuerySnapshot>
{

    private final CollectionReference bookRef = FirebaseFirestore.getInstance()
            .collection("Books");

    private static final float BOOK_SIZE = 130;

    private View view;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipe;

    private boolean Refresh = true;
    private ListenerRegistration Refresher;

    @NonNull
    static Home newInstance(boolean refresh)
    {
        final Home home = new Home();
        home.Refresh = refresh;
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
        swipe = view.findViewById(R.id.home_swipe_refresh);
        swipe.setOnRefreshListener(this);

        if (recyclerView.getLayoutManager() == null)
            recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), getCount(view)));

        if (recyclerView.getAdapter() == null && Refresh)
        {
            swipe.post(() ->
            {
                if (recyclerView.getAdapter() == null) swipe.setRefreshing(true);
            });
            Refresher = bookRef.orderBy("SavedOn", Query.Direction.DESCENDING).limit(20)
                    .addSnapshotListener(this);
        }
        return view;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (Refresher != null)
        {
            Refresher.remove();
            Refresher = null;
        }
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

    /**
     * Called when a swipe gesture triggers a refresh.
     */
    @Override
    public void onRefresh()
    {
        if (!Refresh) return;
        swipe.setRefreshing(true);
        // Create a query against the collection.
        bookRef.orderBy("SavedOn", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20).get()
                .addOnCompleteListener((task) ->
                {
                    if (task.isSuccessful() && task.getResult() != null)
                    {
                        final List<DocumentSnapshot> documents = task.getResult().getDocuments();
                        final List<Book> books = new ArrayList<>();
                        for (int i = 0; i < documents.size(); i++)
                        {
                            final Book book = documents.get(i).toObject(Book.class);
                            if (book != null)
                            {
                                book.setSelfRef(documents.get(i).getReference());
                                books.add(book);
                            }
                        }
                        recyclerView.setAdapter(new RecyclerViewAdapter(getContext(), books));
                    } else firebaseError(task.getException());
                    swipe.setRefreshing(false);
                });
    }

    private void firebaseError(@Nullable Exception error)
    {
        if (error != null) error.printStackTrace(); //TODO add crashlytics
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e)
    {
        if (e != null || queryDocumentSnapshots == null)
        {
            firebaseError(e);
            Utils.alert("Unable to get Books", getContext());
        } else
        {
            final List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();
            final List<Book> books = new ArrayList<>();
            for (int i = 0; i < documents.size(); i++)
            {
                final Book book = documents.get(i).toObject(Book.class);
                if (book != null)
                {
                    book.setSelfRef(documents.get(i).getReference());
                    books.add(book);
                }
            }
            recyclerView.setAdapter(new RecyclerViewAdapter(getContext(), books));
        }
        if (swipe.isRefreshing()) swipe.setRefreshing(false);
    }
}
