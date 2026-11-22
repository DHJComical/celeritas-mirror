#version 150

uniform float FogEnd;

in vec4 vertexColor;
in float vertexDistance;

out vec4 fragColor;

void main() {
    // Cells too transparent to see are never built, so they don't need discarding here
    vec4 color = vertexColor;

    // Clouds fade out over their own range instead of using terrain fog, like vanilla since 1.21.6
    color.a *= 1.0 - clamp(vertexDistance / FogEnd, 0.0, 1.0);

    // Faded out clouds must not write depth
    if (color.a < 1.0 / 255.0) {
        discard;
    }

    fragColor = color;
}
