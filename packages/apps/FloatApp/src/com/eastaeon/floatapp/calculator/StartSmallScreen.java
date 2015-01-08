package com.eastaeon.floatapp.calculator;

import android.content.BroadcastReceiver;  
import android.content.Context;  
import android.content.Intent;  
import android.util.Log;  

public class StartSmallScreen extends BroadcastReceiver {  

	@Override  
	public void onReceive(Context context, Intent intent) {  
		Intent service = new Intent(context,CalculatorSmallScreen.class); 
		if(intent.getAction().equals("eastaeon.intent.calculator.OPEN_CALCULATOR")) {
			context.startService(service);  
		} else if(intent.getAction().equals("eastaeon.intent.calculator.CLOSE_CALCULATOR")) {
			context.stopService(service);
		} else if(intent.getAction().equals("eastaeon.intent.action.CLOSE_FLOTA_APP")) {
			context.stopService(service);
		}
	}  

}  