package com.make.it.kit.librarymanager;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.algolia.search.saas.Client;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EditWindow extends AppCompatActivity implements OnCompleteListener<Void>, OnFailureListener
{
    private Add add;
    private boolean firestore = false;
    private boolean algolia = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_window);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.edit_fragment_container, EditFragment.newInstance(this)).commit();
    }

    void addBook(@NonNull EditText[] textViews, DocumentReference bookRef, byte[] coverPhoto, String deleteRef, Add add)
    {
        this.add = add;
        final Map<String, Object> map = new HashMap<>();
        map.put("Name", textViews[0].getText().toString());
        map.put("Author", textViews[1].getText().toString());
        try
        {
            float price = Float.parseFloat(textViews[2].getText().toString());
            map.put("Price", price);
        } catch (NumberFormatException | NullPointerException error)
        {
            map.put("Price", 0f);
        }
        map.put("Category", textViews[3].getText().toString());
        if (coverPhoto != null && coverPhoto.length > 0)
        {
            if (deleteRef != null)
                FirebaseStorage.getInstance().getReference(deleteRef)
                        .delete().addOnFailureListener(add);

            final String photoRef = "coverPhotos/" + Utils.toSafeFileName(textViews[0]) + '_'
                    + Utils.toSafeFileName(textViews[1]) + new Date().toString();
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(photoRef);
            map.put("PhotoRef", ref.getPath());
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
                        if (task.isSuccessful() && task.getResult() != null)
                        {
                            map.put("Photo", task.getResult().toString());
                            bookRef.update(map).addOnFailureListener(this)
                                    .addOnCompleteListener(this);
                        } else onFailure(task.getException());
                    });
        } else bookRef.update(map).addOnFailureListener(this)
                .addOnCompleteListener(this);
        try
        {
            new Client(getString(R.string.algolia_application_id), MainActivity.ALGOLIA_API_KEY)
                    .getIndex(getString(R.string.algolia_index))
                    .partialUpdateObjectAsync(Utils.mapToJSON(map), bookRef.toString()
                            , (jsonObject, algoliaException) ->
                            {
                                if (algoliaException != null)
                                    onFailure(algoliaException);
                                else if (firestore)
                                {
                                    add.toggleAddingDialog(true);
                                    Utils.showToast(R.string.book_saved, this);
                                    finish();
                                } else algolia = true;
                            });
        } catch (JSONException e)
        {
            onFailure(e);
        }
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        finish();
        return true;
    }

    @Override
    public void onComplete(@NonNull Task<Void> task)
    {
        if (algolia)
        {
            add.toggleAddingDialog(true);
            Utils.showToast(R.string.book_saved, this);
            finish();
        } else firestore = true;
    }

    @Override
    public void onFailure(@Nullable Exception e)
    {
        add.toggleAddingDialog(true);
        Utils.alert(R.string.failed_to_save_book, this);
        if (e != null) e.printStackTrace(); //TODO add crashlytics
    }
}