
package com.chiaramail.chiaramailforandroid.mail;

public abstract class BodyPart implements Part {
    private Multipart mParent;

    public Multipart getParent() {
        return mParent;
    }

    public void setParent(Multipart parent) {
        mParent = parent;
    }
}
