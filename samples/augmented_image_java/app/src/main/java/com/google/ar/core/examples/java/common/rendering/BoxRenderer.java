
/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.java.common.rendering;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Map;
import java.util.TreeMap;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;

/** Renders box for stencil. */
public class BoxRenderer {
  private static final String TAG = BoxRenderer.class.getSimpleName();

  /**
   * Blend mode.
   *
   * @see #setBlendMode(BlendMode)
   */
  public enum BlendMode {
    /** Multiplies the destination color by the source alpha, without z-buffer writing. */
    Shadow,
    /** Normal alpha blending with z-buffer writing. */
    AlphaBlending
  }

  // Shader names.
  private static final String VERTEX_SHADER_NAME = "shaders/box.vert";
  private static final String FRAGMENT_SHADER_NAME = "shaders/box.frag";

  private static final int COORDS_PER_VERTEX = 3;

  // Object vertex buffer variables.
  private int vertexBufferId;
  private int indexBufferId;
  private int indexCount;

  private int program;

  private int modelViewProjectionUniform;

  // Shader location: object attributes.
  private int positionAttribute;

  private BlendMode blendMode = null;

  // Temporary matrices allocated here to reduce number of allocations for each frame.
  private final float[] modelMatrix = new float[16];
  private final float[] modelViewMatrix = new float[16];
  private final float[] modelViewProjectionMatrix = new float[16];

  /**
   * Creates and initializes OpenGL resources needed for rendering the model.
   *
   * @param context Context for loading the shader and below-named model and texture assets.
   * @param objAssetName Name of the OBJ file containing the model geometry.
   * @param diffuseTextureAssetName Name of the PNG file containing the diffuse texture map.
   */
  public void createOnGlThread(Context context, String objAssetName, String diffuseTextureAssetName)
      throws IOException {
    // Compiles and loads the shader based on the current configuration.
    compileAndLoadShaderProgram(context);

    int[] buffers = new int[2];
    GLES20.glGenBuffers(2, buffers, 0);
    vertexBufferId = buffers[0];
    indexBufferId = buffers[1];

    // Load index buffer
//    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
//
//    IntBuffer indices = ByteBuffer.allocateDirect(5 * 6 * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
//    indices.put(new int[] {
//            0, 1 ,4, // 下
//            1, 4, 5, // 下
//            2, 3, 7, // 上
//            2, 6, 7, // 上
//            0, 3, 4, // 左
//            3, 4, 7, // 左
//            1, 2, 5, // 右
//            2, 5, 6, // 右
//            4, 5, 6, // 里
//            4, 6, 7, // 里
//    });
//    indices.position(0);
////    Convert int indices to shorts for GL ES 2.0 compatibility
//    ShortBuffer shortIndices =
//            ByteBuffer.allocateDirect(2 * indices.limit())
//                    .order(ByteOrder.nativeOrder())
//                    .asShortBuffer();
//    while (indices.hasRemaining()) {
//      shortIndices.put((short) indices.get());
//    }
//    shortIndices.rewind();
//    shortIndices.position(0);
    
//    indexCount = shortIndices.limit();
//    GLES20.glBufferData(
//        GLES20.GL_ELEMENT_ARRAY_BUFFER, 2 * indexCount, shortIndices, GLES20.GL_STATIC_DRAW);
//    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

    ShaderUtil.checkGLError(TAG, "OBJ buffer load");

    Matrix.setIdentityM(modelMatrix, 0);
  }

  /**
   * Selects the blending mode for rendering.
   *
   * @param blendMode The blending mode. Null indicates no blending (opaque rendering).
   */
  public void setBlendMode(BlendMode blendMode) {
    this.blendMode = blendMode;
  }

  private void compileAndLoadShaderProgram(Context context) throws IOException {
    // Compiles and loads the shader program based on the selected mode.
    Map<String, Integer> defineValuesMap = new TreeMap<>();
//    defineValuesMap.put(USE_DEPTH_FOR_OCCLUSION_SHADER_FLAG, useDepthForOcclusion ? 1 : 0);

    final int vertexShader =
        ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
    final int fragmentShader =
        ShaderUtil.loadGLShader(
            TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME, defineValuesMap);

    program = GLES20.glCreateProgram();
    GLES20.glAttachShader(program, vertexShader);
    GLES20.glAttachShader(program, fragmentShader);
    GLES20.glLinkProgram(program);
    GLES20.glUseProgram(program);

    ShaderUtil.checkGLError(TAG, "Program creation");

    modelViewProjectionUniform = GLES20.glGetUniformLocation(program, "u_ModelViewProjection");

    positionAttribute = GLES20.glGetAttribLocation(program, "a_Position");

    ShaderUtil.checkGLError(TAG, "Program parameters");
  }

  /**
   * Updates the object model matrix and applies scaling.
   *
   * @param modelMatrix A 4x4 model-to-world transformation matrix, stored in column-major order.
   * @param scaleFactor A separate scaling factor to apply before the {@code modelMatrix}.
   * @see Matrix
   */
  public void updateModelMatrix(float[] modelMatrix, float scaleFactor) {
    float[] scaleMatrix = new float[16];
    Matrix.setIdentityM(scaleMatrix, 0);
    scaleMatrix[0] = scaleFactor;
    scaleMatrix[5] = scaleFactor;
    scaleMatrix[10] = scaleFactor;
    Matrix.multiplyMM(this.modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
  }

  public void draw(
      float[] cameraView,
      float[] cameraPerspective,
      float[] colorCorrectionRgba,
      float[] objColor,
      FloatBuffer vertexBuffer) {
    ShaderUtil.checkGLError(TAG, "Before draw");

//    GLES20.glDisable(GLES20.GL_DEPTH_TEST);
//    GLES20.glDepthMask(false);

    // Build the ModelView and ModelViewProjection matrices
    // for calculating object position and light.
    Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
    Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);

    GLES20.glUseProgram(program);

    // Set the vertex attributes.
//    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId);
//    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * vertexBuffer.limit(), vertexBuffer, GLES20.GL_STATIC_DRAW);
    GLES20.glVertexAttribPointer(
        positionAttribute, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, vertexBuffer);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    // Set the ModelViewProjection matrix in the shader.
    GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);

    // Enable vertex arrays
    GLES20.glEnableVertexAttribArray(positionAttribute);

    // indices
//    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
//    GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, 0);
//    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    
    // stencil
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    

    // Disable vertex arrays
    GLES20.glDisableVertexAttribArray(positionAttribute);

    ShaderUtil.checkGLError(TAG, "After draw");
  }
}
