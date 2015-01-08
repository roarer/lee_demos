package com.eastaeon.floatapp;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;

public class LauncherActivity extends Activity 
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);
		startService(new Intent(this, FloatService.class)); 
        finish();
    }
}