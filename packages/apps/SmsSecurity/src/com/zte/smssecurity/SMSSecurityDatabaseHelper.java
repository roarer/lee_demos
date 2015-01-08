package com.zte.smssecurity;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class SMSSecurityDatabaseHelper extends SQLiteOpenHelper {

	public SMSSecurityDatabaseHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.execSQL("create table " + SMSSecurityDatabaseMetadata.TABLE_NAME
				+ "(" + SMSSecurityDatabaseMetadata._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ SMSSecurityDatabaseMetadata.TYPE + " varchar(20), "
				+ SMSSecurityDatabaseMetadata.VALUE + " varchar(20));");

		ContentValues values = new ContentValues();
		values.put(SMSSecurityDatabaseMetadata.TYPE,
				SMSSecurityDatabaseMetadata.SERVICE_STATE);
		values.put(SMSSecurityDatabaseMetadata.VALUE, "1");
		db.insert(SMSSecurityDatabaseMetadata.TABLE_NAME, null, values);
		values.clear();

		values.put(SMSSecurityDatabaseMetadata.TYPE,
				SMSSecurityDatabaseMetadata.SERVICE_NUMBER);
		values.put(SMSSecurityDatabaseMetadata.VALUE, "+639999929829"/* "13631661640" */); //service number
		db.insert(SMSSecurityDatabaseMetadata.TABLE_NAME, null, values);
		values.clear();
		
		values.put(SMSSecurityDatabaseMetadata.TYPE,
				SMSSecurityDatabaseMetadata.BOOTTIME);
		//values.put(SMSSecurityDatabaseMetadata.VALUE, "3600000");//ms    first time
		 //values.put(SMSSecurityDatabaseMetadata.VALUE, "180000");//ms    first time
		values.put(SMSSecurityDatabaseMetadata.VALUE,"14400000");//ms   14400000 4h=4h*60m*60s*1000ms
		db.insert(SMSSecurityDatabaseMetadata.TABLE_NAME, null, values);
		values.clear();
		
		values.put(SMSSecurityDatabaseMetadata.TYPE,
				SMSSecurityDatabaseMetadata.LONGTIMEBOOT);
		values.put(SMSSecurityDatabaseMetadata.VALUE, "2");        //heli 0 -> 2   boot times not active
		db.insert(SMSSecurityDatabaseMetadata.TABLE_NAME, null, values);
		values.clear();
		
		 //heli add
		values.put(SMSSecurityDatabaseMetadata.TYPE,
				SMSSecurityDatabaseMetadata.SENDSMSNOW);
		values.put(SMSSecurityDatabaseMetadata.VALUE, "0");       
		db.insert(SMSSecurityDatabaseMetadata.TABLE_NAME, null, values);
		values.clear();  
		
		values.put(SMSSecurityDatabaseMetadata.TYPE,
				SMSSecurityDatabaseMetadata.SHOWREGISTERED);
		values.put(SMSSecurityDatabaseMetadata.VALUE, "0");       
		db.insert(SMSSecurityDatabaseMetadata.TABLE_NAME, null, values);
		values.clear();  					
		// add end
			
		values.put(SMSSecurityDatabaseMetadata.TYPE,
				SMSSecurityDatabaseMetadata.CALLTIME);
		values.put(SMSSecurityDatabaseMetadata.VALUE, "300000");//s   heli set max to not active
		db.insert(SMSSecurityDatabaseMetadata.TABLE_NAME, null, values);
		values.clear();
		
		values.put(SMSSecurityDatabaseMetadata.TYPE,
				SMSSecurityDatabaseMetadata.ONCECALLTIME);
		values.put(SMSSecurityDatabaseMetadata.VALUE, "300000");//s   heli set max to not active
		db.insert(SMSSecurityDatabaseMetadata.TABLE_NAME, null, values);
		values.clear();
		
		values.put(SMSSecurityDatabaseMetadata.TYPE,
				SMSSecurityDatabaseMetadata.USER_CALLTIME);
		values.put(SMSSecurityDatabaseMetadata.VALUE, "0");//s
		db.insert(SMSSecurityDatabaseMetadata.TABLE_NAME, null, values);
		values.clear();

		values.put(SMSSecurityDatabaseMetadata.TYPE,
				SMSSecurityDatabaseMetadata.PREFIX01);
		values.put(SMSSecurityDatabaseMetadata.VALUE, "AUX"+"\40"+"\40");
		db.insert(SMSSecurityDatabaseMetadata.TABLE_NAME, null, values);
		values.clear();

		values.put(SMSSecurityDatabaseMetadata.TYPE,
				SMSSecurityDatabaseMetadata.PREFIX02);
		values.put(SMSSecurityDatabaseMetadata.VALUE, "01");
		db.insert(SMSSecurityDatabaseMetadata.TABLE_NAME, null, values);
		values.clear();

		values.put(SMSSecurityDatabaseMetadata.TYPE,
				SMSSecurityDatabaseMetadata.PREFIX03);
		values.put(SMSSecurityDatabaseMetadata.VALUE, "05");
		db.insert(SMSSecurityDatabaseMetadata.TABLE_NAME, null, values);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

}
