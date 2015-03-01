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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;


public class FilterActivity extends Activity {

	public static final String STATE_IMAGE_URI = "com.eleong.fadefilter.image_uri";
	public static final String STATE_FADE = "com.eleong.fadefilter.fade";

	/**
	 * Arbitrary maximum value for the {@link android.widget.SeekBar} because it must be an integer.
	 */
	private static final int MAX_FADE_VALUE = 1000;

	/**
	 * Used to retrieve the requested image.
	 */
	private static final int RESULT_GALLERY = 100;
	/**
	 * Used to retrieve the requested image on devices with the Storage Access Framework.
	 * <p/>
	 * https://developer.android.com/guide/topics/providers/document-provider.html
	 */
	private static final int RESULT_GALLERY_KITKAT = 101;

	/**
	 * Displays the faded image.
	 */
	private RatioGLSurfaceView mFadedView;
	private FadeRenderer mFadeRenderer;

	private TextView mEmptyTextView;
	private SeekBar mFadeSeekBar;

	/**
	 * Minimum size of the bitmap when loading from disk.
	 */
	private int mMinImageSize;
	/**
	 * Path to the image.
	 */
	private Uri mImageUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_filter);

		mFadedView = (RatioGLSurfaceView) findViewById(R.id.faded);
		mFadedView.setEGLContextClientVersion(2);
		mFadeRenderer = new FadeRenderer();
		mFadedView.setRenderer(mFadeRenderer);
		// Only update when necessary to reduce power consumption.
		mFadedView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		mEmptyTextView = (TextView) findViewById(R.id.no_image);

		mFadeSeekBar = (SeekBar) findViewById(R.id.fade_bar);
		mFadeSeekBar.setMax(MAX_FADE_VALUE);
		mFadeSeekBar.setProgress((int) (FadeRenderer.DEFAULT_FADE * MAX_FADE_VALUE));
		mFadeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
				updateRenderer(new Runnable() {
					@Override
					public void run() {
						mFadeRenderer.setFade((float) progress / MAX_FADE_VALUE);
					}
				});
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		// Set the minimum image size based on the size of the screen.
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);

		mMinImageSize = Math.min(size.x, size.y);

		if (savedInstanceState != null) {
			// Load the image path and fade value if possible.

			String imagePath = savedInstanceState.getString(STATE_IMAGE_URI);

			if (!TextUtils.isEmpty(imagePath)) {
				mImageUri = Uri.parse(imagePath);
			} else if (mEmptyTextView != null) {
				mEmptyTextView.setVisibility(View.VISIBLE);
			}

			float fade = savedInstanceState.getFloat(STATE_FADE);
			mFadeSeekBar.setProgress((int) (fade * MAX_FADE_VALUE));

		} else if (mEmptyTextView != null) {
			mEmptyTextView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_filter, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		if (id == R.id.action_load) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType(ImageUtil.TYPE_JPEG);
				Intent chooserIntent = Intent.createChooser(intent,
						getResources().getString(R.string.choose_image));
				startActivityForResult(chooserIntent, RESULT_GALLERY);
			} else {
				Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.setType(ImageUtil.TYPE_JPEG);
				startActivityForResult(intent, RESULT_GALLERY_KITKAT);
			}

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (mImageUri != null) {
			outState.putString(STATE_IMAGE_URI, mImageUri.toString());
		}

		if (mFadeSeekBar != null) {
			outState.putFloat(STATE_FADE, (float) mFadeSeekBar.getProgress() / MAX_FADE_VALUE);
		}

		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mFadedView != null) {
			mFadedView.onPause();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (mFadedView != null) {
			mFadedView.onResume();

			// Load the image if we have one.
			if (mImageUri != null) {
				new ImageTask().execute(mImageUri);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK && data != null && data.getData() != null) {
			mImageUri = data.getData();

			// Attempt to load the image.
			new ImageTask().execute(mImageUri);
		}
	}

	/**
	 * The preferred method of sending a value to the renderer, since it is on a separate thread.
	 * {@link android.opengl.GLSurfaceView#requestRender()} is called afterwards to update the view.
	 *
	 * @param runnable method to run on the thread of the {@link android.opengl.GLSurfaceView}
	 */
	public void updateRenderer(Runnable runnable) {
		if (mFadedView != null) {
			mFadedView.queueEvent(runnable);

			mFadedView.requestRender();
		}
	}

	/**
	 * Loads and displays an image, and hides the "no image" text.
	 */
	private class ImageTask extends AsyncTask<Uri, Void, Bitmap> {

		@Override
		protected Bitmap doInBackground(Uri... params) {
			Uri imageUri = params[0];

			return ImageUtil.loadFromUri(getContentResolver(), imageUri, mMinImageSize);
		}

		@Override
		protected void onPostExecute(final Bitmap bitmap) {
			if (bitmap != null) {
				if (mFadedView != null) {
					mFadedView.setVisibility(View.VISIBLE);

					// Update the ratio.
					mFadedView.setVideoWidthHeightRatio((float) bitmap.getWidth() / bitmap.getHeight());
				}

				if (mEmptyTextView != null) {
					mEmptyTextView.setVisibility(View.GONE);
				}

				// Load the texture on the GPU.
				updateRenderer(new Runnable() {
					@Override
					public void run() {
						mFadeRenderer.loadTexture(bitmap);
					}
				});
			} else if (mEmptyTextView != null) {
				mFadedView.setVisibility(View.GONE);
				mEmptyTextView.setVisibility(View.VISIBLE);
			}
		}
	}
}
