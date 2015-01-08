package com.eastaeon.floatapp;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;

public class LauncherService extends Service implements OnClickListener
{
	private static final String TAG = "LauncherService";
	
	//悬浮窗基本属性
    private Display mDisplay;
    private Matrix mDisplayMatrix;
	private LayoutInflater inflater;
	private ImageButton closeButton;
	private ImageButton settingButton;
	private TextView titleButton;		
	RelativeLayout mMainLayout;
	//定义浮动窗口布局
    RelativeLayout mFloatLayout;
    WindowManager.LayoutParams wmParams;
    //创建浮动窗口设置布局参数的对象
	WindowManager mWindowManager;
	private int X, Y;
	
	//启动界面基本属性
	private ImageButton calculatorButton;
	private ImageButton videoButton;
	private ImageButton soundRecorderButton;	
	private ImageButton musicButton;	
	
	public static final String OPEN_CALCULATOR = "eastaeon.intent.calculator.OPEN_CALCULATOR";
	public static final String OPEN_VIDEOPLAYER = "eastaeon.intent.videoplayer.OPEN_VIDEO";
	public static final String OPEN_SOUNDRECORDER = "eastaeon.intent.soundrecorder.OPEN_SOUND_RECORDER";
	public static final String OPEN_MUSIC ="eastaeon.intent.music.OPEN_MUSIC";
	
	HallLockStateReceiver hallLockStateReceiver;
	
	@Override
	public void onCreate() 
	{
		// TODO Auto-generated method stub
		super.onCreate();
		Log.i(TAG, "oncreat");
		//初始化悬浮窗
		createFloatView();
		//初始化启动界面
		init();	

		hallLockStateReceiver = new HallLockStateReceiver(this);
		registerReceiver(hallLockStateReceiver, new IntentFilter("com.eastaeon.action.HALLCLOSE"));		
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		// TODO Auto-generated method stub
		return null;
	}

	private void createFloatView()
	{
		wmParams = new WindowManager.LayoutParams();
		//获取的是WindowManagerImpl.CompatModeWrapper
		mWindowManager = (WindowManager)getApplication().getSystemService(getApplication().WINDOW_SERVICE);
		Log.i(TAG, "mWindowManager--->" + mWindowManager);
		//设置window type
		wmParams.type = LayoutParams.TYPE_SYSTEM_ERROR; 
		//设置图片格式，效果为背景透明
        wmParams.format = PixelFormat.RGBA_8888; 
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE|LayoutParams.FLAG_NOT_TOUCH_MODAL;      
        //调整悬浮窗显示的停靠位置为左侧置顶
        wmParams.gravity = Gravity.CENTER;       
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        wmParams.x = 0;
        wmParams.y = 0;
		
		mDisplay = mWindowManager.getDefaultDisplay();
		int screenWidth = mDisplay.getWidth();    
        int screenHeight = mDisplay.getHeight();   
		Log.d("panhongyu","screenWidth = "+screenWidth+", screenHeight = "+screenHeight);
		
        //设置悬浮窗口长宽数据  
        wmParams.width = Math.min(screenWidth,screenHeight)/2;//WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = Math.min(screenWidth,screenHeight)/2;//WindowManager.LayoutParams.WRAP_CONTENT;

		 /*// 设置悬浮窗口长宽数据
        wmParams.width = 200;
        wmParams.height = 80;*/
   
        LayoutInflater inflater = LayoutInflater.from(getApplication());


        //浮动窗口按钮
		mMainLayout = (RelativeLayout) inflater.inflate(R.layout.launcher_float_screen, null); 
		titleButton = (TextView) mMainLayout.findViewById(R.id.title_view);
		closeButton = (ImageButton) mMainLayout.findViewById(R.id.close_button);
		closeButton.setOnClickListener(this);
        settingButton = (ImageButton) mMainLayout.findViewById(R.id.setting_button);
		settingButton.setOnClickListener(this);
        //添加mFloatLayout
        mWindowManager.addView(mMainLayout, wmParams);
		
        mMainLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
				View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
				.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        //设置监听浮动窗口的触摸移动
        mMainLayout.setOnTouchListener(new OnTouchListener() 
        {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) 
			{				
				// TODO Auto-generated method stub
				
				Log.d("panhongyu","event.getAction() = "+event.getAction());
				switch (event.getAction() & MotionEvent.ACTION_MASK) {
					
					case MotionEvent.ACTION_DOWN: 
						
						X = (int)event.getRawX();
						Y = (int)event.getRawY(); 
						Log.d("panhongyu","MotionEvent.ACTION_DOWN X = "+X+", Y = "+Y);
						break;
					case MotionEvent.ACTION_UP:  
						Log.d("panhongyu","MotionEvent.ACTION_UP ");
						break;  
					case MotionEvent.ACTION_POINTER_DOWN:  
						break;  
					case MotionEvent.ACTION_POINTER_UP:  
						break;  
					case MotionEvent.ACTION_MOVE:   
						Log.d("panhongyu","MotionEvent.ACTION_MOVE wmParams.x = " +wmParams.x + ", wmParams.y = "+wmParams.y);
						wmParams.x +=  (int)event.getRawX() - X;
						wmParams.y +=  (int)event.getRawY() - Y;  
						Log.d("panhongyu","MotionEvent.ACTION_MOVE wmParams.x = " +wmParams.x + ", wmParams.y = "+wmParams.y);
						X = (int)event.getRawX();
						Y = (int)event.getRawY();			
						Log.d("panhongyu","MotionEvent.ACTION_MOVE X = "+X+", Y = "+Y);						
						mWindowManager.updateViewLayout(mMainLayout, wmParams);
						break;  
				}  						
				
				return false;  //此处必须返回false，否则OnClickListener获取不到监听
			}
		});			
	}

	class HallLockStateReceiver extends BroadcastReceiver{
		Service mService;
		public HallLockStateReceiver(Service service){
			mService = service;
		}
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if("com.eastaeon.action.HALLCLOSE".equals(intent.getAction())){
				sendBroadcast(new Intent("eastaeon.intent.action.float.OPEN_FLOAT"));
				mService.stopSelf();
			}
		}
			
	}
	
	@Override
	public void onDestroy() 
	{
		// TODO Auto-generated method stub
		super.onDestroy();
		
		this.unregisterReceiver(hallLockStateReceiver);
		
		if(mMainLayout != null)
		{
			//移除悬浮窗口
			mWindowManager.removeView(mMainLayout);
		}
		
	}
	
	
	@Override
	public void onClick(View v) {
		Intent mIntent = new Intent();
		if(v.equals(closeButton)){
			startService(new Intent(this, FloatService.class));
			stopSelf();
		} else if(v.equals(calculatorButton)) {
			mIntent.setAction(OPEN_CALCULATOR);
			sendBroadcast(mIntent);
			stopSelf();
		} else if(v.equals(videoButton)) {
			mIntent.setAction(OPEN_VIDEOPLAYER);
			sendBroadcast(mIntent);
			stopSelf();		
		} else if(v.equals(soundRecorderButton)) {
			mIntent.setAction(OPEN_SOUNDRECORDER);
			sendBroadcast(mIntent);
			stopSelf();			
		} else if(v.equals(musicButton)) {
			mIntent.setAction(OPEN_MUSIC);
			sendBroadcast(mIntent);
			stopSelf();			
		} else if(v.equals(settingButton)) {
			mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
			mIntent.setClass(this, SettingActivity.class);
			startActivity(mIntent);
			startService(new Intent(this, FloatService.class));
			stopSelf();	
		}
		
	}
	
		/**
     * @return the current display rotation in degrees
     */
    private float getDegreesForRotation(int value) {
        switch (value) {
        case Surface.ROTATION_90:
            return 360f - 90f;
        case Surface.ROTATION_180:
            return 360f - 180f;
        case Surface.ROTATION_270:
            return 360f - 270f;
        }
        return 0f;
    }
	
	//启动界面相关函数
	public void init() {
		mFloatLayout = (RelativeLayout) mMainLayout.findViewById(R.id.float_fayout);  
		calculatorButton = (ImageButton) mFloatLayout.findViewById(R.id.calculator_button);
		videoButton = (ImageButton) mFloatLayout.findViewById(R.id.videoplayer_button);
		soundRecorderButton = (ImageButton) mFloatLayout.findViewById(R.id.soundrecorder_button);
		musicButton = (ImageButton) mFloatLayout.findViewById(R.id.music_button);
		calculatorButton.setOnClickListener(this);
		videoButton.setOnClickListener(this);
		soundRecorderButton.setOnClickListener(this);
		musicButton.setOnClickListener(this);
	}	
}