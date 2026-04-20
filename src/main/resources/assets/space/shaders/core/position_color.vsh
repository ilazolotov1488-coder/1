#version 150

float roundedBoxSDF(vec2 CenterPosition, vec2 Size, vec4 Radius) {
    vec2 halfSize = Size;
    Radius = min(Radius, vec4(halfSize.x, halfSize.y, halfSize.x, halfSize.y));
    Radius.xy = (CenterPosition.x > 0.0) ? Radius.xy : Radius.zw;
    Radius.x  = (CenterPosition.y > 0.0) ? Radius.x  : Radius.y;
    vec2 q = abs(CenterPosition) - Size + Radius.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - Radius.x;
}

vec2 rvertexcoord(int id) {
    int i = id % 4;
    if (i == 0) return vec2(0.0, 0.0);
    if (i == 1) return vec2(0.0, 1.0);
    if (i == 2) return vec2(1.0, 1.0);
    return vec2(1.0, 0.0);
}

float rdist(vec2 pos, vec2 size, vec4 radius) {
    radius.xy = (pos.x > 0.0) ? radius.xy : radius.wz;
    radius.x  = (pos.y > 0.0) ? radius.x : radius.y;
    vec2 v = abs(pos) - size + radius.x;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - radius.x;
}

float ralpha(vec2 size, vec2 coord, vec4 radius, float smoothness) {
    vec2 center = size * 0.5;
    float dist = rdist(center - (coord * size), center - 1.0, radius);
    return 1.0 - smoothstep(1.0 - smoothness, 1.0, dist);
}

in vec3 Position; // POSITION_COLOR vertex attributes
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 FragCoord;
out vec4 FragColor;

void main() {
    FragCoord = rvertexcoord(gl_VertexID);
    FragColor = Color;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}