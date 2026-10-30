#define VERT_HAS_a_PosId
#define VERT_HAS_a_Color
#define VERT_HAS_a_TexCoord
#define VERT_HAS_a_LightCoord
#define VERT_HAS_a_RdhFactor

uvec4 a_PosId;
vec4 a_Color;
uvec2 a_TexCoord;
uint a_LightCoord;
vec4 a_RdhFactor;

int _vert_corner;

#define VERT_CAN_FETCH_CORNERS

uniform usamplerBuffer u_VertexData;

struct _VertInputs {
    uvec4 a_PosId;
    vec4 a_Color;
    uvec2 a_TexCoord;
    uint a_LightCoord;
    vec4 a_RdhFactor;
};

_VertInputs _vert_fetch_corner(int corner) {
    int _vf_vertex = ((gl_VertexID / 4) * 24) + (corner * 6);
    uint _vf_v0 = texelFetch(u_VertexData, _vf_vertex + 0).r;
    uint _vf_v1 = texelFetch(u_VertexData, _vf_vertex + 1).r;
    uint _vf_v2 = texelFetch(u_VertexData, _vf_vertex + 2).r;
    uint _vf_v3 = texelFetch(u_VertexData, _vf_vertex + 3).r;
    uint _vf_v4 = texelFetch(u_VertexData, _vf_vertex + 4).r;
    uint _vf_v5 = texelFetch(u_VertexData, _vf_vertex + 5).r;
    _VertInputs v;
    v.a_PosId = uvec4((_vf_v0 & 0xFFFFu), ((_vf_v0 >> 16u) & 0xFFFFu), (_vf_v1 & 0xFFFFu), ((_vf_v1 >> 16u) & 0xFFFFu));
    v.a_Color = vec4((float((_vf_v2 & 0xFFu)) / 255.0), (float(((_vf_v2 >> 8u) & 0xFFu)) / 255.0), (float(((_vf_v2 >> 16u) & 0xFFu)) / 255.0), (float(((_vf_v2 >> 24u) & 0xFFu)) / 255.0));
    v.a_TexCoord = uvec2((_vf_v3 & 0xFFFFu), ((_vf_v3 >> 16u) & 0xFFFFu));
    v.a_LightCoord = _vf_v4;
    v.a_RdhFactor = vec4(max(float((int((_vf_v5 << 24u)) >> 24)) / 127.0, -1.0), max(float((int((_vf_v5 << 16u)) >> 24)) / 127.0, -1.0), max(float((int((_vf_v5 << 8u)) >> 24)) / 127.0, -1.0), max(float((int(_vf_v5) >> 24)) / 127.0, -1.0));
    return v;
}

void _vert_fetch() {
    _vert_corner = gl_VertexID % 4;
    _VertInputs v = _vert_fetch_corner(_vert_corner);
    a_PosId = v.a_PosId;
    a_Color = v.a_Color;
    a_TexCoord = v.a_TexCoord;
    a_LightCoord = v.a_LightCoord;
    a_RdhFactor = v.a_RdhFactor;
}
