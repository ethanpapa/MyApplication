package com.shin.nuleedasle.myapplication.models;

import android.os.Parcel;
import android.os.Parcelable;


public class DataModelForList implements Parcelable {
    private String mImageUrl;
    private String mTitle;
    private String mImageID;


    public DataModelForList(){

    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public String getImageID() {
        return mImageID;
    }

    public void setImageID(String imageID) {
        this.mImageID = imageID;
    }

    public void setImageUrl(String mImageUrl) {
        this.mImageUrl = mImageUrl;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mImageUrl);
        dest.writeString(mTitle);
        dest.writeString(mImageID);
    }

    public static final Parcelable.Creator<DataModelForList> CREATOR
            = new Parcelable.Creator<DataModelForList>() {
        public DataModelForList createFromParcel(Parcel in) {
            return new DataModelForList(in);
        }

        public DataModelForList[] newArray(int size) {
            return new DataModelForList[size];
        }
    };

    private DataModelForList(Parcel in) {
        mImageUrl = in.readString();
        mTitle = in.readString();
        mImageID = in.readString();
    }

}