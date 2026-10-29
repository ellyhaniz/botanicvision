package com.example.plantdex.visitor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plantdex.R;
import com.example.plantdex.common.BottomNavHelper;
import com.example.plantdex.common.Roles;
//logic import
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
//live cam
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.camera.core.CameraSelector;
import androidx.camera.view.CameraController;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;


public class CapturePhotoActivity extends AppCompatActivity {
    public static final String EXTRA_PHOTO_URI = "com.example.plantdex.PHOTO_URI";


    //live cam class
    private LifecycleCameraController cameraController;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startPreview();
                } else {
                    Toast.makeText(this, "Camera permission is needed for the preview.", Toast.LENGTH_SHORT).show();
                }
            });



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_photo);

        BottomNavHelper.setup(this, findViewById(R.id.navContainer), Roles.VISITOR);
        ((android.widget.TextView) findViewById(R.id.header).findViewById(R.id.tvRoleId)).setText(Roles.displayId(Roles.VISITOR));

        ImageButton btnShutter = findViewById(R.id.btnShutter);
        btnShutter.setOnClickListener(v -> capturePhoto(btnShutter));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startPreview();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }



    }
    // live cam startPreview
    private void startPreview() {
        PreviewView previewView = findViewById(R.id.cameraPreview);
        cameraController = new LifecycleCameraController(this);
        cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE);
        cameraController.setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA);
        cameraController.setImageCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY);
        previewView.setController(cameraController);
        cameraController.bindToLifecycle(this);
    }

    //capturePhoto method

    private void capturePhoto(ImageButton shutter) {
        if (cameraController == null) {
            Toast.makeText(this, "Camera is not ready yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        File folder = new File(getCacheDir(), "captured_photos");
        if (!folder.isDirectory() && !folder.mkdirs()) {
            Toast.makeText(this, "Could not prepare photo storage.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File photo = File.createTempFile("plantdex_", ".jpg", folder);
            Uri photoUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", photo);
            ImageCapture.OutputFileOptions options =
                    new ImageCapture.OutputFileOptions.Builder(photo).build();

            shutter.setEnabled(false);
            cameraController.takePicture(options, ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(ImageCapture.OutputFileResults results) {
                            shutter.setEnabled(true);
                            Intent intent = new Intent(CapturePhotoActivity.this, ConfirmPhotoActivity.class);
                            intent.putExtra(EXTRA_PHOTO_URI, photoUri.toString());
                            startActivity(intent);
                        }

                        @Override
                        public void onError(ImageCaptureException error) {
                            shutter.setEnabled(true);
                            photo.delete();
                            Toast.makeText(CapturePhotoActivity.this,
                                    "Could not capture photo.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (IOException | RuntimeException error) {
            shutter.setEnabled(true);
            Toast.makeText(this, "Could not capture photo.", Toast.LENGTH_SHORT).show();
        }
    }
}
