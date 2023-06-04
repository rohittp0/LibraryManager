package com.make.it.kit.librarymanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.firestore.DocumentSnapshot;

import org.jetbrains.annotations.Contract;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

final class Utils
{
    private static final char[] PUNCTUATIONS = {'.', ' ', '-'};

    /**
     * Creates a temporary file and returns it's Uri after deleting it.
     *
     * @return Uri of created file
     * @throws IOException If unable to create temp directory , unable to create temp file or
     *                     unable to delete temp file.
     */
    static Uri createTemporaryFile(Context context) throws IOException
    {
        File ret = File.createTempFile("picture", ".png", context.getExternalCacheDir());
        if (!ret.delete()) throw new IOException("Unable to delete file.");
        return Uri.fromFile(ret);
    }

    @Contract("null -> true")
    static boolean checkNull(String string)
    {
        return string == null || string.isEmpty() || string.trim().length() <= 0 || string.toLowerCase().trim().equals("null");
    }

    @NonNull
    @Contract("null -> !null")
    static String toSafeFileName(TextView text)
    {
        if (text == null || text.getText() == null) return " ";
        return text.getText().toString().replaceAll("/", "").trim();
    }

    static Bitmap scaleToFit(Bitmap img, @NonNull View view)
    {
        return Bitmap.createScaledBitmap(img, getSize(view)[0] - 5, getSize(view)[1] - 5, false);
    }

    /**
     * Returns the width and height of the passed view after subtracting padding.
     * First element of the returned array is the width and second is the height.
     *
     * @param view The view to find size of.
     * @return An int array containing width and height.
     */
    @NonNull
    @Contract("null -> fail")
    private static int[] getSize(View view)
    {
        if (view == null) throw new IllegalArgumentException("View can't be null");
        final int paddingHorizontal = view.getPaddingStart() + view.getPaddingEnd();
        final int paddingVertical = view.getPaddingTop() + view.getPaddingBottom();
        return new int[]{
                view.getWidth() - paddingHorizontal,
                view.getHeight() - paddingVertical
        };
    }

    static boolean isInside(@NonNull MotionEvent event, @NonNull Rect rect)
    {
        return event.getX() >= rect.left && event.getX() <= rect.right && event.getY() >= rect.top
                && event.getY() <= rect.bottom;
    }

    static void showToast(int message, Context context)
    {
        Toast.makeText(context, context.getString(message), Toast.LENGTH_LONG).show();
    }

    static boolean isConnected(Context context)
    {
        try
        {
            switch (new CheckInternet().execute(context).get())
            {
                case R.integer.internet_access:
                    return true;
                case R.integer.no_network_acess:
                    alert(R.string.no_network_message, context);
                case R.integer.no_internet_access:
                    alert(R.string.no_internet_message, context);
                case R.integer.unable_to_check_internet:
                    alert(R.string.error_checking_network, context);
                case R.integer.internet_error:
                    alert(R.string.error_message, context);
            }
        } catch (InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
            //TODO add crashlytics
        }
        return false;
    }

    static void alert(int message, Context context)
    {
        AlertDialog alert = new AlertDialog.Builder(context).create();
        alert.setTitle("Error");
        alert.setIcon(R.drawable.ic_error);
        alert.setButton(AlertDialog.BUTTON_POSITIVE, "Ok",
                (dialog, which) -> dialog.dismiss());
        alert.setMessage(context.getString(message));
        alert.show();
    }

    @NonNull
    static List<List> extract(@NonNull List<DocumentSnapshot> docs)
    {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<List> data = new ArrayList<>();

        for (int i = 0; i < docs.size(); i++)
        {
            entries.add(new BarEntry((float) i,
                    Objects.requireNonNull(docs.get(i).getDouble("Books")).floatValue()));
            labels.add(docs.get(i).getString("Name"));
        }
        data.add(entries);
        data.add(labels);
        return data;
    }

    @org.jetbrains.annotations.Nullable
    @org.jetbrains.annotations.Contract(pure = true)
    static String getNameOfFragment(int id)
    {
        return id == R.id.nav_home ? "home" : id == R.id.nav_add ? "add" :
                id == R.id.nav_profile ? "profile" : null;
    }

    @NonNull
    @Contract("_ -> new")
    static String format(String string)
    {
        string = string.toLowerCase().trim();
        if (string.isEmpty()) return "";
        char[] strings = string.toCharArray();
        strings[0] = (strings[0] + "").toUpperCase().charAt(0);
        for (int i = 0; i < strings.length - 1; i++)
            for (final char punctuation : PUNCTUATIONS)
                if (punctuation == strings[i])
                    strings[i + 1] = (strings[i + 1] + "").toUpperCase().charAt(0);
        return new String(strings);
    }

    @NonNull
    static JSONObject mapToJSON(@NonNull Map map) throws JSONException
    {
        JSONObject object = new JSONObject();
        for (Object key : map.keySet().toArray())
            object.put((String) key, map.get(key));
        return object;
    }
}

class CheckInternet extends AsyncTask<Context, Void, Integer>
{

    @Override
    protected Integer doInBackground(Context... params)
    {
        boolean isConnected;
        try
        {
            Context context = params[0];
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (cm != null)
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    isConnected = cm.getNetworkCapabilities(cm.getActiveNetwork()) != null;
                else
                    isConnected = cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
            else isConnected = false;
        } catch (Exception error)
        {
            error.printStackTrace(); // TODO add crashlytics
            return R.integer.unable_to_check_internet;
        }


        if (isConnected)
        {
            try
            {
                HttpURLConnection url = (HttpURLConnection)
                        (new URL("https://clients3.google.com/generate_204")
                                .openConnection());
                url.setRequestProperty("User-Agent", "Android");
                url.setRequestProperty("Connection", "close");
                url.setConnectTimeout(1500);
                url.connect();
                if (url.getResponseCode() == 204 &&
                        url.getContentLength() == 0)
                    return R.integer.internet_access;

            } catch (IOException e)
            {
                e.printStackTrace(); // TODO add catalytic
                return R.integer.no_internet_access;
            }
        } else
            return R.integer.no_network_acess;
        return R.integer.internet_error;
    }

}

