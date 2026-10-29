// https://github.com/Engine-Room/Flywheel/blob/610b1683f3ed0fef5cd387a126bd2c530a9c2ead/common/src/backend/resources/assets/flywheel/flywheel/internal/uniforms/frame.glsl

struct _FlwFrustumPlanes
{
    vec4 xyX; // <nx.x, px.x, ny.x, py.x>
    vec4 xyY; // <nx.y, px.y, ny.y, py.y>
    vec4 xyZ; // <nx.z, px.z, ny.z, py.z>
    vec4 xyW; // <nx.w, px.w, ny.w, py.w>
    vec2 zX; // <nz.x, pz.x>
    vec2 zY; // <nz.y, pz.y>
    vec2 zZ; // <nz.z, pz.z>
    vec2 zW; // <nz.w, pz.w>
};

struct _FlwCullData
{
    float znear;
    float zfar;
    float P00;
    float P11;
    float pyramidWidth;
    float pyramidHeight;
    int pyramidLevels;
    uint orthographic;
};

struct _ClrwlShadowFrustumPlanesGroup
{
    vec4 X; // <a.x, b.x, c.x, d.x>
    vec4 Y; // <a.y, b.y, c.y, d.y>
    vec4 Z; // <a.z, b.z, c.z, d.z>
    vec4 W; // <a.w, b.w, c.w, d.w>
};

struct _ClrwlShadowFrustumPlanes
{
    // Up to 11 planes can be sent, but we're declaring enough floats for 12 planes.
    // To keep data compact and packed for efficient computation, group[2] D plane is assigned to metadata:
    //  * groups[2].X.w = reversed cull distance
    //  * groups[2].Y.w = cull distance
    //  * groups[2].Z.w = -
    //  * groups[2].W.w = -
    _ClrwlShadowFrustumPlanesGroup[3] groups;
};

layout(std140) uniform _ClrwlPassUniforms
{
#if defined _CLRWL_IS_GBUFFERS_PASS
    _FlwFrustumPlanes flw_frustumPlanes;
#elif defined _CLRWL_IS_SHADOW_PASS
    _ClrwlShadowFrustumPlanes clrwl_shadowFrustumPlanes;
#endif

    _FlwCullData _flw_cullData;

    mat4 flw_view;
    mat4 flw_viewInverse;
    mat4 flw_viewPrev;
    mat4 flw_projection;
    mat4 flw_projectionInverse;
    mat4 flw_projectionPrev;
    mat4 flw_viewProjection;
    mat4 flw_viewProjectionInverse;
    mat4 flw_viewProjectionPrev;

    mat3 clrwl_normal;

    vec2 flw_viewportSize;
    float flw_aspectRatio;
    float flw_defaultLineWidth;
    float flw_viewDistance;

    uint flw_ticks;
    float flw_partialTick;
    float flw_renderTicks;
    float flw_renderSeconds;
    float flw_systemSeconds;
    uint flw_systemMillis;
};
