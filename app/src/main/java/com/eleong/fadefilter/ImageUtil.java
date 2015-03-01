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

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * Image loading utilities.
 * <p/>
 * Created by Eric on 3/1/2015.
 */
public class ImageUtil {

	private static final String TAG = ImageUtil.class.getSimpleName();

	/**
	 * The JPEG mime type.
	 */
	public static final String TYPE_JPEG = "image/jpeg";

	/**
	 * A safe default maximum bitmap size.
	 */
	private static final int DEFAULT_MAX_BITMAP_SIZE = 2048;

	/**
	 * Maximum OpenGL texture size.
	 */
	private static final int sMaxBitmapSize;

	static {
		// Get maximum bitmap size from OpenGL
		// http://stackoverflow.com/questions/15313807/android-maximum-allowed-width-height-of-bitmap/26823209#26823209

		// Get EGL Display
		EGL10 egl = (EGL10) EGLContext.getEGL();
		EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

		// Initialise
		int[] version = new int[2];
		egl.eglInitialize(display, version);

		// Query total number of configurations
		int[] totalConfigurations = new int[1];
		egl.eglGetConfigs(display, null, 0, totalConfigurations);

		// Query actual list configurations
		EGLConfig[] configurationsList = new EGLConfig[totalConfigurations[0]];
		egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations);

		int[] textureSize = new int[1];
		int maximumTextureSize = 0;

		// Iterate through all the configurations to located the maximum texture size
		for (int i = 0; i < totalConfigurations[0]; i++) {
			// Only need to check for width since opengl textures are always squared
			egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize);

			// Keep track of the maximum texture size
			if (maximumTextureSize < textureSize[0]) {
				maximumTextureSize = textureSize[0];
			}
		}

		// Release
		egl.eglTerminate(display);

		// Return largest texture size found, or default
		sMaxBitmapSize = Math.max(maximumTextureSize, DEFAULT_MAX_BITMAP_SIZE);
	}

	/**
	 * @param contentResolver the content resolver to use
	 * @param imageUri        document uri to the image
	 * @param reqSize         the required size of the bitmap.
	 *                        -1 if the entire bitmap should be loaded
	 * @return the bitmap from the uri
	 */
	public static Bitmap loadFromUri(ContentResolver contentResolver, Uri imageUri, int reqSize) {
		try {
			Bitmap bitmap;
			ParcelFileDescriptor parcelFileDescriptor =
					contentResolver.openFileDescriptor(imageUri, "r");
			FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

			bitmap = ImageUtil.decodeSampledBitmapFromResource(fileDescriptor, reqSize, reqSize);

			parcelFileDescriptor.close();

			return bitmap;
		} catch (IOException e) {
			Log.e(TAG, "Could not load file from: " + imageUri.toString(), e);
		}

		return null;
	}

	/**
	 * Adapted from:
	 * http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
	 *
	 * @param fileDescriptor the image to load
	 * @param reqWidth       the requested width, in pixels
	 * @param reqHeight      the requested height, in pixels
	 * @return a sampled bitmap that meets the requirements
	 */
	public static Bitmap decodeSampledBitmapFromResource(
			FileDescriptor fileDescriptor, int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
	}

	/**
	 * @param options   contains the raw height and width of the image
	 * @param reqWidth  the desired width
	 * @param reqHeight the desired height
	 * @return the value of {@link android.graphics.BitmapFactory.Options#inSampleSize}
	 * that ensures the resulting bitmap is larger than the desired width and height
	 */
	public static int calculateInSampleSize(
			BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			// Make sure bitmap can be rendered by ImageView by checking dimensions
			while (((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth)
					|| (height / inSampleSize) > sMaxBitmapSize
					|| (width / inSampleSize) > sMaxBitmapSize) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}
}
