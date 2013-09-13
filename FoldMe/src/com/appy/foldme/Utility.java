package com.appy.foldme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.opengl.ETC1;
import android.opengl.ETC1Util;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.View;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Yassine on 22/06/13.
 */
public class Utility {


    public static Bitmap takeScreenshot (View view, Bitmap.Config config, int scroll) {
        int width = view.getWidth();
        int height = view.getHeight();

        if (view != null && width > 0 && height > 0) {
            Bitmap bitmap = Bitmap.createBitmap(width, height-2*scroll, config);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
//            Bitmap bitmap = view.getDrawingCache();
            Log.d(FoldingMenu.DEBUG_TAG,"Bitmap Height = "+bitmap.getHeight());
            return bitmap;
        } else {
            return null;
        }
    }

    public static int setTexture (Bitmap bitmap,int activeTexture, int textureIndex){
        final int[] textureHandle = new int[2];
        GLES20.glGenTextures(2, textureHandle, 0);
        if (textureHandle[textureIndex] != 0)
        {

        GLES20.glActiveTexture(activeTexture); //Activate the texture channel

        // Bind to the texture in OpenGL
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[textureIndex]);

        // Set filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);

//            int size = bitmap.getRowBytes() * bitmap.getHeight();
//            ByteBuffer bb = ByteBuffer.allocateDirect(size); // size is good
//            bb.order(ByteOrder.nativeOrder());
//            bitmap.copyPixelsToBuffer(bb);
//            bb.position(0);
//
//            ETC1Util.ETC1Texture etc1tex;
//// RGB_565 is 2 bytes per pixel
////ETC1Texture etc1tex = ETC1Util.compressTexture(bb, m_TexWidth, m_TexHeight, 2, 2*m_TexWidth);
//
//            final int encodedImageSize = ETC1.getEncodedDataSize(bitmap.getWidth(), bitmap.getHeight());
//            ByteBuffer compressedImage = ByteBuffer.allocateDirect(encodedImageSize).order(ByteOrder.nativeOrder());
//// RGB_565 is 2 bytes per pixel
//            ETC1.encodeImage(bb, bitmap.getWidth(), bitmap.getHeight(), 2, 2*bitmap.getHeight(), compressedImage);
//            etc1tex = new ETC1Util.ETC1Texture(bitmap.getWidth(), bitmap.getHeight(), compressedImage);
//
////ETC1Util.loadTexture(GL10.GL_TEXTURE_2D, 0, 0, GL10.GL_RGB, GL10.GL_UNSIGNED_SHORT_5_6_5, etc1tex);
//            GLES20.glCompressedTexImage2D(GLES20.GL_TEXTURE_2D, 0, ETC1.ETC1_RGB8_OES, bitmap.getWidth(), bitmap.getHeight(), 0, etc1tex.getData().capacity(), etc1tex.getData());
//
//            bb = null;
//            compressedImage = null;
//            etc1tex = null;





        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        // Recycle the bitmap, since its data has been loaded into OpenGL.
        bitmap.recycle();
        }

        if (textureHandle[textureIndex] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }
        return textureHandle[textureIndex];

    }







}
