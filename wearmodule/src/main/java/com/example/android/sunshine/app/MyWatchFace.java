/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.graphics.Palette;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class MyWatchFace extends CanvasWatchFaceService {

    public static final String ACTION_NOTIFY_WEATHER_BITMAP_AVAILABLE = "ACTION_NOTIFY_WEATHER_BITMAP_AVAILABLE";
    public static final String EXTRA_WEATHER_BITMAP = "EXTRA_WEATHER_BITMAP";

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    private static final int MSG_UPDATE_WEATHER_DATA = 1;
    public static final int BITMAP_SIZE = 48;

    private GetRecentWeatherDataTask weatherDataTask;
    private String mRecentWeatherData;
    private Bitmap mWeatherBitmap;
    private float mWeatherImageYOffset;
    private Paint mTimePaint;
    private Paint mWeatherTextPaint;
    Paint mBackgroundPaint;

    private int timeColor;
    private int weatherColor;
    private int backgroundColor;


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                    case MSG_UPDATE_WEATHER_DATA:
                        engine.handleUpdateWeatherData();
                        break;
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("MyWatchFace.onStartCommand " + intent);
        if(intent != null && ACTION_NOTIFY_WEATHER_BITMAP_AVAILABLE.equals(intent.getAction())){
            Parcelable parcelableExtra = intent.getParcelableExtra(EXTRA_WEATHER_BITMAP);
            System.out.println("parcelableExtra = " + parcelableExtra);
            mWeatherBitmap = Bitmap.createScaledBitmap((Bitmap) parcelableExtra, BITMAP_SIZE, BITMAP_SIZE, false);
            if(mWeatherBitmap != null){
                Palette.from(mWeatherBitmap).maximumColorCount(24).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        weatherColor = palette.getVibrantColor(weatherColor);
                        timeColor = palette.getDarkVibrantColor(timeColor);
                        mWeatherTextPaint.setColor(weatherColor);
                        mTimePaint.setColor(timeColor);
//                        backgroundColor = palette.getLightMutedColor(backgroundColor);
//                        mBackgroundPaint.setColor(backgroundColor);

                    }
                });

            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;


        boolean mAmbient;
        Time mTime;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        int mTapCount;

        float mXOffset;
        float mYOffset;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = MyWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mWeatherImageYOffset = resources.getDimension(R.dimen.weather_image_y_offset);

            mBackgroundPaint = new Paint();
            backgroundColor = resources.getColor(R.color.background);
            mBackgroundPaint.setColor(backgroundColor);

            mTimePaint = new Paint();
            mWeatherTextPaint = new Paint();
            timeColor = resources.getColor(R.color.digital_text);
            mTimePaint = createTextPaint(timeColor);
            weatherColor = resources.getColor(R.color.digital_weather_text);
            mWeatherTextPaint = createTextPaint(weatherColor);
            mWeatherTextPaint.setTextAlign(Paint.Align.CENTER);
            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_WEATHER_DATA, 50);

            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = MyWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            float weatherTextSize = resources.getDimension(isRound
                    ? R.dimen.weather_text_size : R.dimen.weather_text_size);

            mTimePaint.setTextSize(textSize);
            mWeatherTextPaint.setTextSize(weatherTextSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTimePaint.setAntiAlias(!inAmbientMode);
                    mWeatherTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = MyWatchFace.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background : R.color.background2));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            if(mWeatherBitmap != null){
                canvas.drawBitmap(mWeatherBitmap, bounds.centerX() - (BITMAP_SIZE /2),  mWeatherImageYOffset, mWeatherTextPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();
            String text = mAmbient
                    ? String.format("%d:%02d", mTime.hour, mTime.minute)
                    : String.format("%d:%02d:%02d", mTime.hour, mTime.minute, mTime.second);
            canvas.drawText(text, bounds.centerX(), mYOffset, mTimePaint);
            //TODO -- Andrew -- Here is where I'd draw the hi/low text
            if(mRecentWeatherData != null && mRecentWeatherData.length() > 0) {
//                canvas.drawText(mRecentWeatherData, mXOffset, mYOffset * 1.25f, mWeatherTextPaint);
                canvas.drawText(mRecentWeatherData, bounds.centerX(), mYOffset * 1.25f, mWeatherTextPaint);
            }


        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        private void handleUpdateWeatherData(){
            System.out.println("Engine.handleUpdateWeatherData");
            if(weatherDataTask != null && !weatherDataTask.isCancelled()){
                weatherDataTask.cancel(true);
            }
            weatherDataTask = new GetRecentWeatherDataTask();
            weatherDataTask.execute();
        }
    }

    private class GetRecentWeatherDataTask extends AsyncTask<Void, Void, String>{
        @Override
        protected String doInBackground(Void... params) {
            // Get today's data from the ContentProvider

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            return prefs.getString("KEY_WEARABLE_WEATHER_DATA", "dummy");
        }

        @Override
        protected void onPostExecute(String s) {
            mRecentWeatherData = s;
        }
    }
}
