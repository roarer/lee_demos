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
import java.util.Timer;
import java.util.TimerTask;
import android.os.Message;

public class FloatService extends Service implements OnClickListener
{
	private static final String TAG = "FloatService";
	
	//悬浮窗基本属性
    private Display mDisplay;
    private DisplayMetrics mDisplayMetrics;
    private Matrix mDisplayMatrix;
	private LayoutInflater inflater;
	private ImageButton launchButton;
		
	RelativeLayout mMainLayout;

    WindowManager.LayoutParams wmParams;
    //创建浮动窗口设置布局参数的对象
	WindowManager mWindowManager;
	private int X, Y;
	private int xOld, yOld;	
	private boolean isMove = false;
	
	
	private Timer mTimer;
	private TimerTask mTimerTask;
	private Handler mHandler;
	private final int HIDE_BUTTON = 0x100;
	
	
	public static final String OPEN_CALCULATOR = "eastaeon.intent.calculator.OPEN_CALCULATOR";
	public static final String OPEN_VIDEOPLAYER = "eastaeon.intent.videoplayer.OPEN_VIDEO";
	public static final String OPEN_SOUNDRECORDER = "eastaeon.intent.soundrecorder.OPEN_SOUND_RECORDER";
	public static final String OPEN_MUSIC ="eastaeon.intent.music.OPEN_MUSIC";
	
	@Override
	public void onCreate() 
	{
		// TODO Auto-generated method stub
		super.onCreate();
		Log.i(TAG, "oncreat");
		//初始化悬浮窗
		createFloatView();	
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		// TODO Auto-generated method stub
		return null;
	}

	class MyTimerTask extends TimerTask{
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.d("TAG", "run...");
			Message msg = mHandler.obtainMessage(HIDE_BUTTON);
			msg.sendToTarget();
		}
	}
	
	public void StartHideButtonTimer(){
     if (mTimer != null){
      if (mTimerTask != null){
       mTimerTask.cancel();  //将原任务从队列中移除
      }
      
     
      mTimerTask = new MyTimerTask();  // 新建一个任务      
      mTimer.schedule(mTimerTask, 3*1000);
     }
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
        wmParams.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;       
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        wmParams.x = 0;
        wmParams.y = 0;

        //设置悬浮窗口长宽数据  
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
   
        LayoutInflater inflater = LayoutInflater.from(getApplication());

        
		mMainLayout = (RelativeLayout) inflater.inflate(R.layout.float_small_screen, null); 
		//浮动窗口按钮
		launchButton = (ImageButton) mMainLayout.findViewById(R.id.launch_button);  
		launchButton.setOnClickListener(this);
        
        //添加mFloatLayout
        mWindowManager.addView(mMainLayout, wmParams);
		
        mMainLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
				View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
				.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
		mDisplay = mWindowManager.getDefaultDisplay();
        mDisplayMetrics = new DisplayMetrics();
        mDisplay.getRealMetrics(mDisplayMetrics);

        //设置监听浮动窗口的触摸移动
        launchButton.setOnTouchListener(new OnTouchListener() 
        {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) 
			{				
				// TODO Auto-generated method stub
				
				Log.d("panhongyu","event.getAction() = "+event.getAction());
				switch (event.getAction() & MotionEvent.ACTION_MASK) {
					
					case MotionEvent.ACTION_DOWN: 
						launchButton.setImageResource(R.drawable.float_icon);
						X = (int)event.getRawX();
						Y = (int)event.getRawY(); 
						Log.d("panhongyu","MotionEvent.ACTION_DOWN X = "+X+", Y = "+Y);
						break;
					case MotionEvent.ACTION_UP:  
						Log.d("panhongyu","MotionEvent.ACTION_UP ");
						if(wmParams.x<540) {
							wmParams.x = 0;
						} else if(wmParams.x>=540) {
							wmParams.x = 1080;
						}
						mWindowManager.updateViewLayout(mMainLayout, wmParams);
						
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
						xOld = X;
						yOld = Y;
						X = (int)event.getRawX();
						Y = (int)event.getRawY();
						if(xOld == X && yOld == Y) {
							isMove = false;
						} else {
							isMove = true;
						} 
						Log.d("panhongyu","MotionEventisMove = "+isMove);	
						Log.d("panhongyu","MotionEvent.ACTION_MOVE X = "+X+", Y = "+Y);						
						mWindowManager.updateViewLayout(mMainLayout, wmParams);
						StartHideButtonTimer();
						break;  
				}  						
				if(isMove) {
					return true;  //此处返回true，OnClickListener不能获取监听
				} else {
					return false;  //此处返回false，OnClickListener能够获取监听
				}
				
			}
		});			
		
		mTimer = new Timer(true);
		mHandler = new Handler(){
			public void handleMessage(Message message){
				Log.d("panhongyu", "message what = " + message.what);
				if (message.what == HIDE_BUTTON) {
					launchButton.setImageResource(R.drawable.float_icon_no_focus);
				}

			}
		};	
		StartHideButtonTimer();		
	}

	
	@Override
	public void onDestroy() 
	{
		// TODO Auto-generated method stub
		super.onDestroy();
		if(mMainLayout != null)
		{
			//移除悬浮窗口
			mWindowManager.removeView(mMainLayout);
		}
		
	}
	
	
	@Override
	public void onClick(View v) {
		
		if(v.equals(launchButton)){
			startService(new Intent(this, LauncherService.class)); 
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
	
}