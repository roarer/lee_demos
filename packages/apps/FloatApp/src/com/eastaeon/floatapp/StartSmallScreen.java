package com.eastaeon.floatapp;

import android.content.BroadcastReceiver;  
import android.content.Context;  
import android.content.Intent;  
import android.util.Log;  

public class StartSmallScreen extends BroadcastReceiver {  

	@Override  
	public void onReceive(Context context, Intent intent) {  
		
		if(intent.getAction().equals("eastaeon.intent.action.float.OPEN_FLOAT")) {
			Intent floatService = new Intent(context,FloatService.class);
			context.startService(floatService); 
		} else if(intent.getAction().equals("eastaeon.intent.action.float.CLOSE_FLOAT")) {
			Intent floatService = new Intent(context,FloatService.class);
			context.stopService(floatService);
		} else if(intent.getAction().equals("eastaeon.intent.action.launch.OPEN_LAUNCH")) {
			Intent launchService = new Intent(context,LauncherService.class);
			context.startService(launchService); 
		} else if(intent.getAction().equals("eastaeon.intent.action.launch.CLOSE_LAUNCH")) {
			Intent launchService = new Intent(context,LauncherService.class);
			context.stopService(launchService);
		} else if(intent.getAction().equals("eastaeon.intent.action.CLOSE_FLOTA_APP")) {
			Intent floatService = new Intent(context,FloatService.class);
			context.stopService(floatService);		
			Intent launchService = new Intent(context,LauncherService.class);
			context.stopService(launchService);
		}
	}  

}  