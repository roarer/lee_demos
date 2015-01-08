package com.eastaeon.floatapp;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.MenuItem;
import android.content.ComponentName;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
public class SettingActivity extends Activity 
{

	private Switch mSwitch;
	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;
	private boolean isCheck = false;
	
	private static final String FLOAT_SWITCH = "float_switch";
	private static final String CLOSE_FLOAT_APP = "eastaeon.intent.action.CLOSE_FLOTA_APP";
	private static final String OPEN_FLOAT_APP = "eastaeon.intent.action.float.OPEN_FLOAT";
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.float_setting_activity);
		/* preferences = getSharedPreferences("floatapp",MODE_PRIVATE);
		editor = preferences.edit(); */
		boolean isFloatAppOn = (Settings.System.getInt(getContentResolver(),Settings.System.FLOAT_APP_STATE, 0) != 0);
		
		mSwitch = (Switch)findViewById(R.id.item_switch);
		mSwitch.setChecked(isFloatAppOn);
		
		mSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {  
		
            @Override  
            public void onCheckedChanged(CompoundButton buttonView,  
                    boolean isChecked) {  
                // TODO Auto-generated method stub
				Intent mIntent = new Intent();
				/* editor.putBoolean(FLOAT_SWITCH,isChecked);
				editor.commit(); */
				Settings.System.putInt(getContentResolver(),Settings.System.FLOAT_APP_STATE,isChecked ? 1 : 0);
                if (isChecked) {  
					mIntent.setAction(OPEN_FLOAT_APP);
					sendBroadcast(mIntent);
                } else {  
                    
					mIntent.setAction(CLOSE_FLOAT_APP);
					sendBroadcast(mIntent);
                }  
            }  
        }); 
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent intent = new Intent();
				intent.setComponent(new ComponentName("com.android.settings","com.android.settings.Settings"));
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}