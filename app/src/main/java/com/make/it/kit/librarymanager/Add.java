package com.make.it.kit.librarymanager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
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

import org.jetbrains.annotations.Contract;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;

public class Add extends Fragment implements OnFailureListener
{
    //Text Extraction
    private static Uri IMAGE_URI;
    //Constants
    private final int capture_image = 123;
    private final int request_permission = 312;
    //Firebase
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final StorageReference storageRef = FirebaseStorage.getInstance().getReference();
    private final FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
            .getOnDeviceTextRecognizer();
    private final Map<Rect, String> TextTable = new HashMap<>();
    //class data
    private Context mContext;
    //UI
    private AlertDialog addingDialog;
    private View view;
    private Bitmap coverPhoto = null;
    private EditText currentEditText;

    @NonNull
    @Contract(" -> new")
    static Add newInstance()
    {
        return new Add();
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
            }
            else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
            {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                try
                {
                    coverPhoto = Utils.scaleToFit(MediaStore.Images.Media.getBitmap(Objects
                                    .requireNonNull(getActivity()).getContentResolver(),
                            result.getUri()),
                            view);
                    new Thread(() -> getData(coverPhoto)).run();
                } catch (IOException error)
                {
                    onFailure(error);
                }
            }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == request_permission)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                capturePhoto();
            else Toast.makeText(getActivity(), "Can't capture photo PERMISSION DENIED",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        mContext = context;
    }

    @SuppressLint("ClickableViewAccessibility")
    @SuppressWarnings("unchecked")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        view = inflater.inflate(R.layout.add_fragment, container, false);
        view.findViewById(R.id.add_select_cover_photo).setOnClickListener(v ->
        {
            if (mContext != null && getActivity() != null)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            request_permission);
                else capturePhoto();
            }
        });

        view.findViewById(R.id.add_book_cover_photo).setOnTouchListener((view, event) ->
        {
            if (event.getAction() == MotionEvent.ACTION_DOWN && currentEditText != null)
            {
                for (Rect rect : TextTable.keySet())
                    if (Utils.isInside(event, rect))
                    {
                        currentEditText.setText(String.format("%s %s", currentEditText.getText(),
                                TextTable.get(rect)));
                        currentEditText = null;
                        Utils.showToast("Copied", mContext);
                        break;
                    }
            }
            return false;
        });

        final TextView[] textViews = {
                view.findViewById(R.id.add_book_name),
                view.findViewById(R.id.add_book_author),
                view.findViewById(R.id.add_book_category),
                view.findViewById(R.id.add_book_price)
        };
        initTextView(textViews);

        db.collection("stats")
                .document("Book_Props").get().addOnCompleteListener(task ->
        {
            final DocumentSnapshot bookProps = Objects.requireNonNull(task.getResult());
            List<String> author = (List<String>) bookProps.get("Authors");
            List<String> category = (List<String>) bookProps.get("Categories");
            initAutoCompleteEditText(R.id.add_book_author, author);
            initAutoCompleteEditText(R.id.add_book_category, category);
        }).addOnFailureListener(this);

        MaterialButton addButton = view.findViewById(R.id.add_book_submit_button);
        addButton.setOnClickListener((clicked_view) ->
        {
            if (Utils.checkNull(textViews[0].getText().toString()))
                Utils.showToast("Please enter a name.", mContext);
            else if (Utils.checkNull(textViews[1].getText().toString()))
                Utils.showToast("Author's name can't be empty.", mContext);
            else if (Utils.checkNull(textViews[2].getText().toString()))
                Utils.showToast("Please enter a category", mContext);
            else addBook(textViews);
        });
        return view;
    }

    private void initTextView(@NonNull TextView[] textViews)
    {
        for (TextView text : textViews)
        {
            text.setOnClickListener(v ->
            {
                currentEditText = (EditText) v;
                Utils.showToast("Selected", mContext);
            });
            text.setOnKeyListener((view,code,event)->
            {
                text.setText(Utils.format(text.getText().toString()));
                return false;
            });
            text.startAnimation(AnimationUtils.loadAnimation(getContext(),
                    R.anim.zoom_in));
        }
    }

    private void initAutoCompleteEditText(int id, List<String> options)
    {
        // Get a reference to the AutoCompleteTextView in the layout
        AutoCompleteTextView textView = view.findViewById(id);
        // Create the adapter and set it to the AutoCompleteTextView
        textView.setAdapter(new ArrayAdapter<>(Objects.requireNonNull(getActivity()),
                android.R.layout.simple_list_item_1, options));
        textView.setThreshold(1);
    }

    @NonNull
    @Contract("null -> !null")
    private String toSafeFileName(TextView text)
    {
        if(text == null || text.getText() == null) return " ";
        return text.getText().toString().replaceAll("/","").trim();
    }

    private void addBook(@NonNull TextView[] textViews)
    {
        toggleAddingDialog();
        if (coverPhoto != null)
        {
            ByteArrayOutputStream biteArrayOutputStream = new ByteArrayOutputStream();
            coverPhoto.compress(Bitmap.CompressFormat.JPEG, 100, biteArrayOutputStream);

            final String photoRef = "coverPhotos/"+toSafeFileName(textViews[0])+'_'
                    +toSafeFileName(textViews[1])+new Date().toString();
            StorageReference ref = storageRef.child(photoRef);
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
                        if (task.isSuccessful())
                            addBook(textViews,
                                    Objects.requireNonNull(task.getResult()).toString(), ref.getPath());
                        else
                        {
                            Utils.alert("Failed to save cover photo.", mContext);
                            onFailure(Objects.requireNonNull(task.getException()));
                        }
                    });
            coverPhoto = null;
            ImageView cover = view.findViewById(R.id.add_book_cover_photo);
            cover.setImageBitmap(null);
        } else addBook(textViews, null, null);
    }

    private void toggleAddingDialog()
    {
        if (addingDialog == null)
        {
            addingDialog = new AlertDialog.Builder(mContext).create();
            addingDialog.setCancelable(false);
            addingDialog.setMessage(mContext.getString(R.string.adding_text));
            addingDialog.setIcon(R.drawable.ic_info);
        }
        if (addingDialog.isShowing()) addingDialog.dismiss();
        else addingDialog.show();
    }

    private void addBook(@NonNull TextView[] textViews, String photo, String photoRef)
    {
        float price = 0;
        String price_text = textViews[3].getText().toString().trim();
        try
        {
            if (!Utils.checkNull(price_text)) price = Float.parseFloat(price_text);
        } catch (NumberFormatException error)
        {
            error.printStackTrace();
        }

        Book book = new Book(textViews[0].getText().toString(),
                textViews[1].getText().toString(),
                textViews[2].getText().toString(),
                photo, photoRef,
                price);
        book.setSavedOn(new Timestamp(new Date()));
        db.collection("Books")
                .add(book.toMap())
                .addOnCompleteListener((doc) -> toggleAddingDialog())
                .addOnSuccessListener(documentReference ->{
                    for(TextView text :textViews) text.setText("");
                        Utils.showToast("Added", mContext);
                })
                .addOnFailureListener(error ->
                {
                    Utils.alert("Failed to add book.", mContext);
                    onFailure(error);
                });
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
            //Intent[] intentArray = {cameraIntent};
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntent);
            startActivityForResult(chooser, capture_image);
        } catch (IOException error)
        {
            onFailure(error);
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
                .start(mContext, this);
    }

    private void getData(Bitmap image)
    {
        if (view == null) return;
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

    @Override
    public void onFailure(@NonNull Exception error)
    {
        error.printStackTrace();
        //TODO add crashlytics
    }

    private void addRectangles()
    {
        if (coverPhoto == null || TextTable.isEmpty()) return;
        Bitmap temp = coverPhoto;
        ImageView cover = view.findViewById(R.id.add_book_cover_photo);
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
}
