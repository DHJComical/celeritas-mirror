package org.embeddedt.embeddium.impl.render.clouds;

import org.joml.Matrix4fc;

/**
 * What the platform knows about the clouds for one frame.
 *
 * @param modelView     the camera rotation, without translation
 * @param cloudHeight   the height of the bottom of the clouds
 * @param ticks         the game time in ticks, including the partial tick
 * @param red           the cloud color
 * @param range         how far clouds are drawn, in blocks
 * @param fogEnd        the distance at which clouds have faded out completely
 */
public record CloudFrame(Matrix4fc modelView, Matrix4fc projection,
                         double cameraX, double cameraY, double cameraZ,
                         float cloudHeight, double ticks,
                         float red, float green, float blue,
                         boolean fancy, float range, float fogEnd) {
}
