package com.appy.foldme;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


public class ExternalPage {

	private float width;
    private Context mContext;
	private final FloatBuffer vertexBuffer;
    private final ShortBuffer drawListBuffer;
    private final FloatBuffer mNormals;
    private final FloatBuffer mTextureCoordinates;

    /** This will be used to pass in the transformation matrix. */
    private int mMVPMatrixHandle;

    /** This will be used to pass in the modelview matrix. */
    private int mMVMatrixHandle;

    /** This will be used to pass in the light position. */
    private int mLightPosHandle;

    /** This will be used to pass in the texture. */
    private int mTextureUniformHandle;

    /** This will be used to pass in model position information. */
    private int mPositionHandle;

    /** This will be used to pass in model normal information. */
    private int mNormalHandle;

    /** This will be used to pass in model texture coordinate information. */
    private int mTextureCoordinateHandle;

    /** Size of the normal data in elements. */
    private final int mNormalDataSize = 3;

    /** Size of the texture coordinate data in elements. */
    private final int mTextureCoordinateDataSize = 2;

    /** This is a handle to our page shading program. */
    private int mProgram;

    /** This is a handle to our texture data. */
    public static int mTextureDataHandle = 0;



    private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 u_MVPMatrix; " +
            "uniform mat4 u_MVMatrix; " +
            "attribute vec4 a_Position; " +
            "attribute vec3 a_Normal; " +
            "attribute vec2 a_TexCoordinate; " +
            "varying vec3 v_Position; " +
            "varying vec3 v_Normal; " +
            "varying vec2 v_TexCoordinate;" +
            "void main() {" +
            "v_Position = vec3(u_MVMatrix * a_Position); " +
            "v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0)); " +
            "v_TexCoordinate = a_TexCoordinate;" +
            // the matrix must be included as a modifier of gl_Position
            "  gl_Position = u_MVPMatrix * a_Position; " +

            "}";

	private final String fragmentShaderCode =
		    "precision mediump float; " +
            "uniform sampler2D u_Texture; " +
            "uniform vec3 u_LightPos; " +
            "varying vec3 v_Position; " +
            "varying vec3 v_Normal; " +
            "varying vec2 v_TexCoordinate; " +
		    "void main() {" +
            "float distance = length(u_LightPos - v_Position); " +
            "vec3 lightVector = normalize(u_LightPos - v_Position); " +
            "float diffuse = max(dot(v_Normal, lightVector), 0.0); " +
            "diffuse = diffuse * (1.0 / (1.0 + (0.10 * distance))); " +
            "diffuse = diffuse + 0.3; " +
		    "gl_FragColor = (diffuse * texture2D(u_Texture, v_TexCoordinate));" +
		    "}";

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;

    private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

    /****** fields used in Draw method ******/

	private final int vertexStride = COORDS_PER_VERTEX * 4; // bytes per vertex

    private static boolean isSetTexture = false;

    public float getwidth(){
        return width;
    }

    public void setwidth(float value){
        width = value;
    }

//    public int getTextureDataHandle(){
//        return mTextureDataHandle;
//    }
//
//    public void setTextureDataHandle(int value){
//        mTextureDataHandle = value;
//    }

    public ExternalPage(Context context) {

        mContext = context;

        // vertexes coordinates in counterclockwise order:
        float pageCoords[] = {
                -1.0f,  1.0f, 0.0f,   // 0. top left
                -1.0f, -1.0f, 0.0f,   // 1. bottom left
                1.0f, -1.0f, 0.0f,  // 2. bottom right
                1.0f,  1.0f, 0,0f   // 3. top right
                };

        float pageNormalData[] =
                {
                        0.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f,
                        0.0f, 0.0f, 1.0f
                };

        float TextureCoordinateData[] =
                {
                        0.0f, 0.0f,
                        0.0f, 1.0f,
                        1.0f, 1.0f,
                        1.0f, 0.0f
                };

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                    // (# of coordinate values * 4 bytes per float)
                    pageCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(pageCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);


        mTextureCoordinates = ByteBuffer.allocateDirect(TextureCoordinateData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureCoordinates.put(TextureCoordinateData).position(0);

        mNormals = ByteBuffer.allocateDirect(pageNormalData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mNormals.put(pageNormalData).position(0);

        int vertexShader = OpenGlRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = OpenGlRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables
//        Log.i("FoldMe","constructors");
    }



    public static void setTexture(Bitmap bitmap,int activeTexture,int index){
        if (mTextureDataHandle==0) mTextureDataHandle = Utility.setTexture(bitmap,activeTexture,index);
//        Log.i("FoldMe","setTexture");
        isSetTexture = true;
    }
    
    public void draw(float[] mvpMatrix, float[] mvMatrix, float[] mLightPosInEyeSpace ) {

        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVMatrix");
        mLightPosHandle = GLES20.glGetUniformLocation(mProgram, "u_LightPos");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
        mNormalHandle = GLES20.glGetAttribLocation(mProgram, "a_Normal");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate");



        // Set the active texture unit to texture unit 0.
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);





        // Prepare the page coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     vertexStride, vertexBuffer);
        // Enable a handle to the fold vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);


        // Pass in the normal information
        mNormals.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
                0, mNormals);

        GLES20.glEnableVertexAttribArray(mNormalHandle);


        if(isSetTexture){

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 1.
            GLES20.glUniform1i(mTextureUniformHandle, 1);
        // Pass in the texture coordinate information
        mTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
                0, mTextureCoordinates);

        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        }

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mvMatrix, 0);

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        OpenGlRenderer.checkGlError("glUniformMatrix4fv");


        // Pass in the light position in eye space.
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

        // Draw the page
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                              GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
      //  }
    }
}
