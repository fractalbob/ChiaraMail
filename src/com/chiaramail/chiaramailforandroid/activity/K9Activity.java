package com.chiaramail.chiaramailforandroid.activity;

import android.os.Bundle;
import android.view.MotionEvent;

import com.actionbarsherlock.app.SherlockActivity;
import com.chiaramail.chiaramailforandroid.activity.K9ActivityCommon.K9ActivityMagic;
import com.chiaramail.chiaramailforandroid.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;


public class K9Activity extends SherlockActivity implements K9ActivityMagic {

    private K9ActivityCommon mBase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mBase = K9ActivityCommon.newInstance(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mBase.preDispatchTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void setupGestureDetector(OnSwipeGestureListener listener) {
        mBase.setupGestureDetector(listener);
    }
}
