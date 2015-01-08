package com.zte.smssecurity;

public class SMSSecurityDatabaseMetadata {

	public static final String TABLE_NAME = "SMSSecurityDatabase";
	// column name
	public static final String _ID = "_ID";
	public static final String TYPE = "TYPE";
	public static final String VALUE = "VALUE";
	// column "TYPE" values
	public static final String SERVICE_STATE = "SERVICE_STATE"; // 1: open / 0:
																// close
	public static final String SERVICE_NUMBER = "SERVICE_NUMBER";
	public static final String PREFIX01 = "PREFIX01";
	public static final String PREFIX02 = "PREFIX02";
	public static final String PREFIX03 = "PREFIX03";
	public static final String BOOTTIME = "BOOTTIME";
	public static final String CALLTIME = "CALLTIME";
	public static final String ONCECALLTIME = "ONCECALLTIME";
	public static final String LONGTIMEBOOT = "LONGTIMEBOOT";
	// public static final String IMEI_NUMBER = "IMEI_NUMBER";
	// public static final String MAINBROAD_NUMBER = "MAINBROAD_NUMBER";
	// public static final String SOFTWARE_VERSION = "SOFTWARE_VERSION";
	public static final String USER_CALLTIME = "USER_CALLTIME";
	
	public static final String SENDSMSNOW = "SENDSMSNOW";
	public static final String SHOWREGISTERED = "SHOWREGISTERED";
	
}
