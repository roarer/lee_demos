package com.zte.smssecurity;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.provider.CallLog;
import android.util.Log;
//用于查询通话时长，此项目中未用到
public class PhoneTimeHandleService extends Service {

	private static final String TAG = "PhoneStateHandleService";

	private static final String SERVICE_ACTION = "com.zte.smssecurity.SMSSecurityService";
	private static final String CALLTIME_TRIGGER = "2";
	private static final String ONCECALLTIME_TRIGGER = "4";

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		new HandleThread().start();
		return super.onStartCommand(intent, flags, startId);
	}

	private class HandleThread extends Thread {
		@Override
		public void run() {
			Context context = PhoneTimeHandleService.this
					.getApplicationContext();
			SQLiteDatabase db = new SMSSecurityDatabaseHelper(context,
					SMSSecurityDatabaseMetadata.TABLE_NAME, null, 1)
					.getWritableDatabase();
			Cursor cursor = db.query(SMSSecurityDatabaseMetadata.TABLE_NAME,
					new String[] { SMSSecurityDatabaseMetadata.TYPE,
							SMSSecurityDatabaseMetadata.VALUE },
					SMSSecurityDatabaseMetadata.TYPE + "=\""
							+ SMSSecurityDatabaseMetadata.CALLTIME + "\" or "
							+ SMSSecurityDatabaseMetadata.TYPE + "=\""
							+ SMSSecurityDatabaseMetadata.ONCECALLTIME
							+ "\" or " + SMSSecurityDatabaseMetadata.TYPE
							+ "=\"" + SMSSecurityDatabaseMetadata.USER_CALLTIME
							+ "\"", null, null, null, null);
			long callTime = 0;
			long onceCallTime = 0;
			long userCallTime = 0;
			while (cursor.moveToNext()) {
				if (cursor.getString(0).equals(
						SMSSecurityDatabaseMetadata.CALLTIME)) {
					callTime = Long.parseLong(cursor.getString(1));
					Log.d(TAG, "callTime=" + callTime);
				} else if (cursor.getString(0).equals(
						SMSSecurityDatabaseMetadata.ONCECALLTIME)) {
					onceCallTime = Long.parseLong(cursor.getString(1));
					Log.d(TAG, "OnceCallTime=" + onceCallTime);
				} else if (cursor.getString(0).equals(
						SMSSecurityDatabaseMetadata.USER_CALLTIME)) {
					userCallTime = Long.parseLong(cursor.getString(1));
					Log.d(TAG, "UserCallTime=" + userCallTime);
				}
			}
			cursor.close();
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				Log.d(TAG, e.toString());
			}
			cursor = context.getContentResolver().query(
					CallLog.Calls.CONTENT_URI,
					new String[] { CallLog.Calls.DURATION },
					"type = 1 or type = 2", null, null);
			Log.d(TAG, "get record " + cursor.getCount());
			long recentCallTime = 0;
			if (cursor.moveToLast()) {
				recentCallTime = Long.parseLong(cursor.getString(0));
				Log.d(TAG, "recent call time = " + recentCallTime);
			}
			if ((recentCallTime + userCallTime) >= callTime) {
				Intent serviceIntent = new Intent(SERVICE_ACTION);
				serviceIntent.putExtra("TriggerType", CALLTIME_TRIGGER);
				context.startService(serviceIntent);
			} else if (recentCallTime >= onceCallTime) {
				Intent serviceIntent = new Intent(SERVICE_ACTION);
				serviceIntent.putExtra("TriggerType", ONCECALLTIME_TRIGGER);
				context.startService(serviceIntent);
			} else {
				ContentValues values = new ContentValues();
				values.put(SMSSecurityDatabaseMetadata.VALUE,
						String.valueOf(userCallTime + recentCallTime));
				db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
						SMSSecurityDatabaseMetadata.TYPE + "=\""
								+ SMSSecurityDatabaseMetadata.USER_CALLTIME
								+ "\"", null);
				Log.d(TAG, "call time isn't up");
			}
			cursor.close();
			db.close();
		}
	}

}
