package com.chiaramail.chiaramailforandroid.activity;

import android.os.Bundle;
import android.view.MotionEvent;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.chiaramail.chiaramailforandroid.activity.K9ActivityCommon.K9ActivityMagic;
import com.chiaramail.chiaramailforandroid.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;

public class K9FragmentActivity extends SherlockFragmentActivity implements K9ActivityMagic {

    private K9ActivityCommon mBase;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        mBase = K9ActivityCommon.newInstance(this);
        super.onCreate(savedInstanceState);
    	//Prevent screen capture
    	this.getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE);
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
