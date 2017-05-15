package com.harpe.harpemessenger;

import android.graphics.Bitmap;
import android.location.Location;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by anthony on 14/05/2017.
 */

public class HEPicture {

    private Bitmap picture;
    private String name;
    private int size;
    private Location location;
    private Date date;
    private String address;
    private static ArrayList<HEPicture> pictures;

    static {
        pictures = new ArrayList<>();
        pictures.add(new HEPicture(null,"Amazing picture",10,null,null,"Nowhere"));
        pictures.add(new HEPicture(null,"Amazing picture2",10,null,null,"Nowhere"));
        pictures.add(new HEPicture(null,"Amazing picture3",10,null,null,"Nowhere"));
        pictures.add(new HEPicture(null,"Amazing picture4",10,null,null,"Nowhere"));
    }

    public HEPicture(Bitmap picture, String name, int size, Location location, Date date,String address) {
        this.picture = picture;
        this.name = name;
        this.size = size;
        this.location = location;
        this.date = date;
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public Bitmap getPicture() {
        return picture;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public Location getLocation() {
        return location;
    }

    public Date getDate() {
        return date;
    }

    public static ArrayList<HEPicture> getPictures() {
        return pictures;
    }
}
