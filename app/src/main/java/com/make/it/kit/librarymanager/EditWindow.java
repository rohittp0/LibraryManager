package com.make.it.kit.librarymanager;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EditWindow extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_window);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.edit_fragment_container, EditFragment.newInstance(this)).commit();
    }

    void addBook(@NonNull TextView[] textViews, DocumentReference bookRef, byte[] coverPhoto, String deleteRef, Add add)
    {
        final Map<String, Object> map = new HashMap<>();
        map.put("Name", textViews[0].getText());
        map.put("Author", textViews[1].getText());
        map.put("Price", textViews[2].getText());
        map.put("Category", textViews[3].getText());
        if (coverPhoto != null)
        {
            if (deleteRef != null)
                FirebaseStorage.getInstance().getReference(deleteRef)
                        .delete().addOnFailureListener(add);

            final String photoRef = "coverPhotos/" + Utils.toSafeFileName(textViews[0]) + '_'
                    + Utils.toSafeFileName(textViews[1]) + new Date().toString();
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(photoRef);
            map.put("PhotoRef", ref.getPath() + "");
            ref.putBytes(coverPhoto)
                    .continueWithTask(task ->
                    {
                        if (!task.isSuccessful())
                            throw Objects.requireNonNull(task.getException());
                        // Continue with the task to get the download URL
                        return ref.getDownloadUrl();
                    })
                    .addOnCompleteListener(task ->
                    {
                        add.toggleAddingDialog(true);
                        if (task.isSuccessful() && task.getResult() != null)
                        {
                            map.put("Photo", task.getResult().toString());
                            bookRef.update(map);
                            Utils.showToast("Saved.", this);
                            finish();
                        } else
                        {
                            Utils.alert("Failed to save cover photo.", this);
                            add.onFailure(Objects.requireNonNull(task.getException()));
                        }
                    });
        } else
        {
            bookRef.update(map);
            add.toggleAddingDialog(true);
            Utils.showToast("Saved.", this);
            finish();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        finish();
        return true;
    }

}