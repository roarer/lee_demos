package com.zte.smssecurity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class SMSSecurityReceiver extends BroadcastReceiver {

	private static final String TAG = "SMSSecurityReceiver";

	private static final String BOOTTIME_TRIGGER = "3";

	private static final String BOOT_ACTION = "android.intent.action.BOOT_COMPLETED";
	private static final String START_ACTION = "com.zte.smssecurity.action.startservice";
	private static final String STOP_ACTION = "com.zte.smssecurity.action.stopservice";
        private static final String BACK_ACTION = "android.intent.action.showSmsSecurityBack";   
          
	private static final String OPTION = "option";
	private static final int SERVICESTATE = 2;

	private static boolean mbStart = false;
	private SQLiteDatabase db = null;
	private Cursor cursor = null;
	PendingIntent pendingIntent = null;

        public static int backCount = 0;

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		db = new SMSSecurityDatabaseHelper(context,
				SMSSecurityDatabaseMetadata.TABLE_NAME, null, 1)
				.getReadableDatabase();
		Intent serviceIntent = new Intent(context, SMSSecurityService.class);
		serviceIntent.putExtra("TriggerType", BOOTTIME_TRIGGER);
		pendingIntent = PendingIntent.getService(context, 0, serviceIntent, 0);
		
	 boolean serviceState = Util.getServiceState();
	 if(serviceState){	
		if (intent.getAction().equals(BOOT_ACTION)) {
		Log.d("lee", TAG+":received boot action...");
			ContentValues values = new ContentValues();
			values.put(SMSSecurityDatabaseMetadata.VALUE, "0");
				db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
						SMSSecurityDatabaseMetadata.TYPE + "=\""
								+ SMSSecurityDatabaseMetadata.SENDSMSNOW  //heli add
								+ "\"", null);								
			
			cursor = db.query(SMSSecurityDatabaseMetadata.TABLE_NAME,
					new String[] { SMSSecurityDatabaseMetadata.VALUE },
					SMSSecurityDatabaseMetadata.TYPE + "=\""
							+ SMSSecurityDatabaseMetadata.SHOWREGISTERED + "\"",
					null, null, null, null);
			while (cursor.moveToNext()) {
				if (cursor.getString(0).equals("0")) {
					// Log.d(TAG, "Complete to boot up");
					// Intent it = new Intent(SERVICE_ACTION);
					cursor.close();
					startService(context, intent);
					break;
				}
			}
		}
	}else{
		Log.d("lee",TAG+":read file FLAG=0,already sent sms,exit!");
			stopService(context, intent);
        }	
		
		 if (intent.getAction().equals(START_ACTION)) {
			if (!mbStart) {
				ContentValues values = new ContentValues();
				values.put(SMSSecurityDatabaseMetadata.VALUE, "1");
				db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
						SMSSecurityDatabaseMetadata.TYPE + "=\""
								+ SMSSecurityDatabaseMetadata.SERVICE_STATE
								+ "\"", null);
				values.clear();
				values.put(SMSSecurityDatabaseMetadata.VALUE, "0");
				db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
						SMSSecurityDatabaseMetadata.TYPE + "=\""
								+ SMSSecurityDatabaseMetadata.USER_CALLTIME
								+ "\"", null);
				values.clear();
				values.put(SMSSecurityDatabaseMetadata.VALUE, "0");
				db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
						SMSSecurityDatabaseMetadata.TYPE + "=\""
								+ SMSSecurityDatabaseMetadata.LONGTIMEBOOT
								+ "\"", null);
				values.clear();
				values.put(SMSSecurityDatabaseMetadata.VALUE, "0");
				db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
						SMSSecurityDatabaseMetadata.TYPE + "=\""
								+ SMSSecurityDatabaseMetadata.SENDSMSNOW  //heli add
								+ "\"", null);				
						
				startService(context, intent);
			}
			showSetting(context);
		} else if (intent.getAction().equals(STOP_ACTION)) {
			ContentValues values = new ContentValues();
			values.put(SMSSecurityDatabaseMetadata.VALUE, "0");
			db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
					SMSSecurityDatabaseMetadata.TYPE + "=\""
							+ SMSSecurityDatabaseMetadata.SERVICE_STATE + "\"",
					null);
			stopService(context, intent);
			showSetting(context);
		} else if (intent.getAction().equals(BACK_ACTION)) {
			if (intent.getStringExtra("back").equals("ok")){
				Log.d(TAG, "back ok");
				ContentValues values = new ContentValues();   
				values.put(SMSSecurityDatabaseMetadata.VALUE, "1");
				db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
						SMSSecurityDatabaseMetadata.TYPE + "=\""
								+ SMSSecurityDatabaseMetadata.SENDSMSNOW
								+ "\"", null);
				values.clear();				
			       /*  values.put(SMSSecurityDatabaseMetadata.VALUE, "1");
				db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
						SMSSecurityDatabaseMetadata.TYPE + "=\""
								+ SMSSecurityDatabaseMetadata.SHOWREGISTERED  
								+ "\"", null); */
				
				
			         startServiceDelay(context, intent, 500);   //500ms   immediately send
		         }
			else if (intent.getStringExtra("back").equals("cancel")){
				backCount++;
				Log.d(TAG, "backCount = "+ backCount);
				switch (backCount) {
				 case 1:
				     startServiceDelay(context, intent, 3600000);     //second time
				 break;
		
				 case 2:
				     ContentValues values = new ContentValues();   
				     values.put(SMSSecurityDatabaseMetadata.VALUE, "1");
				     db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
						SMSSecurityDatabaseMetadata.TYPE + "=\""
								+ SMSSecurityDatabaseMetadata.SENDSMSNOW
								+ "\"", null);
				     values.clear();
				     values.put(SMSSecurityDatabaseMetadata.VALUE, "0");
				     db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
						SMSSecurityDatabaseMetadata.TYPE + "=\""
								+ SMSSecurityDatabaseMetadata.SHOWREGISTERED  
								+ "\"", null);
				     values.clear();
			             startServiceDelay(context, intent, 7200000);   
			             break; 
				 default:
				 break;
			       }  
				 
			}				
			
		}
		//aeon lee add start
		Log.d("lee",TAG+":action="+intent.getAction());
		if("com.zte.smssecurity.action.check_if_sent".equals(intent.getAction())){
			Cursor cursor = db.query(
						SMSSecurityDatabaseMetadata.TABLE_NAME, new String[] {
								SMSSecurityDatabaseMetadata.TYPE,
								SMSSecurityDatabaseMetadata.VALUE },
						SMSSecurityDatabaseMetadata.TYPE + "=" + "\""
								+ SMSSecurityDatabaseMetadata.SHOWREGISTERED
								+ "\"", null, null, null, null);
			int showRegistered = 0;
			while (cursor.moveToNext()) {
				if (cursor.getString(0).equals(
						SMSSecurityDatabaseMetadata.SHOWREGISTERED)) {
					showRegistered = Integer.parseInt(cursor.getString(1));
				}
			}
			if(showRegistered==1){
				Toast.makeText(context, "message has been sent!", Toast.LENGTH_LONG).show();
			}else{
				Toast.makeText(context, "message not sent yet!", Toast.LENGTH_LONG).show();
			}
			cursor.close();
		}else if("com.zte.smssecurity.action.clear_flags".equals(intent.getAction())){
			clearFlags(context,db);
			Toast.makeText(context, "Sales tracker data cleared!", Toast.LENGTH_LONG).show();
		}else if("com.zte.smssecurity.action.sms_result".equals(intent.getAction())){
			//从MMS中返回的结果,写入到数据库
			int result = intent.getIntExtra("sms_result",0);
			Log.d("lee",TAG+":result from mms to SmsSecurity:"+result);
			if(result==1){
				ContentValues values = new ContentValues();
				values.put(SMSSecurityDatabaseMetadata.VALUE, "1");
				db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
						SMSSecurityDatabaseMetadata.TYPE + "=\""
								+ SMSSecurityDatabaseMetadata.SHOWREGISTERED  
								+ "\"", null);
				Util.writeCloseFlag();
				
				AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
				alarmManager.cancel(pendingIntent);
				mbStart = false;//cancel alarms that have never triggered
			}else{
			}
		}
		//aeon add end
		
		db.close();
	}

           private void startServiceDelay(Context context, Intent intent, int setTime) {    //heli add 
		Log.d(TAG, "Start Service");
		PackageManager pm = context.getPackageManager();
		ComponentName component = new ComponentName("com.zte.smssecurity",
				"com.zte.smssecurity.PhoneStateReceiver");
		pm.setComponentEnabledSetting(component,
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);
		Cursor cursor = db.query(SMSSecurityDatabaseMetadata.TABLE_NAME,
				new String[] { SMSSecurityDatabaseMetadata.TYPE,
						SMSSecurityDatabaseMetadata.VALUE },
				SMSSecurityDatabaseMetadata.TYPE + "=\""
						+ SMSSecurityDatabaseMetadata.BOOTTIME + "\"", null,
				null, null, null);
		long bootTime = setTime; 	
		bootTime += SystemClock.elapsedRealtime();
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);							
		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, bootTime,
				pendingIntent);
		Log.d(TAG, "register alarm");
		mbStart = true;
	}




	private void startService(Context context, Intent intent) {
		Log.d("lee", TAG+":Start Alarm by BOOTTIME_TRIGGER");
		//enable PhoneStateReceiver
		/* PackageManager pm = context.getPackageManager();
		ComponentName component = new ComponentName("com.zte.smssecurity",
				"com.zte.smssecurity.PhoneStateReceiver");
		pm.setComponentEnabledSetting(component,
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP); */
		Cursor cursor = db.query(SMSSecurityDatabaseMetadata.TABLE_NAME,
				new String[] { SMSSecurityDatabaseMetadata.TYPE,
						SMSSecurityDatabaseMetadata.VALUE },
				SMSSecurityDatabaseMetadata.TYPE + "=\""
						+ SMSSecurityDatabaseMetadata.BOOTTIME + "\"", null,
				null, null, null);
		long bootTime = 0;
		while (cursor.moveToNext()) {
			if (cursor.getString(0)
					.equals(SMSSecurityDatabaseMetadata.BOOTTIME)) {
				bootTime = Long.parseLong(cursor.getString(1));
				Log.d(TAG, "boot time" + bootTime);
				break;
			}
		}
		cursor.close();
		bootTime += SystemClock.elapsedRealtime();
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);		
		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, bootTime,
				pendingIntent);
		Log.d(TAG, "register alarm");
		mbStart = true;
	}

	private void stopService(Context context, Intent intent) {
		Log.d(TAG, "Stop Service");
		
		//write close flag to db
		//dbh.stopService();
		
		//write close flag to file
		//Util.writeCloseFlag();//aeon lee,only close flag when sms arrived to destination
 	    
 	    //disable PhoneStateReceiver
		/* PackageManager pm = context.getPackageManager();
		ComponentName component = new ComponentName("com.zte.smssecurity",
				"com.zte.smssecurity.PhoneStateReceiver");
		pm.setComponentEnabledSetting(component,
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
		Log.d(TAG, "Disable PhoneStateReceiver!"); */
	    
	    //disable SMSSecurityReceiver
	        /* ComponentName component_1 = new ComponentName("com.zte.smssecurity",
				"com.zte.smssecurity.SMSSecurityReceiver");
		pm.setComponentEnabledSetting(component_1,
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP); */
				
					
		
		//remove boottime alarm
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);
		Log.d(TAG, "Remove alarm");
		mbStart = false;
	}
	
	private void showSetting(Context context){
		Intent settingIntent = new Intent();
		settingIntent.setClassName("com.zte.smssecurity",
				"com.zte.smssecurity.SMSSecuritySettings");
		settingIntent.putExtra(OPTION, SERVICESTATE);
		settingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(settingIntent);
	}
	private void clearFlags(Context context,SQLiteDatabase db){
		Util.writeOpenFlag();
		ContentValues values = new ContentValues();   
		//-------------------------
		values.put(SMSSecurityDatabaseMetadata.VALUE, "0");
		db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
				SMSSecurityDatabaseMetadata.TYPE + "=\""
						+ SMSSecurityDatabaseMetadata.SENDSMSNOW
						+ "\"", null);
		values.clear(); 
		//-------------------------	
		values.put(SMSSecurityDatabaseMetadata.VALUE, "1");
		db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
				SMSSecurityDatabaseMetadata.TYPE + "=\""
						+ SMSSecurityDatabaseMetadata.SERVICE_STATE
						+ "\"", null);
		values.clear(); 
		//-------------------------	
		values.put(SMSSecurityDatabaseMetadata.VALUE, "0");
		db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
			SMSSecurityDatabaseMetadata.TYPE + "=\""
			+ SMSSecurityDatabaseMetadata.SHOWREGISTERED  
				+ "\"", null);
		values.clear(); 
		//-------------------------	
		context.stopService(new Intent("com.zte.smssecurity.SMSSecurityService"));
		//-------------------------	
		Log.d("lee",TAG+":clearFlags finish!");
	}
}
