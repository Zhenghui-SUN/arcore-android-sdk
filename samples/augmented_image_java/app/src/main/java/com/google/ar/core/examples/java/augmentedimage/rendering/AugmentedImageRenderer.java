/*
 * Copyright 2018 Google LLC
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
package com.google.ar.core.examples.java.augmentedimage.rendering;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Pose;
import com.google.ar.core.examples.java.common.rendering.BoxRenderer;
import com.google.ar.core.examples.java.common.rendering.ObjectRenderer;
import com.google.ar.core.examples.java.common.rendering.ObjectRenderer.BlendMode;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/** Renders an augmented image. */
public class AugmentedImageRenderer {
  private static final String TAG = "AugmentedImageRenderer";

  private static final float TINT_INTENSITY = 0.1f;
  private static final float TINT_ALPHA = 1.0f;
  private static final int[] TINT_COLORS_HEX = {
    0x000000, 0xF44336, 0xE91E63, 0x9C27B0, 0x673AB7, 0x3F51B5, 0x2196F3, 0x03A9F4, 0x00BCD4,
    0x009688, 0x4CAF50, 0x8BC34A, 0xCDDC39, 0xFFEB3B, 0xFFC107, 0xFF9800,
  };

//  private final ObjectRenderer imageFrameUpperLeft = new ObjectRenderer();
//  private final ObjectRenderer imageFrameUpperRight = new ObjectRenderer();
//  private final ObjectRenderer imageFrameLowerLeft = new ObjectRenderer();
//  private final ObjectRenderer imageFrameLowerRight = new ObjectRenderer();

//  private final BoxRenderer boxRenderer = new BoxRenderer();

  private final ObjectRenderer boxRenderer = new ObjectRenderer();
  private final BoxRenderer stencilRenderer = new BoxRenderer();

  public AugmentedImageRenderer() {}

  public void createOnGlThread(Context context) throws IOException {

//    imageFrameUpperLeft.createOnGlThread(
//        context, "models/frame_upper_left.obj", "models/frame_base.png");
//    imageFrameUpperLeft.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
//    imageFrameUpperLeft.setBlendMode(BlendMode.AlphaBlending);
//
//    imageFrameUpperRight.createOnGlThread(
//        context, "models/frame_upper_right.obj", "models/frame_base.png");
//    imageFrameUpperRight.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
//    imageFrameUpperRight.setBlendMode(BlendMode.AlphaBlending);
//
//    imageFrameLowerLeft.createOnGlThread(
//        context, "models/frame_lower_left.obj", "models/frame_base.png");
//    imageFrameLowerLeft.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
//    imageFrameLowerLeft.setBlendMode(BlendMode.AlphaBlending);
//
//    imageFrameLowerRight.createOnGlThread(
//        context, "models/frame_lower_right.obj", "models/frame_base.png");
//    imageFrameLowerRight.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
//    imageFrameLowerRight.setBlendMode(BlendMode.AlphaBlending);

//    boxRenderer.createOnGlThread(
//        context, "models/frame_lower_right.obj", "models/frame_base.png");
//    boxRenderer.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
//    boxRenderer.setBlendMode(BoxRenderer.BlendMode.AlphaBlending);

    // stencil
    stencilRenderer.createOnGlThread(
        context, "models/frame_lower_right.obj", "models/frame_base.png");
//    stencilRenderer.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
    stencilRenderer.setBlendMode(BoxRenderer.BlendMode.AlphaBlending);
    
    boxRenderer.createOnGlThread(
        context, "models/box.obj", "models/box.png");
    boxRenderer.setMaterialProperties(0.0f, 3.5f, 0.0f, 6.0f);
    boxRenderer.setBlendMode(BlendMode.AlphaBlending);
  }

  public void draw(
      float[] viewMatrix,
      float[] projectionMatrix,
      AugmentedImage augmentedImage,
      Anchor centerAnchor,
      float[] colorCorrectionRgba) {
    float[] tintColor =
        convertHexToColor(TINT_COLORS_HEX[augmentedImage.getIndex() % TINT_COLORS_HEX.length]);

    Pose[] localBoundaryPoses = {
      Pose.makeTranslation(
          -0.5f * augmentedImage.getExtentX(),
          0.0f,
          -0.5f * augmentedImage.getExtentZ()), // upper left
      Pose.makeTranslation(
          0.5f * augmentedImage.getExtentX(),
          0.0f,
          -0.5f * augmentedImage.getExtentZ()), // upper right
      Pose.makeTranslation(
          0.5f * augmentedImage.getExtentX(),
          0.0f,
          0.5f * augmentedImage.getExtentZ()), // lower right
      Pose.makeTranslation(
          -0.5f * augmentedImage.getExtentX(),
          0.0f,
          0.5f * augmentedImage.getExtentZ()) // lower left
    };

    Pose anchorPose = centerAnchor.getPose();
    Pose[] worldBoundaryPoses = new Pose[4];
    for (int i = 0; i < 4; ++i) {
      worldBoundaryPoses[i] = anchorPose.compose(localBoundaryPoses[i]);
    }


    float x = augmentedImage.getExtentX();
    float scaleFactor = x / 2.0f;
    float[] modelMatrix = new float[16];

//    // vertices
//    FloatBuffer floatBuffer = ByteBuffer.allocateDirect(8 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
//    floatBuffer.rewind();
//    floatBuffer.limit(8 * 3);
//    floatBuffer.put(worldBoundaryPoses[0].getTranslation());
//    floatBuffer.put(worldBoundaryPoses[1].getTranslation());
//    floatBuffer.put(worldBoundaryPoses[2].getTranslation());
//    floatBuffer.put(worldBoundaryPoses[3].getTranslation());
//    float delta = 0.1f;
//    float[] deltaTranslation = {0f, -0.1f, 0f};
//    floatBuffer.put(worldBoundaryPoses[0].transformPoint(deltaTranslation));
//    floatBuffer.put(worldBoundaryPoses[1].transformPoint(deltaTranslation));
//    floatBuffer.put(worldBoundaryPoses[2].transformPoint(deltaTranslation));
//    floatBuffer.put(worldBoundaryPoses[3].transformPoint(deltaTranslation));
//    floatBuffer.position(0);

//    augmentedImage.getCenterPose().toMatrix(modelMatrix, 0);
////    boxRenderer.updateModelMatrix(modelMatrix, scaleFactor);
//    boxRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor, floatBuffer);

    // stencil
    GLES20.glClearStencil(0);
    GLES20.glClear(GLES20.GL_STENCIL_BUFFER_BIT);
    FloatBuffer stencilBuffer = ByteBuffer.allocateDirect(6 * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    stencilBuffer.rewind();
    stencilBuffer.limit(6 * 3);
    stencilBuffer.put(worldBoundaryPoses[0].getTranslation());
    stencilBuffer.put(worldBoundaryPoses[1].getTranslation());
    stencilBuffer.put(worldBoundaryPoses[2].getTranslation());
    stencilBuffer.put(worldBoundaryPoses[0].getTranslation());
    stencilBuffer.put(worldBoundaryPoses[2].getTranslation());
    stencilBuffer.put(worldBoundaryPoses[3].getTranslation());
    stencilBuffer.position(0);
    GLES20.glEnable(GLES20.GL_STENCIL_TEST);
    GLES20.glStencilMask(0xff);
    GLES20.glStencilFunc(GLES20.GL_ALWAYS, 1, 0xff);
    GLES20.glStencilOp(GLES20.GL_KEEP, GLES20.GL_KEEP, GLES20.GL_REPLACE);
    GLES20.glColorMask(false, false, false, false);
    augmentedImage.getCenterPose().toMatrix(modelMatrix, 0);
//    boxRenderer.updateModelMatrix(modelMatrix, scaleFactor);
    stencilRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor, stencilBuffer);
    GLES20.glColorMask(true, true, true, true);
    GLES20.glStencilFunc(GLES20.GL_EQUAL, 1, 0xff);
    GLES20.glDisable(GLES20.GL_DEPTH_TEST); // TODO why must call disable GL_DEPTH_TEST ?

    augmentedImage.getCenterPose().toMatrix(modelMatrix, 0);
    boxRenderer.updateModelMatrix(modelMatrix, scaleFactor);
    boxRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
    
    GLES20.glDisable(GLES20.GL_STENCIL_TEST);
    GLES20.glEnable(GLES20.GL_DEPTH_TEST);

    // original demo
//    float scaleFactor = 1.0f;
//    float[] modelMatrix = new float[16];
//
//    worldBoundaryPoses[0].toMatrix(modelMatrix, 0);
//    imageFrameUpperLeft.updateModelMatrix(modelMatrix, scaleFactor);
//    imageFrameUpperLeft.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
//
//    worldBoundaryPoses[1].toMatrix(modelMatrix, 0);
//    imageFrameUpperRight.updateModelMatrix(modelMatrix, scaleFactor);
//    imageFrameUpperRight.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
//
//    worldBoundaryPoses[2].toMatrix(modelMatrix, 0);
//    imageFrameLowerRight.updateModelMatrix(modelMatrix, scaleFactor);
//    imageFrameLowerRight.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
//
//    worldBoundaryPoses[3].toMatrix(modelMatrix, 0);
//    imageFrameLowerLeft.updateModelMatrix(modelMatrix, scaleFactor);
//    imageFrameLowerLeft.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
  }

  private static float[] convertHexToColor(int colorHex) {
    // colorHex is in 0xRRGGBB format
    float red = ((colorHex & 0xFF0000) >> 16) / 255.0f * TINT_INTENSITY;
    float green = ((colorHex & 0x00FF00) >> 8) / 255.0f * TINT_INTENSITY;
    float blue = (colorHex & 0x0000FF) / 255.0f * TINT_INTENSITY;
    return new float[] {red, green, blue, TINT_ALPHA};
  }
}
