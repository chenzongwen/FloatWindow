package com.owenchan.demo.service;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.util.Property;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import com.ouwenchan.demo.FloatingAIDL;
import com.owenchan.demo.R;
import com.owenchan.demo.activity.FinishActivity;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Owen Chan on 16/4/25.
 * Copyright © 2016 Owen Chan. All rights reserved.
 */
public class FloatingService extends Service {

    private static Context mContext;

    private static final int NOTIFICATION_ID = 1;
    private static final float SCREEN_HORIZON_RATIO = 0.55f;
    private static final int DOUBLE_PRESS_INTERVAL = 300;
    private static final int LIMITED_MOVE_DISTANCE = 10;
    private static final long ANIMATION_DURATION = 100;

    public static RemoteBinder remoteBinder;
    private WindowManager mWindowManager;
    private NotificationManager mNotificationManager;

    private ImageView dragIcon;
    private LayoutParams floatWinParams;
    private static RelativeLayout dragIconContainer;

    private static boolean moveToSide;
    private boolean isMoved = false;
    private boolean isDoubleClicked = false;

    private Timer mTimer;
    private long lastPressTime;

    private int screenWidth;
    private int screenHeight;

    private int dragIconWidth;
    private int dragIconHeight;

    private int screenCenterX;
    private int screenCenterY;



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return remoteBinder;
    }

    /**
     * if the screen orientation is changed from portrait to landscape
     * we should init display metrics again
     * @param newConfig new config
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            initDisplayMetrics();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * if the service is restart we should finish this service
     * @param intent intent
     * @param flags flag
     * @param startId start id
     * @return START_NOT_STICKY
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (startId == 1 || flags == 1) {
                initialize();
                initDisplayMetrics();
                initDragIcon();
                setDragIconListener();
                startNotification();
            } else {
                stopService(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_NOT_STICKY;
    }

    /**
     * init WindowManager and timer
     */
    private void initialize() {
        try {
            mContext = FloatingService.this;
            remoteBinder = new RemoteBinder();
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            bindService(new Intent(this, FloatingService.class), serviceConnection, Service.BIND_AUTO_CREATE);

            mTimer = new Timer();
            moveToSide = remoteBinder.getAnimation();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * init drag icon width and height and the screen metrics
     * @throws RemoteException
     */
    private void initDisplayMetrics() throws RemoteException {
        Display windowDisplay = mWindowManager.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowDisplay.getMetrics(displayMetrics);

        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        if (windowDisplay.getRotation() == Surface.ROTATION_0) {
            dragIconWidth = (int) (screenWidth * remoteBinder.setSize());
            dragIconHeight = (int) (screenHeight * remoteBinder.setSize());
        } else {
            dragIconWidth = (int) (screenWidth * remoteBinder.setSize() * SCREEN_HORIZON_RATIO);
            dragIconHeight = (int) (screenHeight * remoteBinder.setSize() * SCREEN_HORIZON_RATIO);
        }

        screenCenterX = (screenWidth - dragIconWidth) >> 1;
        screenCenterY = (screenWidth - dragIconHeight) >> 1;
    }

    /**
     * init the drag icon
     * @throws RemoteException
     */
    private void initDragIcon() throws RemoteException {
        dragIcon = new ImageView(this);
        dragIcon.setImageResource(remoteBinder.setDragIcon());
        dragIcon.setLayoutParams(new RelativeLayout.LayoutParams(dragIconWidth, dragIconHeight));

        dragIconContainer = new RelativeLayout(this);
        dragIconContainer.addView(dragIcon);
        dragIconContainer.setBackgroundResource(android.R.color.transparent);

        floatWinParams = new LayoutParams(LayoutParams.TYPE_PHONE, LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.RGBA_8888);
        floatWinParams.windowAnimations = android.R.style.Animation_Dialog;
        floatWinParams.gravity = Gravity.TOP | Gravity.LEFT;
        floatWinParams.x = screenCenterX;
        floatWinParams.y = screenCenterY;
        floatWinParams.width = dragIconWidth;
        floatWinParams.height = dragIconHeight;

        mWindowManager.addView(dragIconContainer, floatWinParams);
    }

    /**
     * set listener including touch event click event
     * and long click event
     */
    private void setDragIconListener(){
        dragIconContainer.setOnTouchListener(mOnTouchListener);
        dragIconContainer.setOnClickListener(mOnClickListener);
        dragIconContainer.setOnLongClickListener(mOnLongClickListener);
    }

    /**
     * set the drag icon visibility
     * @param bool true visible false invisible
     */
    public static void setVisibility(boolean bool) {
        if (bool) {
            getDragIconContainer().setVisibility(View.VISIBLE);
        } else {
            getDragIconContainer().setVisibility(View.GONE);
        }
    }

    /**
     * get container of the drag icon
     * @return dragIconContainer
     */
    public static RelativeLayout getDragIconContainer() {
        return dragIconContainer;
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @SuppressLint("HandlerLeak")
        @Override
        public void onClick(View v) {
            try {
                if (v == getDragIconContainer()) {
                    long pressTime = System.currentTimeMillis();
                    if ((pressTime - lastPressTime) <= DOUBLE_PRESS_INTERVAL) {
                        remoteBinder.doubleTouched();
                        isDoubleClicked = true;
                    } else {
                        isDoubleClicked = false;
                        Message message = new Message();
                        Handler handler = new Handler() {
                            public void handleMessage(Message message) {
                                if (!isDoubleClicked && !isMoved) {
                                    try {
                                        remoteBinder.touched();
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        };
                        handler.sendMessageDelayed(message, DOUBLE_PRESS_INTERVAL);
                    }
                    lastPressTime = pressTime;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    private View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (!isMoved) {
                try {
                    remoteBinder.longTouched();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
    };

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        private int initialX;
        private int initialY;
        private float initialTouchX;
        private float initialTouchY;
        private int moveTouchX;
        private int moveTouchY;

        private RelativeLayout dragViewContainer;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            dragViewContainer = getDragIconContainer();
            try {
                if (v == dragViewContainer) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            isMoved = false;

                            initialX = floatWinParams.x;
                            initialY = floatWinParams.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            break;

                        case MotionEvent.ACTION_MOVE:
                            moveTouchX = (int) (event.getRawX() - initialTouchX);
                            moveTouchY = (int) (event.getRawY() - initialTouchY);

                            floatWinParams.x = initialX + moveTouchX;
                            floatWinParams.y = initialY + moveTouchY;

                            mWindowManager.updateViewLayout(getDragIconContainer(), floatWinParams);

                            isMoved = true;
                            if (Math.abs(moveTouchX) < LIMITED_MOVE_DISTANCE && Math.abs(moveTouchY) < LIMITED_MOVE_DISTANCE) {
                                isMoved = false;
                            }
                            break;

                        case MotionEvent.ACTION_UP:
                            if (isMoved) {
                                mTimer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        isMoved = false;
                                    }
                                }, DOUBLE_PRESS_INTERVAL + 100);
                            }

                            if (moveToSide && isMoved) {
                                moveToSide();
                            }
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }


        private void moveToSide() {
            initialX = floatWinParams.x;
            initialY = floatWinParams.y;

            int toX = floatWinParams.x;
            int toY = floatWinParams.y;

            if (floatWinParams.x > screenCenterX) {
                if (floatWinParams.y > screenCenterY) {
                    if (screenWidth - (floatWinParams.x + dragViewContainer.getWidth()) > screenHeight - (floatWinParams.y + dragViewContainer.getHeight())) {
                        toY = screenHeight - dragViewContainer.getHeight();
                    } else {
                        toX = screenWidth - dragViewContainer.getWidth();
                    }
                } else {
                    if (screenWidth - (floatWinParams.x + dragViewContainer.getWidth()) > floatWinParams.y) {
                        toY = 0;
                    } else {
                        toX = screenWidth - dragViewContainer.getWidth();
                    }
                }
            } else {
                if (floatWinParams.y > screenCenterY) {
                    if (floatWinParams.x > screenHeight - (floatWinParams.y + dragViewContainer.getHeight())) {
                        toY = screenHeight - dragViewContainer.getHeight();
                    } else {
                        toX = 0;
                    }
                } else {
                    if (floatWinParams.x > floatWinParams.y) {
                        toY = 0;
                    } else {
                        toX = 0;
                    }
                }
            }

            floatWinParams.x = toX;
            floatWinParams.y = toY;

            startAnimator(dragIcon, initialX, initialY, toX, toY);
        }
    };

    public void startAnimator(View dragView, int srcX, int srcY, final int desX, final int desY) {
        floatWinParams.width = LayoutParams.MATCH_PARENT;
        floatWinParams.height = LayoutParams.MATCH_PARENT;
        mWindowManager.updateViewLayout(getDragIconContainer(), floatWinParams);

        Property<View, Float> xProp = View.X;
        Property<View, Float> yProp = View.Y;

        float[] xFloat = new float[2];
        xFloat[0] = srcX;
        xFloat[1] = desX;

        float[] yFloat = new float[2];
        yFloat[0] = srcY;
        yFloat[1] = desY;

        ObjectAnimator xAnimator = ObjectAnimator.ofFloat(dragView, xProp, xFloat);
        ObjectAnimator yAnimator = ObjectAnimator.ofFloat(dragView, yProp, yFloat);

        final AnimatorSet localAnimatorSet = new AnimatorSet();
        localAnimatorSet.setInterpolator(new LinearInterpolator());
        localAnimatorSet.setDuration(ANIMATION_DURATION);

        localAnimatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                getDragIconContainer().setEnabled(false);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                getDragIconContainer().setEnabled(true);

                floatWinParams.width = dragIconWidth;
                floatWinParams.height = dragIconHeight;

                floatWinParams.x = desX;
                floatWinParams.y = desY;

                mWindowManager.updateViewLayout(getDragIconContainer(), floatWinParams);

                dragIcon.setX(0);
                dragIcon.setY(0);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }
        });

        localAnimatorSet.playTogether(xAnimator, yAnimator);
        localAnimatorSet.start();
    }

    public void onDestroy() {
        super.onDestroy();
        if (getDragIconContainer() != null) {
            mWindowManager.removeView(getDragIconContainer());
        }
        mNotificationManager.cancel(NOTIFICATION_ID);
    }



    private void startNotification() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(mContext, FinishActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(getApplicationContext()).setContentTitle(getString(R.string.app_name))
                .setContentText("stop service").setSmallIcon(R.mipmap.ic_launcher).setTicker("float window").setOngoing(true)
                .setContentIntent(pendingIntent).build();
        notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_NO_CLEAR;

        mNotificationManager.notify(NOTIFICATION_ID, notification);
        startForeground(NOTIFICATION_ID, notification);
    }







    public static class RemoteBinder extends FloatingAIDL.Stub {
        @Override
        public int setDragIcon() throws RemoteException {
            return R.mipmap.ic_launcher;
        }

        @Override
        public void setFloatingVisibility(boolean value) throws RemoteException {
            setVisibility(value);
        }

        @Override
        public int getFloatingVisibility() throws RemoteException {
            return getDragIconContainer().getVisibility();
        }

        @Override
        public float setSize() throws RemoteException {
            return 0.14f;
        }

        @Override
        public boolean getAnimation() throws RemoteException {
            return true;
        }

        @Override
        public void touched() throws RemoteException {
//            Intent intent = new Intent(mContext, MyNotificationWindow.class);
//            setFloatingVisibility(false);
//            mContext.startService(intent);
        }

        @Override
        public void doubleTouched() throws RemoteException {
            moveToSide = !moveToSide;
            if (moveToSide) {
                Toast.makeText(mContext, " side mode  ", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, " floating mode ", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void longTouched() throws RemoteException {
            try {
                mContext.unbindService(serviceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Intent intent = new Intent(mContext, FinishActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }
    }

    private static ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            remoteBinder = (RemoteBinder) iBinder;
            mContext.unbindService(serviceConnection);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
}
