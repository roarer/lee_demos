package com.zte.smssecurity;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.CallLog;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PhoneStateReceiver extends BroadcastReceiver {//通话状态改变时，就启动PhoneTimeHandleService服务去查询通话时长是否达到要求，达到要求则发送短信，此广播接收器在项目中并未使用
	
	private static final String TAG = "PhoneStateReceiver";
	private static final String SERVICE_ACTION = "com.zte.smssecurity.PhoneTimeHandleService";

	private TelephonyManager telephonyManager = null;

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.d(TAG, "PhoneStateChange");
		telephonyManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		if (TelephonyManager.CALL_STATE_OFFHOOK == telephonyManager
				.getCallState()) {
			Log.d(TAG, "offhook");
		} else if (TelephonyManager.CALL_STATE_RINGING == telephonyManager
				.getCallState()) {
			Log.d(TAG, "ringing");
		} else if (TelephonyManager.CALL_STATE_IDLE == telephonyManager
				.getCallState()) {
			context.startService(new Intent(SERVICE_ACTION));
			Log.d(TAG, "idle");
		}
	}
}
