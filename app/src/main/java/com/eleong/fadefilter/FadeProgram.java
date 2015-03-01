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

import android.opengl.GLES20;

/**
 * Creates and controls the OpenGL shader program for fading a texture.
 * <p/>
 * Created by Eric on 3/1/2015.
 */
public class FadeProgram {

	/**
	 * A simple vertex shader that does not modify the vertex or texture coordinates.
	 */
	private static final String VERTEX_SHADER =
			"attribute vec4 aPosition;\n" +
					"attribute vec4 aTextureCoord;\n" +
					"varying vec2 vTextureCoord;\n" +
					"void main() {\n" +
					"    gl_Position = aPosition;\n" +
					"    vTextureCoord = aTextureCoord.xy;\n" +
					"}\n";

	/**
	 * A fragment shader that applies a maps the full range of input colors to a smaller range via
	 * the function: f(x) = a + (1.0 - a) * x, where x is the input color.
	 */
	private static final String FRAGMENT_SHADER =
			"precision mediump float;\n" +
					"varying vec2 vTextureCoord;\n" +
					"uniform sampler2D sTexture;\n" +
					"uniform float uFade;\n" +
					"void main() {\n" +
					"    vec4 color = texture2D(sTexture, vTextureCoord);\n" +
					"    float r = uFade + (1.0 - uFade) * color.r;\n" +
					"    float g = uFade + (1.0 - uFade) * color.g;\n" +
					"    float b = uFade + (1.0 - uFade) * color.b;\n" +
					"    gl_FragColor = vec4(r, g, b, 1.0);\n" +
					"}\n";

	/**
	 * The reference to the OpenGL program.
	 */
	private int mProgramHandle;
	/**
	 * The reference to the vertex coordinates.
	 */
	private int maPositionLoc;
	/**
	 * The reference to the texture coordinates.
	 */
	private int maTextureCoordLoc;
	/**
	 * The reference to the fade value.
	 */
	private int muFadeLoc;

	/**
	 * Creates the shader program. Can only be called once the OpenGL context has been created
	 * (usually in {@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated(javax.microedition.khronos.opengles.GL10, javax.microedition.khronos.egl.EGLConfig)}.
	 */
	public void initProgram() {
		mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);

		maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
		GlUtil.checkLocation(maPositionLoc, "aPosition");
		maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
		GlUtil.checkLocation(maTextureCoordLoc, "aTextureCoord");
		muFadeLoc = GLES20.glGetUniformLocation(mProgramHandle, "uFade");
		GlUtil.checkLocation(muFadeLoc, "uFade");
	}

	/**
	 * Creates a clamped OpenGL texture for image rendering.
	 *
	 * @return a reference to the texture created
	 */
	public int createTexture() {
		int[] textures = new int[1];
		GLES20.glGenTextures(1, textures, 0);
		GlUtil.checkGlError("glGenTextures");

		int texId = textures[0];
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
		GlUtil.checkGlError("glBindTexture " + texId);

		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
				GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
				GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
				GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
				GLES20.GL_CLAMP_TO_EDGE);
		GlUtil.checkGlError("glTexParameter");

		return texId;
	}

	/**
	 * Draws a faded texture.
	 *
	 * @param textureId the id of the texture to draw.
	 * @param fade      the amount of fade, valid values range from 0.0 and 1.0, inclusive.
	 */
	public void draw(int textureId, float fade) {
		// Select the program.
		GLES20.glUseProgram(mProgramHandle);
		GlUtil.checkGlError("glUseProgram");

		// Set the texture.
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

		// Set the fade value.
		GLES20.glUniform1f(muFadeLoc, fade);

		// Enable the "aPosition" vertex attribute.
		GLES20.glEnableVertexAttribArray(maPositionLoc);
		GlUtil.checkGlError("glEnableVertexAttribArray");

		// Connect vertexBuffer to "aPosition".
		GLES20.glVertexAttribPointer(maPositionLoc, Rectangle.COORDS_PER_VERTEX,
				GLES20.GL_FLOAT, false, Rectangle.VERTEX_STRIDE, Rectangle.VERTEX_BUFFER);
		GlUtil.checkGlError("glVertexAttribPointer");

		// Enable the "aTextureCoord" vertex attribute.
		GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
		GlUtil.checkGlError("glEnableVertexAttribArray");

		// Connect texBuffer to "aTextureCoord".
		GLES20.glVertexAttribPointer(maTextureCoordLoc, Rectangle.COORDS_PER_TEX_COORD,
				GLES20.GL_FLOAT, false, Rectangle.TEX_COORD_STRIDE, Rectangle.TEXTURE_BUFFER);
		GlUtil.checkGlError("glVertexAttribPointer");

		// Draw the rect.
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0,
				Rectangle.VERTEX_COORDS.length / Rectangle.COORDS_PER_VERTEX);
		GlUtil.checkGlError("glDrawArrays");
	}
}
