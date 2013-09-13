package com.appy.foldme;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.view.View;


public class OpenGlRenderer implements Renderer {


    // Tag to be used in LogCat
	private static final String TAG = "OpenGlRenderer";

	// Model matrix
	private final float[] mMMatrix = new float[16];
    // Projection Matrix
    private final float[] mProjMatrix = new float[16];
    // View Matrix (camera's view angle)
    private final float[] mVMatrix = new float[16];
    private final float[] mMVMatrix = new float[16];
    // Projection Matrix * View Matrix
    private final float[] mMVPMatrix = new float[16];

    /** Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
     *  we multiply this by our transformation matrices. */
    private final float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};

    /** Used to hold the current position of the light in world space (after transformation via model matrix). */
    private final float[] mLightPosInWorldSpace = new float[4];

    /** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix) */
    private  final float[] mLightPosInEyeSpace = new float[4];

    /**
     * Stores a copy of the model matrix specifically for the light position.
     */
    private float[] mLightModelMatrix = new float[16];


    /** This is a handle to our light point program. */
    private int mPointProgramHandle;

    private final String pointVertexShaderCode =
            "uniform mat4 u_MVPMatrix;"+
                    "attribute vec4 a_Position;"+
                    "void main() {"+
                    "gl_Position = u_MVPMatrix * a_Position;"+
                    "gl_PointSize = 5.0;"+
                    "}";

    private final String pointFragmentShaderCode =
            "precision mediump float;"+
                    "void main() {"+
                    "gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);"+
                    "}";

    private Page[] Folds = new Page[FoldingMenu.TOTAL];
    private ExternalPage ExtPage;

    private Context renderContext;

    private float c = 1 - ((float) 1/ FoldingMenu.TOTAL);

    private float lastangle = 90 / (float) Math.pow(c,((FoldingMenu.TOTAL-1)/2));

    // Declare as volatile because we are updating it from another thread
    public volatile float mAngle;

	/***** OpenGlRenderer Constructor *****/
	public OpenGlRenderer(Context context) {
		renderContext = context;
	}

	/***** Shaders compilator *****/
	 public static int loadShader(int type, String shaderCode){

	        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
	        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
	        int shader = GLES20.glCreateShader(type);

	        // add the source code to the shader and compile it
	        GLES20.glShaderSource(shader, shaderCode);
	        GLES20.glCompileShader(shader);

	        return shader;
	    }

	@Override
	public void onDrawFrame(GL10 gl) {

        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mVMatrix, 0, 0, 0, 3, 0f, 0f, 0f, 0.0f, 1.0f, 0.0f);

        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 1.0f);
        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mVMatrix, 0, mLightPosInWorldSpace, 0);

        GLES20.glUseProgram(mPointProgramHandle);
        drawLight();

            for (int i = 0; i < Folds.length; i++){

                // Folding Parameters
                float sum = 0;
                float angle = 0;
                int factor = i /2;
                int max = (i + 1)/2 -1;
                angle = (float)(mAngle * Math.pow(c,factor));
                if (angle > 90) angle = 90;
                for (int j = 0; j<=max; j++){
                    sum += (Math.cos(Math.toRadians(Math.pow(c, j) * mAngle)) > 0) ? Math.cos(Math.toRadians(Math.pow(c,j)* mAngle)) :0;
                }
                float trans = 2 * Folds[i].getwidth() * ((max+1) - sum);

                // Page Transformation
                if (i % 2 == 0){
                    drawEvenPage(Folds[i],i,angle,trans,FoldingMenu.DIRECTION);
                } else {
                    drawOddPage(Folds[i],i,angle,trans,FoldingMenu.DIRECTION);
                }

                if (i == Folds.length-1) {
                    float exttrans;
                    if (i % 2 == 0){
                        exttrans = 2-(2-(FoldingMenu.TOTAL)*Folds[i].getwidth())-trans; // This is currently not supported but needs some fixing (OR ELSE WE NEED TO THROW SOME EXCETIONS)
                    } else {
                        exttrans = 2-(2-FoldingMenu.TOTAL*Folds[i].getwidth())-trans;
                    }
                    drawExternalPage(exttrans,FoldingMenu.DIRECTION);
                }
            }
	}


    private void drawEvenPage (Page page,int order ,float angle ,float trans,int direction){

        float width = page.getwidth();

        Matrix.setIdentityM(mMMatrix,0);
        Matrix.translateM(mMMatrix, 0, direction * (-1 + order * width), 0, 0);
        if(trans <  order * width){
            Matrix.translateM(mMMatrix, 0,  - direction * trans, 0, 0);
        } else {
            Matrix.translateM(mMMatrix, 0,  - direction * order * width, 0, 0);
        }
        Matrix.rotateM(mMMatrix, 0, direction * angle , 0f, 1.0f, 0f);
        Matrix.multiplyMM(mMVMatrix, 0,mVMatrix , 0, mMMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0,mProjMatrix , 0, mMVMatrix, 0);

        page.draw(mMVPMatrix ,mMVMatrix ,mLightPosInEyeSpace);

    }

    private void drawOddPage (Page page,int order ,float angle ,float trans,int direction){

        float width = page.getwidth();
        Matrix.setIdentityM(mMMatrix,0);
        Matrix.translateM(mMMatrix, 0, direction * (-1 + (order+1) * width), 0, 0);
        if(trans < (order+1) * width){
            Matrix.translateM(mMMatrix, 0, - direction * trans, 0, 0);
        }else{
            Matrix.translateM(mMMatrix, 0, - direction * (order+1) * width, 0, 0);
        }
        Matrix.rotateM(mMMatrix, 0, - direction * angle, 0f, 1.0f, 0f);
        Matrix.multiplyMM(mMVMatrix, 0,mVMatrix , 0, mMMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0,mProjMatrix , 0, mMVMatrix, 0);

        page.draw(mMVPMatrix ,mMVMatrix ,mLightPosInEyeSpace);

    }


    private void drawExternalPage(float trans,int direction){

        Matrix.setIdentityM(mMMatrix,0);
        Matrix.translateM(mMMatrix, 0, direction * trans , 0, 0);
        Matrix.multiplyMM(mMVMatrix, 0,mVMatrix , 0, mMMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0,mProjMatrix , 0, mMVMatrix, 0);

        ExtPage.draw(mMVPMatrix ,mMVMatrix ,mLightPosInEyeSpace);

    }


	// Call back after onSurfaceCreated() or whenever the window's size changes
	   @Override
	   public void onSurfaceChanged(GL10 gl, int width, int height) {
	      	// Adjust the viewport based on geometry changes,
	        // such as screen rotation
//	        GLES20.glViewport(0, 0, width, height);
//	        float ratio = (float) height / width;

	        // this projection matrix is applied to object coordinates
	        // in the onDrawFrame() method
	        Matrix.frustumM(mProjMatrix, 0, -1, 1, -1, 1, 3, 4);

	   /* My OpenGL|ES display re-sizing code here */

	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        mAngle = lastangle;

        int pointVertexShader = OpenGlRenderer.loadShader(GLES20.GL_VERTEX_SHADER, pointVertexShaderCode);
        int pointfragmentShader = OpenGlRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShaderCode);

        mPointProgramHandle = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mPointProgramHandle, pointVertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mPointProgramHandle, pointfragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mPointProgramHandle);                  // creates OpenGL ES program executables


		// Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

	      /* My OpenGL|ES initialization code here */
        for (int i = 0; i < FoldingMenu.TOTAL;i++){
            Folds[i] = new Page(renderContext,i,FoldingMenu.TOTAL,FoldingMenu.DIRECTION);
        }

        ExtPage = new ExternalPage(renderContext);

        setTexture(FoldingMenu.bmp, GLES20.GL_TEXTURE0, 0);
        setTexture(FoldingMenu.extbmp,GLES20.GL_TEXTURE1,1);

//        Log.d(FoldingMenu.DEBUG_TAG, "ONSURFACECREATED");

	}

    private void drawLight()
    {

        final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");

        // Pass in the position.
        GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);

        // Since we are not using a buffer object, disable vertex arrays for this attribute.
        GLES20.glDisableVertexAttribArray(pointPositionHandle);

        // Pass in the transformation matrix.
        Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mLightModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);

    }

    public static void setTexture(Bitmap bitmap,int activeTexture,int index){

        if (activeTexture == GLES20.GL_TEXTURE0){
            Page.setTexture(bitmap,activeTexture,index);
        } else {
            ExternalPage.setTexture(bitmap,activeTexture,index);
        }


    }


	/**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     *
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

}
