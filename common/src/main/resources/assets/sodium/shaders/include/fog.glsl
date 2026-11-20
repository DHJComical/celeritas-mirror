const int FOG_SHAPE_SPHERICAL = 0;
const int FOG_SHAPE_CYLINDRICAL = 1;
const int FOG_SHAPE_PLANAR = 2;

uniform int u_FogShape;
uniform vec4 u_FogColor;

#ifdef USE_FOG_SMOOTH
uniform float u_FogStart;
uniform float u_FogEnd;
#endif

#ifdef USE_FOG_POSTMODERN
uniform float u_RenderDistFogStart;
uniform float u_RenderDistFogEnd;
uniform float u_EnvFogStart;
uniform float u_EnvFogEnd;
#endif

#if defined(USE_FOG_EXP) || defined(USE_FOG_EXP2)
uniform float u_FogDensity;
#endif

#if defined(USE_SKY_FOG) && defined(FRAGMENT_SHADER)
#define CELERITAS_SKY_FOG

uniform sampler2D u_SkyTex;
uniform float u_SkyFogEdgeStart;
uniform float u_SkyFogEdgeEnd;

vec3 _skyColor() {
#ifdef LEGACY
    return texelFetch2D(u_SkyTex, ivec2(gl_FragCoord.xy), 0).rgb;
#else
    return texelFetch(u_SkyTex, ivec2(gl_FragCoord.xy), 0).rgb;
#endif
}
#endif

float _linearFogValue(float fragDistance, float fogStart, float fogEnd) {
#ifdef USE_FOG
    if (fragDistance <= fogStart) {
        return 0.0;
    } else if (fragDistance >= fogEnd) {
        return 1.0;
    }

    return smoothstep(fogStart, fogEnd, fragDistance);
#else
    return 1.0;
#endif
}

vec3 _fogBackground(vec4 fogColor) {
#ifdef CELERITAS_SKY_FOG
    return _skyColor();
#else
    return fogColor.rgb;
#endif
}

// environment: fog from the surroundings (water, weather, blindness), which fades toward the fog color.
// edge: fog hiding the render distance limit, which with sky fog fades toward the sky behind the fragment.
vec4 _blendFog(vec4 fragColor, float environment, float edge, vec4 fogColor) {
    float fogValue = max(environment, edge);

    if (fogValue <= 0.0) {
        return fragColor;
    }

    vec3 target = fogColor.rgb;

#ifdef CELERITAS_SKY_FOG
    // The dominant fog picks the color. Nearly opaque environmental fog also turns into the sky, so that fully
    // fogged terrain matches the background.
    float skyWeight = edge >= environment ? 1.0 : edge / environment;
    skyWeight = max(skyWeight, smoothstep(0.9, 1.0, environment));
    target = mix(target, _skyColor(), skyWeight);
#endif

    return vec4(mix(fragColor.rgb, target, fogValue * fogColor.a), fragColor.a);
}

// Single-range fog. With sky fog, the render distance edge comes from u_SkyFogEdge* and fades into the sky separately.
vec4 _linearFog(vec4 fragColor, float fragDistance, vec4 fogColor, float fogStart, float fogEnd) {
#ifdef USE_FOG
    float environment = _linearFogValue(fragDistance, fogStart, fogEnd);
#ifdef CELERITAS_SKY_FOG
    float edge = _linearFogValue(fragDistance, u_SkyFogEdgeStart, u_SkyFogEdgeEnd);
#else
    float edge = environment;
#endif
    return _blendFog(fragColor, environment, edge, fogColor);
#else
    return fragColor;
#endif
}

vec4 _exp2Fog(vec4 fragColor, float fragDistance, vec4 fogColor, float fogDensity) {
#ifdef USE_FOG
    float dist = fragDistance * fogDensity;
    float factor = clamp(1.0 / exp2(dist * dist), 0.0, 1.0);
    vec3 blended = mix(fogColor.rgb, fragColor.rgb, factor * fogColor.a);

    return vec4(blended, fragColor.a); // alpha value of fragment cannot be modified
#else
    return fragColor;
#endif
}

vec4 _expFog(vec4 fragColor, float fragDistance, vec4 fogColor, float fogDensity) {
#ifdef USE_FOG
    float dist = fragDistance * fogDensity;
    float factor = clamp(1.0 / exp(dist), 0.0, 1.0);
    vec3 blended = mix(fogColor.rgb, fragColor.rgb, factor * fogColor.a);

    return vec4(blended, fragColor.a); // alpha value of fragment cannot be modified
#else
    return fragColor;
#endif
}

float getFragDistance(int fogShape, vec3 position) {
    // Use the maximum of the horizontal and vertical distance to get cylindrical fog if fog shape is cylindrical
    if (fogShape == FOG_SHAPE_CYLINDRICAL) {
        return max(length(position.xz), abs(position.y));
    } else {
        return length(position);
    }
}
