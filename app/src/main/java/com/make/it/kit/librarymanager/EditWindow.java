package com.make.it.kit.librarymanager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EditWindow extends AppCompatActivity implements View.OnClickListener, OnFailureListener, View.OnTouchListener
{
    private final FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
            .getOnDeviceTextRecognizer();
    private static Uri IMAGE_URI;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final Map<Rect, String> TextTable = new HashMap<>();
    private final StorageReference storageRef = FirebaseStorage.getInstance().getReference();

    private Bitmap coverPhoto;
    private final int capture_image = 1233;
    private AlertDialog addingDialog;
    private EditText currentEditText;
    private boolean imageChanged = false;
    private String deleteRef = null;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_window);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        EditText[] textViews =
                {
                        findViewById(R.id.edit_book_name),
                        findViewById(R.id.edit_book_author),
                        findViewById(R.id.edit_book_price),
                        findViewById(R.id.edit_book_category)
                };
        initTextView(textViews);

        final DocumentReference bookRef = db.document(Objects.requireNonNull(
                Objects.requireNonNull(getIntent().getExtras())
                        .getString(RecyclerViewAdapter.CURRENT_BOOK)));

        bookRef.get().addOnCompleteListener((snapshotTask) ->
        {
            final Book book = Objects.requireNonNull(snapshotTask.getResult()).toObject(Book.class);
            if (book == null || !snapshotTask.getResult().exists())
                throw new NullPointerException("No such book exists.");
            deleteRef = book.getPhotoRef();
            book.toScreen(textViews, findViewById(R.id.edit_book_cover_photo), this);
        })
                .addOnFailureListener(this);
        MaterialButton cover = findViewById(R.id.edit_select_cover_photo);
        cover.setOnClickListener(this);

        MaterialButton save = findViewById(R.id.edit_book_submit_button);
        save.setOnClickListener((view) ->
        {
            if (Utils.checkNull(textViews[0].getText().toString()))
                Utils.showToast("Please enter a name.", this);
            else if (Utils.checkNull(textViews[1].getText().toString()))
                Utils.showToast("Author's name can't be empty.", this);
            else if (Utils.checkNull(textViews[3].getText().toString()))
                Utils.showToast("Please enter a category", this);
            else addBook(textViews, bookRef);
        });

        db.collection("stats")
                .document("Book_Props").get().addOnCompleteListener(task ->
        {
            final DocumentSnapshot bookProps = Objects.requireNonNull(task.getResult());
            initAutoCompleteEditText(R.id.edit_book_author, (List<String>) bookProps.get("Authors"));
            initAutoCompleteEditText(R.id.edit_book_category, (List<String>) bookProps.get("Categories"));
        }).addOnFailureListener(this);

        findViewById(R.id.edit_book_cover_photo).setOnTouchListener(this);
    }

    private void addBook(@NonNull TextView[] textViews, DocumentReference bookRef)
    {
        toggleSavingDialog();
        final Map<String, Object> map = new HashMap<>();
        map.put("Name", textViews[0].getText());
        map.put("Author", textViews[1].getText());
        map.put("Price", textViews[2].getText());
        map.put("Category", textViews[3].getText());
        if (imageChanged)
        {
            if (deleteRef != null)
                FirebaseStorage.getInstance().getReference(deleteRef)
                        .delete().addOnFailureListener(this);

            ByteArrayOutputStream biteArrayOutputStream = new ByteArrayOutputStream();
            coverPhoto.compress(Bitmap.CompressFormat.JPEG, 100, biteArrayOutputStream);

            final String photoRef = "coverPhotos/" + Utils.toSafeFileName(textViews[0]) + '_'
                    + Utils.toSafeFileName(textViews[1]) + new Date().toString();
            StorageReference ref = storageRef.child(photoRef);
            map.put("PhotoRef", ref.getPath());
            ref.putBytes(biteArrayOutputStream.toByteArray())
                    .continueWithTask(task ->
                    {
                        if (!task.isSuccessful())
                            throw Objects.requireNonNull(task.getException());
                        // Continue with the task to get the download URL
                        return ref.getDownloadUrl();
                    })
                    .addOnCompleteListener(task ->
                    {
                        toggleSavingDialog();
                        if (task.isSuccessful() && task.getResult() != null)
                        {
                            map.put("Photo", task.getResult().toString());
                            bookRef.update(map);
                            Utils.showToast("Saved.", this);
                            finish();
                        } else
                        {
                            Utils.alert("Failed to save cover photo.", this);
                            onFailure(Objects.requireNonNull(task.getException()));
                        }
                    });
        } else
        {
            bookRef.update(map);
            toggleSavingDialog();
            Utils.showToast("Saved.", this);
            finish();
        }
    }

    private void initAutoCompleteEditText(int id, List<String> options)
    {
        // Get a reference to the AutoCompleteTextView in the layout
        AutoCompleteTextView textView = findViewById(id);
        // Create the adapter and set it to the AutoCompleteTextView
        textView.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, options));
        textView.setThreshold(1);
    }

    private void initTextView(@NonNull EditText[] textViews)
    {
        for (TextView text : textViews)
        {
            text.setOnClickListener(v ->
            {
                currentEditText = (EditText) v;
                Utils.showToast("Selected", this);
            });
            text.setOnFocusChangeListener((view, bool) ->
                    text.setText(Utils.format(text.getText().toString())));
            text.startAnimation(AnimationUtils.loadAnimation(this,
                    R.anim.zoom_in));
        }
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
        int request_permission = 9447;
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
            onFailure(error);
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
                            findViewById(R.id.edit_parent_container));
                    new Thread(() -> getData(coverPhoto)).run();
                } catch (IOException error)
                {
                    onFailure(error);
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
        ImageView cover = findViewById(R.id.edit_book_cover_photo);
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
        imageChanged = true;
    }

    private void toggleSavingDialog()
    {
        if (addingDialog == null)
        {
            addingDialog = new AlertDialog.Builder(this).create();
            addingDialog.setCancelable(false);
            addingDialog.setMessage(getString(R.string.saving_text));
            addingDialog.setIcon(R.drawable.ic_info);
        }
        if (addingDialog.isShowing()) addingDialog.dismiss();
        else addingDialog.show();
    }

    @Override
    public void onFailure(@NonNull Exception error)
    {
        error.printStackTrace(); //TODO add crashlytics.
    }

    /**
     * Called when a touch event is dispatched to a view. This allows listeners to
     * get a chance to respond before the target view.
     *
     * @param v     The view the touch event has been dispatched to.
     * @param event The MotionEvent object containing full information about
     *              the event.
     * @return True if the listener has consumed the event, false otherwise.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN && currentEditText != null)
        {
            for (Rect rect : TextTable.keySet())
                if (Utils.isInside(event, rect))
                {
                    currentEditText.setText(String.format("%s %s", currentEditText.getText(),
                            TextTable.get(rect)));
                    currentEditText = null;
                    Utils.showToast("Copied", this);
                    break;
                }
        }
        return false;
    }
}
