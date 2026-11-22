#version 150

const int FLAG_MASK_DIR = 7;
const int FLAG_INSIDE_FACE = 1 << 3;
const int FLAG_USE_TOP_COLOR = 1 << 4;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec4 ColorModulator;
uniform vec3 CloudOffset;
uniform vec3 CellSize;
uniform int CenterCellX;
uniform int CenterCellZ;

uniform isamplerBuffer CloudFaces;
uniform sampler2D CloudColors;

out float vertexDistance;
out vec4 vertexColor;

const vec3[] vertices = vec3[](
    // Bottom face
    vec3(1, 0, 0), vec3(1, 0, 1), vec3(0, 0, 1), vec3(0, 0, 0),
    // Top face
    vec3(0, 1, 0), vec3(0, 1, 1), vec3(1, 1, 1), vec3(1, 1, 0),
    // North face
    vec3(0, 0, 0), vec3(0, 1, 0), vec3(1, 1, 0), vec3(1, 0, 0),
    // South face
    vec3(1, 0, 1), vec3(1, 1, 1), vec3(0, 1, 1), vec3(0, 0, 1),
    // West face
    vec3(0, 0, 1), vec3(0, 1, 1), vec3(0, 1, 0), vec3(0, 0, 0),
    // East face
    vec3(1, 0, 0), vec3(1, 1, 0), vec3(1, 1, 1), vec3(1, 0, 1)
);

const float[] faceShades = float[](0.7, 1.0, 0.8, 0.8, 0.9, 0.9);

int wrap(int value, int size) {
    return value - size * int(floor(float(value) / float(size)));
}

void main() {
    int quadVertex = gl_VertexID % 4;
    int face = texelFetch(CloudFaces, gl_VertexID / 4).r;

    int direction = face & FLAG_MASK_DIR;
    bool isInsideFace = (face & FLAG_INSIDE_FACE) != 0;
    bool useTopColor = (face & FLAG_USE_TOP_COLOR) != 0;
    // Arithmetic shifts sign extend the 12-bit cell position
    int cellX = (face << 12) >> 20;
    int cellZ = face >> 20;

    vec3 faceVertex = vertices[(direction * 4) + (isInsideFace ? 3 - quadVertex : quadVertex)];
    vec3 pos = (faceVertex * CellSize) + (vec3(cellX, 0, cellZ) * CellSize) + CloudOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    ivec2 textureSize = textureSize(CloudColors, 0);
    ivec2 texel = ivec2(wrap(CenterCellX + cellX, textureSize.x), wrap(CenterCellZ + cellZ, textureSize.y));
    float shade = useTopColor ? faceShades[1] : faceShades[direction];

    vertexDistance = length(pos);
    vertexColor = texelFetch(CloudColors, texel, 0) * vec4(vec3(shade), 1.0) * ColorModulator;
}
