#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }
    color *= vertexColor * ColorModulator * lightMapColor;
    float flash = clamp((1.0 - overlayColor.a) / 0.75, 0.0, 1.0);
    color.rgb = mix(color.rgb, vec3(1.0), flash);
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
