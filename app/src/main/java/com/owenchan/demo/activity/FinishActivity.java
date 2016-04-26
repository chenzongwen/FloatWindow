package com.owenchan.demo.activity;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.owenchan.demo.service.FloatingService;

public class FinishActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stopServices();
        Toast.makeText(getApplicationContext(), "-- finish float window --", Toast.LENGTH_SHORT).show();

        finish();
    }

    private void stopServices() {
        Intent intent;

        intent = new Intent(FinishActivity.this, FloatingService.class);
        stopService(intent);

//		intent = new Intent(FinishService.this, QuicklicMainService.class);
//		stopService(intent);
//
//		intent = new Intent(FinishService.this, QuicklicFavoriteService.class);
//		stopService(intent);
//
//		intent = new Intent(FinishService.this, QuicklicHardwareService.class);
//		stopService(intent);
//
//		intent = new Intent(FinishService.this, QuicklicKeyBoardService.class);
//		stopService(intent);
    }
}
