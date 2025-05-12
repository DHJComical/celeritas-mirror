#version 330 core

layout (lines_adjacency) in;
layout (triangle_strip, max_vertices = 6) out;

// TODO: [VEN][GEO-NEO-AO] Cleaner layout definitions?
in VS_OUT
{
    vec4 v_Color;// The interpolated vertex color
    vec2 v_TexCoord;// The interpolated block texture coordinates

    float v_MaterialMipBias;
#ifdef USE_FRAGMENT_DISCARD
    float v_MaterialAlphaCutoff;
#endif

#ifdef USE_FOG
    float v_FragDistance;// The fragment's distance from the camera
#endif
} gs_in[4];

out GS_OUT
{
    vec2 edge;
    flat vec4 v_Color[4];
    vec2 v_TexCoord;

    float v_MaterialMipBias;
#ifdef USE_FRAGMENT_DISCARD
    float v_MaterialAlphaCutoff;
#endif

#ifdef USE_FOG
    float v_FragDistance;// The fragment's distance from the camera
#endif
} gs_out;

void setVertex(int i) {
    // TODO: [VEN][GEO-NEO-AO] These numbers were found by 'guessing'
    const vec2 edges[4] = vec2[](vec2(0.0, 0.0), vec2(0.0, 1.0), vec2(1.0, 1.0), vec2(1.0, 0.0));

    gl_Position = gl_in[i].gl_Position;

    gs_out.edge = edges[i];

    gs_out.v_TexCoord = gs_in[i].v_TexCoord;

    gs_out.v_MaterialMipBias = gs_in[i].v_MaterialMipBias;

    #ifdef USE_FRAGMENT_DISCARD
    gs_out.v_MaterialAlphaCutoff = gs_in[i].v_MaterialAlphaCutoff;
    #endif

    #ifdef USE_FOG
    gs_out.v_FragDistance = gs_in[i].v_FragDistance;
    #endif
}

void main() {
    setVertex(0);
    EmitVertex();

    setVertex(1);
    EmitVertex();

    setVertex(2);
    gs_out.v_Color[0] = gs_in[0].v_Color;
    gs_out.v_Color[1] = gs_in[3].v_Color;
    gs_out.v_Color[2] = gs_in[1].v_Color;
    gs_out.v_Color[3] = gs_in[2].v_Color;
    EmitVertex();

    EndPrimitive();

    setVertex(0);
    EmitVertex();

    setVertex(2);
    EmitVertex();

    setVertex(3);

    gs_out.v_Color[0] = gs_in[0].v_Color;
    gs_out.v_Color[1] = gs_in[3].v_Color;
    gs_out.v_Color[2] = gs_in[1].v_Color;
    gs_out.v_Color[3] = gs_in[2].v_Color;
    EmitVertex();

    EndPrimitive();
}