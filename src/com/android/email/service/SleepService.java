package com.android.email.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.android.email.Email;

public class SleepService extends CoreService
{
    
    private static String ALARM_FIRED = "com.android.email.service.SleepService.ALARM_FIRED";
    private static String LATCH_ID = "com.android.email.service.SleepService.LATCH_ID_EXTRA";
    
    
    private static ConcurrentHashMap<Integer, SleepDatum> sleepData = new ConcurrentHashMap<Integer, SleepDatum>();
    
    private static AtomicInteger latchId = new AtomicInteger();
    
    public static void sleep(Context context, long sleepTime, WakeLock wakeLock, long wakeLockTimeout)
    {
        Integer id = latchId.getAndIncrement();
        if (Email.DEBUG)
        {
            Log.d(Email.LOG_TAG, "SleepService Preparing CountDownLatch with id = " + id + ", thread " + Thread.currentThread().getName());
        }
        SleepDatum sleepDatum = new SleepDatum();
        CountDownLatch latch = new CountDownLatch(1);
        sleepDatum.latch = latch;
        sleepData.put(id, sleepDatum);
        
        Intent i = new Intent();
        i.setClassName(context.getPackageName(), "com.android.email.service.SleepService");
        i.putExtra(LATCH_ID, id);
        i.setAction(ALARM_FIRED + "." + id);
        long startTime = System.currentTimeMillis();
        long nextTime = startTime + sleepTime;
        BootReceiver.scheduleIntent(context, nextTime, i);
        if (wakeLock != null)
        {
            sleepDatum.wakeLock = wakeLock;
            sleepDatum.timeout = wakeLockTimeout;
            wakeLock.release();
        }
        try
        {
            boolean timedOut = latch.await(sleepTime, TimeUnit.MILLISECONDS);
            if (timedOut == false)
            {
                if (Email.DEBUG)
                {
                    Log.d(Email.LOG_TAG, "SleepService latch timed out for id = " + id + ", thread " + Thread.currentThread().getName());
                }
                // don't call endSleep here or remove the sleepDatum here, instead of the following block.  
                // We might not get the wakeLock before
                // falling asleep again, so we have to get the wakeLock *first*  The alarmed version will
                // already be running in a WakeLock due to the nature of AlarmManager
                sleepDatum = sleepData.get(id);
                if (sleepDatum != null)
                {
                    reacquireWakeLock(sleepDatum);
                    // OK, we have the wakeLock, now we can remove the sleepDatum
                    sleepData.remove(id);
                }
                
            }
        }
        catch (InterruptedException ie)
        {
            Log.e(Email.LOG_TAG, "SleepService Interrupted", ie);
        }
        long endTime = System.currentTimeMillis();
        long actualSleep = endTime - startTime;
        if (Email.DEBUG)
        {
            Log.d(Email.LOG_TAG, "SleepService requested sleep time was " + sleepTime + ", actual was " + actualSleep);
        }
        if (actualSleep < sleepTime)
        {
            Log.w(Email.LOG_TAG, "SleepService sleep time too short: requested was " + sleepTime + ", actual was " + actualSleep);
        }
    }
    
    private static void endSleep(Integer id)
    {
        if (id != -1)
        {
            SleepDatum sleepDatum = sleepData.remove(id);
            if (sleepDatum != null)
            {
                CountDownLatch latch = sleepDatum.latch;
                if (latch == null)
                {
                    Log.e(Email.LOG_TAG, "SleepService No CountDownLatch available with id = " + id);
                }
                else
                {
                    if (Email.DEBUG)
                    {
                        Log.d(Email.LOG_TAG, "SleepService Counting down CountDownLatch with id = " + id);
                    }
                    latch.countDown();
                }
                reacquireWakeLock(sleepDatum);
            }
            else
            {
                if (Email.DEBUG)
                {
                    Log.d(Email.LOG_TAG, "SleepService Sleep for id " + id + " already finished");
                }
            }
        }
    }
    
    private static void reacquireWakeLock(SleepDatum sleepDatum)
    {
        WakeLock wakeLock = sleepDatum.wakeLock;
        if (wakeLock != null)
        {
            synchronized(wakeLock)
            {
                long timeout = sleepDatum.timeout;
                if (Email.DEBUG)
                {
                    Log.d(Email.LOG_TAG, "SleepService Acquring wakeLock for id for " + timeout + "ms");
                }
                wakeLock.acquire(timeout);
            }
        }
    }

    @Override
    public void startService(Intent intent, int startId)
    {
        if (intent.getAction().startsWith(ALARM_FIRED)) {
            Integer id = intent.getIntExtra(LATCH_ID, -1);
            endSleep(id);
        }
        stopSelf(startId);
    }
    
    private static class SleepDatum
    {
        CountDownLatch latch;
        WakeLock wakeLock;
        long timeout;
    }

}
