package com.eastaeon.floatapp.music;

import android.content.BroadcastReceiver;  
import android.content.Context;  
import android.content.Intent;  
import android.util.Log;  

public class StartSmallScreen extends BroadcastReceiver {  

	@Override  
	public void onReceive(Context context, Intent intent) {  
		Intent musicService = new Intent(context,MusicSmallScreen.class);
		if(intent.getAction().equals("eastaeon.intent.music.OPEN_MUSIC")) {
			context.startService(musicService); 
		} else if(intent.getAction().equals("eastaeon.intent.music.CLOSE_MUSIC")) {
			context.stopService(musicService);
		} else if(intent.getAction().equals("eastaeon.intent.action.CLOSE_FLOTA_APP")) {
			context.stopService(musicService);
		}
	}  

}  