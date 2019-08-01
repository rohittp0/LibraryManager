package com.make.it.kit.librarymanager;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.squareup.picasso.Picasso;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
class Book implements Serializable
{
    private final int[] images = {R.mipmap.cover1, R.mipmap.cover2, R.mipmap.cover3};
    private String Name;
    private String Author;
    private String Category;
    private String Photo;
    private String PhotoRef;
    private DocumentReference SelfRef;
    private Timestamp SavedOn;
    private float Price;
    private int PhotoID = 944747011;


    /**
     * @param Name     Name of the book
     * @param Author   Author of the book
     * @param Category Category of the book
     * @param Photo    Cover-Photo of the book
     * @param PhotoRef Reference of the location of cover-photo of the book
     * @param Price    Price of the book
     */
    Book(String Name, String Author, String Category, String Photo,
         String PhotoRef, float Price)
    {
        setName(Name);
        setAuthor(Author);
        setCategory(Category);
        setPhoto(Photo);
        setPhotoRef(PhotoRef);
        setPrice(Price);
        if (Utils.checkNull(Photo) || Utils.checkNull(PhotoRef))
            PhotoID = images[(int) Math.round(Math.random() * 100) % 3];
    }

    public Book()
    {
    }

    /**
     * This function loads the properties of this Book to provided fields.
     *
     * @param textViews Array in the order Name,Author,Price,Category.
     * @param img       The ImageView to load cover photo into.
     * @param context   Current context.
     */
    void toScreen(@NonNull TextView[] textViews, @Nullable ImageView img, @NonNull Context context)
    {
        NumberFormat formatter = NumberFormat.getNumberInstance();
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        if (textViews.length > 0) textViews[0].setText(getName());
        if (textViews.length > 1) textViews[1].setText(getAuthor());
        if (textViews.length > 2)
            textViews[2].setText(context.getString(R.string.book_popup_rupee_sign,
                    formatter.format(getPrice())));
        if (textViews.length > 3) textViews[3].setText(getCategory());

        if (img != null)
            if (!Utils.checkNull(getPhoto()))
                Picasso.get().load(getPhoto()).into(img);
    }

    public String getName()
    {
        return Name;
    }

    public void setName(String name)
    {
        Name = name;
    }

    public String getAuthor()
    {
        return Author;
    }

    public void setAuthor(String author)
    {
        Author = author;
    }

    public String getCategory()
    {
        return Category;
    }

    public void setCategory(String category)
    {
        Category = category;
    }

    public String getPhoto()
    {
        return Photo;
    }

    public void setPhoto(String photo)
    {
        Photo = Utils.checkNull(photo) ? null : photo;
    }

    public String getPhotoRef()
    {
        return PhotoRef;
    }

    public void setPhotoRef(String photoRef)
    {
        PhotoRef = Utils.checkNull(photoRef) ? null : photoRef;
    }

    public Timestamp getSavedOn()
    {
        return SavedOn;
    }

    public void setSavedOn(Timestamp savedOn)
    {
        SavedOn = savedOn;
    }

    public float getPrice()
    {
        return Price;
    }

    public void setPrice(float price)
    {
        Price = price;
    }

    public int getPhotoID()
    {
        if (PhotoID == 944747011) PhotoID = images[(int) Math.round(Math.random() * 100) % 3];
        return PhotoID;
    }

    Map<String, Object> toMap()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("Name", Utils.format(getName()));
        map.put("Author", Utils.format(getAuthor()));
        map.put("Category", Utils.format(getCategory()));
        map.put("Photo", getPhoto());
        map.put("PhotoRef", getPhotoRef());
        map.put("SavedOn", getSavedOn());
        map.put("Price", getPrice());
        map.put("PhotoID", getPhotoID());
        return map;
    }

    DocumentReference getSelfRef()
    {
        return SelfRef;
    }

    void setSelfRef(DocumentReference selfRef)
    {
        SelfRef = selfRef;
    }
}
