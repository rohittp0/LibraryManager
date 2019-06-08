package com.make.it.kit.librarymanager;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.otaliastudios.cameraview.Audio;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Preview;

import java.util.List;

public class CameraActivity extends AppCompatActivity implements OnSuccessListener<List<FirebaseVisionObject>>, OnFailureListener
{
    private final FirebaseVisionObjectDetector objectDetector =
            FirebaseVision.getInstance().getOnDeviceObjectDetector(new FirebaseVisionObjectDetectorOptions.Builder()
                    .setDetectorMode(FirebaseVisionObjectDetectorOptions.STREAM_MODE)
                    .enableClassification()  // Optional
                    .build());

    private OverlayView rectView;

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
        camera.addFrameProcessor((frame) ->
                objectDetector.processImage(FirebaseVisionImage.fromByteArray(frame.getData(),
                        new FirebaseVisionImageMetadata.Builder()
                                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                                .setRotation(frame.getRotation())
                                .setHeight(frame.getSize().getHeight())
                                .setWidth(frame.getSize().getWidth())
                                .build()))
                        .addOnSuccessListener(this)
                        .addOnFailureListener(this));
        camera.setAudio(Audio.OFF);
        camera.setPreview(Preview.GL_SURFACE);
        camera.open();
    }

    @Override
    public void onSuccess(List<FirebaseVisionObject> detectedObjects)
    {
        for (FirebaseVisionObject obj : detectedObjects)
        {
            Integer id = obj.getTrackingId();
            String entityId = obj.getEntityId();
            rectView.drawRect(obj.getBoundingBox());
            System.out.println(id + " " + entityId);
        }
        //setResult(10,new Intent());
    }

    @Override
    public void onFailure(@NonNull Exception e)
    {
        e.printStackTrace(); //TODO add crashlytics
    }
}
