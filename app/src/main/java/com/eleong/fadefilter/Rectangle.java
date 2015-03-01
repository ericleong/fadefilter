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

import java.nio.FloatBuffer;

/**
 * Vertex and texture coordinate constants for a rectangle that covers the viewport.
 * <p/>
 * Created by Eric on 3/1/2015.
 */
public class Rectangle {
	/**
	 * A rectangle, extending from -1 to +1 in both dimensions.  When the model/view/projection
	 * matrix is identity, this will exactly cover the viewport.
	 */
	public static final float VERTEX_COORDS[] = {
			-1.0f, -1.0f,   // 0 bottom left
			1.0f, -1.0f,   // 1 bottom right
			-1.0f, 1.0f,   // 2 top left
			1.0f, 1.0f,   // 3 top right
	};
	public static final float TEXTURE_COORDS[] = {
			0.0f, 1.0f,     // 0 bottom left
			1.0f, 1.0f,     // 1 bottom right
			0.0f, 0.0f,     // 2 top left
			1.0f, 0.0f      // 3 top right
	};

	/**
	 * Number of coordinates used to represent each vertex. 2 because we are in 2D.
	 */
	public static final int COORDS_PER_VERTEX = 2;
	/**
	 * Number of coordinates used to represent each texture coordinate.
	 */
	public static final int COORDS_PER_TEX_COORD = 2;

	/**
	 * The size of a vertex coordinate, in bytes.
	 */
	public static final int VERTEX_STRIDE = COORDS_PER_VERTEX * GlUtil.SIZEOF_FLOAT;
	/**
	 * The size of a texture coordinate, in bytes.
	 */
	public static final int TEX_COORD_STRIDE = COORDS_PER_TEX_COORD * GlUtil.SIZEOF_FLOAT;

	/**
	 * A {@link java.nio.FloatBuffer} representing the vertices.
	 */
	public static final FloatBuffer VERTEX_BUFFER = GlUtil.createFloatBuffer(VERTEX_COORDS);
	/**
	 * A {@link java.nio.FloatBuffer} representing the texture coordinates.
	 */
	public static final FloatBuffer TEXTURE_BUFFER = GlUtil.createFloatBuffer(TEXTURE_COORDS);
}
