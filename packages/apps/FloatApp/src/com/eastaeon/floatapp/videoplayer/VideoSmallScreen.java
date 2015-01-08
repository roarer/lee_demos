package com.eastaeon.floatapp.videoplayer;

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
import android.view.SurfaceHolder; 
import android.view.SurfaceView;
import android.media.MediaPlayer;
import java.io.IOException;
import com.eastaeon.floatapp.R;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.content.AsyncQueryHandler;
import java.util.ArrayList;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.content.Context;
import android.database.Cursor;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.widget.RelativeLayout;
import java.util.Locale;
import com.mediatek.drm.OmaDrmStore;
import android.content.BroadcastReceiver;  
import android.os.storage.StorageVolume;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video.VideoColumns;
import android.os.storage.StorageManager;
import android.provider.MediaStore.Video.Media;
import android.app.ProgressDialog;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.content.ContentUris;
import android.content.IntentFilter;
import android.media.AudioManager;
import java.util.LinkedList;
import java.util.ListIterator;

public class VideoSmallScreen extends Service implements OnClickListener,SurfaceHolder.Callback,MediaPlayer.OnCompletionListener,MediaPlayer.OnErrorListener,MediaPlayer.OnSeekCompleteListener,MediaPlayer.OnVideoSizeChangedListener,MediaPlayer.OnPreparedListener
{
	private static final String TAG = "VideoSmallScreen";
	
	//悬浮窗基本属性
    private Display mDisplay;
    private Matrix mDisplayMatrix;
	private LayoutInflater inflater;
	private Button minimizeButton;
	private Button listButton;
	private TextView titleButton;
	RelativeLayout mTitleLyout;
	
	LinearLayout mMainLayout;
	//定义浮动窗口布局
    FrameLayout mFloatLayout;
    WindowManager.LayoutParams wmParams;
    //创建浮动窗口设置布局参数的对象
	WindowManager mWindowManager;
	private int X, Y;
	
	
	//视频播放界面基本属性
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private MediaPlayer	mediaPlayer;
	
	private Button videoPlayButton;//播放
	private Button videoPluseButton;//暂停
	private Button videoListButton;//列表
	private Button videoNextButton;//下一个
	private Button videoFFButton;//快进
	private Button videoFBButton;//快退
	private TextView mTitleTextView;
	private TextView mDurationTextView;
	private RelativeLayout videoControl;
	
	private Timer mTimer;
	private TimerTask mTimerTask;
	private Handler mHandler;
	private Uri videoUri;
	
	private int currentHours = 0;
	private int currentMinutes = 0;
	private int currentSeconds = 0;
	
	private int durationHours = 0;
	private int durationMinutes = 0;
	private int durationSeconds = 0;
	
	private final int HIDE_CONTROL_BUTTON = 0x100;
	private final int REFRESH_TIME = 0x200;
	private final int NEXT_REFRESH_DELAY = 500;
	private final int SHOW_PLAY = 0x300;

	private ListView mListView;
    private TextView mEmptyView;
	private ViewGroup mNoSdView;
	private MovieListAdapter mAdapter;
	private ThumbnailCache mThumbnailCache;
	private CachedVideoInfo mCachedVideoInfo;
	private static String[] sExternalStoragePaths;
	private ProgressDialog mProgressDialog;
	private boolean mIsFocused = false;
	
	private static final int INDEX_ID = 0;
	private static final int INDEX_DISPLAY_NAME = 1;
    private static final int INDEX_TAKEN_DATE = 2;
    private static final int INDEX_DRUATION = 3;
    private static final int INDEX_MIME_TYPE = 4;
    private static final int INDEX_DATA = 5;
    private static final int INDEX_FILE_SIZE = 6;
    private static final int INDEX_IS_DRM = 7;
    private static final int INDEX_DATE_MODIFIED = 8;
    private static final int INDEX_SUPPORT_3D = 9;
	
	private static final Uri VIDEO_URI = Media.EXTERNAL_CONTENT_URI;
	private static final String[] PROJECTION = new String[]{
		BaseColumns._ID,
		MediaColumns.DISPLAY_NAME,
		VideoColumns.DATE_TAKEN,
		VideoColumns.DURATION,
		MediaColumns.MIME_TYPE,
		MediaColumns.DATA,
		MediaColumns.SIZE,
		Media.IS_DRM,
		MediaColumns.DATE_MODIFIED,
		Media.STEREO_TYPE,
	};
	
    private static final String ORDER_COLUMN =
        VideoColumns.DATE_TAKEN + " DESC, " + 
        BaseColumns._ID + " DESC ";
		
    private static final String KEY_LOGO_BITMAP = "logo-bitmap";
    private static final String KEY_TREAT_UP_AS_BACK = "treat-up-as-back";
	private static final String EXTRA_ALL_VIDEO_FOLDER = "mediatek.intent.extra.ALL_VIDEO_FOLDER";
    private static final String EXTRA_ENABLE_VIDEO_LIST = "mediatek.intent.extra.ENABLE_VIDEO_LIST";	
	private static final String OPEN_VIDEO_PLAYER = "action.eastaeon.open.videoplayer";		

	private static final String[] PROJECTION2 = new String[]{
		BaseColumns._ID,
		MediaColumns.DISPLAY_NAME
	};
	
	
	private LinkedList<Uri> videoURIs;
	
	HallLockStateReceiver hallLockStateReceiver;
	
	@Override
	public void onCreate() 
	{
		// TODO Auto-generated method stub
		super.onCreate();
		Log.d(TAG, "onCreate()");
		//初始化悬浮窗
		createFloatView();
		//初始化计算器界面
		init();	
		
		hallLockStateReceiver = new HallLockStateReceiver(this);
		registerReceiver(hallLockStateReceiver, new IntentFilter("com.eastaeon.action.HALLCLOSE"));
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) { 
		// TODO Auto-generated method stub
		Log.d(TAG, "onStartCommand()");
		return super.onStartCommand(intent, flags, startId);
		
		//setVideoURI(intent.getData());
		//Log.d(TAG, "onStartCommand() videoUri = " + videoUri);
		//初始化悬浮窗
		//createFloatView();
		//初始化计算器界面
		//init();		
		//return START_REDELIVER_INTENT;
		
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
        wmParams.width = Math.min(screenWidth,screenHeight);
        wmParams.height = (Math.min(screenWidth,screenHeight)*9*6)/(16*5);
   
        inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
		mMainLayout = (LinearLayout) inflater.inflate(R.layout.video_float_screen, null); 
		mFloatLayout = (FrameLayout)mMainLayout.findViewById(R.id.float_fayout);
		surfaceView = (SurfaceView) mFloatLayout.findViewById(R.id.video_play_screen);
		videoControl = (RelativeLayout) mFloatLayout.findViewById(R.id.video_control);
		
		mTitleLyout = (RelativeLayout)mMainLayout.findViewById(R.id.title_layout);
		titleButton = (TextView) mTitleLyout.findViewById(R.id.title);
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

		
		videoControl.setOnClickListener(this);
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
		
		if (mThumbnailCache != null) {
            mThumbnailCache.removeListener(mAdapter);
            mThumbnailCache.clear();
        }
		if(mCachedVideoInfo!= null )
		mCachedVideoInfo.setLocale(null);
		unregisterReceiver(mStorageListener);		
		if(mMainLayout != null)
		{
			//移除悬浮窗口
			mWindowManager.removeView(mMainLayout);
		}
		
	}	
	
	class MyTimerTask extends TimerTask{
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.d("TAG", "run...");
			Message msg = mHandler.obtainMessage(HIDE_CONTROL_BUTTON);
			msg.sendToTarget();
		}
	}
 
 
    public void StartHideControlButtonTimer(){
     if (mTimer != null){
      if (mTimerTask != null){
       mTimerTask.cancel();  //将原任务从队列中移除
      }
      
     
      mTimerTask = new MyTimerTask();  // 新建一个任务      
      mTimer.schedule(mTimerTask, 3*1000);
     }
    }
	
	@Override
	public void onClick(View v) {
		Log.d("TAG","onClick() v = "+v);
		if(v.equals(videoControl)){
			if(mediaPlayer != null && !mediaPlayer.isPlaying()) {
				videoPlayButton.setVisibility(View.VISIBLE);
				videoPluseButton.setVisibility(View.INVISIBLE);
			} else {
				videoPluseButton.setVisibility(View.VISIBLE);
				videoPlayButton.setVisibility(View.INVISIBLE);
			}
			videoListButton.setVisibility(View.VISIBLE);
			videoNextButton.setVisibility(View.VISIBLE);
			videoFFButton.setVisibility(View.VISIBLE);
			videoFBButton.setVisibility(View.VISIBLE);
			StartHideControlButtonTimer();
		}  else if(v.equals(videoPlayButton)) {
			if (mediaPlayer != null && !mediaPlayer.isPlaying()) {  
                mediaPlayer.start();
				videoPlayButton.setVisibility(View.INVISIBLE);
				videoPluseButton.setVisibility(View.VISIBLE);
            }
			refreshNow();
			queueNextRefresh(NEXT_REFRESH_DELAY); 
			StartHideControlButtonTimer();
		} else if(v.equals(videoPluseButton)) {
			if (mediaPlayer != null && mediaPlayer.isPlaying()) {  
                mediaPlayer.pause();
				videoPluseButton.setVisibility(View.INVISIBLE);
				videoPlayButton.setVisibility(View.VISIBLE);
            }
			StartHideControlButtonTimer();
		} else if(v.equals(videoListButton)) {
			/*Intent intent = new Intent(this,VideoListSmallScreen.class);
			this.startService(intent);
			stopSelf();*/
			mediaPlayer.stop();
			refreshSdStatus(MtkUtils.isMediaMounted(this));			
		} else if(v.equals(videoNextButton)) {
			nextVideo();
			StartHideControlButtonTimer();
		} else if(v.equals(videoFFButton)) {
			int pos = mediaPlayer.getCurrentPosition();
			pos += 15000; // milliseconds
			mediaPlayer.seekTo(pos);		
			refreshNow();
			StartHideControlButtonTimer();
		} else if(v.equals(videoFBButton)) {
			int pos = mediaPlayer.getCurrentPosition();
			pos -= 5000; // milliseconds
			mediaPlayer.seekTo(pos);	
			refreshNow();
			StartHideControlButtonTimer();
		} else if(v.equals(minimizeButton)){
			sendBroadcast(new Intent("eastaeon.intent.action.float.OPEN_FLOAT"));
			stopSelf();
		} else if(v.equals(listButton)) {
			sendBroadcast(new Intent("eastaeon.intent.action.launch.OPEN_LAUNCH"));
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
	
	
	//视频播放器相关函数
    public void init() {	

		mListView = (ListView) mFloatLayout.findViewById(R.id.list);
		mEmptyView = (TextView) mFloatLayout.findViewById(R.id.empty);
		mNoSdView = (ViewGroup) mFloatLayout.findViewById(R.id.no_sdcard);
		final StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
		sExternalStoragePaths = storageManager.getVolumePaths();
		
		mAdapter = new MovieListAdapter(this, R.layout.movielist_item, null, new String[]{}, new int[]{});
        mListView.setAdapter(mAdapter);
        mListView.setOnScrollListener(mAdapter);
		
		registerStorageListener();
		refreshSdStatus(MtkUtils.isMediaMounted(this));
		mThumbnailCache = new ThumbnailCache(this);
        mThumbnailCache.addListener(mAdapter);	
		mCachedVideoInfo = new CachedVideoInfo();
		mCachedVideoInfo.setLocale(Locale.getDefault());

		
		videoPlayButton = (Button) videoControl.findViewById(R.id.play_button);
		videoPluseButton = (Button) videoControl.findViewById(R.id.pause_button);
		videoListButton = (Button) videoControl.findViewById(R.id.list_button);
		videoNextButton = (Button) videoControl.findViewById(R.id.next_button);
		videoFFButton = (Button) videoControl.findViewById(R.id.fast_forward_button);
		videoFBButton = (Button) videoControl.findViewById(R.id.fast_backward_button);
		mTitleTextView = (TextView) videoControl.findViewById(R.id.title_text);
		mDurationTextView = (TextView) videoControl.findViewById(R.id.duration_text);
		
		videoPlayButton.setOnClickListener(this);
		videoPluseButton.setOnClickListener(this);
		videoListButton.setOnClickListener(this);
		videoNextButton.setOnClickListener(this);
		videoFFButton.setOnClickListener(this);
		videoFBButton.setOnClickListener(this);
		
		mTimer = new Timer(true);
		mHandler = new Handler(){
			public void handleMessage(Message message){
				Log.d("TAG", "message what = " + message.what);
				if (message.what == HIDE_CONTROL_BUTTON) {
					videoPlayButton.setVisibility(View.GONE);
					videoPluseButton.setVisibility(View.GONE);
					videoListButton.setVisibility(View.GONE);
					videoNextButton.setVisibility(View.GONE);
					videoFFButton.setVisibility(View.GONE);
					videoFBButton.setVisibility(View.GONE);
				} else if (message.what == REFRESH_TIME) {
					refreshNow(); // 更新music播放界面进度条  
					queueNextRefresh(NEXT_REFRESH_DELAY); // 下次更新进度条的时间间隔，延迟next时间后更新进度条 				
				} else if(message.what == SHOW_PLAY) {
					release();
					resume();
				}

			}
		};
		initVideoList();
		initVideoView();	
	}

	private void refreshSdStatus(final boolean mounted) {
        Log.d(TAG, "refreshSdStatus(" + mounted + ")");
        if (mounted) {
            if (MtkUtils.isMediaScanning(this)) {
                Log.d(TAG, "refreshSdStatus() isMediaScanning true");
                showScanningProgress();
                showList();
                //MtkUtils.disableSpinnerState(this);
            } else {
                Log.d(TAG, "refreshSdStatus() isMediaScanning false");
                hideScanningProgress();
                showList();
                refreshMovieList();
                //MtkUtils.enableSpinnerState(this);
            }
        } else {
            hideScanningProgress();
            showSdcardLost();
            //MtkUtils.disableSpinnerState(this);
        }
    }

    private void showList() {
        mListView.setVisibility(View.VISIBLE);
        mEmptyView.setVisibility(View.GONE);
		mNoSdView.setVisibility(View.GONE);
		videoControl.setVisibility(View.GONE);
		surfaceView.setVisibility(View.GONE);
		//release();
    }
    
    private void showEmpty() {
        mListView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.VISIBLE);
		mNoSdView.setVisibility(View.GONE);
		videoControl.setVisibility(View.GONE);
		surfaceView.setVisibility(View.GONE);
		//release();		
    }
	
    private void showSdcardLost() {
        mListView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.GONE);
        mNoSdView.setVisibility(View.VISIBLE);
		videoControl.setVisibility(View.GONE);
		surfaceView.setVisibility(View.GONE);	
		//release();		
    }	

	private void showVideoPlayer() {
		//context.startService(intent);
		//stopSelf();
		surfaceView.setVisibility(View.VISIBLE);
		videoControl.setVisibility(View.VISIBLE);	
        mListView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.GONE);
        mNoSdView.setVisibility(View.GONE);
	};
	
	private void refreshNow() {
		Log.d("TAG","refreshNow()");
		getCurrentTimes();
		if(mediaPlayer.isPlaying())
		mDurationTextView.setText(getString(R.string.video_duration, currentHours,currentMinutes,currentSeconds,durationHours,durationMinutes,durationSeconds));		
	}
	
	private void queueNextRefresh(long delay) {  
		if (mediaPlayer.isPlaying()) {  
			Message msg = mHandler.obtainMessage(REFRESH_TIME); // 得到REFRESH消息  
			mHandler.removeMessages(REFRESH_TIME); // 从队列中移除未处理的消息  
			mHandler.sendMessageDelayed(msg, delay); //重新发送REFRESH消息，在接受到REFRESH消息后，又会调用到此处，这样就可以循环更新  
		}  
	}
	
	private void getCurrentTimes() {
		currentHours = mediaPlayer.getCurrentPosition()/(1000*60*60);
		currentMinutes = (mediaPlayer.getCurrentPosition()%(1000*60*60))/(1000*60);
		currentSeconds = (mediaPlayer.getCurrentPosition()%(1000*60))/1000;	
		Log.d("TAG","getCurrentTimes() currentHours = " + currentHours + " currentMinutes = " + currentMinutes + " currentSeconds " + currentSeconds);
	}
	
	private void getDurationTimes() {
		durationHours = mediaPlayer.getDuration()/(1000*60*60);
		durationMinutes = (mediaPlayer.getDuration()%(1000*60*60))/(1000*60);
		durationSeconds = (mediaPlayer.getDuration()%(1000*60))/1000;	
	}

    private void initVideoView() { 
//获取surfaceHolder，控制surfaceview
		surfaceHolder = surfaceView.getHolder();
		//回调，检测surfaceview的三种状态
		surfaceHolder.addCallback(this);
		//surfaceview的显示源类型
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		//mediaplayer初始化
		mediaPlayer = new MediaPlayer();
		//设置不同的监听接口
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnSeekCompleteListener(this);
		mediaPlayer.setOnVideoSizeChangedListener(this);	
		
		// 本地地址和网络地址都可以
		//try {
			//mediaPlayer.setDataSource(this, videoUri);
		//} catch (IllegalArgumentException e) {
			// TODO: handle exception
			//Log.v("panhongyu", e.getMessage());
			//onExit();
		//} catch (IOException e) {
		
		//}		
    } 
	
    private void openVideo() {
		Log.d("TAG","openVideo() videoUri = " + videoUri);
        mediaPlayer = new MediaPlayer(); 
		mediaPlayer.setDisplay(surfaceHolder);// 若无次句，将只有声音而无图像
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnSeekCompleteListener(this);
		mediaPlayer.setOnVideoSizeChangedListener(this);
        try { 
			if(videoUri != null)
            mediaPlayer.setDataSource(this, videoUri); 
        } catch (Exception e) { 
            Log.e(TAG, e.getMessage()); 
            throw new RuntimeException(e); 
        } 
		if(videoUri != null) {
			try {		
				mediaPlayer.prepare(); 
				mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC); 
				mediaPlayer.start();
			} catch (IllegalStateException e) {
			
			} catch (IOException e) {
			
			}
		}
    }	

    private void release() { 
        if (mediaPlayer != null) { 
            mediaPlayer.reset(); 
            mediaPlayer.release(); 
            mediaPlayer = null; 
        } 
    } 
	
    public void resume() { 
        if (surfaceHolder == null) { 
            return; 
        } 
        if (mediaPlayer != null) { 
            return; 
        } 
        openVideo(); 
    } 

	private void initVideoList() {
		videoURIs = new LinkedList<Uri>();
		ContentResolver resolver = this.getContentResolver();
        if (resolver == null) {
            System.out.println("resolver = null");
        } else {
            String whereclause = MediaColumns.DISPLAY_NAME + " != ''";
            /// M: add for chinese sorting
            Cursor cur = resolver.query(Media.EXTERNAL_CONTENT_URI,
                PROJECTION2, null, null,
                ORDER_COLUMN);
            if (cur != null && cur.moveToFirst()) {
                //sub.addSeparator(1, 0);
                while (! cur.isAfterLast()) {
					videoURIs.add(ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI,cur.getLong(0)));
                    cur.moveToNext();
                }
            }
            if (cur != null) {
                cur.close();
            }
        }	
	}
	private void nextVideo() {
		Log.d("panhongyu","nextVideo() videoURIs.size() = " + videoURIs.size() );
		
		if(videoURIs != null && videoURIs.size() != 0) {
			ListIterator<Uri> iterator = videoURIs.listIterator();
			Log.d("panhongyu","nextVideo() iterator.hasNext() = " + iterator.hasNext() );
			while (iterator.hasNext()) {
				Log.d("panhongyu","nextVideo() videoUri = " + videoUri );
				if(iterator.next().equals(videoUri)) {
					Log.d("panhongyu","iterator.next().equals(videoUri) = true ");
					Log.d("panhongyu","nextVideo() iterator.hasNext() = " + iterator.hasNext() );
					if(iterator.hasNext()) {
						setVideoURI(iterator.next());
						release();
						resume();
						break;
					}
				}
			}			
		}
	}	

    public void setVideoURI(Uri uri) { 
		Log.d("TAG","setVideoURI uri = " + uri);
        videoUri = uri; 		
    }
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("TAG","surfaceCreated() mediaPlayer = " + mediaPlayer);
		/*mediaPlayer.setDisplay(holder);// 若无次句，将只有声音而无图像
		try {
			//播放视频
			mediaPlayer.prepare();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.start();

		} catch (IllegalStateException e) {
			//onExit();
		} catch (IOException e) {
		
		}*/
	}
	
	@Override
	public void surfaceDestroyed (SurfaceHolder holder) {
		Log.d("TAG","surfaceDestroyed()");
		//surfaceHolder = null;  		
	}
	
	@Override
	public void surfaceChanged (SurfaceHolder holder, int format, int width, int height){
		Log.d("TAG","surfaceChanged()");
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {		
		Log.d("TAG","onCompletion()");
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {	
		Log.d("TAG","onPrepared()");
		getDurationTimes();
		refreshNow();
		queueNextRefresh(NEXT_REFRESH_DELAY); 
	}
	
	@Override
	public void onSeekComplete(MediaPlayer mp) {
		Log.d("TAG","onSeekComplete()");
	}
	
	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		Log.d("TAG","onVideoSizeChanged()");
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.d("TAG","onError()");
		return true;
	}

    class MovieListAdapter extends SimpleCursorAdapter implements ThumbnailCache.ThumbnailStateListener, OnScrollListener {
        private static final String TAG = "MovieListAdapter";
        private final QueryHandler mQueryHandler;
        private final ArrayList<ViewHolder> mCachedHolder = new ArrayList<ViewHolder>();
        private static final String VALUE_IS_DRM = "1";
        
        QueryHandler getQueryHandler() {
            return mQueryHandler;
        }
        
        public MovieListAdapter(final Context context, final int layout, final Cursor c,
                final String[] from, final int[] to) {
            super(context, layout, c, from, to);
            mQueryHandler = new QueryHandler(getContentResolver());
			Log.d(TAG, "new MovieListAdapter");
        }
        
        @Override
        public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
            final View view = super.newView(context, cursor, parent);
            final ViewHolder holder = new ViewHolder();
            holder.mIcon = (ImageView) view.findViewById(R.id.item_icon);
            holder.mTitleView = (TextView) view.findViewById(R.id.item_title);
            holder.mFileSizeView = (TextView) view.findViewById(R.id.item_date);
            holder.mDurationView = (TextView) view.findViewById(R.id.item_duration);
            int width = mThumbnailCache.getDefaultThumbnailWidth();
            int height = mThumbnailCache.getDefaultThumbnailHeight();
            holder.mFastDrawable = new FastBitmapDrawable(width, height);
            view.setTag(holder);
            mCachedHolder.add(holder);
			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.d("TAG","onClick view = " + v);
					final Object o = v.getTag();
					ViewHolder holder = null;
					if (o instanceof ViewHolder) {
						holder = (ViewHolder) o;
						//final Intent intent = new Intent(context,VideoSmallScreen.class);
						String mime = "video/*";
						if (!(holder.mMimetype == null || "".equals(holder.mMimetype.trim()))) {
							mime = holder.mMimetype;
						}
						//intent.setDataAndType(ContentUris.withAppendedId(VIDEO_URI, holder.mId), mime);
						//intent.putExtra(EXTRA_ALL_VIDEO_FOLDER, true);
						//intent.putExtra(KEY_TREAT_UP_AS_BACK, true);
						//intent.putExtra(EXTRA_ENABLE_VIDEO_LIST, true);
						//startVideoPlayer(context,intent);
						setVideoURI(ContentUris.withAppendedId(VIDEO_URI, holder.mId));
						showVideoPlayer();

						Message msg = mHandler.obtainMessage(SHOW_PLAY);  
						mHandler.sendMessageDelayed(msg, 50); 
						
					}			
				}
			});
            Log.d(TAG, "newView() mCachedHolder.size()=" + mCachedHolder.size());
            return view;
        }
		
        public void onChanged(final long rowId, final int type, final Bitmap drawable) {
            Log.d(TAG, "onChanged(" + rowId + ", " + type + ", " + drawable + ")");
            for (final ViewHolder holder : mCachedHolder) {
                if (holder.mId == rowId) {
                    refreshThumbnail(holder);
                    break;
                }
            }
        }
        
        public void clearCachedHolder() {
            mCachedHolder.clear();
        }
        
        @Override
        public void bindView(final View view, final Context context, final Cursor cursor) {
            final ViewHolder holder = (ViewHolder) view.getTag();
            holder.mId = cursor.getLong(INDEX_ID);
            holder.mTitle = cursor.getString(INDEX_DISPLAY_NAME);
            holder.mDateTaken = cursor.getLong(INDEX_TAKEN_DATE);
            holder.mMimetype = cursor.getString(INDEX_MIME_TYPE);
            holder.mData = cursor.getString(INDEX_DATA);
            holder.mFileSize = cursor.getLong(INDEX_FILE_SIZE);
            holder.mDuration = cursor.getLong(INDEX_DRUATION);
            holder.mIsDrm = VALUE_IS_DRM.equals(cursor.getString(INDEX_IS_DRM));
            holder.mDateModified = cursor.getLong(INDEX_DATE_MODIFIED);
            holder.mSupport3D = MtkUtils.isStereo3D(cursor.getInt(INDEX_SUPPORT_3D));
            
            holder.mTitleView.setText(holder.mTitle);
            holder.mFileSizeView.setText(mCachedVideoInfo.getFileSize(VideoSmallScreen.this, holder.mFileSize));
            holder.mDurationView.setText(mCachedVideoInfo.getDuration(holder.mDuration));
            
            refreshThumbnail(holder);
            Log.d(TAG, "bindeView() " + holder);
        }
        
        private void refreshThumbnail(final ViewHolder holder) {
            Bitmap bitmap = mThumbnailCache.getCachedThumbnail(holder.mId, holder.mDateModified,
                    holder.mSupport3D, !mFling);
            if (MtkUtils.isSupportDrm() && holder.mIsDrm) {
                bitmap = MtkUtils.overlayDrmIcon(VideoSmallScreen.this, holder.mData, OmaDrmStore.Action.PLAY, bitmap);
            }
            holder.mFastDrawable.setBitmap(bitmap);
            holder.mIcon.setImageDrawable(holder.mFastDrawable);
            holder.mIcon.invalidate();
        }
        
        @Override
        public void changeCursor(final Cursor c) {
            super.changeCursor(c);
        }
        
        @Override
        protected void onContentChanged() {
            super.onContentChanged();
            mQueryHandler.onQueryComplete(0, null, getCursor());
        }
        
        class QueryHandler extends AsyncQueryHandler {

            QueryHandler(final ContentResolver cr) {
                super(cr);
            }
            
            @Override
            protected void onQueryComplete(final int token, final Object cookie,
                    final Cursor cursor) {
                Log.d(TAG, "onQueryComplete(" + token + "," + cookie + "," + cursor + ")");
                //MtkUtils.disableSpinnerState(VideoListSmallScreen.this);
                if (cursor == null || cursor.getCount() == 0) {
                    showEmpty();
                    if (cursor != null) { //to observe database change
                        changeCursor(cursor);
                    }
                } else {
                    showList();
                    changeCursor(cursor);
                }
                if (cursor != null) {
                    Log.d(TAG, "onQueryComplete() end");
                }
            }
        }

        @Override
        public void onScroll(final AbsListView view, final int firstVisibleItem,
                final int visibleItemCount, final int totalItemCount) {
            
        }

        private boolean mFling = false;
        @Override
        public void onScrollStateChanged(final AbsListView view, final int scrollState) {
            switch (scrollState) {
            case OnScrollListener.SCROLL_STATE_IDLE:
                mFling = false;
                //notify data changed to load bitmap from mediastore.
                notifyDataSetChanged();
                break;
            case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                mFling = false;
                break;
            case OnScrollListener.SCROLL_STATE_FLING:
                mFling = true;
                break;
            default:
                break;
            }
            Log.d(TAG, "onScrollStateChanged(" + scrollState + ") mFling=" + mFling);
        }
    }	
	
    public class ViewHolder {
        long mId;
        String mTitle;
        String mMimetype;
        String mData;
        long mDuration;
        long mDateTaken;
        long mFileSize;
        boolean mIsDrm;
        long mDateModified;
        boolean mSupport3D;
        ImageView mIcon;
        TextView mTitleView;
        TextView mFileSizeView;
        TextView mDurationView;
        FastBitmapDrawable mFastDrawable;
        
        @Override
        public String toString() {
            return new StringBuilder()
                    .append("ViewHolder(mId=")
                    .append(mId)
                    .append(", mTitle=")
                    .append(mTitle)
                    .append(", mDuration=")
                    .append(mDuration)
                    .append(", mIsDrm=")
                    .append(mIsDrm)
                    .append(", mData=")
                    .append(mData)
                    .append(", mFileSize=")
                    .append(mFileSize)
                    .append(", mSupport3D=")
                    .append(mSupport3D)
                    .append(")")
                    .toString();
        }
        
        /**
         * just clone info
         */
        @Override
        protected ViewHolder clone() {
            final ViewHolder holder = new ViewHolder();
            holder.mId = mId;
            holder.mTitle = mTitle;
            holder.mMimetype = mMimetype;
            holder.mData = mData;
            holder.mDuration = mDuration;
            holder.mDateTaken = mDateTaken;
            holder.mFileSize = mFileSize;
            holder.mIsDrm = mIsDrm;
            holder.mDateModified = mDateModified;
            holder.mSupport3D = mSupport3D;
            return holder;
        }
    }	

    private void registerStorageListener() {
        final IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        iFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        iFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        iFilter.addDataScheme("file");
        registerReceiver(mStorageListener, iFilter);
    }
	
    private final BroadcastReceiver mStorageListener = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.d(TAG, "mStorageListener.onReceive(" + intent + ")");
            final String action = intent.getAction();
            if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)) {
                refreshSdStatus(MtkUtils.isMediaMounted(VideoSmallScreen.this));
            } else if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
                refreshSdStatus(MtkUtils.isMediaMounted(VideoSmallScreen.this));
            } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action) || Intent.ACTION_MEDIA_EJECT.equals(action)) {
                if (intent.hasExtra(StorageVolume.EXTRA_STORAGE_VOLUME)) {
                    final StorageVolume storage = (StorageVolume)intent.getParcelableExtra(
                            StorageVolume.EXTRA_STORAGE_VOLUME);
                    if (storage != null && storage.getPath().equals(sExternalStoragePaths[0])) {
                        refreshSdStatus(false);
                        mAdapter.changeCursor(null);
                    } // else contentObserver will listen it.
                    Log.d(TAG, "mStorageListener.onReceive() eject storage="
                            + (storage == null ? "null" : storage.getPath()));
                }
            }
        };

    };	
	
    private void showScanningProgress() {
        showProgress(getString(R.string.scanning), new OnCancelListener() {

            @Override
            public void onCancel(final DialogInterface dialog) {
                Log.d(TAG, "mProgressDialog.onCancel()");
                hideScanningProgress();
                stopSelf();
            }

        });
    }
    
    private void hideScanningProgress() {
        hideProgress();
    }

    private void showProgress(final String message, final OnCancelListener cancelListener) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setCancelable(cancelListener != null);
            mProgressDialog.setOnCancelListener(cancelListener);
            mProgressDialog.setMessage(message);
        }
        // Show the dialog when get focused
        if (mIsFocused) {
            mProgressDialog.show();
        }
    }
    
    private void hideProgress() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }
	
    private void refreshMovieList() {
        mAdapter.getQueryHandler().removeCallbacks(null);
        mAdapter.getQueryHandler().startQuery(0, null,
                VIDEO_URI,
                PROJECTION,
                null,
                null,
                ORDER_COLUMN);
    }	
    
	
}