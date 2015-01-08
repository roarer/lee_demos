package com.eastaeon.floatapp.videoplayer;

import android.content.BroadcastReceiver;  
import android.content.Context;  
import android.content.Intent;  
import android.util.Log;  

public class StartSmallScreen extends BroadcastReceiver {  

	@Override  
	public void onReceive(Context context, Intent intent) {  
		Intent serviceVideoList = new Intent(context,VideoSmallScreen.class);
		if(intent.getAction().equals("eastaeon.intent.videoplayer.OPEN_VIDEO")) {
			context.startService(serviceVideoList); 
		} else if(intent.getAction().equals("eastaeon.intent.videoplayer.CLOSE_VIDEO")) {
			context.stopService(serviceVideoList);
		} else if(intent.getAction().equals("eastaeon.intent.action.CLOSE_FLOTA_APP")) {
			context.stopService(serviceVideoList);
		}
	}  

}  