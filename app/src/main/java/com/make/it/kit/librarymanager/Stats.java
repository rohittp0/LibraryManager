package com.make.it.kit.librarymanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.Locale;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link Stats#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Stats extends Fragment
{
    private View view;

    private List<List> Authors;
    private List<List> Categories;

    private BarChart AuthorChart;
    private BarChart CategoryChart;

    private int bookCount = 0;
    private int authorCount = 0;
    private int categoryCount = 0;
    private int userCount = 0;

    public Stats()
    {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment profile.
     */
    @NonNull
    @Contract(" -> new")
    static Stats newInstance()
    {
        return new Stats();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_stats, container, false);

        TextView book = view.findViewById(R.id.book_count);
        TextView author = view.findViewById(R.id.author_count);
        TextView category = view.findViewById(R.id.category_count);
        TextView user = view.findViewById(R.id.user_count);

        book.setText(String.format(Locale.US, "%d", bookCount));
        author.setText(String.format(Locale.US, "%d", authorCount));
        category.setText(String.format(Locale.US, "%d", categoryCount));
        final int userCount = 0;
        user.setText(String.format(Locale.US, "%d", userCount));

        return view;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        AuthorChart = view.findViewById(R.id.author_chart);
        CategoryChart = view.findViewById(R.id.category_chart);

        HorizontalScrollView cards = view.findViewById(R.id.stats_cards_scrollView);
        cards.post(() -> cards.smoothScrollTo(view.findViewById(R.id.book_count).getWidth(), 0));

        initChart(AuthorChart, Authors.get(1), "Author Details.");
        initChart(CategoryChart, Categories.get(1), "Category Details.");

        if (Authors != null)
            updateChart(AuthorChart, Authors, "Authors");

        if (Categories != null)
            updateChart(CategoryChart, Categories, "Categories");
        super.onViewCreated(view, savedInstanceState);
    }

    private void initChart(@NonNull BarChart chart, List<String> dataSet, String popUpTitle)
    {
        chart.setDrawValueAboveBar(true);
        chart.getDescription().setEnabled(false);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        chart.setFitBars(true);
        chart.setHighlightPerDragEnabled(false);
        chart.animateXY(1000, 1000);
        chart.setNoDataText("Loading...");
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener()
        {
            /**
             * Called when a value has been selected inside the chart.
             *
             * @param e The selected Entry
             * @param h The corresponding highlight object that contains information
             */
            @Override
            public void onValueSelected(Entry e, Highlight h)
            {
                int index = (int) h.getX();
                final AlertDialog alert = new AlertDialog.Builder(Objects.requireNonNull(getContext()))
                        .create();
                alert.setMessage(dataSet.get(index) + "  " + (int) h.getY() + " Books");
                alert.setTitle(popUpTitle);
                alert.show();
            }

            /**
             * Called when nothing has been selected or an "un-select" has been made.
             */
            @Override
            public void onNothingSelected()
            {

            }
        });
    }

    @SuppressWarnings("unchecked")
    private void updateChart(@NonNull BarChart chart, @NonNull List<List> data, String label)
    {
        final BarDataSet dataSet = new BarDataSet(data.get(0), label);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        chart.setData(new BarData(dataSet));
        chart.invalidate();
    }

    void setAuthors(List<List> authors)
    {
        Authors = authors;
        if (AuthorChart != null)
            updateChart(AuthorChart, Authors, "Authors");
        this.authorCount = authors.get(0).size();
        if (view != null)
        {
            TextView text = view.findViewById(R.id.author_count);
            text.setText(String.format(Locale.US, "%d", authorCount));
        }
    }

    void setCategories(List<List> categories)
    {
        Categories = categories;
        if (CategoryChart != null)
            updateChart(CategoryChart, Categories, "Categories");
        this.categoryCount = categories.get(0).size();
        if (view != null)
        {
            TextView text = view.findViewById(R.id.category_count);
            text.setText(String.format(Locale.US, "%d", categoryCount));
        }
    }

    void setBookCount(int bookCount)
    {
        this.bookCount = bookCount;
        if (view != null)
        {
            TextView text = view.findViewById(R.id.book_count);
            text.setText(String.format(Locale.US, "%d", bookCount));
        }
    }

    void setUserCount(int userCount)
    {
        this.userCount = userCount;
        if (view != null)
        {
            TextView text = view.findViewById(R.id.user_count);
            text.setText(String.format(Locale.US, "%d", userCount));
        }
    }

}
