package com.limemobile.app.sdk.http.internal;

import java.util.Date;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import android.os.Parcel;
import android.os.Parcelable;

public class ParcelCookie implements Parcelable {

    private transient final Cookie cookie;
    private transient BasicClientCookie clientCookie;

    public ParcelCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    public ParcelCookie(Parcel src) {
        super();

        String name = src.readString();
        String value = src.readString();
        clientCookie = new BasicClientCookie(name, value);
        clientCookie.setComment(src.readString());
        clientCookie.setDomain(src.readString());
        clientCookie.setExpiryDate(new Date(src.readLong()));
        clientCookie.setPath(src.readString());
        clientCookie.setVersion(src.readInt());
        clientCookie.setSecure(src.readInt() == 0 ? false : true);

        cookie = clientCookie;
    }

    public Cookie getCookie() {
        Cookie bestCookie = cookie;
        if (clientCookie != null) {
            bestCookie = clientCookie;
        }
        return bestCookie;
    }

    // private void writeObject(ObjectOutputStream out) throws IOException {
    // out.writeObject(cookie.getName());
    // out.writeObject(cookie.getValue());
    // out.writeObject(cookie.getComment());
    // out.writeObject(cookie.getDomain());
    // out.writeObject(cookie.getExpiryDate());
    // out.writeObject(cookie.getPath());
    // out.writeInt(cookie.getVersion());
    // out.writeBoolean(cookie.isSecure());
    // }
    //
    // private void readObject(ObjectInputStream in) throws IOException,
    // ClassNotFoundException {
    // String name = (String) in.readObject();
    // String value = (String) in.readObject();
    // clientCookie = new BasicClientCookie(name, value);
    // clientCookie.setComment((String) in.readObject());
    // clientCookie.setDomain((String) in.readObject());
    // clientCookie.setExpiryDate((Date) in.readObject());
    // clientCookie.setPath((String) in.readObject());
    // clientCookie.setVersion(in.readInt());
    // clientCookie.setSecure(in.readBoolean());
    // }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(cookie.getName());
        dest.writeString(cookie.getValue());
        dest.writeString(cookie.getComment());
        dest.writeString(cookie.getDomain());
        dest.writeLong(cookie.getExpiryDate().getTime());
        dest.writeString(cookie.getPath());
        dest.writeInt(cookie.getVersion());
        dest.writeInt(cookie.isSecure() ? 1 : 0);
    }

    public static Parcelable.Creator<ParcelCookie> CREATOR = new Parcelable.Creator<ParcelCookie>() {

        @Override
        public ParcelCookie createFromParcel(Parcel source) {
            return new ParcelCookie(source);
        }

        @Override
        public ParcelCookie[] newArray(int size) {
            return new ParcelCookie[size];
        }
    };
}