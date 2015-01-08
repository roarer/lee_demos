package com.eastaeon.floatapp.soundrecorder;

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
import android.widget.FrameLayout;
import android.widget.TextView;
import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.ProgressBar;
import android.os.PowerManager.WakeLock;
import android.content.SharedPreferences;
import android.content.ServiceConnection;
import android.content.ComponentName;
import com.eastaeon.floatapp.soundrecorder.ext.IQualityLevel;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import java.io.File;
import android.content.res.Resources;
import com.eastaeon.floatapp.soundrecorder.ext.ExtensionHelper;
import android.app.FragmentManager;
import com.eastaeon.floatapp.R;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

public class SoundRecorderSmallScreen extends Service implements SoundRecorderService.OnEventListener, SoundRecorderService.OnErrorListener, SoundRecorderService.OnStateChangedListener, Button.OnClickListener, SoundRecorderService.OnUpdateTimeViewListener
{
	private static final String TAG = "SoundRecorderSmallScreen";
	
	//悬浮窗基本属性
    private Display mDisplay;
    private Matrix mDisplayMatrix;
	private LayoutInflater inflater;
	private Button listButton;
	private Button minimizeButton;
	private TextView titleView;	
	RelativeLayout mTitleLyout;
	RelativeLayout mMainLayout;
	//定义浮动窗口布局
    LinearLayout mFloatLayout;
    WindowManager.LayoutParams wmParams;
    //创建浮动窗口设置布局参数的对象
	WindowManager mWindowManager;
	private int X, Y;
	
	//初始化录音界面
	
    private static final String NULL_STRING = "";
    private static final int OPTIONMENU_SELECT_FORMAT = 0;
    private static final int OPTIONMENU_SELECT_MODE = 1;
    private static final int OPTIONMENU_SELECT_EFFECT = 2;
    private static final int DIALOG_SELECT_MODE = 0;
    private static final int DIALOG_SELECT_FORMAT = 1;
    public static final int DIALOG_SELECT_EFFECT = 2;
    private static final int TWO_BUTTON_WEIGHT_SUM = 2;
    private static final int THREE_BUTTON_WEIGHT_SUM = 3;
    private static final int REQURST_FILE_LIST = 1;
    private static final int TIME_BASE = 60;
    private static final long MAX_FILE_SIZE_NULL = -1L;
    private static final int TIME_NINE_MIN = 540;
    private static final int MMS_FILE_LIMIT = 190;
    private static final long ONE_SECOND = 1000;
    private static final int DONE = 100;
    public static final int TWO_LEVELS = 2;
    public static final int THREE_LEVELS = 3;
    private static final String INTENT_ACTION_MAIN = "android.intent.action.MAIN";
    private static final String EXTRA_MAX_BYTES = android.provider.MediaStore.Audio.Media.EXTRA_MAX_BYTES;
    private static final String AUDIO_NOT_LIMIT_TYPE = "audio/*";
    private static final String DIALOG_TAG_SELECT_MODE = "SelectMode";
    private static final String DIALOG_TAG_SELECT_FORMAT = "SelectFormat";
    private static final String DIALOG_TAG_SELECT_EFFECT = "SelectEffect";
    private static final String SOUND_RECORDER_DATA = "sound_recorder_data";
    private static final String PATH = "path";
    public static final String PLAY = "play";
    public static final String RECORD = "record";
    public static final String INIT = "init";
    public static final String DOWHAT = "dowhat";
    public static final String EMPTY = "";
    public static final String ERROR_CODE = "errorCode";

    private int mSelectedFormat = -1;
    private int mSelectedMode = -1;
    private boolean[] mSelectEffectArray = new boolean[3];
    private boolean[] mSelectEffectArrayTemp = new boolean[3];

    private int mCurrentState = SoundRecorderService.STATE_IDLE;
    private String mRequestedType = AUDIO_NOT_LIMIT_TYPE;
    private String mTimerFormat = null;
    private String mFileName = "";
    private String mDoWhat = null;
    private String mDoWhatFilePath = null;
    private long mMaxFileSize = -1L;
    private boolean mRunFromLauncher = true;
    private boolean mHasFileSizeLimitation = false;
    private boolean mBackPressed = false;
    private boolean mOnSaveInstanceStateHasRun = false;
    private WakeLock mWakeLock = null;
    private boolean mIsStopService = false;
    // M: used for saving record file when SoundRecorder launch from other
    // application
    private boolean mSetResultAfterSave = true;
    // private WakeLock mWakeLock = null;
    private SharedPreferences mPrefs = null;
	
	private boolean mFileFromList = false;
	
	
    private Button mAcceptButton;
    private Button mDiscardButton;
    private ImageButton mRecordButton;
    private ImageButton mPlayButton;
    private ImageButton mStopButton;
    private ImageButton mFileListButton;
    private ImageButton mPauseRecordingButton;
    // image view at the left of mStateTextView
    private ImageView mRecordingStateImageView;
    // image view at the left of mRecordingFileNameTextView
    private ImageView mPlayingStateImageView;
    private TextView mRemainingTimeTextView; // message below the state message
    private TextView mStateTextView; // state message with LED
    private TextView mTimerTextView;
    private TextView mRecordingFileNameTextView;
    private ProgressBar mStateProgressBar;
    private LinearLayout mExitButtons;
    private VUMeter mVUMeter;
    private LinearLayout mButtonParent;
    private OnScreenHint mStorageHint;
    private ImageView mFirstLine;
    private ImageView mSecondLine;
    private FrameLayout mFrameLayout;	
	
	private SoundRecorderService mService = null;
	
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            LogUtils.i(TAG, "<onServiceConnected> Service connected");
            mService = ((SoundRecorderService.SoundRecorderBinder) arg1).getService();
            initWhenHaveService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            LogUtils.i(TAG, "<onServiceDisconnected> Service dis connected");
            mService = null;
        }
    };
	
	HallLockStateReceiver hallLockStateReceiver;
	@Override
	public void onCreate() 
	{
		// TODO Auto-generated method stub
		super.onCreate();
		Log.d(TAG, "onCreate()");
		/*			//初始化悬浮窗
			createFloatView();
			//初始化计算器界面
			init();	
		*/
		hallLockStateReceiver = new HallLockStateReceiver(this);
		registerReceiver(hallLockStateReceiver, new IntentFilter("com.eastaeon.action.HALLCLOSE"));
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) { 
		// TODO Auto-generated method stub
		super.onCreate();
		Log.d(TAG, "onStartCommand()");
		//初始化悬浮窗
		createFloatView();
		//初始化计算器界面
		init();		
		
		if ((null != mService) && (null != mFileListButton)) {
            mFileListButton.setEnabled(true);
        }
        
        Bundle bundle = intent.getExtras();
        if (null == bundle) {
            LogUtils.i(TAG, "<onActivityResult> bundle == null, return");
            return START_REDELIVER_INTENT;
        }
        mDoWhat = bundle.getString(DOWHAT);
        if (null != mDoWhat) {
            if (mDoWhat.equals(PLAY)) {
                if ((null != intent.getExtras()) && (null != intent.getExtras().getString(PATH))) {
                    mDoWhatFilePath = intent.getExtras().getString(PATH);
                    mFileFromList = true;
                }
            }
        }

		return START_REDELIVER_INTENT;
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
		wmParams.type = LayoutParams.TYPE_SYSTEM_ERROR; //LayoutParams.TYPE_SYSTEM_OVERLAY;
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
        wmParams.width = (Math.min(screenWidth,screenHeight)*9/10)*9/14;
        wmParams.height = Math.min(screenWidth,screenHeight)*9/10;
   
        inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
		mMainLayout = (RelativeLayout) inflater.inflate(R.layout.sound_recorder_float_screen, null); 
		mFloatLayout = (LinearLayout)mMainLayout.findViewById(R.id.float_fayout);
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
		Log.d(TAG, "onDestroy()");
		
		this.unregisterReceiver(hallLockStateReceiver);
		
		LogUtils.i(TAG, "<onStop> unbind service");
		unbindService(mServiceConnection);
		
		if(mMainLayout != null)
		{
			//移除悬浮窗口
			mWindowManager.removeView(mMainLayout);
		}
		
	}	
	@Override
    public void onClick(View button) {
        if (!button.isEnabled()) {
            return;
        }
        LogUtils.i(TAG, "<onClick> Activity = " + this.toString());
        switch (button.getId()) {
        case R.id.recordButton:
            LogUtils.i(TAG, "<onClick> recordButton");
            onClickRecordButton();
            break;
        case R.id.playButton:
            LogUtils.i(TAG, "<onClick> playButton");
            onClickPlayButton();
            break;
        case R.id.stopButton:
            LogUtils.i(TAG, "<onClick> stopButton");
            onClickStopButton();
            break;
        case R.id.acceptButton:
            LogUtils.i(TAG, "<onClick> acceptButton");
            onClickAcceptButton();
            break;
        case R.id.discardButton:
            LogUtils.i(TAG, "<onClick> discardButton");
            onClickDiscardButton();
            break;
        case R.id.fileListButton:
            onClickFileListButton();
            break;
        case R.id.pauseRecordingButton:
            LogUtils.i(TAG, "<onClick> pauseRecordingButton");
            onClickPauseRecordingButton();
            break;
		case R.id.minimize_button:
			sendBroadcast(new Intent("eastaeon.intent.action.float.OPEN_FLOAT"));
			stopSelf();
			break;	
		case R.id.list_button:
			sendBroadcast(new Intent("eastaeon.intent.action.launch.OPEN_LAUNCH"));
			stopSelf();		
			break;
        default:
            break;
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
	
	
	//录音机相关函数
    public void init() {
		//addOptionsMenuInflaterFactory();
		//setContentView(R.layout.main);
		RecordParamsSetting.initRecordParamsSharedPreference(SoundRecorderSmallScreen.this);
		//mFloatLayout = (LinearLayout)mMainLayout.findViewById(R.id.float_fayout);	
		if (mService == null) {
            // start service
            LogUtils.i(TAG, "<onResume> start service");
            if (null == startService(new Intent(SoundRecorderSmallScreen.this, SoundRecorderService.class))) {
                LogUtils.e(TAG, "<onResume> fail to start service");
                stopSelf();
                return;
            }
            
            // bind service
            LogUtils.i(TAG, "<onResume> bind service");
            if (!bindService(new Intent(SoundRecorderSmallScreen.this, SoundRecorderService.class),
                    mServiceConnection, BIND_AUTO_CREATE)) {
                LogUtils.e(TAG, "<onResume> fail to bind service");
                stopSelf();
                return;
            }
			
            // M: reset ui to initial state, or else the UI may be abnormal before service not bind
            resetUi();
        } else {
            // M: when switch SoundRecorder and RecordingFileList quickly, it's
            // possible that onStop was not been called,
            // but onResume is called, in this case, mService has not been
            // unbind, so mService != null
            // but we still should do some initial operation, such as play
            // recording file which select from RecordingFileList
            initWhenHaveService();
        }
	}    

    private void resetUi() {
        initResourceRefsWhenNoService();
        //setTitle(getResources().getString(R.string.app_name));
        mButtonParent.setWeightSum(TWO_BUTTON_WEIGHT_SUM);
        mRecordButton.setEnabled(true);
        mRecordButton.setFocusable(true);
        mRecordButton.setSoundEffectsEnabled(true);
        mRecordButton.requestFocus();
        mPlayButton.setVisibility(View.GONE);
        mStopButton.setEnabled(false);
        mStopButton.setFocusable(false);
        mStopButton.setVisibility(View.GONE);
        mFileListButton.setVisibility(View.VISIBLE);
        mFileListButton.setEnabled(true);
        mFileListButton.setFocusable(true);
        mPauseRecordingButton.setVisibility(View.GONE);
        mPauseRecordingButton.setSoundEffectsEnabled(false);
        mRemainingTimeTextView.setVisibility(View.INVISIBLE);
        mRecordingStateImageView.setVisibility(View.INVISIBLE);
        mStateTextView.setVisibility(View.INVISIBLE);
        mPlayingStateImageView.setVisibility(View.GONE);
        mRecordingFileNameTextView.setVisibility(View.INVISIBLE);
        mExitButtons.setVisibility(View.INVISIBLE);
        mVUMeter.setVisibility(View.VISIBLE);
        mVUMeter.mCurrentAngle = 0;
        mStateProgressBar.setVisibility(View.INVISIBLE);
        /**M: Avoid fresh the timer view if current is recording state.@{**/
        if (mCurrentState != SoundRecorderService.STATE_RECORDING) {
            setTimerTextView(true);
        }
        /**@}**/
    }	
    
    private void initWhenHaveService() {
        LogUtils.i(TAG, "<initWhenHaveService> start");
        mService.setErrorListener(SoundRecorderSmallScreen.this);
        mService.setEventListener(SoundRecorderSmallScreen.this);
        mService.setStateChangedListener(SoundRecorderSmallScreen.this);
        mService.setShowNotification(mRunFromLauncher);
        //M:Add for update time view through implements the listener defined by SoundRecorderService.
        mService.setUpdateTimeViewListener(SoundRecorderSmallScreen.this);
        mCurrentState = mService.getCurrentState();
        LogUtils.i(TAG, "<initWhenHaveService> mCurrentState = " + mCurrentState);
        initResourceRefs();
        // M: if run from other application, we will stop recording and auto
        // save the recording file
        // and reset SoundRecorder to innitial state
        if (!mRunFromLauncher) {
            if ((SoundRecorderService.STATE_RECORDING == mCurrentState)
                    || (SoundRecorderService.STATE_PAUSE_RECORDING == mCurrentState)) {
                LogUtils.i(TAG, "<initWhenHaveService> stop record when run from other ap");
                mService.stopRecord();
            }
            if (mService.isCurrentFileWaitToSave()) {
                // M: set mSetResultAfterSave = false ,and set
                // mSetResultAfterSave = true in onEvent
                mSetResultAfterSave = false;
                LogUtils.i(TAG, "<initWhenHaveService> save record when run from other ap");
                mService.saveRecord();
            } else {
                mService.reset();
            }
        }
        restoreRecordParamsSettings();
        mHandler.sendEmptyMessage(mService.getCurrentState());
        // do action that need to bo in onActivityResult
        if (RECORD.equals(mDoWhat)) {
            onClickRecordButton();
        } else if (PLAY.equals(mDoWhat)) {
            mService.playFile(mDoWhatFilePath);
        }
        mDoWhat = null;
        mDoWhatFilePath = null;
        LogUtils.i(TAG, "<initWhenHaveService> end");
    }

    private void initResourceRefsWhenNoService() {
        mRecordButton = (ImageButton) mFloatLayout.findViewById(R.id.recordButton);
        mStopButton = (ImageButton) mFloatLayout.findViewById(R.id.stopButton);
        mPlayButton = (ImageButton) mFloatLayout.findViewById(R.id.playButton);
        mFileListButton = (ImageButton) mFloatLayout.findViewById(R.id.fileListButton);
        mPauseRecordingButton = (ImageButton) mFloatLayout.findViewById(R.id.pauseRecordingButton);
        mButtonParent = (LinearLayout) mFloatLayout.findViewById(R.id.buttonParent);
        mRecordingStateImageView = (ImageView) mFloatLayout.findViewById(R.id.stateLED);
        mRemainingTimeTextView = (TextView) mFloatLayout.findViewById(R.id.stateMessage1);
        mStateTextView = (TextView) mFloatLayout.findViewById(R.id.stateMessage2);
        mStateProgressBar = (ProgressBar) mFloatLayout.findViewById(R.id.stateProgressBar);
        mTimerTextView = (TextView) mFloatLayout.findViewById(R.id.timerView);
        mPlayingStateImageView = (ImageView) mFloatLayout.findViewById(R.id.currState);
        mRecordingFileNameTextView = (TextView) mFloatLayout.findViewById(R.id.recordingFileName);
        mExitButtons = (LinearLayout) mFloatLayout.findViewById(R.id.exitButtons);
        mVUMeter = (VUMeter) mFloatLayout.findViewById(R.id.uvMeter);
        mTimerFormat = getResources().getString(R.string.timer_format);
        mAcceptButton = (Button) mFloatLayout.findViewById(R.id.acceptButton);
        mDiscardButton = (Button) mFloatLayout.findViewById(R.id.discardButton);
        mFirstLine = (ImageView) mFloatLayout.findViewById(R.id.firstLine);
        mSecondLine = (ImageView) mFloatLayout.findViewById(R.id.secondLine);
        mFrameLayout = (FrameLayout) mFloatLayout.findViewById(R.id.frameLayout);
    }	

    private void updateUiOnPausePlayingState() {
        LogUtils.i(TAG, "<updateUiOnPausePlayingState> start");
        mButtonParent.setWeightSum(THREE_BUTTON_WEIGHT_SUM);
        mRecordButton.setEnabled(true);
        mRecordButton.setFocusable(true);
        mRecordButton.setSoundEffectsEnabled(true);
        mPlayButton.setVisibility(View.VISIBLE);
        mPlayButton.setEnabled(true);
        mPlayButton.setFocusable(true);
        mPlayButton.setImageResource(R.drawable.play);
        mStopButton.setVisibility(View.VISIBLE);
        mFileListButton.setVisibility(View.GONE);
        mPauseRecordingButton.setVisibility(View.GONE);
        mPauseRecordingButton.setSoundEffectsEnabled(false);
        mStopButton.setEnabled(true);
        mStopButton.setFocusable(true);
        mRemainingTimeTextView.setVisibility(View.INVISIBLE);
        mRecordingStateImageView.setVisibility(View.INVISIBLE);
        mStateTextView.setVisibility(View.INVISIBLE);
        mPlayingStateImageView.setImageResource(R.drawable.pause);
        mPlayingStateImageView.setVisibility(View.VISIBLE);
        mRecordingFileNameTextView.setVisibility(View.VISIBLE);
        boolean isCurrentFileWaitToSave = false;
        if (null != mService) {
            isCurrentFileWaitToSave = mService.isCurrentFileWaitToSave();
        }
        if (!isCurrentFileWaitToSave) {
            mExitButtons.setVisibility(View.INVISIBLE);
            mFirstLine.setVisibility(View.INVISIBLE);
            mSecondLine.setVisibility(View.VISIBLE);
        } else {
            mFrameLayout.setBackgroundColor(getResources().getColor(R.color.blackColor));
            mExitButtons.setVisibility(View.VISIBLE);
            mAcceptButton.setEnabled(true);
            mDiscardButton.setEnabled(true);
            mFirstLine.setVisibility(View.VISIBLE);
            mSecondLine.setVisibility(View.INVISIBLE);
        }
        setTimerTextView(false);
        mStateProgressBar.setVisibility(View.VISIBLE);
        mStateProgressBar.setProgress((int) (100 
                * mService.getCurrentProgressInMillSecond() 
                / mService.getCurrentFileDurationInMillSecond()));
        mVUMeter.setVisibility(View.INVISIBLE);
        LogUtils.i(TAG, "<updateUiOnPausePlayingState> end");
    }

    private void updateUiOnRecordingState() {
        LogUtils.i(TAG, "<updateUiOnRecordingState> start");
        mFrameLayout.setBackgroundColor(getResources().getColor(R.color.frameLayoutGrayColor));
        Resources res = getResources();
        mButtonParent.setWeightSum(TWO_BUTTON_WEIGHT_SUM);
        mRecordButton.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.GONE);
        mStopButton.setVisibility(View.VISIBLE);
        mStopButton.setEnabled(true);
        mStopButton.setFocusable(true);
        if (mRunFromLauncher) {
            mFileListButton.setVisibility(View.GONE);
            mPauseRecordingButton.setVisibility(View.VISIBLE);
            mPauseRecordingButton.setEnabled(true);
            mPauseRecordingButton.setFocusable(true);
            mPauseRecordingButton.setSoundEffectsEnabled(false);
        } else {
            mRecordButton.setVisibility(View.VISIBLE);
            mRecordButton.setEnabled(false);
            mRecordButton.setFocusable(false);
            mRecordButton.setSoundEffectsEnabled(true);
            mFileListButton.setVisibility(View.GONE);
            mPauseRecordingButton.setVisibility(View.GONE);
        }
        mRecordingStateImageView.setVisibility(View.VISIBLE);
        mRecordingStateImageView.setImageResource(R.drawable.recording_led);
        mStateTextView.setVisibility(View.VISIBLE);
        mStateTextView.setText(res.getString(R.string.recording));
        mRemainingTimeTextView.setText(EMPTY);
        mRemainingTimeTextView.setVisibility(View.VISIBLE);
        mPlayingStateImageView.setVisibility(View.GONE);
        mRecordingFileNameTextView.setVisibility(View.VISIBLE);
        mExitButtons.setVisibility(View.INVISIBLE);
        mFirstLine.setVisibility(View.INVISIBLE);
        mSecondLine.setVisibility(View.VISIBLE);
        mVUMeter.setVisibility(View.VISIBLE);
        mStateProgressBar.setVisibility(View.INVISIBLE);
        int remainingTime = (int) mService.getRemainingTime();
        boolean isUpdateRemainingTimerView = mHasFileSizeLimitation ? (remainingTime < MMS_FILE_LIMIT)
                : (remainingTime < TIME_NINE_MIN);
        if ((remainingTime > 0) && isUpdateRemainingTimerView) {
            updateRemainingTimerView(remainingTime);
        }
        setTimerTextView(false);
        LogUtils.i(TAG, "<updateUiOnRecordingState> end");
    }

    private void updateUiOnPlayingState() {
        LogUtils.i(TAG, "<updateUiOnPlayingState> start");
        mButtonParent.setWeightSum(THREE_BUTTON_WEIGHT_SUM);
        mRecordButton.setEnabled(true);
        mRecordButton.setFocusable(true);
        mRecordButton.setSoundEffectsEnabled(true);
        mPlayButton.setVisibility(View.VISIBLE);
        mPlayButton.setEnabled(true);
        mPlayButton.setFocusable(true);
        mPlayButton.setImageResource(R.drawable.pause);
        mFileListButton.setVisibility(View.GONE);
        mPauseRecordingButton.setVisibility(View.GONE);
        mPauseRecordingButton.setSoundEffectsEnabled(false);
        mStopButton.setVisibility(View.VISIBLE);
        mStopButton.setEnabled(true);
        mStopButton.setFocusable(true);
        mRecordingStateImageView.setVisibility(View.INVISIBLE);
        mStateTextView.setVisibility(View.INVISIBLE);
        mRemainingTimeTextView.setVisibility(View.INVISIBLE);
        mPlayingStateImageView.setVisibility(View.VISIBLE);
        mPlayingStateImageView.setImageResource(R.drawable.play);
        mRecordingFileNameTextView.setVisibility(View.VISIBLE);
        if (!mService.isCurrentFileWaitToSave()) {
            mExitButtons.setVisibility(View.INVISIBLE);
            mFirstLine.setVisibility(View.INVISIBLE);
            mSecondLine.setVisibility(View.VISIBLE);
        } else {
            mFrameLayout.setBackgroundColor(getResources().getColor(R.color.blackColor));
            mExitButtons.setVisibility(View.VISIBLE);
            mAcceptButton.setEnabled(true);
            mDiscardButton.setEnabled(true);
            mFirstLine.setVisibility(View.VISIBLE);
            mSecondLine.setVisibility(View.INVISIBLE);
        }
        mVUMeter.setVisibility(View.INVISIBLE);
        mStateProgressBar.setVisibility(View.VISIBLE);
        setTimerTextView(true);
        LogUtils.i(TAG, "<updateUiOnPlayingState> end");
    }
	
    private void updateUiOnIdleState() {
        LogUtils.i(TAG, "<updateUiOnIdleState> start");
        boolean isCurrentFileWaitToSave = mService.isCurrentFileWaitToSave();
        int time = 0;
        if (mFileFromList) {
            mFileFromList = false;
        } else {
            time = (int) mService.getCurrentFileDurationInSecond();
        }
        String timerString = String.format(mTimerFormat, time / TIME_BASE, time % TIME_BASE);
        setTimerViewTextSize(time);
        LogUtils.i(TAG, "<updateUiOnIdleState> time = " + timerString);
        mTimerTextView.setText(timerString);
        
        String currentFilePath = mService.getCurrentFilePath();
        mRecordingStateImageView.setVisibility(View.INVISIBLE);
        mStateTextView.setVisibility(View.INVISIBLE);
        mStateProgressBar.setProgress(0);
        mStateProgressBar.setVisibility(View.INVISIBLE);
        mButtonParent.setWeightSum(TWO_BUTTON_WEIGHT_SUM);
        mRecordButton.setVisibility(View.VISIBLE);
        mRecordButton.setEnabled(true);
        mRecordButton.setFocusable(true);
        mRecordButton.setSoundEffectsEnabled(true);
        mRecordButton.requestFocus();
        mPauseRecordingButton.setVisibility(View.GONE);
        mPauseRecordingButton.setSoundEffectsEnabled(false);
        mStopButton.setEnabled(false);
        mStopButton.setVisibility(View.GONE);
        if (null == currentFilePath) {
            mFrameLayout.setBackgroundColor(getResources().getColor(R.color.frameLayoutGrayColor));
            mPlayButton.setVisibility(View.GONE);
            if (mRunFromLauncher) {
                mFileListButton.setVisibility(View.VISIBLE);
                mFileListButton.setEnabled(true);
                mFileListButton.setFocusable(true);
            } else {
                mFileListButton.setVisibility(View.GONE);
                mStopButton.setVisibility(View.VISIBLE);
                mStopButton.setEnabled(false);
                mStopButton.setFocusable(false);
            }
            mRemainingTimeTextView.setVisibility(View.INVISIBLE);
            mPlayingStateImageView.setVisibility(View.GONE);
            mRecordingFileNameTextView.setVisibility(View.INVISIBLE);
            mExitButtons.setVisibility(View.INVISIBLE);
            mFirstLine.setVisibility(View.INVISIBLE);
            mSecondLine.setVisibility(View.VISIBLE);
            mVUMeter.setVisibility(View.VISIBLE);
            mVUMeter.mCurrentAngle = 0;
        } else {
            if (mRunFromLauncher) {
                mButtonParent.setWeightSum(THREE_BUTTON_WEIGHT_SUM);
                mPlayButton.setVisibility(View.VISIBLE);
                mPlayButton.setEnabled(true);
                mPlayButton.setFocusable(true);
                mPlayButton.setImageResource(R.drawable.play);
            }
            mRemainingTimeTextView.setVisibility(View.INVISIBLE);
            mPlayingStateImageView.setImageResource(R.drawable.stop);
            mPlayingStateImageView.setVisibility(View.VISIBLE);
            mRecordingFileNameTextView.setVisibility(View.VISIBLE);
            mExitButtons.setVisibility(View.INVISIBLE);
            mVUMeter.setVisibility(View.INVISIBLE);
            mFirstLine.setVisibility(View.INVISIBLE);
            mFileListButton.setVisibility(View.VISIBLE);
            if (isCurrentFileWaitToSave) {
                mFrameLayout.setBackgroundColor(getResources().getColor(R.color.blackColor));
                mSecondLine.setVisibility(View.INVISIBLE);
                mFirstLine.setVisibility(View.VISIBLE);
                mExitButtons.setVisibility(View.VISIBLE);
                mAcceptButton.setEnabled(true);
                mDiscardButton.setEnabled(true);
                mStopButton.setVisibility(View.VISIBLE);
                mStopButton.setEnabled(false);
                mStopButton.setFocusable(false);
                mFileListButton.setVisibility(View.GONE);
            } else {
                mFrameLayout.setBackgroundColor(getResources().getColor(
                        R.color.frameLayoutGrayColor));
                mSecondLine.setVisibility(View.VISIBLE);
                mFileListButton.setEnabled(true);
            }
        }
        LogUtils.i(TAG, "<updateUiOnIdleState> end");
    }

    private void updateUiOnPauseRecordingState() {
        LogUtils.i(TAG, "<updateUiOnPauseRecordingState> start");
        Resources res = getResources();
        mButtonParent.setWeightSum(TWO_BUTTON_WEIGHT_SUM);
        mRecordButton.setVisibility(View.VISIBLE);
        mRecordButton.setEnabled(true);
        mRecordButton.setFocusable(true);
        mRecordButton.setSoundEffectsEnabled(false);
        mPlayButton.setVisibility(View.GONE);
        mFileListButton.setVisibility(View.GONE);
        mPauseRecordingButton.setVisibility(View.GONE);
        mStopButton.setVisibility(View.VISIBLE);
        mStopButton.setEnabled(true);
        mStopButton.setFocusable(true);
        mRecordingStateImageView.setVisibility(View.VISIBLE);
        mRecordingStateImageView.setImageResource(R.drawable.idle_led);
        mStateTextView.setVisibility(View.VISIBLE);
        mStateTextView.setText(res.getString(R.string.recording_paused));
        mRemainingTimeTextView.setVisibility(View.INVISIBLE);
        mPlayingStateImageView.setVisibility(View.GONE);
        mRecordingFileNameTextView.setVisibility(View.VISIBLE);
        mExitButtons.setVisibility(View.INVISIBLE);
        mFirstLine.setVisibility(View.INVISIBLE);
        mSecondLine.setVisibility(View.VISIBLE);
        mVUMeter.setVisibility(View.VISIBLE);
        mVUMeter.mCurrentAngle = 0;
        mStateProgressBar.setVisibility(View.INVISIBLE);
        setTimerTextView(false);
        LogUtils.i(TAG, "<updateUiOnPauseRecordingState> end");
    }
	
    private void restoreRecordParamsSettings() {
        LogUtils.i(TAG, "<restoreRecordParamsSettings> ");
        if (null == mPrefs) {
            mPrefs = getSharedPreferences(SOUND_RECORDER_DATA, 0);
        }
        IQualityLevel qualityLevel = ExtensionHelper.getExtension(SoundRecorderSmallScreen.this);
        int levelNumber = qualityLevel.getLevelNumber();
        if (TWO_LEVELS == levelNumber) {
            mSelectedFormat = mPrefs.getInt(SoundRecorderService.SELECTED_RECORDING_FORMAT,
                    RecordParamsSetting.FORMAT_HIGH);
            if ((RecordParamsSetting.FORMAT_HIGH != mSelectedFormat)
                    && (RecordParamsSetting.FORMAT_STANDARD != mSelectedFormat)) {
                mSelectedFormat = 0;
            }
        } else if (THREE_LEVELS == levelNumber) {
            mSelectedFormat = mPrefs.getInt(SoundRecorderService.SELECTED_RECORDING_FORMAT,
                    RecordParamsSetting.FORMAT_LOW);
            if (mSelectedFormat < 0) {
                mSelectedFormat = RecordParamsSetting.FORMAT_LOW;
            }
        }
        mSelectedMode = mPrefs.getInt(SoundRecorderService.SELECTED_RECORDING_MODE, RecordParamsSetting.MODE_NORMAL);
        if (mSelectedMode < 0) {
            mSelectedMode = RecordParamsSetting.MODE_NORMAL;
        }
        mSelectEffectArray[RecordParamsSetting.EFFECT_AEC] = mPrefs.getBoolean(
                SoundRecorderService.SELECTED_RECORDING_EFFECT_AEC, false);
        mSelectEffectArray[RecordParamsSetting.EFFECT_AGC] = mPrefs.getBoolean(
                SoundRecorderService.SELECTED_RECORDING_EFFECT_AGC, false);
        mSelectEffectArray[RecordParamsSetting.EFFECT_NS] = mPrefs.getBoolean(
                SoundRecorderService.SELECTED_RECORDING_EFFECT_NS, false);
        mSelectEffectArrayTemp[RecordParamsSetting.EFFECT_AEC] = mPrefs.getBoolean(
                SoundRecorderService.SELECTED_RECORDING_EFFECT_AEC_TMP, false);
        mSelectEffectArrayTemp[RecordParamsSetting.EFFECT_AGC] = mPrefs.getBoolean(
                SoundRecorderService.SELECTED_RECORDING_EFFECT_AGC_TMP, false);
        mSelectEffectArrayTemp[RecordParamsSetting.EFFECT_NS] = mPrefs.getBoolean(
                SoundRecorderService.SELECTED_RECORDING_EFFECT_NS_TMP, false);
        if (null != mService) {
            mService.setSelectedFormat(mSelectedFormat);
            mService.setSelectedMode(mSelectedMode);
            mService.setSelectEffectArray(mSelectEffectArray);
            mService.setSelectEffectArrayTmp(mSelectEffectArrayTemp);
        }
        LogUtils.i(TAG, "mSelectedFormat is:" + mSelectedFormat + "; mSelectedMode is:" + mSelectedMode);
    }

    @Override
    public void onEvent(int eventCode) {
        switch (eventCode) {
        case SoundRecorderService.EVENT_SAVE_SUCCESS:
            LogUtils.i(TAG, "<onEvent> EVENT_SAVE_SUCCESS");
            Uri uri = mService.getSaveFileUri();
            if (null != uri) {
                mHandler.sendEmptyMessage(SoundRecorderService.STATE_SAVE_SUCESS);
            }
            if (!mRunFromLauncher) {
                LogUtils.i(TAG, "<onEvent> mSetResultAfterSave = " + mSetResultAfterSave);
                if (mSetResultAfterSave) {
                    //setResult(RESULT_OK, new Intent().setData(uri));
                    LogUtils.i(TAG, "<onEvent> finish");
                    LogUtils.i(TAG, "<onEvent> Activity = " + this.toString());
                    stopSelf();//finish();
                } else {
                    mSetResultAfterSave = true;
                }
            }
            mService.reset();
            mHandler.sendEmptyMessage(mService.getCurrentState());
            long mEndSaveTime = System.currentTimeMillis();
            Log.i(TAG, "[Performance test][SoundRecorder] recording save end [" + mEndSaveTime
                    + "]");
            break;
        case SoundRecorderService.EVENT_DISCARD_SUCCESS:
            LogUtils.i(TAG, "<onEvent> EVENT_DISCARD_SUCCESS");
            if (mRunFromLauncher) {
                mService.reset();
                mHandler.sendEmptyMessage(mService.getCurrentState());
            } else {
                mService.reset();
                LogUtils.i(TAG, "<onEvent> finish");
                LogUtils.i(TAG, "<onEvent> Activity = " + this.toString());
                stopSelf();//finish();
            }
            break;
        case SoundRecorderService.EVENT_STORAGE_MOUNTED:
            LogUtils.i(TAG, "<onEvent> EVENT_STORAGE_MOUNTED");
            // remove error dialog after sd card mounted
            removeOldFragmentByTag(ErrorHandle.ERROR_DIALOG_TAG);
            break;
        default:
            LogUtils.i(TAG, "<onEvent> event out of range, event code = " + eventCode);
            break;
        }
    }

    @Override
    public void onStateChanged(int stateCode) {
        LogUtils.i(TAG, "<onStateChanged> change from " + mCurrentState + " to " + stateCode);
        if (!mRunFromLauncher) {
            if (stateCode == SoundRecorderService.STATE_RECORDING) {
                acquireWakeLock();
            } else {
                releaseWakeLock();
            }
        }
        mCurrentState = stateCode;
        mHandler.removeMessages(stateCode);
        mHandler.sendEmptyMessage(stateCode);
    }

    @Override
    public void onError(int errorCode) {
        LogUtils.i(TAG, "<onError> errorCode = " + errorCode);
        // M: if OnSaveInstanceState has run, we do not show Dialogfragment now,
        // or else FragmentManager will throw IllegalStateException
        if (!mOnSaveInstanceStateHasRun) {
            Bundle bundle = new Bundle(1);
            bundle.putInt(ERROR_CODE, errorCode);
            Message msg = mHandler.obtainMessage(SoundRecorderService.STATE_ERROR);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    }

    @Override
    public void updateTimerView(int time) {
        LogUtils.i(TAG, "<updateTimerView> start time = " + time);
        int state = mService.getCurrentState();
        // update progress bar
        if (SoundRecorderService.STATE_PLAYING == state) {
            long fileDuration = mService.getCurrentFileDurationInMillSecond();
            LogUtils.i(TAG, "<updateTimerView> fileDuration = " + fileDuration);
            if (fileDuration > ONE_SECOND) {
                long progress = mService.getCurrentProgressInMillSecond();
                LogUtils.i(TAG, "<updateTimerView> progress = " + (fileDuration - progress));
                if (fileDuration - progress < SoundRecorderService.WAIT_TIME) {
                    mStateProgressBar.setProgress(DONE);
                } else {
                    mStateProgressBar.setProgress((int) (100 * progress / fileDuration));
                }
            } else {
                mStateProgressBar.setProgress(DONE);
            }
        }
        // update timer
        setTimerTextView(time);
        // update remaining time
        if (SoundRecorderService.STATE_RECORDING == mService.getCurrentState()) {
            int remainingTime = (int) mService.getRemainingTime();
            if (mService.isStorageLower()) {
                showStorageHint(getString(R.string.storage_low));
            } else {
                hideStorageHint();
            }
            boolean isUpdateRemainingTimerView = mHasFileSizeLimitation ? (remainingTime < MMS_FILE_LIMIT)
                    : (remainingTime < TIME_NINE_MIN);
            if ((remainingTime > 0) && isUpdateRemainingTimerView) {
                updateRemainingTimerView(remainingTime);
            }
        }
        LogUtils.i(TAG, "<updateTimerView> end");
    }	
	
    private void initResourceRefs() {
        LogUtils.i(TAG, "<initResourceRefs> start");
        initResourceRefsWhenNoService();
        /**
         * M: set related property according to if SoundRecorder is started by
         * launcher @{
         */
        if (mRunFromLauncher) {
            mPlayButton.setOnClickListener(this);
            mFileListButton.setOnClickListener(this);
            mPauseRecordingButton.setOnClickListener(this);
        } else {
            mPlayButton.setVisibility(View.GONE);
            mFileListButton.setVisibility(View.GONE);
            mPauseRecordingButton.setVisibility(View.GONE);
        }
        /** @} */
        mRecordButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);
        mAcceptButton.setOnClickListener(this);
        mDiscardButton.setOnClickListener(this);
        //setTitle(getResources().getString(R.string.app_name));
        mVUMeter.setRecorder(mService.getRecorder());
        setTimerTextView(true);
        LogUtils.i(TAG, "<initResourceRefs> end");
    }

    public void setTimerTextView(boolean initial) {
        int time = 0;
        if (!initial) {
            if (null != mService) {
                time = (int) mService.getCurrentProgressInSecond();
            }
        }
        setTimerTextView(time);
    }
    
    private void setTimerTextView(int time){
        LogUtils.i(TAG, "<setTimerTextView> start with time = " + time);
        String timerString = String.format(mTimerFormat, time / TIME_BASE, time % TIME_BASE);
        setTimerViewTextSize(time);
        mTimerTextView.setText(timerString);
        LogUtils.i(TAG, "<setTimerTextView> end");
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            LogUtils.i(TAG, "<handleMessage> start with msg.what-"+msg.what);
            if (null == mService) {
                return;
            }
            updateOptionsMenu();
            String filePath = mService.getCurrentFilePath();
            LogUtils.i(TAG, "<handleMessage> mService.getCurrentFilePath() = " + filePath);
            mFileName = NULL_STRING;
            if (null != filePath) {
                mFileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1, filePath
                        .length());
                mFileName = (mFileName.endsWith(Recorder.SAMPLE_SUFFIX)) ? mFileName.substring(0,
                        mFileName.lastIndexOf(Recorder.SAMPLE_SUFFIX)) : mFileName;
            }
            LogUtils.i(TAG, "<updateUi> mRecordingFileNameTextView.setText : " + mFileName);
            mRecordingFileNameTextView.setText(mFileName);
            mAcceptButton.setText(R.string.accept);
            if (mRunFromLauncher) {
                mAcceptButton.setText(R.string.save_record);
            }
            hideStorageHint();
            switch (msg.what) {
                case SoundRecorderService.STATE_IDLE:
                    updateUiOnIdleState();
                    break;
                case SoundRecorderService.STATE_PAUSE_PLAYING:
                    updateUiOnPausePlayingState();
                    break;
                case SoundRecorderService.STATE_RECORDING:
                    updateUiOnRecordingState();
                    break;
                case SoundRecorderService.STATE_PAUSE_RECORDING:
                    updateUiOnPauseRecordingState();
                    break;
                case SoundRecorderService.STATE_PLAYING:
                    updateUiOnPlayingState();
                    break;
                case SoundRecorderService.STATE_ERROR:
                    Bundle bundle = msg.getData();
                    int errorCode = bundle.getInt(ERROR_CODE);
                    //ErrorHandle.showErrorInfo(SoundRecorderSmallScreen.this, errorCode);
                    break;
                case SoundRecorderService.STATE_SAVE_SUCESS:
                    updateUiOnSaveSuccessState();
                    SoundRecorderUtils.getToast(SoundRecorderSmallScreen.this,
                            R.string.tell_save_record_success);
                    break;
                default:
                    break;
            }
            mVUMeter.invalidate();
            LogUtils.i(TAG, "<handleMessage> end");
        }
    };

    void onClickRecordButton() {
        if (null != mService) {
            disableButton();
            mService.startRecordingAsync(RecordParamsSetting.getRecordParams(mRequestedType, mSelectedFormat,
                    mSelectedMode, mSelectEffectArray), (int) mMaxFileSize);
        }
        long mEndRecordingTime = System.currentTimeMillis();
        Log.i(TAG, "[Performance test][SoundRecorder] recording end [" + mEndRecordingTime + "]");
    }
	
    private void removeOldFragmentByTag(String tag) {
        LogUtils.i(TAG, "<removeOldFragmentByTag> start");
        /*FragmentManager fragmentManager = getFragmentManager();
        DialogFragment oldFragment = (DialogFragment) fragmentManager.findFragmentByTag(tag);
        LogUtils.i(TAG, "<removeOldFragmentByTag> oldFragment = " + oldFragment);
        if (null != oldFragment) {
            oldFragment.dismissAllowingStateLoss();
            LogUtils.i(TAG, "<removeOldFragmentByTag> remove oldFragment success");
        }*/
        LogUtils.i(TAG, "<removeOldFragmentByTag> end");
    }

    private void acquireWakeLock() {
        if ((null != mWakeLock) && !mWakeLock.isHeld()) {
            mWakeLock.acquire();
            LogUtils.i(TAG, "<acquireWakeLock>");
        }
    }

    private void releaseWakeLock() {
        // if mWakeLock is not release, release it
        if ((null != mWakeLock) && mWakeLock.isHeld()) {
            mWakeLock.release();
            LogUtils.i(TAG, "<releaseWakeLock>");
        }
    }

    private void showStorageHint(String message) {
        if (null == mStorageHint) {
            mStorageHint = OnScreenHint.makeText(this, message);
        } else {
            mStorageHint.setText(message);
        }
        mStorageHint.show();
    }

    private void hideStorageHint() {
        if (null != mStorageHint) {
            mStorageHint.cancel();
            mStorageHint = null;
        }
    }

    private void updateRemainingTimerView(int second) {
        String timeString = "";
        if (second < 0) {
            mRemainingTimeTextView.setText(NULL_STRING);
        } else if (second < TIME_BASE) {
            timeString = String.format(getString(R.string.sec_available), second);
        } else {
            if (second % TIME_BASE == 0) {
                timeString = String.format(getString(R.string.min_available), second / TIME_BASE);
            } else {
                timeString = String.format(getString(R.string.time_available), second / TIME_BASE,
                        second % TIME_BASE);
            }
        }
        LogUtils.i(TAG, "<updateRemainingTimerView> mRemainingTimeTextView.setText: "
                + timeString);
        mRemainingTimeTextView.setText(timeString);
        mRemainingTimeTextView.setVisibility(View.VISIBLE);
    }

    private void setTimerViewTextSize(int time) {
        /** M: set text size bigger if >= 100 @{ */
        final int textSizeChangeBoundary = 100;
	if ((time / TIME_BASE) >= textSizeChangeBoundary) {
            mTimerTextView.setTextSize(getResources().getDimension(R.dimen.timerView_TextSize_Small));
        } else {
            mTimerTextView.setTextSize(getResources().getDimension(R.dimen.timerView_TextSize_Big));
        }
        /** @} */
    }

    private void updateOptionsMenu() {
        LogUtils.i(TAG, "<updateOptionsMenu>");
		/*
        if (null == mMenu) {
            LogUtils.i(TAG, "<updateOptionsMenu> mMenu == null, return");
            return;
        }

        boolean allowSelectFormatAndMode = mRunFromLauncher;
        if (null != mService) {
            allowSelectFormatAndMode = mRunFromLauncher
                    && (SoundRecorderService.STATE_IDLE == mService.getCurrentState());
        }

        if (RecordParamsSetting.canSelectFormat()) {
            MenuItem item1 = mMenu.getItem(OPTIONMENU_SELECT_FORMAT);
            if (null != item1) {
                item1.setVisible(allowSelectFormatAndMode);
            }
        }
        if (RecordParamsSetting.canSelectMode()) {
            MenuItem item2 = mMenu.getItem(OPTIONMENU_SELECT_MODE);
            if (null != item2) {
                item2.setVisible(allowSelectFormatAndMode);
            }
        }
        if (RecordParamsSetting.canSelectEffect()) {
            MenuItem item3 = mMenu.getItem(OPTIONMENU_SELECT_EFFECT);
            if (null != item3) {
                item3.setVisible(allowSelectFormatAndMode);
            }
        }
		*/
    }

    private void updateUiOnSaveSuccessState() {
        LogUtils.i(TAG, "<updateUiOnSaveSuccessState> start");
        updateUiOnIdleState();
        LogUtils.i(TAG, "<updateUiOnSaveSuccessState> end");
    }	

    private void disableButton() {
        LogUtils.i(TAG, "<disableButton>");
        mRecordButton.setEnabled(false);
        mPauseRecordingButton.setEnabled(false);
        mStopButton.setEnabled(false);
        mPlayButton.setEnabled(false);
        mFileListButton.setEnabled(false);
        mDiscardButton.setEnabled(false);
        mAcceptButton.setEnabled(false);
    }	
	
    void onClickPlayButton() {
        if (null != mService) {
            disableButton();
            if ((SoundRecorderService.STATE_RECORDING == mCurrentState)
                    || (SoundRecorderService.STATE_PAUSE_RECORDING == mCurrentState)) {
                LogUtils.i(TAG, "<playCurrentFile> in record or pause record state, can't play");
                return;
            } else if (SoundRecorderService.STATE_PAUSE_PLAYING == mCurrentState) {
                mService.goonPlaybackAsync();
            } else if (SoundRecorderService.STATE_PLAYING == mCurrentState) {
                mService.pausePlaybackAsync();
            } else {
                mService.startPlaybackAsync();
            }
        }
    }

    void onClickStopButton() {
        if (null == mService) {
            long mEndStopTime = System.currentTimeMillis();
            Log.i(TAG, "[Performance test][SoundRecorder] recording stop end [" + mEndStopTime
                    + "]");
            return;
        }
        disableButton();
        int state = mService.getCurrentState();
        if ((SoundRecorderService.STATE_PAUSE_PLAYING == state)
                || (SoundRecorderService.STATE_PLAYING == state)) {
            LogUtils.i(TAG, "<onClickStopButton> mService.stopPlay()");
            mService.stopPlaybackAsync();
        } else if ((SoundRecorderService.STATE_RECORDING == state)
                || (SoundRecorderService.STATE_PAUSE_RECORDING == state)) {
            LogUtils.i(TAG, "<onClickStopButton> mService.stopRecord()");
            mService.stopRecordingAsync();
        }
        long mEndStopTime = System.currentTimeMillis();
        Log.i(TAG, "[Performance test][SoundRecorder] recording stop end [" + mEndStopTime + "]");
    }

    /**
     * process after click accept button
     */
    void onClickAcceptButton() {
        if (null == mService) {
            return;
        }
        disableButton();
        int state = mService.getCurrentState();
        if ((SoundRecorderService.STATE_PAUSE_PLAYING == state)
                || (SoundRecorderService.STATE_PLAYING == state)) {
            LogUtils.i(TAG, "<onClickAcceptButton> mService.stopPlay() first");
            mService.stopPlaybackAsync();
        }
        mService.saveRecordAsync();
    }

    /**
     * process after click discard button
     */
    void onClickDiscardButton() {
        disableButton();
        int state = mService.getCurrentState();
        if ((SoundRecorderService.STATE_PAUSE_PLAYING == state)
                || (SoundRecorderService.STATE_PLAYING == state)) {
            LogUtils.i(TAG, "<onClickDiscardButton> mService.stopPlay() first");
            mService.stopPlaybackAsync();
        }
        mService.discardRecordAsync();
        mVUMeter.mCurrentAngle = 0;
        mVUMeter.invalidate();
    }

    /**
     * process after click file list button
     */
    void onClickFileListButton() {
        disableButton();
        if (null != mService) {
            LogUtils.i(TAG, "<onClickFileListButton> mService.reset()");
            mService.reset();
        }
        Intent mIntent = new Intent();
        mIntent.setClass(this, RecordingFileListSmallScreen.class);
        //startActivityForResult(mIntent, REQURST_FILE_LIST);
		startService(mIntent);
		stopSelf();
    }

    /**
     * process after click pause recording button
     */
    void onClickPauseRecordingButton() {
        if (null != mService) {
            disableButton();
            mService.pauseRecordingAsync();
        }
    }
	
}