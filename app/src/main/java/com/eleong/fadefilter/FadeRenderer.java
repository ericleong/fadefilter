/*
 * Copyright (C) 2015 Eric Leong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.eleong.fadefilter;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Renders a faded image onto a {@link android.opengl.GLSurfaceView}.
 * <p/>
 * Created by Eric on 3/1/2015.
 */
public class FadeRenderer implements GLSurfaceView.Renderer {

	/**
	 * The default fade amount.
	 */
	public static final float DEFAULT_FADE = 0.2f;

	/**
	 * The OpenGL shader program.
	 */
	private final FadeProgram mProgram;
	/**
	 * The id of the texture to draw.
	 */
	private int mTextureId = -1;
	/**
	 * The bitmap that will be transferred to the GPU for drawing.
	 */
	private Bitmap mBitmap;
	/**
	 * The current fade amount.
	 */
	private float mFade = DEFAULT_FADE;

	public FadeRenderer() {
		mProgram = new FadeProgram();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		mProgram.initProgram();
		mTextureId = mProgram.createTexture();

		if (mBitmap != null) {
			loadTexture(mBitmap);
		}
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		// Draw what is on the texture currently.
		if (isTextureCreated()) {
			mProgram.draw(mTextureId, mFade);
		}
	}

	/**
	 * Loads a bitmap as a texture on the GPU.
	 *
	 * @param bitmap the bitmap to draw
	 */
	public void loadTexture(Bitmap bitmap) {
		if (isTextureCreated()) {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
			GlUtil.checkGlError("glBindTexture " + mTextureId);

			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
			GlUtil.checkGlError("texImage2D");

			mBitmap = null;
		} else {
			mBitmap = bitmap;
		}
	}

	/**
	 * @param fade the fade amount, a value from 0.0 to 1.0, inclusive.
	 */
	public void setFade(float fade) {
		mFade = fade;
	}

	/**
	 * @return whether or not the texture object has been created.
	 */
	public boolean isTextureCreated() {
		return mTextureId >= 0;
	}
}
