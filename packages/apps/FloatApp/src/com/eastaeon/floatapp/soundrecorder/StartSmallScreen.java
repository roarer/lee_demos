package com.eastaeon.floatapp.soundrecorder;

import android.content.BroadcastReceiver;  
import android.content.Context;  
import android.content.Intent;  
import android.util.Log;  

public class StartSmallScreen extends BroadcastReceiver {  

	@Override  
	public void onReceive(Context context, Intent intent) {  
		Intent service = new Intent(context,SoundRecorderSmallScreen.class);
		if(intent.getAction().equals("eastaeon.intent.soundrecorder.OPEN_SOUND_RECORDER")) {
			context.startService(service); 
		} else if(intent.getAction().equals("eastaeon.intent.soundrecorder.CLOSE_SOUND_RECORDER")) {
			context.stopService(service);
		} else if(intent.getAction().equals("eastaeon.intent.action.CLOSE_FLOTA_APP")) {
			context.stopService(service);
		}
	}  

}  