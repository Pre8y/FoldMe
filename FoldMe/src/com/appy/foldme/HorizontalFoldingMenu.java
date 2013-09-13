package com.appy.foldme;

import com.appy.foldme.FoldingMenu.MoveState;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

public class HorizontalFoldingMenu extends FrameLayout implements GestureDetector.OnGestureListener{

    // TEST GIT
    public static final String H_DEBUG_TAG = "FoldMe";
    private static final int H_FLING_THRESHOLD = 100;
    private static final int H_FLING_VELOCITY_THRESHOLD = 100;

    private GestureDetector mDetector;


    public static int H_TOTAL;
    public static boolean H_UNIFORMITY;
    public static int H_DIRECTION;
    public static Bitmap H_bmp, H_extbmp;

    private HorizontalMySurfaceView mGLView;
    private View inView = null;
    private View outView = null;
    private View capturedView;

    private int scrollcounter = 0;
    private float c;
    private float lastangle;
    private float calculatedAngle;

    boolean allowScroll = false;
    boolean allowFling = false;
    enum MoveState { MS_NONE, MS_HSCROLL, MS_VSCROLL };
    private MoveState moveState = MoveState.MS_NONE;

    public HorizontalFoldingMenu (Context context){
        super(context);
    }

    public HorizontalFoldingMenu (Context context, AttributeSet attrs) {

        super(context,attrs);
        init(context,attrs);
        mGLView = new HorizontalMySurfaceView(context);
        addView(mGLView,LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);//new..glview contains screenshots of both inview and outview
//        Log.d(DEBUG_TAG, "CONSTRUCTOR");
    }

    public HorizontalFoldingMenu (Context context, AttributeSet attrs, int defStyle) {
        super(context,attrs,defStyle);
    }

    private void init(Context context, AttributeSet attrs){
        mDetector = new GestureDetector(context,this);
        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.settings);
        H_TOTAL = a.getInteger(R.styleable.settings_totalfolds,2);
        H_UNIFORMITY = a.getBoolean(R.styleable.settings_uniform,false);
        H_DIRECTION = a.getInteger(R.styleable.settings_direction,-1);
        //Implement input checks
        c = (H_UNIFORMITY)? 1 : 1 - ((float) 1/ H_TOTAL);
        lastangle = 90 / (float) Math.pow(c,((H_TOTAL-1)/2));
        //Folds OPEN/CLOSED at start?
        calculatedAngle = lastangle;
        a.recycle();
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (inView == null){
//            Log.d(DEBUG_TAG, "INVIEW NILL");
            inView = findViewById(R.id.intView);
            capturedView = findViewById(R.id.ViewToCapture);
            capturedView.setDrawingCacheEnabled(true);
            H_bmp = Utility.takeScreenshot(capturedView, Bitmap.Config.ARGB_8888,0);
        };
        if (outView == null){
//            Log.d(DEBUG_TAG, "OUTVIEW NILL");
            outView = findViewById(R.id.outView);
            outView.setDrawingCacheEnabled(true);
            H_extbmp = Utility.takeScreenshot(outView, Bitmap.Config.ARGB_8888,0);
        }


        if(H_DIRECTION == 1){
            findViewById(R.id.GapView1).setVisibility(GONE);
        } else {
            findViewById(R.id.GapView2).setVisibility(GONE);
        }


//        Log.d(DEBUG_TAG, "ONLAYOUT");
//        Log.d(DEBUG_TAG, "INVIEW WIDTH "+capturedView.getWidth());
//        Log.d(DEBUG_TAG, "GAP1 WIDTH "+findViewById(R.id.GapView1).getWidth());
//        Log.d(DEBUG_TAG, "GAP2 WIDTH "+findViewById(R.id.GapView2).getWidth());
    }

    @Override
    protected void onMeasure(int a,int b){
        super.onMeasure(a,b);
    }

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private boolean isSetTexture = false;


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){

//        Log.d(DEBUG_TAG, "onTouchEvent");
//        Log.d(DEBUG_TAG, "Moving State Is : "+moveState);
//        Log.d(DEBUG_TAG, "Scroll Is : "+ capturedView.getScrollY());
//        Log.d(DEBUG_TAG, "MAX Scroll Is : "+ ((ScrollView)capturedView).getMaxScrollAmount());
//        Log.d(DEBUG_TAG, "Capture View Height Is : "+ capturedView.getHeight());
//        Log.d(DEBUG_TAG, "Img View Height Is : "+ findViewById(R.id.img).getHeight());

        // Be sure to call the superclass implementation
        super.onTouchEvent(event);
        mDetector.onTouchEvent(event); // pass the event for gesture handling (fling, scroll...).

        if(calculatedAngle == 0 && moveState!=MoveState.MS_HSCROLL){
            if (allowScroll) inView.dispatchTouchEvent(event); // Pass event to Inview's childs.
        }

        if (event.getAction()==MotionEvent.ACTION_UP) {
            moveState = MoveState.MS_NONE;

            if (calculatedAngle == 0) {
                inView.setVisibility(VISIBLE);
                allowScroll = true;
            }
            if (calculatedAngle!=0 && calculatedAngle!=lastangle){
                allowFling = true;
                Fling(0,calculatedAngle,true);
            }
            if (calculatedAngle == lastangle) {
                outView.setVisibility(VISIBLE);
            }

            scrollcounter = 0;

        }

        return true;

    }

    @Override
    public boolean onDown(MotionEvent event) {
        Log.d(H_DEBUG_TAG,"onDown");

//        Bitmap newbmp=Utility.takeScreenshot(capturedView, Bitmap.Config.ARGB_8888,capturedView.getScrollY());
//        mGLView.setTexture(extbmp, GLES20.GL_TEXTURE0,0);
//        mGLView.requestRender();
        return true;
    }


    @Override
    public void onLongPress(MotionEvent event) {
//        Log.d(DEBUG_TAG, "onLongPress");
    }


    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
//        Log.d(DEBUG_TAG, "SCROLLING");

        if (moveState != MoveState.MS_VSCROLL && Math.abs(distanceX) > Math.abs(distanceY)){
            if((H_DIRECTION * distanceX<0 && outView.getVisibility()==VISIBLE)||(H_DIRECTION * distanceX>0 && inView.getVisibility()==VISIBLE)){ // Set visibility (invisible) only when the scroll is in the right direction
                Log.d(H_DEBUG_TAG,"HIT");
                inView.setVisibility(INVISIBLE);
                outView.setVisibility(INVISIBLE);
            }
            ++scrollcounter;
            moveState = MoveState.MS_HSCROLL;
            allowScroll = false;
            if(scrollcounter>1) { // this counter is a workaround to fix the fast fling glitch
                allowFling = true;
                calculatedAngle += - FoldingMenu.DIRECTION * (-distanceX/3) * TOUCH_SCALE_FACTOR;  // = 180.0f / 320
                calculatedAngle = Math.max(0,calculatedAngle);
                calculatedAngle = Math.min(calculatedAngle,lastangle);

                mGLView.getRenderer().mAngle = calculatedAngle;
                mGLView.requestRender();
            }

        }

        if (moveState != MoveState.MS_HSCROLL && calculatedAngle == 0 && Math.abs(distanceX) < Math.abs(distanceY)){
            moveState = MoveState.MS_VSCROLL;
            allowScroll = true; //allow scrolling after the inView is wide open.
            allowFling = false;

        }
        return true;

    }


    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
//        Log.d(DEBUG_TAG, "FlINGING");
        float diffX = event2.getX() - event1.getX();
        if (Math.abs(diffX) > H_FLING_THRESHOLD && Math.abs(velocityX) > H_FLING_VELOCITY_THRESHOLD) {
            Fling(diffX, calculatedAngle,false);

        }

        return true;

    }


    public void Fling(float diffx, float startingAngle, boolean attachToLimits) {
        if (allowFling){
            if (diffx * FoldingMenu.DIRECTION > 0 || (attachToLimits && calculatedAngle < 0.5 * lastangle)){ // Fling A
//                Log.d(DEBUG_TAG, "Fling A");
                float iterator = (float)((Math.log(lastangle+0.05)-Math.log(startingAngle+0.05))/(0.015 * Math.log(2))); // Figuring out at what iteration the fling started..this is just the reciprocal function of calculatedAngle applied to startingAngle.
                while (calculatedAngle > 0){
                    iterator += 0.01f;
                    calculatedAngle = (float) ((lastangle+0.05) * (Math.pow(2,-0.015* iterator)) - 0.05); // Exponential Ease Out (Slightly Modified).
                    calculatedAngle = Math.max(0,calculatedAngle);
                    mGLView.getRenderer().mAngle = calculatedAngle;
                    mGLView.requestRender();
                }

                inView.setVisibility(VISIBLE);
                allowFling = false;

            }
            if (diffx * FoldingMenu.DIRECTION < 0 || (attachToLimits && calculatedAngle >= 0.5 * lastangle)){ // Fling B
//                Log.d(DEBUG_TAG, "Fling B");
                float iterator = (float)((Math.log(lastangle+0.05)-Math.log(lastangle+0.05-startingAngle))/(0.015 * Math.log(2))); // Figuring out at what iteration the fling started..this is just the reciprocal function of calculatedAngle applied to startingAngle.
                while (calculatedAngle < lastangle){
                    iterator += 0.01f;
                    calculatedAngle = (float) ((lastangle+0.05) * (-Math.pow(2,-0.015* iterator)+1)); // Exponential Ease Out (Slightly Modified) and inverted.
                    calculatedAngle = Math.min(calculatedAngle,lastangle);
                    mGLView.getRenderer().mAngle = calculatedAngle;
                    mGLView.requestRender();
                }
                outView.setVisibility(VISIBLE);
                allowFling = false;
            }
        }
    }


    @Override
    public void onShowPress(MotionEvent event) {
//        Log.d(DEBUG_TAG, "onShowPress");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
//        Log.d(DEBUG_TAG, "onSingleTapUp" );
        return true;
    }



}
