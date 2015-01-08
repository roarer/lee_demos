package com.eastaeon.floatapp.music;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
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
import android.view.ViewGroup;
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
import java.util.Locale;
import com.mediatek.drm.OmaDrmStore;
import android.content.BroadcastReceiver;  
import android.os.storage.StorageVolume;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore;
import android.os.storage.StorageManager;
import android.provider.MediaStore.Audio.Media;
import android.app.ProgressDialog;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.content.ContentUris;

import android.content.IntentFilter;
import com.eastaeon.floatapp.videoplayer.MtkUtils;
import android.widget.RelativeLayout;
import android.media.MediaPlayer;
import android.media.AudioManager;
import java.io.IOException;
import com.eastaeon.floatapp.music.lrc.*;
import java.util.ArrayList;
import android.view.animation.AnimationUtils;
import java.util.regex.Pattern;
import java.util.List;
import android.os.Handler;

public class MusicSmallScreen extends Service implements OnClickListener, OnItemClickListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, MediaPlayer.OnDurationUpdateListener 
{
	private static final String TAG = "MusicSmallScreen";
	
	//悬浮窗基本属性
    private Display mDisplay;
	private LayoutInflater inflater;
	private Button minimizeButton;
	private Button listButton;
	private TextView titleButton;
	RelativeLayout mMainLayout;
	//定义浮动窗口布局
	RelativeLayout mTitleLyout;
    FrameLayout mFloatLayout;
    WindowManager.LayoutParams wmParams;
    //创建浮动窗口设置布局参数的对象
	WindowManager mWindowManager;
	private int X, Y;
		
	//视频列表基本属性
	private ListView mListView;
    private TextView mEmptyView;
	private ViewGroup mNoSdView;
	private RelativeLayout mMusicPlayView;
	private Button mMusicPlayPrevious;
	private Button mMusicPlayPause;
	private Button mMusicPlayNext;
	private TextView mMusicName;
	
	private LrcView mLrcShowView;
	private TextView mNoLrcView;
	private LrcProcess mLrcProcess;
	private List<LrcContent> lrcList = new ArrayList<LrcContent>();
	Handler handler = new Handler();
	private int index = 0;	
	
	private MusicListAdapter mAdapter;
	private static String[] sExternalStoragePaths;
	private ProgressDialog mProgressDialog;
	private boolean mIsFocused = false;
	private MediaPlayer mCurrentPlayer;
	private MediaPlayer mNextPlayer;
	private ArrayList<ViewHolder> musicList;
	private boolean isPlayScreen = false;
	private boolean isErrorMusic = false;
	
	private ViewHolder currentMusic;
	private static final Uri AUDIO_URI = Media.EXTERNAL_CONTENT_URI;
    private static final String ORDER_COLUMN = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
		
    private static final String KEY_LOGO_BITMAP = "logo-bitmap";
    private static final String KEY_TREAT_UP_AS_BACK = "treat-up-as-back";
	private static final String EXTRA_ALL_VIDEO_FOLDER = "mediatek.intent.extra.ALL_VIDEO_FOLDER";
    private static final String EXTRA_ENABLE_VIDEO_LIST = "mediatek.intent.extra.ENABLE_VIDEO_LIST";	
	private static final String OPEN_VIDEO_PLAYER = "action.eastaeon.open.videoplayer";	
	
	HallLockStateReceiver hallLockStateReceiver;
	
	@Override
	public void onCreate() 
	{
		// TODO Auto-generated method stub
		super.onCreate();
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
		wmParams.type = LayoutParams.TYPE_SYSTEM_ERROR; //LayoutParams.TYPE_SYSTEM_OVERLAY;
		//设置图片格式，效果为背景透明
        wmParams.format = PixelFormat.RGBA_8888; 
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE|LayoutParams.FLAG_NOT_TOUCH_MODAL;      // 
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
        wmParams.width = Math.min(screenWidth,screenHeight)*2/3;
        wmParams.height = (Math.min(screenWidth,screenHeight)*2/3)*4/5;
   
        inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
		mMainLayout = (RelativeLayout) inflater.inflate(R.layout.music_float_screen, null); 
		mTitleLyout = (RelativeLayout)mMainLayout.findViewById(R.id.title_layout);
		mFloatLayout = (FrameLayout)mMainLayout.findViewById(R.id.float_layout);
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
		unregisterReceiver(mStorageListener);
		handler.removeCallbacks(mRunnable);
		
		this.unregisterReceiver(hallLockStateReceiver);
		
		if(mCurrentPlayer != null) {
			mCurrentPlayer.stop();
		}
		
		if(mMainLayout != null)
		{
			//移除悬浮窗口
			mWindowManager.removeView(mMainLayout);
		}
		
	}	
	

 

	
	@Override
	public void onClick(View v) {
		Log.d("TAG","onClick() v = "+v);
		if(v.equals(minimizeButton)){
			sendBroadcast(new Intent("eastaeon.intent.action.float.OPEN_FLOAT"));
			stopSelf();
		} else if(v.equals(mMusicPlayPrevious)) {
			playNextMusic(false);
		} else if(v.equals(mMusicPlayPause)) {
			if(mCurrentPlayer.isPlaying()){
				pause();
				mMusicPlayPause.setBackgroundResource(R.drawable.music_play);
			}else {
				start();
				mMusicPlayPause.setBackgroundResource(R.drawable.music_pause);
			}
		} else if(v.equals(mMusicPlayNext)) {
			playNextMusic(true);
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
		Log.d("TAG","initList()");
		
		mListView = (ListView) mFloatLayout.findViewById(R.id.list);
		mEmptyView = (TextView) mFloatLayout.findViewById(R.id.empty);
		mNoSdView = (ViewGroup) mFloatLayout.findViewById(R.id.no_sdcard);
		
		mMusicPlayView = (RelativeLayout) mFloatLayout.findViewById(R.id.music_play_view);
		mMusicPlayPrevious = (Button) mMusicPlayView.findViewById(R.id.music_previous);
		mMusicPlayPause = (Button) mMusicPlayView.findViewById(R.id.music_play);
		mMusicPlayNext = (Button) mMusicPlayView.findViewById(R.id.music_next);
		mMusicName = (TextView) mMusicPlayView.findViewById(R.id.music_name);
		mLrcShowView = (LrcView) mMusicPlayView.findViewById(R.id.music_lrc);
		mNoLrcView = (TextView) mMusicPlayView.findViewById(R.id.music_no_lrc);
		
		mMusicPlayPrevious.setOnClickListener(this);
		mMusicPlayPause.setOnClickListener(this);
		mMusicPlayNext.setOnClickListener(this);
		
		final StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
		sExternalStoragePaths = storageManager.getVolumePaths();
		
		mAdapter = new MusicListAdapter(this, R.layout.music_list_item, null, new String[]{}, new int[]{});
        mListView.setAdapter(mAdapter);
        mListView.setOnScrollListener(mAdapter);
		mListView.setOnItemClickListener(this);
		initMusicList();
		registerStorageListener();
		refreshSdStatus(MtkUtils.isMediaMounted(this));
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
                refreshMusicList();
				initMusicList();
                //MtkUtils.enableSpinnerState(this);
            }
        } else {
            hideScanningProgress();
            showSdcardLost();
            //MtkUtils.disableSpinnerState(this);
        }
    }
	private void showLrc(boolean bool) {
		if(bool){
			mLrcShowView.setVisibility(View.VISIBLE);
			mNoLrcView.setVisibility(View.GONE);
		} else {
			mLrcShowView.setVisibility(View.GONE);
			mNoLrcView.setVisibility(View.VISIBLE);		
			if(isErrorMusic) {
				mNoLrcView.setText(R.string.music_error);
			} else {
				mNoLrcView.setText(R.string.music_no_lrc);
			}
		}
	}

	private void showPlay() {
		mMusicPlayView.setVisibility(View.VISIBLE);
		mListView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.GONE);
		mNoSdView.setVisibility(View.GONE);
		isPlayScreen = true;
		//listButton.setVisibility(View.VISIBLE);
	}
	
    private void showList() {
        mListView.setVisibility(View.VISIBLE);
        mEmptyView.setVisibility(View.GONE);
		mNoSdView.setVisibility(View.GONE);
		mMusicPlayView.setVisibility(View.GONE);
		isPlayScreen = false;
		//listButton.setVisibility(View.INVISIBLE);
    }
    
    private void showEmpty() {
        mListView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.VISIBLE);
		mNoSdView.setVisibility(View.GONE);
		mMusicPlayView.setVisibility(View.GONE);
		isPlayScreen = false;
		//listButton.setVisibility(View.INVISIBLE);
    }
	
    private void showSdcardLost() {
        mListView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.GONE);
        mNoSdView.setVisibility(View.VISIBLE);
		mMusicPlayView.setVisibility(View.GONE);
		isPlayScreen = false;
		//listButton.setVisibility(View.INVISIBLE);
    }	
	
    class MusicListAdapter extends SimpleCursorAdapter implements OnScrollListener {
        private static final String TAG = "MusicListAdapter";
        private final QueryHandler mQueryHandler;
        private final ArrayList<ViewHolder> mCachedHolder = new ArrayList<ViewHolder>();
        private static final String VALUE_IS_DRM = "1";
        
        QueryHandler getQueryHandler() {
            return mQueryHandler;
        }
        
        public MusicListAdapter(final Context context, final int layout, final Cursor c,
                final String[] from, final int[] to) {
            super(context, layout, c, from, to);
            mQueryHandler = new QueryHandler(getContentResolver());
			Log.d(TAG, "new MusicListAdapter");
        }
        
        @Override
        public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
            final View view = super.newView(context, cursor, parent);
            final ViewHolder holder = new ViewHolder();
            
            holder.mTitleView = (TextView) view.findViewById(R.id.item_title);
            holder.mSingerView = (TextView) view.findViewById(R.id.item_singer);
            holder.mDurationView = (TextView) view.findViewById(R.id.item_duration);
            view.setTag(holder);
            mCachedHolder.add(holder);
            Log.d(TAG, "newView() mCachedHolder.size()=" + mCachedHolder.size());
            return view;
        }
		
        public void onChanged(final long rowId, final int type, final Bitmap drawable) {
            Log.d(TAG, "onChanged(" + rowId + ", " + type + ", " + drawable + ")");
            for (final ViewHolder holder : mCachedHolder) {
                if (holder.mId == rowId) {
                    
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
            holder.mId = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
            holder.mTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            holder.mSinger = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            holder.mDuration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)); 
			Log.d("panhongyu","holder.mSinger = "+holder.mSinger+" holder.mSingerView = "+holder.mSingerView);
            holder.mTitleView.setText(holder.mTitle);
            holder.mSingerView.setText(holder.mSinger);
            holder.mDurationView.setText(toTime(holder.mDuration));
			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.d("TAG","onClick view = " + v);
					final Object o = v.getTag();
					ViewHolder holder = null;
					if (o instanceof ViewHolder) {
						holder = (ViewHolder) o;
						if(currentMusic!=null&&currentMusic.mId == holder.mId) {
							showPlay();
						} else {
							currentMusic = holder;
							showPlay();
							next(ContentUris.withAppendedId(AUDIO_URI, holder.mId));
							refreshMusicName();	
						}					
					}			
				}
			});
            
            Log.d(TAG, "bindeView() " + holder);
        }
        
		public String toTime(long time) {  
			time /= 1000;  
			long minute = time / 60;  
			long hour = minute / 60;  
			long second = time % 60;  
			minute %= 60;  
			return String.format("%02d:%02d", minute, second);  
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
        int mId;
        String mTitle;
		String mSinger;
        long mDuration;

        TextView mTitleView;
        TextView mSingerView;
        TextView mDurationView;
        
        @Override
        public String toString() {
            return new StringBuilder()
                    .append("ViewHolder(mId=")
                    .append(mId)
                    .append(", mTitle=")
                    .append(mTitle)
                    .append(", mDuration=")
                    .append(mDuration)
                    .append(", mSinger=")
                    .append(mSinger)
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
            holder.mSinger = mSinger;
            holder.mDuration = mDuration;
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
                refreshSdStatus(MtkUtils.isMediaMounted(MusicSmallScreen.this));
            } else if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
                refreshSdStatus(MtkUtils.isMediaMounted(MusicSmallScreen.this));
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
	
    private void refreshMusicList() {
        mAdapter.getQueryHandler().removeCallbacks(null);
        mAdapter.getQueryHandler().startQuery(0, null,
                AUDIO_URI,
                null,
                Media.IS_MUSIC + "=1",
                null,
                ORDER_COLUMN);
    }	
	
    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		Log.d("TAG", "onItemClick(" + position + ", " + id + ") ");
        final Object o = view.getTag();
        ViewHolder holder = null;
        if (o instanceof ViewHolder) {
            holder = (ViewHolder) o;
			
			currentMusic = holder;
			showPlay();
			next(ContentUris.withAppendedId(AUDIO_URI, holder.mId));
			refreshMusicName();
        }
        Log.d("TAG", "onItemClick(" + position + ", " + id + ") holder=" + holder);
    }
	
	private MediaPlayer initMediaPlayer(Uri uri) {
		MediaPlayer mediaPlayer = new MediaPlayer();
		//设置不同的监听接口
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnInfoListener(this);
		mediaPlayer.setOnDurationUpdateListener(this);	
        try { 
			if(uri != null) {
				mediaPlayer.setDataSource(this, uri); 	
			} else {
				Log.e(TAG, "uri == null"); 
			}
        } catch (Exception e) { 
            Log.e(TAG, e.getMessage()); 
            throw new RuntimeException(e); 
        } 
		try {
			mediaPlayer.prepare(); 
			isErrorMusic = false;
		} catch (IOException e) {
            Log.e(TAG, e.getMessage());
			Log.d("panhongyu", "mediaPlayer.prepare() fail"); 		
			isErrorMusic = true;			
            //throw new RuntimeException(e); 		
		}
 		
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC); 
		return mediaPlayer;
	}
	
	public void stop() {
		if(mCurrentPlayer!=null)
		mCurrentPlayer.stop();
	}
	
	public void start() {
		if(mCurrentPlayer!=null){
			mCurrentPlayer.start();
			handler.removeCallbacks(mRunnable);
			updateLrcStatus();
		}
	}

	public void pause() {
		if(mCurrentPlayer!=null)
		mCurrentPlayer.pause();
	}

	public void next(Uri uri) {
		Log.d("panhongyu", "next() uri = " + uri+" mCurrentPlayer = " +mCurrentPlayer);
		if(mCurrentPlayer!=null) {
			mCurrentPlayer.stop();
			mCurrentPlayer = null;
			mCurrentPlayer = initMediaPlayer(uri);
			start();
		} else {
			mCurrentPlayer = initMediaPlayer(uri);
			start();
		}
	}	

	private void initMusicList() {
		if(musicList!=null) {
			musicList.clear();
		} else {
			musicList = new ArrayList<ViewHolder>();
		}
		
		ContentResolver resolver = this.getContentResolver();
        if (resolver == null) {
            System.out.println("resolver = null");
        } else {
            
            /// M: add for chinese sorting
            Cursor cur = resolver.query(AUDIO_URI,
                null, Media.IS_MUSIC + "=1", null,
                ORDER_COLUMN);
            if (cur != null && cur.moveToFirst()) {
                while (! cur.isAfterLast()) {
					ViewHolder mViewHolder= new ViewHolder();
					mViewHolder.mId = cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media._ID));
					mViewHolder.mTitle = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE));
					musicList.add(mViewHolder);
					cur.moveToNext();
                }
            }
            if (cur != null) {
                cur.close();
            }
        }	
	}
	
	private void refreshMusicName() {
		mMusicName.setText(currentMusic.mTitle);
	}
	
	private void playNextMusic(boolean next) {
		    for (int i = 0;i < musicList.size();i++) {
			Log.d("panhongyu","musicList.size() = " + musicList.size());
			Log.d("panhongyu","currentMusic.mId = " + currentMusic.mId);
			Log.d("panhongyu","musicList.get("+i+").mId = " + musicList.get(i).mId);
                if (musicList.get(i).mId == currentMusic.mId) {
                    if(next) {
						if(i!=musicList.size()-1) {
							currentMusic = musicList.get(i+1);
						} else {
							currentMusic = musicList.get(0);
						}
						
					} else {
						if(i!=0) {
							currentMusic = musicList.get(i-1);
						} else {
							currentMusic = musicList.get(musicList.size()-1);
						}
					}
					next(ContentUris.withAppendedId(AUDIO_URI,currentMusic.mId));
					refreshMusicName();
                    break;
                }
            }
	}
	@Override
	public void onCompletion(MediaPlayer mp) {		
		Log.d("TAG","onCompletion()");
		playNextMusic(true);
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.d("TAG","onError()");
		return true;
	}
	
	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		Log.d("TAG","onInfo()");
		return true;
	}

	@Override
	public void onDurationUpdate(MediaPlayer mediaPlayer, int duration) {
		Log.d("TAG","onDurationUpdate()");
	}	
	
	public int lrcIndex(){
		long currentTime = 0,duration = 0;

			//if(mService.isPlaying()){
				currentTime = mCurrentPlayer.getCurrentPosition();
				duration = mCurrentPlayer.getDuration();
			//}
			if(currentTime < duration){
				for(int i = 0; i< lrcList.size(); i++) {
					if(i < lrcList.size() - 1){
						if(currentTime < lrcList.get(i).getLrcTime() && i == 0){
							index = i;
						}
						if(currentTime > lrcList.get(i).getLrcTime() &&
							currentTime < lrcList.get(i + 1).getLrcTime()){
								index = i;
							}
					}
					if(i == lrcList.size() - 1 &&
						currentTime > lrcList.get(i).getLrcTime()){
							index = i;
						}
				}
				if(lrcList.size() == 0) {
					index = -1;
				}
			}
		
		return index;
	}
	
	private String updateLrcInfo(){
		String ret = "error";
			
		String filePath = getRealPathFromURI(getApplicationContext(),ContentUris.withAppendedId(AUDIO_URI,currentMusic.mId));
		Log.i("panhongyu","filePath = " + filePath);
		
		mLrcProcess = new LrcProcess();
		ret = mLrcProcess.readLRC(filePath);
		if(!ret.equals("ok")){
			Log.i("panhongyu","!ret != ok ret = " + ret);
			return ret;
		}
		lrcList = mLrcProcess.getLrcList();
		//mLrcShowView.setVisibility(View.VISIBLE);
		mLrcShowView.setmLrcList(lrcList);
		mLrcShowView.setAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.alpha));
		handler.post(mRunnable);
			
		Log.i("panhongyu","updateLrcInfo ret = " + ret);
		return ret;
	}
	
	public String getRealPathFromURI(Context context, Uri contentUri) {
		Cursor cursor = null;
		try { 
			String[] proj = { MediaStore.Images.Media.DATA };
			cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
	
	Runnable mRunnable = new Runnable(){
		public void run(){
			mLrcShowView.setIndex(lrcIndex());
			mLrcShowView.invalidate();
			handler.postDelayed(mRunnable,200);
		}
	};
	
	public void updateLrcStatus(){	
		String ret = updateLrcInfo();
		if(ret.equals("none")||ret.equals("error")){
			showLrc(false);
		} else {
			showLrc(true);
		}
		
	}	
}