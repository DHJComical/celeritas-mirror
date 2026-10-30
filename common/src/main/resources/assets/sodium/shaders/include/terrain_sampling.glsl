// Block atlas sampling, configured by TerrainSampling. For the filtered modes, the sampler bound to the atlas must
// use LINEAR_MIPMAP_LINEAR/LINEAR filtering.
#if defined(CELERITAS_TEX_FILTER_NEAREST_EMULATED) || defined(CELERITAS_TEX_FILTER_RGSS)

const vec2 ATLAS_TEXEL_SIZE = 1.0 / CELERITAS_ATLAS_SIZE;
const float ATLAS_MAX_DIMENSION = max(CELERITAS_ATLAS_SIZE.x, CELERITAS_ATLAS_SIZE.y);

#ifdef CELERITAS_ATLAS_EDGE_MASK
uniform usampler2D u_AtlasEdgeMask;

// Must match AtlasEdgeMask.
const uint EDGE_MASK_BORDER_LEFT = 1u;
const uint EDGE_MASK_BORDER_RIGHT = 2u;
const uint EDGE_MASK_BORDER_TOP = 4u;
const uint EDGE_MASK_BORDER_BOTTOM = 8u;
const uint EDGE_MASK_FALLBACK = 16u;

// Bounds of the fragment's sprite, in texels, on the sides of its cell that border another sprite.
vec2 _atlas_bound_min;
vec2 _atlas_bound_max;

// Returns false if the cell has no usable bounds and must be sampled like NATIVE.
bool _atlas_load_bounds(vec2 uv) {
    ivec2 cell = clamp(ivec2(uv * (CELERITAS_ATLAS_SIZE / CELERITAS_ATLAS_CELL_SIZE)),
            ivec2(0), CELERITAS_ATLAS_EDGE_MASK_CELLS - 1);
    uint flags = texelFetch(u_AtlasEdgeMask, cell, 0).r;

    vec2 origin = vec2(cell) * CELERITAS_ATLAS_CELL_SIZE;
    _atlas_bound_min = vec2((flags & EDGE_MASK_BORDER_LEFT) != 0u ? origin.x : -1.0e9,
            (flags & EDGE_MASK_BORDER_TOP) != 0u ? origin.y : -1.0e9);
    _atlas_bound_max = vec2((flags & EDGE_MASK_BORDER_RIGHT) != 0u ? origin.x + CELERITAS_ATLAS_CELL_SIZE : 1.0e9,
            (flags & EDGE_MASK_BORDER_BOTTOM) != 0u ? origin.y + CELERITAS_ATLAS_CELL_SIZE : 1.0e9);

    return (flags & EDGE_MASK_FALLBACK) == 0u;
}

// The UV range (min in xy, max in zw) that keeps the bilinear footprint of fetches from the given mip level inside
// the bounds.
vec4 _atlas_tap_bounds(float level) {
    float inset = 0.5 * exp2(level);
    return vec4((_atlas_bound_min + inset) * ATLAS_TEXEL_SIZE, (_atlas_bound_max - inset) * ATLAS_TEXEL_SIZE);
}

vec2 _atlas_clamp(vec2 uv, vec4 bounds) {
    return clamp(uv, bounds.xy, bounds.zw);
}

// Mip levels past the last one would make the inset larger than the cell.
float _atlas_limit_level(float level) {
    return min(level, CELERITAS_ATLAS_MAX_MIP);
}

// The mask is only used without anisotropic filtering, so textureGrad() stays within the clamp.
void _atlas_limit_footprint(inout vec2 du, inout vec2 dv) {
}
#else
// The atlas padding keeps every fetch inside its sprite.
bool _atlas_load_bounds(vec2 uv) {
    return true;
}

vec4 _atlas_tap_bounds(float level) {
    return vec4(0.0);
}

vec2 _atlas_clamp(vec2 uv, vec4 bounds) {
    return uv;
}

float _atlas_limit_level(float level) {
    return level;
}

// Anisotropic filtering spreads its samples over the whole footprint even past the last mip level, which near the
// horizon reaches beyond the padding. Without anisotropy, this only shortens footprints that use the last level anyway.
void _atlas_limit_footprint(inout vec2 du, inout vec2 dv) {
    float major = max(length(du * CELERITAS_ATLAS_SIZE), length(dv * CELERITAS_ATLAS_SIZE));

    if (major > CELERITAS_ATLAS_PADDING) {
        float scale = CELERITAS_ATLAS_PADDING / major;
        du *= scale;
        dv *= scale;
    }
}
#endif

// A level at least as coarse as the ones textureGrad() reads for these derivatives.
float _atlas_grad_level(vec2 du, vec2 dv) {
    vec2 dx = du * CELERITAS_ATLAS_SIZE;
    vec2 dy = dv * CELERITAS_ATLAS_SIZE;
    float lod = 0.5 * log2(max(dot(dx, dx), dot(dy, dy)));
    return _atlas_limit_level(max(floor(lod) + 1.0, 0.0));
}

vec4 _sample_texel_center(sampler2D tex, vec2 uv, float level) {
    float scale = exp2(level);
    vec2 texel = floor(uv * CELERITAS_ATLAS_SIZE / scale) + 0.5;
    return textureLod(tex, texel * (ATLAS_TEXEL_SIZE * scale), level);
}

// Matches what NATIVE sampling with NEAREST_MIPMAP_LINEAR filtering returns.
vec4 _sample_native(sampler2D tex, vec2 uv, vec2 du, vec2 dv) {
    vec2 dx = du * CELERITAS_ATLAS_SIZE;
    vec2 dy = dv * CELERITAS_ATLAS_SIZE;
    float lod = _atlas_limit_level(max(0.5 * log2(max(dot(dx, dx), dot(dy, dy))), 0.0));
    float low = floor(lod);

    return mix(_sample_texel_center(tex, uv, low),
            _sample_texel_center(tex, uv, _atlas_limit_level(low + 1.0)),
            lod - low);
}

// du and dv select the mip level; texelScreenSize is the unbiased footprint of one screen pixel in UV space.
vec4 _sample_nearest(sampler2D tex, vec2 uv, vec2 du, vec2 dv, vec2 texelScreenSize) {
    // Find out how far the UV is from the nearest texel center
    vec2 uvTexelCoords = uv * CELERITAS_ATLAS_SIZE;
    vec2 texelCenter = round(uvTexelCoords) - 0.5;
    vec2 texelOffset = uvTexelCoords - texelCenter;

    // Move the offset closer to the texel center based on the texel's size on screen
    texelOffset = (texelOffset - 0.5) * ATLAS_TEXEL_SIZE / texelScreenSize + 0.5;
    texelOffset = clamp(texelOffset, 0.0, 1.0);

    vec2 sampleUv = _atlas_clamp((texelCenter + texelOffset) * ATLAS_TEXEL_SIZE,
            _atlas_tap_bounds(_atlas_grad_level(du, dv)));
    _atlas_limit_footprint(du, dv);
    return textureGrad(tex, sampleUv, du, dv);
}

// Rotated-grid supersampling. du and dv are unbiased; biasScale is exp2(bias).
vec4 _sample_rgss(sampler2D tex, vec2 uv, vec2 du, vec2 dv, vec2 texelScreenSize, float bias, float biasScale) {
    float maxTexelSize = max(texelScreenSize.x, texelScreenSize.y);
    float minPixelSize = 1.0 / ATLAS_MAX_DIMENSION;

    float blendFactor = smoothstep(minPixelSize, minPixelSize * 2.0, maxTexelSize);

    if (blendFactor <= 0.0) {
        return _sample_nearest(tex, uv, du * biasScale, dv * biasScale, texelScreenSize);
    }

    // log2(sqrt(length(du) * length(dv)) / minPixelSize)
    vec2 duTexels = du * ATLAS_MAX_DIMENSION;
    vec2 dvTexels = dv * ATLAS_MAX_DIMENSION;
    float lod = _atlas_limit_level(max(0.0, 0.25 * log2(dot(duTexels, duTexels) * dot(dvTexels, dvTexels)) + bias));

    // The sampler blends the two levels around lod, so the coarser one sets the inset.
    vec4 bounds = _atlas_tap_bounds(_atlas_limit_level(floor(lod) + 1.0));

    vec4 rgssColor = textureLod(tex, _atlas_clamp(uv + vec2(0.125, 0.375) * ATLAS_TEXEL_SIZE, bounds), lod);
    rgssColor += textureLod(tex, _atlas_clamp(uv + vec2(-0.125, -0.375) * ATLAS_TEXEL_SIZE, bounds), lod);
    rgssColor += textureLod(tex, _atlas_clamp(uv + vec2(0.375, -0.125) * ATLAS_TEXEL_SIZE, bounds), lod);
    rgssColor += textureLod(tex, _atlas_clamp(uv + vec2(-0.375, 0.125) * ATLAS_TEXEL_SIZE, bounds), lod);
    rgssColor *= 0.25;

    if (blendFactor >= 1.0) {
        return rgssColor;
    }

    vec4 nearestColor = _sample_nearest(tex, uv, du * biasScale, dv * biasScale, texelScreenSize);

    return mix(nearestColor, rgssColor, blendFactor);
}

#endif

// Samples the block atlas, offsetting the selected mip level by bias like texture(tex, uv, bias) does.
vec4 _sample_terrain(sampler2D tex, vec2 uv, float bias) {
#if defined(CELERITAS_TEX_FILTER_NEAREST_EMULATED) || defined(CELERITAS_TEX_FILTER_RGSS)
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);
    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    float biasScale = exp2(bias);

    if (!_atlas_load_bounds(uv)) {
        return _sample_native(tex, uv, du * biasScale, dv * biasScale);
    }

#ifdef CELERITAS_TEX_FILTER_RGSS
    return _sample_rgss(tex, uv, du, dv, texelScreenSize, bias, biasScale);
#else
    return _sample_nearest(tex, uv, du * biasScale, dv * biasScale, texelScreenSize);
#endif
#else
    return texture(tex, uv, bias);
#endif
}
