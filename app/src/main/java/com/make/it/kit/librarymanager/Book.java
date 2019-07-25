package com.make.it.kit.librarymanager;

import com.google.firebase.Timestamp;

@SuppressWarnings("unused")
class Book
{
    private final int[] images = {R.mipmap.cover1, R.mipmap.cover2, R.mipmap.cover3};
    private String Name;
    private String Author;
    private String Category;
    private String Photo;
    private String PhotoRef;
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
        Photo = Utils.checkNull(photo) ? null : Photo;
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
}
