package com.owenchan.demo.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.owenchan.demo.service.FloatingService;

public class StartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, FloatingService.class);
        startService(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}
