#include veil:fog

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out vec2 texCoord0;
out float vertexDistance;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = fog_distance(ModelViewMat, Position, FogShape);

    texCoord0 = UV0;
}
