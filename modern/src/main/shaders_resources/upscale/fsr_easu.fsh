// Fragment wrapper around AMD FidelityFX FSR 1.0 EASU (edge-adaptive spatial upsampling).
//
// NOTE: this file is not a complete shader on its own. FsrUpscaler prepends the
// #version directive, the A_GPU / A_GLSL / FSR_EASU_F defines, and the contents of
// upscale/ffx_a.h and upscale/ffx_fsr1.h before compiling it.
//
// On the coordinate convention: the reference implementation assumes a top-left
// origin (D3D). We feed it gl_FragCoord and sample a render target that both use a
// bottom-left origin, so the whole computation runs in a vertically mirrored frame.
// GL's textureGather component order (.x = +1 in y, i.e. the top of the 2x2 footprint)
// mirrors D3D's (.x = the bottom of the footprint) in exactly the same way, so the
// mirroring is self-consistent and the filter needs no adjustment. The kernel derives
// its orientation from local image content, not from an absolute screen direction.

uniform sampler2D inputImage;

// Constants produced by FsrEasuCon(): float values bit-cast to uint on the Java side, per
// FsrEasuF's AU4 parameters.
uniform uvec4 easuCon0;
uniform uvec4 easuCon1;
uniform uvec4 easuCon2;
uniform uvec4 easuCon3;

out vec4 iris_FragColor;

AF4 FsrEasuRF(AF2 p) { return textureGather(inputImage, p, 0); }
AF4 FsrEasuGF(AF2 p) { return textureGather(inputImage, p, 1); }
AF4 FsrEasuBF(AF2 p) { return textureGather(inputImage, p, 2); }

void main() {
    AF3 color;

    FsrEasuF(color, AU2(gl_FragCoord.xy), easuCon0, easuCon1, easuCon2, easuCon3);

    iris_FragColor = vec4(color, 1.0);
}
