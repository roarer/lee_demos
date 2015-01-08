package com.zte.smssecurity;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mediatek.common.featureoption.FeatureOption;
import android.os.SystemProperties;

//zhangle add start 
import com.mediatek.telephony.TelephonyManagerEx;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.gemini.MTKPhoneFactory;
//zhangle add end

public class SMSSecuritySettings extends PreferenceActivity implements
		OnPreferenceChangeListener {

	private static final String TAG = "SMSSecuritySettings";

	private static final String START_ACTION = "com.zte.smssecurity.action.startservice";
	private static final String STOP_ACTION = "com.zte.smssecurity.action.stopservice";

	private static final String KEY_SERVICESTATE_CHECKBOX = "KEY_SERVICESTATE_CHECKBOX";
	private static final String KEY_SERVICENUMBER_EDITOR = "KEY_SERVICENUMBER_EDITOR";
	private static final String KEY_BOOTTIME = "KEY_BOOTTIME";
	private static final String KEY_CALLTIME = "KEY_CALLTIME";
	private static final String KEY_ONCECALLTIME = "KEY_ONCECALLTIME";
	private static final String KEY_PREFIX01 = "KEY_PREFIX01";
	private static final String KEY_PREFIX02 = "KEY_PREFIX02";
	private static final String KEY_PREFIX03 = "KEY_PREFIX03";
	//private static final String KEY_CONTENT = "KEY_CONTENT";
	private static final String KEY_IMEI_NUMBER = "KEY_IMEI_NUMBER";
	private static final String KEY_MAINBROAD_NUMBER = "KEY_MAINBROAD_NUMBER";
	private static final String KEY_SOFTWARE_VERSION = "KEY_SOFTWARE_VERSION";

	private static final String KEY_SMSSECURITYSETTING="KEY_SMSSECURITYSETTING";
	private static final String KEY_SERVICESTATE_PRF="KEY_SERVICESTATE_PRF";
	private static final String KEY_SERVICENUMBER_PRF = "KEY_SERVICENUMBER_PRF";
	private static final String KEY_TIMESETTINGS_PRF = "KEY_TIMESETTINGS_PRF";
	private static final String KEY_CONTENT_PRF = "KEY_CONTENT_PRF";
	
	private static final String OPTION = "option";
	private static final int EDITNUMBER = 1;
	private static final int SERVICESTATE = 2;

	private CheckBoxPreference startServiceCheckBox = null;
	private ListPreference bootTimeList = null;
	private ListPreference callTimeList = null;
	private ListPreference onceCallTimeList = null;
	private ListPreference prefixList01 = null;
	private ListPreference prefixList02 = null;
	private ListPreference prefixList03 = null;
	private EditTextPreference serviceNumberEditor = null;
	//private Preference contentP = null;
	private Preference IMEI_NumberP = null;
	private Preference mainbroadNumberP = null;
	private Preference softwareVersionP = null;

	private SMSSecurityDatabaseHelper dbh = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.smssecurity_settings);

		startServiceCheckBox = (CheckBoxPreference) findPreference(KEY_SERVICESTATE_CHECKBOX);
		startServiceCheckBox.setOnPreferenceChangeListener(this);
		bootTimeList = (ListPreference) findPreference(KEY_BOOTTIME);
		bootTimeList.setOnPreferenceChangeListener(this);
		callTimeList = (ListPreference) findPreference(KEY_CALLTIME);
		callTimeList.setOnPreferenceChangeListener(this);
		onceCallTimeList = (ListPreference) findPreference(KEY_ONCECALLTIME);
		onceCallTimeList.setOnPreferenceChangeListener(this);
		prefixList01 = (ListPreference) findPreference(KEY_PREFIX01);
		prefixList01.setOnPreferenceChangeListener(this);
		prefixList02 = (ListPreference) findPreference(KEY_PREFIX02);
		prefixList02.setOnPreferenceChangeListener(this);
		prefixList03 = (ListPreference) findPreference(KEY_PREFIX03);
		prefixList03.setOnPreferenceChangeListener(this);
		serviceNumberEditor = (EditTextPreference) findPreference(KEY_SERVICENUMBER_EDITOR);
		serviceNumberEditor.setOnPreferenceChangeListener(this);
		//contentP = findPreference(KEY_CONTENT);
		IMEI_NumberP = findPreference(KEY_IMEI_NUMBER);
		mainbroadNumberP = findPreference(KEY_MAINBROAD_NUMBER);
		softwareVersionP = findPreference(KEY_SOFTWARE_VERSION);

		PreferenceGroup parent = (PreferenceGroup) findPreference(KEY_SMSSECURITYSETTING);
		switch(getIntent().getIntExtra(OPTION, 0)){
		case EDITNUMBER:
			parent.removePreference(findPreference(KEY_SERVICESTATE_PRF));
			parent.removePreference(findPreference(KEY_TIMESETTINGS_PRF));
			parent.removePreference(findPreference(KEY_CONTENT_PRF));
			serviceNumberEditor.setEnabled(true);
			serviceNumberEditor.setSelectable(true);
			break;
		case SERVICESTATE:
			parent.removePreference(findPreference(KEY_SERVICENUMBER_PRF));
			parent.removePreference(findPreference(KEY_TIMESETTINGS_PRF));
			parent.removePreference(findPreference(KEY_CONTENT_PRF));
			break;
	    default:
	    	break;		
		}

		dbh = new SMSSecurityDatabaseHelper(this,
				SMSSecurityDatabaseMetadata.TABLE_NAME, null, 1);
		SQLiteDatabase db = dbh.getReadableDatabase();
		Cursor cursor = db.query(SMSSecurityDatabaseMetadata.TABLE_NAME,
				new String[] { SMSSecurityDatabaseMetadata.TYPE,
						SMSSecurityDatabaseMetadata.VALUE }, null, null, null,
				null, null);
		String type = null;
		while (cursor.moveToNext()) {
			type = cursor.getString(0);
			if (type.equals(SMSSecurityDatabaseMetadata.SERVICE_NUMBER)) {
				serviceNumberEditor.setSummary(cursor.getString(1));
			} else if (type.equals(SMSSecurityDatabaseMetadata.BOOTTIME)) {
				bootTimeList.setSummary(timeFormat(cursor.getString(1), "ms"));
			} else if (type.equals(SMSSecurityDatabaseMetadata.CALLTIME)) {
				callTimeList.setSummary(timeFormat(cursor.getString(1), "s"));
			} else if (type.equals(SMSSecurityDatabaseMetadata.ONCECALLTIME)) {
				onceCallTimeList
						.setSummary(timeFormat(cursor.getString(1), "s"));
			} else if (type.equals(SMSSecurityDatabaseMetadata.PREFIX01)) {
				prefixList01.setSummary(cursor.getString(1));
			} else if (type.equals(SMSSecurityDatabaseMetadata.PREFIX02)) {
				prefixList02.setSummary(cursor.getString(1));
			} else if (type.equals(SMSSecurityDatabaseMetadata.PREFIX03)) {
				prefixList03.setSummary(cursor.getString(1));
			} else if (type.equals(SMSSecurityDatabaseMetadata.SERVICE_STATE)) {
				String state = cursor.getString(1);
				if (state.equals("1")) {
					startServiceCheckBox.setChecked(true);
				} else if (state.equals("0")) {
					startServiceCheckBox.setChecked(false);
				}
			}
			Log.d(TAG, cursor.getString(1));
		}

		Resources r = getResources();
		int length = bootTimeList.getEntries().length;
		String[] array = new String[length];
		int i = 0;
		for (CharSequence c : bootTimeList.getEntries()) {
			array[i] = new String(c.toString() + r.getString(R.string.Hour));
			i++;
		}
		bootTimeList.setEntries(array);

		length = callTimeList.getEntries().length;
		i = 0;
		for (CharSequence c : callTimeList.getEntries()) {
			array[i] = new String(c.toString() + r.getString(R.string.Minute));
			i++;
		}
		callTimeList.setEntries(array);

		length = onceCallTimeList.getEntries().length;
		i = 0;
		for (CharSequence c : onceCallTimeList.getEntries()) {
			array[i] = new String(c.toString() + r.getString(R.string.Minute));
			i++;
		}
		onceCallTimeList.setEntries(array);
		cursor.close();
		db.close();
		String IMEI = null;
		if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
			//IMEI = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceIdGemini(0);
			IMEI = TelephonyManagerEx.getDefault().getDeviceId(PhoneConstants.GEMINI_SIM_1);;
		} else {
			IMEI = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
		}
		IMEI_NumberP.setSummary(IMEI);
        /*String SN = TelephonyManager.getDefault().getSN();*/
		//zhangle add start 
		PhoneProxy proxyPhone = (PhoneProxy)(MTKPhoneFactory.getDefaultPhone());
		String SN = proxyPhone.getSN();
		//zhangle add end
	    SN = SN.length()>12?SN.substring(0,12):SN;
		mainbroadNumberP.setSummary(SN);
		softwareVersionP.setSummary(Build.DISPLAY);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// TODO Auto-generated method stub
		String key = preference.getKey();
		if (startServiceCheckBox.getKey().equals(key)) {
			final Object value = newValue;
			AlertDialog.Builder builder = new Builder(this);
			builder.setMessage(R.string.ChangeServiceDialog_Message);
			builder.setTitle(R.string.ChangeServiceDialog_Title);
			builder.setPositiveButton(R.string.ChangeServiceDialog_Ok,
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							if (((Boolean) value) == false) {
								ContentValues values = new ContentValues();
								values.put(SMSSecurityDatabaseMetadata.VALUE, "0");
								SQLiteDatabase db = dbh.getWritableDatabase();
								db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
										SMSSecurityDatabaseMetadata.TYPE + "=\""
												+ SMSSecurityDatabaseMetadata.SERVICE_STATE+"\"", null);
								db.close();
								startServiceCheckBox.setChecked(false);
								sendBroadcast(new Intent(STOP_ACTION));
								Log.d(TAG, "send stopService");
							} else {
								ContentValues values = new ContentValues();
								values.put(SMSSecurityDatabaseMetadata.VALUE, "1");
								SQLiteDatabase db = dbh.getWritableDatabase();
								db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
										SMSSecurityDatabaseMetadata.TYPE + "=\""
												+ SMSSecurityDatabaseMetadata.SERVICE_STATE+"\"", null);
								db.close();
								startServiceCheckBox.setChecked(true);
								sendBroadcast(new Intent(START_ACTION));
								Log.d(TAG, "send StartService");
							}
						}
					});
			builder.setNegativeButton(R.string.ChangeServiceDialog_Cancel,
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							if (((Boolean) value) == false) {
								startServiceCheckBox.setChecked(true);
							} else {
								startServiceCheckBox.setChecked(false);
							}
						}
					});
			builder.create().show();
		} else if (serviceNumberEditor.getKey().equals(key)) {
			if (serviceNumberEditor.getSummary().equals((String) newValue)) {
				return false;
			}
			
			boolean isNumber = true;
			for (char c : ((String) newValue).toCharArray()) {
				if (c > '9' || c < '0') {
					isNumber = false;
				}
			}
			if (isNumber) {
				if (!((String) newValue).equals("")) {
					SQLiteDatabase db = dbh.getReadableDatabase();
					db.execSQL("update smssecuritydatabase set value = \""+(String)newValue+"\" where type = \"SERVICE_NUMBER\";");
					db.close();
					Log.d(TAG, "update ServiceNumber"+ newValue.toString());
					serviceNumberEditor.setSummary((String) newValue);
				}
				return true;
			} else {
				return false;
			}
		} else if (bootTimeList.getKey().equals(key)) {
			if (bootTimeList.getSummary().equals((String) newValue)) {
				return false;
			}
		} else if (callTimeList.getKey().equals(key)) {
			if (callTimeList.getSummary().equals((String) newValue)) {
				return false;
			}
		} else if (onceCallTimeList.getKey().equals(key)) {
			if (onceCallTimeList.getSummary().equals((String) newValue)) {
				return false;
			}
		} else if (prefixList01.getKey().equals(key)) {
			if (prefixList01.getSummary().equals((String) newValue)) {
				return false;
			}
			String value = (String) newValue;
			if (((String) newValue).equals("ZTE")) {
				value = value +"\40\40";
			}
			ContentValues values = new ContentValues();
			values.put(SMSSecurityDatabaseMetadata.VALUE, value);
			SQLiteDatabase db = dbh.getWritableDatabase();
			db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
					SMSSecurityDatabaseMetadata.TYPE + "=\""
							+ SMSSecurityDatabaseMetadata.PREFIX01+"\"", null);
			db.close();
			prefixList01.setSummary(value);
		} else if (prefixList02.getKey().equals(key)) {
			if (prefixList02.getSummary().equals((String) newValue)) {
				return false;
			}
			ContentValues values = new ContentValues();
			values.put(SMSSecurityDatabaseMetadata.VALUE, (String)newValue);
			SQLiteDatabase db = dbh.getWritableDatabase();
			db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
					SMSSecurityDatabaseMetadata.TYPE + "=\""
							+ SMSSecurityDatabaseMetadata.PREFIX02+"\"", null);
			db.close();
			prefixList02.setSummary((String)newValue);
		} else if (prefixList03.getKey().equals(key)) {
			if (prefixList03.getSummary().equals((String) newValue)) {
				return false;
			}
			ContentValues values = new ContentValues();
			values.put(SMSSecurityDatabaseMetadata.VALUE, (String)newValue);
			SQLiteDatabase db = dbh.getWritableDatabase();
			db.update(SMSSecurityDatabaseMetadata.TABLE_NAME, values,
					SMSSecurityDatabaseMetadata.TYPE + "=\""
							+ SMSSecurityDatabaseMetadata.PREFIX03+"\"", null);
			db.close();
			prefixList03.setSummary((String)newValue);
		} else {
			return false;
		}
		return false;
	}

	private String timeFormat(String time, String unit) {
		StringBuffer buf = new StringBuffer();
		long timeTemp = Long.parseLong(time);
		if ("ms" == unit) {
			timeTemp /= 1000;
		}
		buf.append(timeTemp / 3600);
		buf.append(":");
		long timeM = timeTemp / 60;
		buf.append(timeM % 60 < 10 ? "0" + timeM % 60 : timeM % 60);
		buf.append(":");
		buf.append(timeTemp % 60 < 10 ? "0" + timeTemp % 60 : timeTemp % 60);

		return buf.toString();
	}

}
