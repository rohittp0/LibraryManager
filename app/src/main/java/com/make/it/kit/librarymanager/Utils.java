package com.make.it.kit.librarymanager;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.github.mikephil.charting.data.BarEntry;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static android.content.Context.CAMERA_SERVICE;

@SuppressWarnings("unused")
final class Utils
{
    static final String API_KEY = "AIzaSyCvmNRcN-WGh9jy6vgHb8XM4s4D2rDdOxs";
    private static final char[] PUNCTUATIONS = {'.', ' ', '-'};
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static
    {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Creates a temporary file and returns it's Uri after deleting it.
     *
     * @return Uri of created file
     *
     * @throws IOException If unable to create temp directory , unable to create temp file or
     *                     unable to delete temp file.
     */
    static Uri createTemporaryFile() throws IOException
    {
        File tempDir = Environment.getExternalStorageDirectory();
        tempDir = new File(tempDir.getAbsolutePath() + "/.temp/");
        if (!tempDir.exists())
        {
            if (!tempDir.mkdirs()) throw new IOException("Unable to create temp.");
        }
        File ret = File.createTempFile("picture", ".png", tempDir);
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
     *
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

    static void setTexts(@NonNull View header, @NonNull FirebaseAuth auth)
    {
        TextView user = header.findViewById(R.id.nav_user_name);
        TextView email = header.findViewById(R.id.nav_email_id);
        if (auth.getCurrentUser() != null)
        {
            user.setText(auth.getCurrentUser().getDisplayName());
            if (auth.getCurrentUser().getEmail() != null)
                email.setText(auth.getCurrentUser().getEmail());
            else email.setText(auth.getCurrentUser().getPhoneNumber());
        }
    }

    static boolean isInside(@NonNull MotionEvent event, @NonNull Rect rect)
    {
        return event.getX() >= rect.left && event.getX() <= rect.right && event.getY() >= rect.top
                && event.getY() <= rect.bottom;
    }

    static void showToast(String message, Context context)
    {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    static boolean isConnected(Context context)
    {
        try
        {
            switch (new CheckInternet().execute(context).get())
            {
                case CheckInternet.INTERNET_ACCESS:
                    return true;
                case CheckInternet.NO_NETWORK_ACCESS:
                    alert(context.getString(R.string.no_network_message), context);
                case CheckInternet.NO_INTERNET_ACCESS:
                    alert(context.getString(R.string.no_internet_message), context);
                case CheckInternet.UNABLE_TO_CHECK:
                    alert(context.getString(R.string.error_checking_network), context);
                case CheckInternet.ERROR:
                    alert(context.getString(R.string.error_message), context);
            }
        } catch (InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
            //TODO add crashlytics
        }
        return false;
    }

    static void alert(CharSequence message, Context context)
    {
        AlertDialog alert = new AlertDialog.Builder(context).create();
        alert.setTitle("Error");
        alert.setIcon(R.drawable.ic_error);
        alert.setButton(AlertDialog.BUTTON_POSITIVE, "Ok",
                (dialog, which) -> dialog.dismiss());
        alert.setMessage(message);
        alert.show();
    }

    static List<List> extract(@NonNull Task<QuerySnapshot> task)
    {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<List> data = new ArrayList<>();
        final DocumentSnapshot[] docs =
                Objects.requireNonNull(task.getResult()).getDocuments().toArray(new DocumentSnapshot[0]);
        for (int i = 0; i < docs.length; i++)
        {
            entries.add(new BarEntry((float) i,
                    Objects.requireNonNull(docs[i].getDouble("Books")).floatValue()));
            labels.add(docs[i].getString("Name"));
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

    @Nullable
    static Camera getCameraInstance()
    {
        Camera c = null;
        try
        {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e)
        {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    /**
     * Get the angle by which an image must be rotated given the device's current
     * orientation.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static int getRotationCompensation(String cameraId, @NonNull Activity activity,
                                       @NonNull Context context)
    {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        CameraManager cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        int sensorOrientation = 0;
        try
        {
            sensorOrientation = Objects.requireNonNull(cameraManager
                    .getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.SENSOR_ORIENTATION));
        } catch (CameraAccessException | NullPointerException e)
        {
            e.printStackTrace(); //TODO add crashlytics
        }
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360;

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        int result;
        switch (rotationCompensation)
        {
            case 0:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                result = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                result = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                result = FirebaseVisionImageMetadata.ROTATION_270;
                break;
            default:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                System.out.println("Bad rotation value: " + rotationCompensation);
        }
        return result;
    }

    @NonNull
    @Contract("_ -> new")
    static String format(String string)
    {
        string = string.toLowerCase().trim();
        if(string.isEmpty()) return "";
        char[] strings = string.toCharArray();
        strings[0] = (strings[0] + "").toUpperCase().charAt(0);
        for (int i = 0; i < strings.length-1; i++)
            for (final char punctuation : PUNCTUATIONS)
                if (punctuation == strings[i])
                    strings[i+1] = (strings[i+1] + "").toUpperCase().charAt(0);
        return new String(strings);
    }
}

class CheckInternet extends AsyncTask<Context, Void, Integer>
{

    static final int INTERNET_ACCESS = 0;
    static final int ERROR = 1;
    static final int NO_INTERNET_ACCESS = 2;
    static final int UNABLE_TO_CHECK = 3;
    static final int NO_NETWORK_ACCESS = 4;

    @Override
    protected Integer doInBackground(Context... params)
    {
        boolean isConnected;
        try
        {
            Context context = params[0];
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            assert cm != null;
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null &&
                    activeNetwork.isConnected();
        } catch (Exception error)
        {
            error.printStackTrace(); // TODO add crashlytics
            return UNABLE_TO_CHECK;
        }


        if (isConnected)
        {
            try
            {
                HttpURLConnection url = (HttpURLConnection)
                        (new URL("http://clients3.google.com/generate_204")
                                .openConnection());
                url.setRequestProperty("User-Agent", "Android");
                url.setRequestProperty("Connection", "close");
                url.setConnectTimeout(1500);
                url.connect();
                if (url.getResponseCode() == 204 &&
                        url.getContentLength() == 0)
                    return INTERNET_ACCESS;

            } catch (IOException e)
            {
                e.printStackTrace(); // TODO add catalytic
                return NO_INTERNET_ACCESS;
            }
        } else
            return NO_NETWORK_ACCESS;
        return ERROR;
    }

}

