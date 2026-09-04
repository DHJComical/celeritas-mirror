// Shared vertex shader for the render-scale upscaling passes.
//
// NOTE: this file is not a complete shader on its own. FsrUpscaler prepends the
// #version directive before compiling it, so that the same prologue is used for
// the vertex and fragment stages.
//
// FullScreenQuadRenderer draws a POSITION_TEX quad spanning (0,0) to (1,1), so we
// only need the position attribute and can map it straight to clip space. Both
// upscaling passes address themselves via gl_FragCoord, so no varyings are needed.

layout(location = 0) in vec3 iris_Position;

void main() {
    gl_Position = vec4(iris_Position.xy * 2.0 - 1.0, 0.0, 1.0);
}
