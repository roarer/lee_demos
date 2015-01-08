package com.eastaeon.floatapp.calculator;

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
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.PagerAdapter;
import android.os.Parcelable;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.view.ViewGroup;
import android.widget.ImageButton;
import com.eastaeon.floatapp.R;
import android.os.Vibrator;
import android.content.Context;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.RelativeLayout;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

public class CalculatorSmallScreen extends Service implements OnClickListener,Logic.Listener
{
	private static final String TAG = "CalculatorSmallScreen";
	
	//悬浮窗基本属性
    private Display mDisplay;
    private Matrix mDisplayMatrix;
	private LayoutInflater inflater;
	private Button listButton;
	private Button minimizeButton;
	private TextView titleView;		
	FrameLayout mMainLayout;
	//定义浮动窗口布局
	RelativeLayout mTitleLyout;
    LinearLayout mFloatLayout;
    WindowManager.LayoutParams wmParams;
    //创建浮动窗口设置布局参数的对象
	WindowManager mWindowManager;
	private int X, Y;
	
	//计算器界面基本属性
    private View mClearButton;
    private View mBackspaceButton;	
	private Logic mLogic;
	private CalculatorDisplay mCalculatorDisplay;
    private Persist mPersist;
    private History mHistory;
	private ViewPager mPager;
	EventListener mListener = new EventListener();
	private static Context sContext;
	HallLockStateReceiver hallLockStateReceiver;
	@Override
	public void onCreate() 
	{
		// TODO Auto-generated method stub
		super.onCreate();
		
		sContext = getApplication();
		
		Log.d(TAG, "oncreat");
		//初始化悬浮窗
		createFloatView();
		//初始化计算器界面
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
		
		// 设置悬浮窗口长宽数据
        wmParams.width = (Math.min(screenWidth,screenHeight)*9/10)*9/16;
        wmParams.height = Math.min(screenWidth,screenHeight)*9/10;
   
        inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
		mMainLayout = (FrameLayout) inflater.inflate(R.layout.calculator_float_screen, null); 
		mTitleLyout = (RelativeLayout)mMainLayout.findViewById(R.id.title_layout);
		titleView = (TextView) mTitleLyout.findViewById(R.id.title);
		minimizeButton = (Button) mTitleLyout.findViewById(R.id.minimize_button);
		minimizeButton.setOnClickListener(this);
		listButton = (Button) mTitleLyout.findViewById(R.id.list_button);
		listButton.setOnClickListener(this);        		
        //添加mFloatLayout
        mWindowManager.addView(mMainLayout, wmParams);
        //浮动窗口按钮

        mMainLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
				View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
				.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        //设置监听浮动窗口的触摸移动
        mTitleLyout.setOnTouchListener(new OnTouchListener() 
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
				
				return true;  //此处必须返回false，否则OnClickListener获取不到监听
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
		
		//退出时，保存计算器数据
		mLogic.updateHistory();
        mPersist.setDeleteMode(mLogic.getDeleteMode());
        mPersist.save();
		
		if(mMainLayout != null)
		{
			//移除悬浮窗口
			mWindowManager.removeView(mMainLayout);
		}
		
	}	
	
	@Override
	public void onClick(View v) {
		if(v.equals(minimizeButton)){
			sendBroadcast(new Intent("eastaeon.intent.action.float.OPEN_FLOAT"));
			stopSelf();
		} else if(v.equals(listButton)) {
			sendBroadcast(new Intent("eastaeon.intent.action.launch.OPEN_LAUNCH"));
			stopSelf();
		}
		
	}
	
	public static void vibrate() {
        Vibrator vibrator = (Vibrator) sContext.getSystemService(sContext.VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(new long[] { 100, 100 }, -1);
        } else {
            //log("Device not have vibrator");
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
	
	//计算器相关函数
	public void init() {
		mFloatLayout = (LinearLayout) mMainLayout.findViewById(R.id.float_fayout);  
		mPager = (ViewPager) mFloatLayout.findViewById(R.id.panelswitch);

		if (mPager != null) {
            mPager.setAdapter(new PageAdapter(mPager));
        } else {
            // Single page UI
            final TypedArray buttons = getResources().obtainTypedArray(R.array.buttons);
            for (int i = 0; i < buttons.length(); i++) {
                setOnClickListener(null, buttons.getResourceId(i, 0));
            }
            buttons.recycle();
        }
		
		if (mClearButton == null) {
            mClearButton = mFloatLayout.findViewById(R.id.clear);
            mClearButton.setOnClickListener(mListener);
            mClearButton.setOnLongClickListener(mListener);
        }
        if (mBackspaceButton == null) {
            mBackspaceButton = mFloatLayout.findViewById(R.id.del);
            mBackspaceButton.setOnClickListener(mListener);
            mBackspaceButton.setOnLongClickListener(mListener);
        }
		
		mPersist = new Persist(this);
        mPersist.load();

        mHistory = mPersist.history;

        mCalculatorDisplay = (CalculatorDisplay) mFloatLayout.findViewById(R.id.display);

        mLogic = new Logic(this, mHistory, mCalculatorDisplay);
        mLogic.setListener(this);
		
        mLogic.setDeleteMode(mPersist.getDeleteMode());
        mLogic.setLineLength(mCalculatorDisplay.getMaxDigits());		
		
        HistoryAdapter historyAdapter = new HistoryAdapter(this, mHistory, mLogic);
        mHistory.setObserver(historyAdapter);
       
        mListener.setHandler(mLogic, mPager);
        mCalculatorDisplay.setOnKeyListener(mListener);
		
		mLogic.resumeWithHistory();
        updateDeleteMode();		
	}
	
    private void updateDeleteMode() {
        if (mLogic.getDeleteMode() == Logic.DELETE_MODE_BACKSPACE) {
            mClearButton.setVisibility(View.GONE);
            mBackspaceButton.setVisibility(View.VISIBLE);
        } else {
            mClearButton.setVisibility(View.VISIBLE);
            mBackspaceButton.setVisibility(View.GONE);
        }
    }
	
    @Override
    public void onDeleteModeChange() {
        updateDeleteMode();
    }
	
	void setOnClickListener(View root, int id) {
        final View target = root != null ? root.findViewById(id) : mFloatLayout.findViewById(id);
		Log.d("panhongyu","target = " + target);
		Log.d("panhongyu","mListener = " + mListener);
		Log.d("panhongyu","root = " + root);
		Log.d("panhongyu","mFloatLayout = " + mFloatLayout);
		Log.d("panhongyu","id = " + id);
        target.setOnClickListener(mListener);
    }
	
	class PageAdapter extends PagerAdapter {
        private View mSimplePage;
        private View mAdvancedPage;

        public PageAdapter(ViewPager parent) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final View simplePage = inflater.inflate(R.layout.simple_pad, parent, false);
            final View advancedPage = inflater.inflate(R.layout.advanced_pad, parent, false);
            mSimplePage = simplePage;
            mAdvancedPage = advancedPage;

            final Resources res = getResources();
            final TypedArray simpleButtons = res.obtainTypedArray(R.array.simple_buttons);
            for (int i = 0; i < simpleButtons.length(); i++) {
                setOnClickListener(simplePage, simpleButtons.getResourceId(i, 0));
            }
            simpleButtons.recycle();

            final TypedArray advancedButtons = res.obtainTypedArray(R.array.advanced_buttons);
            for (int i = 0; i < advancedButtons.length(); i++) {
                setOnClickListener(advancedPage, advancedButtons.getResourceId(i, 0));
            }
            advancedButtons.recycle();

            final View clearButton = simplePage.findViewById(R.id.clear);
            if (clearButton != null) {
                mClearButton = clearButton;
            }

            final View backspaceButton = simplePage.findViewById(R.id.del);
            if (backspaceButton != null) {
                mBackspaceButton = backspaceButton;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public void startUpdate(View container) {
        }

        @Override
        public Object instantiateItem(View container, int position) {
            final View page = position == 0 ? mSimplePage : mAdvancedPage;
            ((ViewGroup) container).addView(page);
            return page;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            ((ViewGroup) container).removeView((View) object);
        }

        @Override
        public void finishUpdate(View container) {
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }
    }	
}