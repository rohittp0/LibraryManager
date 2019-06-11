package com.make.it.kit.librarymanager;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Frame;
import com.otaliastudios.cameraview.FrameProcessor;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.Preview;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends AppCompatActivity implements OnFailureListener, FrameProcessor
{
    static final int SUCCESS = 546;
    private final FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
            .getOnDeviceImageLabeler();
    private final FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance()
            .getOnDeviceTextRecognizer();
    private OverlayView rectView;
    private TextView labelView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);

        final FrameLayout rootView = findViewById(R.id.camera_root_container);
        rectView = new OverlayView(this);
        labelView = findViewById(R.id.camera_image_label);

        rootView.addView(rectView, 1);
        rectView.setZOrderOnTop(true);

        CameraView camera = findViewById(R.id.camera_view);
        addCapture(camera, findViewById(R.id.camera_image_capture));
        camera.addFrameProcessor(this);
        camera.open();
    }

    private void addCapture(@NonNull CameraView camera, @NonNull ImageButton button)
    {
        camera.addCameraListener(new CameraListener()
        {
            @Override
            public void onPictureTaken(@NonNull PictureResult result)
            {
                result.toBitmap((image) ->
                {
                    try
                    {
                        final Dialog dialog = new Dialog(CameraActivity.this,
                                R.style.Dialog_FrameLess);
                        dialog.setContentView(R.layout.progressbar);
                        dialog.setCancelable(false);
                        dialog.show();
                        final Intent ret = CameraActivity.this.getIntent();
                        final File imageFile = new File(Utils.createTemporaryFile().getPath());
                        final FileOutputStream fos = new FileOutputStream(imageFile);
                        assert image != null;
                        image.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush();
                        ret.putExtra("image", imageFile.toURI());
                        CameraActivity.this.setResult(SUCCESS, ret);
                        dialog.dismiss();
                        CameraActivity.this.finish();
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                });
            }
        });
        camera.setLifecycleOwner(this);
        camera.setPreview(Preview.GL_SURFACE);
        button.setOnClickListener((event) -> camera.takePicture());
    }

    @Override
    public void onFailure(@NonNull Exception e)
    {
        e.printStackTrace(); //TODO add crashlytics
    }

    @Override
    public void process(@NonNull Frame frame)
    {
        {
            final FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                    .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                    .setRotation(((int) Math.round(frame.getRotation() / 90.0)))
                    .setHeight(frame.getSize().getHeight())
                    .setWidth(frame.getSize().getWidth())
                    .build();
            final FirebaseVisionImage textImage =
                    FirebaseVisionImage.fromByteArray(frame.getData(), metadata);
            final OnSuccessListener<FirebaseVisionText> textRecognitionComplected = (result) ->
            {
                rectView.clearCanvas();
                for (FirebaseVisionText.TextBlock block :
                        result.getTextBlocks())
                    for (FirebaseVisionText.Line line :
                            block.getLines())
                        if (line.getBoundingBox() != null && line.getText() != null
                                && line.getText().length() > 0)
                            rectView.drawText(line.getBoundingBox(),
                                    line.getText());
                System.gc();
            };
            labeler.processImage(FirebaseVisionImage.fromByteArray(frame.getData(), metadata))
                    .addOnSuccessListener((labels) ->
                    {
                        for (FirebaseVisionImageLabel label : labels)
                            if (label.getText().toLowerCase().trim().equals("poster"))
                            {
                                textRecognizer.processImage(textImage)
                                        .addOnSuccessListener(textRecognitionComplected)
                                        .addOnFailureListener(this);
                                labelView.setText(CameraActivity.this
                                        .getString(R.string.camera_view_book_label));
                                break;
                            } else labelView.setText(label.getText());
                    })
                    .addOnFailureListener(this);
        }
    }
}
