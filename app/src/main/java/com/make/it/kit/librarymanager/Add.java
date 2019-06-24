package com.make.it.kit.librarymanager;

import android.Manifest;
import android.annotation.SuppressLint;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;

public class Add extends Fragment implements OnFailureListener
{

    private static Uri IMAGE_URI;
    private final int capture_image = 123;
    private final int request_permission = 312;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Map<Rect, String> TextTable = new HashMap<>();
    private final FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
            .getOnDeviceTextRecognizer();
    private Context mContext;
    private View view;
    //Text Extraction
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
            } else if (requestCode == CameraActivity.SUCCESS && data.getExtras() != null)
                performCrop((Uri) data.getExtras().get("image"));
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
                } catch (IOException e)
                {
                    e.printStackTrace(); //TODO add crashlytics
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
            if (textViews[0].getText().toString().isEmpty())
                Utils.showToast("Please enter a name.", mContext);
            else if (textViews[1].getText().toString().isEmpty())
                Utils.showToast("Author's name can't be empty.", mContext);
            else if (textViews[2].getText().toString().isEmpty())
                Utils.showToast("Please enter a category", mContext);
            else
            {
                float price;

                if (textViews[3].getText().toString().isEmpty()) price = 0f;
                else price = Float.parseFloat(textViews[3].getText().toString());

                Book book = new Book(textViews[0].getText().toString(),
                        textViews[1].getText().toString(), textViews[2].getText().toString(),
                        "", "",
                        price);

            }
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

    private void capturePhoto()
    {
        try
        {
            IMAGE_URI = Utils.createTemporaryFile();
            Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            Intent appIntent = new Intent(getContext(), CameraActivity.class);
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Intent chooser = new Intent(Intent.ACTION_CHOOSER);

            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, IMAGE_URI);

            chooser.putExtra(Intent.EXTRA_INTENT, galleryIntent);
            chooser.putExtra(Intent.EXTRA_TITLE, getString(R.string.add_book_heading));
            Intent[] intentArray = {cameraIntent, appIntent};
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
            startActivityForResult(chooser, capture_image);
        } catch (IOException error)
        {
            error.printStackTrace(); //TODO add crashlytics
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
        int id = MainActivity.createNotification("Extracting Text",
                getString(R.string.extracting_text_notification_message));
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
                    } catch (Exception e)
                    {
                        e.printStackTrace(); //TODO add crashlytics
                    } finally
                    {
                        MainActivity.disposeNotification(id);
                    }
                })
                .addOnFailureListener(this);
    }

    private void addRectangles()
    {
        if (coverPhoto == null || TextTable.isEmpty()) return;
        ImageView cover = view.findViewById(R.id.add_book_cover_photo);
        Canvas canvas = new Canvas(coverPhoto);
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
        canvas.drawBitmap(coverPhoto, 0, 0, rectPaint);
        for (Rect rect : TextTable.keySet())
        {
            canvas.drawRect(rect, rectPaint);
            canvas.drawText(Objects.requireNonNull(TextTable.get(rect)), rect.left, rect.bottom,
                    textPaint);
        }

        cover.setImageBitmap(coverPhoto);
    }

    @Override
    public void onFailure(@NonNull Exception e)
    {

    }
}
