package com.eastaeon.floatapp.music.lrc;

import java.util.ArrayList;  
import java.util.List;  
import android.content.Context;  
import android.graphics.Canvas;  
import android.graphics.Color;  
import android.graphics.Paint;  
import android.graphics.Typeface;  
import android.util.AttributeSet;  
import android.util.Log;
import android.view.WindowManager;
import android.view.Display;

public class LrcView extends android.widget.TextView {
    private float width;//歌词视图宽度
    private float height;//歌词视图高度
    private Paint currentPaint;//当前画笔对象
    private Paint notCurrentPaint;//非当前画笔对象
    private float textHeight = 45;//文本高度
    private float textSize = 35;//文本大小
    private int index = -1;//list集合下标
    private int lastIndex = 0;
	private float x = 0f; 
	private String direct = "left";
	private final String M_TAG = "huanyu";
	
	private Context mContext;
	private Display mDisplay;
	private WindowManager mWindowManager;	
	private int screenWidth;
	private int screenHeight;
	
    private List<LrcContent> mLrcList = new ArrayList<LrcContent>();
    public void setmLrcList(List<LrcContent> mLrcList) {
        this.mLrcList = mLrcList;
    }
    public LrcView(Context context) {
        super(context);
		mContext = context;
        init();
    }
    public LrcView(Context context, AttributeSet attrs, int defStyle) {	
        super(context, attrs, defStyle);
		mContext = context;
        init();
    }

    public LrcView(Context context, AttributeSet attrs) {
        super(context, attrs);
		mContext = context;
        init();
    }

    private void init() {
        setFocusable(true);     //设置可对焦
		Log.i(M_TAG,"LrcView init()");
		
		mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
		mDisplay = mWindowManager.getDefaultDisplay();
		mDisplay = mWindowManager.getDefaultDisplay();
		screenWidth = mDisplay.getWidth();    
        screenHeight = mDisplay.getHeight();   
		Log.d("panhongyu","screenWidth = "+screenWidth+", screenHeight = "+screenHeight);
		if(Math.min(screenWidth,screenHeight) == 1080){
			textHeight = 45;
			textSize = 35;		
		} else if(Math.min(screenWidth,screenHeight) == 720) {
			textHeight = 30;
			textSize = 24;
		}

	
        //高亮部分
        currentPaint = new Paint();
        currentPaint.setAntiAlias(true);    //设置抗锯齿，让文字美观饱满
        currentPaint.setTextAlign(Paint.Align.CENTER);//设置文本对齐方式

        //非高亮部分
        notCurrentPaint = new Paint();
        notCurrentPaint.setAntiAlias(true);
        notCurrentPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (canvas == null) {
            return;
        }
		if(index == -1) {
			return;
		}
		Log.i(M_TAG,"LrcView onDraw()");
        currentPaint.setColor(Color.rgb(115, 150, 255));
        notCurrentPaint.setColor(Color.rgb(140, 255, 255));
		if(Math.min(screenWidth,screenHeight) == 1080){
			currentPaint.setTextSize(45);	
		} else if(Math.min(screenWidth,screenHeight) == 720) {
			currentPaint.setTextSize(30);
		} else {
			currentPaint.setTextSize(45);
		}
        //currentPaint.setTextSize(45);
        currentPaint.setTypeface(Typeface.SERIF);

        notCurrentPaint.setTextSize(textSize);
        notCurrentPaint.setTypeface(Typeface.DEFAULT);
		
		String currentString = mLrcList.get(index).getLrcStr();
		float currentWidth = currentPaint.measureText(currentString);
		Log.i(M_TAG,"current text currentWidth = " + currentWidth + "\n  width = " + width + "\n x = " + x);
		
		if(currentWidth > width+10){
			currentPaint.setTextAlign(Paint.Align.LEFT);
			canvas.drawText(currentString,0,currentString.length(), x, height / 2, currentPaint);
			if(lastIndex == index){
				if(direct.equals("left")){
					if(-x < currentWidth - width){
						x-=3f;
					}else{
						direct = "right";
					}
					
				}else{
					if(x < 0){
						x+=3f;
					}else{
						direct = "left";
					}
					
				}

			}else{
				x = 0f;
				direct = "left";
			}
			lastIndex = index;
		}else{
			currentPaint.setTextAlign(Paint.Align.CENTER);
			canvas.drawText(currentString, width / 2, height / 2, currentPaint);
		}
		
		
		
		float tempY = height / 2;
		//画出本句之前的句子
		for (int i = index - 1; i >= 0; i--) {
			tempY = tempY - textHeight;
			notCurrentPaint.setAlpha(255-(index-i)*30);
			canvas.drawText(mLrcList.get(i).getLrcStr(), width / 2, tempY, notCurrentPaint);
		}
		tempY = height / 2;
		//画出本句之后的句子
		for (int i = index + 1; i < mLrcList.size(); i++) {
			tempY = tempY + textHeight;
			notCurrentPaint.setAlpha(255-(i-index)*30);
			canvas.drawText(mLrcList.get(i).getLrcStr(), width / 2, tempY, notCurrentPaint);
		}
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
		Log.i(M_TAG,"LrcView onSizeChanged()");
        this.width = w;
        this.height = h;
    }

    public void setIndex(int index) {
		Log.i(M_TAG,"LrcView setIndex = " + index);
        this.index = index;
    }
}
