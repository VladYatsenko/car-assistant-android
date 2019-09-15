package com.carassistant.model.entity;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;

import java.util.Date;
import java.util.UUID;

public class SignEntity {

    private UUID uuid;
    private String name;
    @IdRes
    private int image;
    private Date date;

    public SignEntity(String name, int image){
        this.uuid = UUID.randomUUID();
        this.name = name;
        this.image = image;
        this.date = new Date();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public int getImage() {
        return image;
    }

    public Date getDate() {
        return date;
    }

}
