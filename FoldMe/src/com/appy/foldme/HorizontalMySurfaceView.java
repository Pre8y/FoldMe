package com.appy.foldme;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;

public class HorizontalMySurfaceView extends GLSurfaceView {

    private HorizontalOpenGlRenderer mRenderer;

    public HorizontalMySurfaceView(Context context){
        super(context);

        setPreserveEGLContextOnPause(true);

        // Create an OpenGL ES 2.0 context (permission added to manifest.xml)
        setEGLContextClientVersion(2);

        //setEGLConfigChooser(8 , 8, 8, 8, 16, 0);
        mRenderer = new HorizontalOpenGlRenderer(context);
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);

        // [OPTIONAL] Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public HorizontalOpenGlRenderer getRenderer(){
        return mRenderer;
    }

    public void setTexture(final Bitmap bitmap,final int activeTexture,final int index){
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setTexture(bitmap,activeTexture,index);

            }
        });
    }


}
