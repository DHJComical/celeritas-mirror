// Fragment wrapper around AMD FidelityFX FSR 1.0 RCAS (robust contrast-adaptive sharpening).
//
// NOTE: this file is not a complete shader on its own. FsrUpscaler prepends the
// #version directive, the A_GPU / A_GLSL / FSR_RCAS_F defines, and the contents of
// upscale/ffx_a.h and upscale/ffx_fsr1.h before compiling it.
//
// RCAS runs at output resolution over the EASU result. Its 3x3 cross is symmetric, so
// unlike EASU it is entirely indifferent to the origin convention.

uniform sampler2D inputImage;

// Sharpening amount in stops: 0.0 is maximum sharpening, and each whole step halves it.
uniform float rcasSharpness;

// Largest valid texel coordinate in inputImage, used to clamp the taps at the image
// border. texelFetch outside the texture is undefined, and RCAS reads one texel out
// in each direction.
uniform ivec2 rcasMaxCoord;

out vec4 iris_FragColor;

AF4 FsrRcasLoadF(ASU2 p) { return texelFetch(inputImage, clamp(p, ivec2(0), rcasMaxCoord), 0); }

void FsrRcasInputF(inout AF1 r, inout AF1 g, inout AF1 b) {
    // The input is already display-referred and in [0, 1], so no transform is needed.
}

void main() {
    // Equivalent to FsrRcasCon(); only the first component is read by the 32-bit path.
    AU4 con = AU4(floatBitsToUint(exp2(-rcasSharpness)), 0u, 0u, 0u);

    AF1 r, g, b;
    FsrRcasF(r, g, b, AU2(gl_FragCoord.xy), con);

    iris_FragColor = vec4(r, g, b, 1.0);
}
