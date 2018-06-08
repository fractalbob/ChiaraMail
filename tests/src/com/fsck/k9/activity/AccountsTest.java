package com.chiaramail.chiaramailforandroid.activity;

import android.test.ActivityInstrumentationTestCase2;
import com.chiaramail.chiaramailforandroid.activity.Accounts;
/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.chiaramail.chiaramailforandroid.activity.AccountsTest \
 * com.chiaramail.chiaramailforandroid.tests/android.test.InstrumentationTestRunner
 */
public class AccountsTest extends ActivityInstrumentationTestCase2<Accounts> {

    public AccountsTest() {
        super("com.chiaramail.chiaramailforandroid", Accounts.class);
    }

}
