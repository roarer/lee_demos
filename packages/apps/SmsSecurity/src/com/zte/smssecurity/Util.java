package com.zte.smssecurity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.util.Log;

public class Util {
	private static final String TAG = "SMSSecurityUtil";
	private static final String NVFILE = "data/app/.smssecurity";

	public static boolean getServiceState() {
		try {
			File keepFile = new File(NVFILE);
			if (!keepFile.exists()) {
				keepFile.createNewFile();
				BufferedWriter buffWrite = new BufferedWriter(new FileWriter(
						keepFile));
				buffWrite.write("1");
				buffWrite.close();
				Log.d(TAG, "no smssecurity file, create and init it");
				return true;
			}
			BufferedReader buffRead = new BufferedReader(new FileReader(
					keepFile));
			String state = buffRead.readLine();
			buffRead.close();
			if (state == null) {
				BufferedWriter buffWrite = new BufferedWriter(new FileWriter(
						keepFile));
				buffWrite.write("1");
				buffWrite.close();
				Log.d(TAG, "smssecurity file is empty, init it");
				return true;
			}
			if (state.contains("0")) {
				return false;
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return true;
		}
	}
	
	public static void writeOpenFlag(){
		writeFlag("1");
	}
	
	public static void writeCloseFlag(){
		writeFlag("0");
	}
	
	public static void writeFlag(String flag){
		File keepFile = new File(NVFILE);
		BufferedWriter buffWrite = null;
 	    try{
 	       buffWrite = new BufferedWriter(new FileWriter(keepFile));
           buffWrite.write(flag);
           buffWrite.close();
           Log.d(TAG,"write .smssecurity close smssecurity");
 	    } catch (FileNotFoundException e) {
	       	 e.printStackTrace();
	    } catch (IOException e) {
		     e.printStackTrace();
	    }
	}
}
