package com.make.it.kit.librarymanager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EditWindow extends AppCompatActivity implements View.OnClickListener, OnFailureListener
{
    private static Uri IMAGE_URI;
    private Book cBook;
    private final FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
            .getOnDeviceTextRecognizer();
    private final Map<Rect, String> TextTable = new HashMap<>();
    private int request_permission = 9447;
    private int capture_image = 1233;
    private Bitmap coverPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_window);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        cBook = (Book) getIntent().getSerializableExtra(RecyclerViewAdapter.CURRENT_BOOK);
        MaterialButton cover = findViewById(R.id.edit_select_cover_photo);
        cover.setOnClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        finish();
        return true;
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    request_permission);
        else capturePhoto();
    }

    private void capturePhoto()
    {
        try
        {
            IMAGE_URI = Utils.createTemporaryFile();
            Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Intent chooser = new Intent(Intent.ACTION_CHOOSER);

            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, IMAGE_URI);

            chooser.putExtra(Intent.EXTRA_INTENT, galleryIntent);
            chooser.putExtra(Intent.EXTRA_TITLE, getString(R.string.add_book_heading));
            Intent[] intentArray = {cameraIntent};
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
            startActivityForResult(chooser, capture_image);
        } catch (IOException error)
        {
            error.printStackTrace(); //TODO: Add crashlytics
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
            if (requestCode == capture_image)
            {
                if (data != null && data.getData() != null)
                {
                    // this case will occur in case of picking image from the Gallery,
                    // but not when taking picture with a camera
                    performCrop(data.getData());
                } else
                {
                    // this case will occur when taking a picture with a camera
                    performCrop(IMAGE_URI);
                }
            } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
            {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                try
                {
                    coverPhoto = Utils.scaleToFit(MediaStore.Images.Media.getBitmap(
                            getContentResolver(),
                            result.getUri()),
                            null);
                    new Thread(() -> getData(coverPhoto)).run();
                } catch (IOException error)
                {
                    error.printStackTrace(); //TODO: add crashlytics
                }
            }
    }

    /**
     * this function does the crop operation.
     *
     * @param picUri uri of the image to crop.
     */
    private void performCrop(Uri picUri)
    {
        CropImage.activity(picUri)
                .setAllowRotation(true)
                .setAspectRatio(5, 7)
                .setMinCropResultSize(250, 350)
                .setAllowCounterRotation(true)
                .setGuidelines(CropImageView.Guidelines.OFF)
                .setAutoZoomEnabled(true)
                .setMultiTouchEnabled(true)
                .setOutputCompressQuality(100)
                .setShowCropOverlay(true)
                .start(this);
    }

    private void getData(Bitmap image)
    {
        final FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(image);
        detector.processImage(firebaseVisionImage)
                .addOnSuccessListener(result ->
                {
                    try
                    {
                        for (FirebaseVisionText.TextBlock block : result.getTextBlocks())
                            for (FirebaseVisionText.Line line : block.getLines())
                                if (line.getBoundingBox() != null && line.getText() != null
                                        && line.getText().length() > 0)
                                    TextTable.put(line.getBoundingBox(), line.getText());
                        addRectangles();
                    } catch (Exception error)
                    {
                        onFailure(error);
                    }
                })
                .addOnFailureListener(this);
    }

    private void addRectangles()
    {
        if (coverPhoto == null || TextTable.isEmpty()) return;
        Bitmap temp = coverPhoto;
        ImageView cover = findViewById(R.id.add_book_cover_photo);
        Canvas canvas = new Canvas(temp);
        Paint rectPaint = new Paint();
        Paint textPaint = new Paint();

        rectPaint.setColor(Color.BLUE); // Text Color
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(6f);

        textPaint.setColor(Color.RED);
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.LEFT);

        // Pattern
        // some more settings...
        canvas.drawBitmap(temp, 0, 0, rectPaint);
        for (Rect rect : TextTable.keySet())
        {
            canvas.drawRect(rect, rectPaint);
            canvas.drawText(Objects.requireNonNull(TextTable.get(rect)), rect.left, rect.bottom,
                    textPaint);
        }

        cover.setImageBitmap(temp);
    }

    @Override
    public void onFailure(@NonNull Exception error)
    {
        error.printStackTrace(); //TODO add crashlytics.
    }
}
