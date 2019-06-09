package com.make.it.kit.librarymanager;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.otaliastudios.cameraview.Audio;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Mode;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.Preview;

import java.util.HashMap;

public class CameraActivity extends AppCompatActivity implements OnFailureListener
{
    private final FirebaseVisionObjectDetector objectDetector =
            FirebaseVision.getInstance().getOnDeviceObjectDetector(new FirebaseVisionObjectDetectorOptions.Builder()
                    .setDetectorMode(FirebaseVisionObjectDetectorOptions.STREAM_MODE)
                    .enableClassification()  // Optional
                    .build());
    private final FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
            .getOnDeviceImageLabeler();

    private final HashMap<String, String> labelTable = new HashMap<>();
    private final HashMap<String, Rect> rectTable = new HashMap<>();

    private OverlayView rectView;

    private int SUCCESS = 546;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);

        final FrameLayout rootView = findViewById(R.id.camera_root_container);
        rectView = new OverlayView(this);

        rootView.addView(rectView, 1);
        rectView.setZOrderOnTop(true);

        CameraView camera = findViewById(R.id.camera_view);
        addCapture(camera, findViewById(R.id.camera_image_capture));
        camera.addFrameProcessor((frame) ->
        {
            final FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                    .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                    .setRotation(((int) Math.round(frame.getRotation() / 90.0)))
                    .setHeight(frame.getSize().getHeight())
                    .setWidth(frame.getSize().getWidth())
                    .build();
            objectDetector.processImage(FirebaseVisionImage.fromByteArray(frame.getData(),
                    metadata))
                    .addOnSuccessListener(detectedObjects ->
                    {
                        for (FirebaseVisionObject obj : detectedObjects)
                        {
                            if (labelTable.containsKey(obj.getEntityId()))
                            {
                                rectView.drawRect(obj.getBoundingBox(),
                                        labelTable.get(obj.getEntityId()));
                                labelTable.remove(obj.getEntityId());
                            } else rectTable.put(obj.getEntityId(), obj.getBoundingBox());
                        }
                    })
                    .addOnFailureListener(this);
            labeler.processImage(FirebaseVisionImage.fromByteArray(frame.getData(), metadata))
                    .addOnSuccessListener((labels) ->
                    {
                        final FirebaseVisionImageLabel label = labels.get(0);
                        if (rectTable.containsKey(label.getEntityId()))
                        {
                            rectView.drawRect(rectTable.get(label.getEntityId()),
                                    label.getText());
                            rectTable.remove(label.getEntityId());
                        } else labelTable.put(label.getEntityId(), label.getText());
                    })
                    .addOnFailureListener(this);
        });
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
                    final Intent ret = new Intent();
                    ret.putExtra("image", image);
                    CameraActivity.this.setResult(SUCCESS, ret);
                });
            }
        });
        camera.setLifecycleOwner(this);
        camera.setAudio(Audio.OFF);
        camera.setPreview(Preview.GL_SURFACE);
        camera.setMode(Mode.PICTURE);
        button.setOnClickListener((event) -> camera.takePicture());
    }

    @Override
    public void onFailure(@NonNull Exception e)
    {
        e.printStackTrace(); //TODO add crashlytics
    }
}
