package com.eastaeon.floatapp.music.lrc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import android.util.Xml.Encoding;
import android.widget.SlidingDrawer;
import org.apache.http.util.EncodingUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Collections;
import android.util.Log;
import java.io.BufferedInputStream;
import info.monitorenter.cpdetector.io.*;
import java.nio.charset.Charset;

public class LrcProcess{
	private List<LrcContent> lrcList;
	private LrcContent mLrcContent;
	private final String M_TAG = "huanyu";
	public LrcProcess(){
		mLrcContent = new LrcContent();
		lrcList = new ArrayList<LrcContent>();
	}
		
	private String getEncoding(String path) {
		String encoding = null;	
		CodepageDetectorProxy detector = CodepageDetectorProxy.getInstance();  
        detector.add(JChardetFacade.getInstance());  
          
        Charset charset = null;  
          
        File f = new File(path);    
          
        try {     
            charset = detector.detectCodepage(f.toURL());    		
			if(charset!=null){     
				Log.d("panhongyu",f.getName()+"编码是："+charset.name());   
				encoding = charset.name();
			} else {   
				Log.d("panhongyu",f.getName()+"未知");    
			}  
		} catch (Exception e) {  
            e.printStackTrace();  
			return encoding;
        } 
		return encoding;
	}
	
	public String readLRC(String path){
		String  ret = "";
		String encoding = null;	
		
		if(!path.contains(".mp3")) {
			return "none";
		}
		
		encoding = getEncoding(path.replace(".mp3", ".lrc"));

		if(encoding == null) {
			return "none";
		}
		
		File f = new File(path.replace(".mp3", ".lrc"));
		FileInputStream fis;
		InputStreamReader isr;
		BufferedReader br;
		try{
			fis = new FileInputStream(f);
			isr = new InputStreamReader(fis,encoding);
			br = new BufferedReader(isr);
			
			String s = "",temp = "";
			while((temp = br.readLine()) != null){
				s = EncodingUtils.getString(temp.getBytes(),"utf-8");
				parserLine(s);
			}
			Collections.sort(lrcList);
			fis.close();
			isr.close();
			br.close();
			ret = "ok";
		}catch(FileNotFoundException e){
			ret += "none";
			Log.i(M_TAG,"FileNotFoundException2 = " + e.getMessage());
			e.printStackTrace();
		}catch(IOException e1){
			ret += "error";
			Log.i(M_TAG,"IOException2 = " + e1.getMessage());
			e1.printStackTrace();
		}
		Log.i(M_TAG,"readLRC ret = " + ret);
		return ret;
	}

	private void parserLine(String line) {
		//???
        if(line.startsWith("[ti:")){
            String title =line.substring(4,line.length()-1);
            Log.i(M_TAG,"title-->"+title);
        }
		//??
        else if(line.startsWith("[ar:")){
            String artist = line.substring(4, line.length()-1);
            Log.i(M_TAG,"artist-->"+artist);
        }
		//??
        else if(line.startsWith("[al:")){
            String album =line.substring(4, line.length()-1);
            Log.i(M_TAG,"album-->"+album);
        }
		//????
        else if(line.startsWith("[by:")){
            String bysomebody=line.substring(4, line.length()-1);
            Log.i(M_TAG,"by-->"+bysomebody);
        }else{
			long currentTime = 0;
			String currentContent="";
			//???????
            String reg ="\\[(\\d{1,2}:\\d{1,2}\\.\\d{1,2})\\]|\\[(\\d{1,2}:\\d{1,2})\\]";
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher=pattern.matcher(line);
			//???????
            while(matcher.find()){
				//?????
                String msg=matcher.group();
				//????????
                int start = matcher.start();
				//????????
                int end = matcher.end();
				//?????????
                int groupCount = matcher.groupCount();
                for(int index =0;index<groupCount;index++){
                    String timeStr = matcher.group(index);
                    Log.i(M_TAG,"time["+index+"]="+timeStr);
                    if(index==0){
						//???????????????????
                        currentTime=str2Long(timeStr.substring(1, timeStr.length()-1));
                    }
                }
				//?????????
                String[] content = pattern.split(line);
                //for(int index =0; index<content.length; index++){
                   // Log.i(M_TAG,"content="+content[content.length-1]);
                    //if(index==content.length-1){
                        //??????????
						if(content.length == 0)
						currentContent="......";
						else
                        currentContent = content[content.length-1];
                    //}
                //}
				mLrcContent.setLrcTime(currentTime);
				mLrcContent.setLrcStr(currentContent);
				//????
				lrcList.add(mLrcContent);
				mLrcContent = new LrcContent();
                Log.i(M_TAG,"currentTime--->"+currentTime+"   currentContent--->"+currentContent);  
            }
        }
    }
	
	private long str2Long(String timeStr){
		//xx:xx:xx
		Log.i(M_TAG,"timeStr="+timeStr);
		String[] s = timeStr.split("\\:");
		int min = Integer.parseInt(s[0]);
		int sec=0;
		int mill=0;
		if(s[1].contains(".")){
			String[] ss=s[1].split("\\.");
			sec =Integer.parseInt(ss[0]);
			mill=Integer.parseInt(ss[1]);
			Log.i(M_TAG,"s[0]="+s[0]+"s[1]"+s[1]+"ss[0]="+ss[0]+"ss[1]="+ss[1]);
		}else{
			sec=Integer.parseInt(s[1]);
			Log.i(M_TAG,"s[0]="+s[0]+"s[1]"+s[1]);
		}
		return min*60*1000+sec*1000+mill*10;
	}
	
	public List<LrcContent> getLrcList(){
		return lrcList;
	}
}