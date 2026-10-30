#define VERT_HAS_a_PosId
#define VERT_HAS_a_Color
#define VERT_HAS_a_TexCoord
#define VERT_HAS_a_LightCoord
#define VERT_HAS_a_RdhFactor

in uvec4 a_PosId;
in vec4 a_Color;
in uvec2 a_TexCoord;
in uint a_LightCoord;
in vec4 a_RdhFactor;

int _vert_corner;

void _vert_fetch() {
    _vert_corner = gl_VertexID % 4;
}
