#version 150

in vec2 texCoord0;
in vec4 vertexColor;

uniform float Time;
uniform float Power;

out vec4 fragColor;

float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

float band(float x, float center, float width) {
    return 1.0 - smoothstep(0.0, width, abs(x - center));
}

void main() {
    float u = texCoord0.x;
    float t = texCoord0.y;

    float fadeIn = smoothstep(0.0, 0.045, t);
    float fadeOut = 1.0 - smoothstep(0.82, 1.0, t);
    float alpha = vertexColor.a * fadeIn * fadeOut;

    float side = abs(fract(u * 4.0) - 0.5) * 2.0;
    float wave = sin((t * 42.0) - Time * 22.0);
    float micro = hash(floor(u * 80.0) + floor(t * 60.0) * 19.0);
    float shimmer = 0.94 + 0.045 * wave + 0.035 * micro;

    bool blueFire = vertexColor.b > vertexColor.r && vertexColor.g > vertexColor.r;

    if (blueFire) {
        float core = 1.0 - smoothstep(0.0, 0.42, t);
        float body = 1.0 - smoothstep(0.28, 1.0, t);

        float diamonds =
        band(t, 0.18, 0.030) * 0.65 +
        band(t, 0.35, 0.040) * 0.42 +
        band(t, 0.54, 0.055) * 0.24;

        vec3 whiteBlue = vec3(0.82, 0.96, 1.0);
        vec3 cyan = vec3(0.12, 0.62, 1.0);
        vec3 deepBlue = vec3(0.035, 0.12, 0.85);
        vec3 violetEdge = vec3(0.18, 0.045, 0.55);

        vec3 color = mix(violetEdge, deepBlue, 1.0 - t);
        color = mix(color, cyan, body * 0.72);
        color = mix(color, whiteBlue, core * 0.62);
        color = mix(color, whiteBlue, diamonds * 0.45);

        color *= 0.86 + micro * 0.22;
        color *= 0.82 + Power * 0.42;

        alpha *= 0.82 + diamonds * 0.24;
        alpha *= 0.95 + Power * 0.10;

        if (alpha <= 0.003) discard;
        fragColor = vec4(color, alpha);
        return;
    }

    bool plasmaLike = vertexColor.b > vertexColor.r && vertexColor.b > vertexColor.g;

    if (plasmaLike) {
        float core = 1.0 - smoothstep(0.0, 0.36, t);
        float needle = 1.0 - smoothstep(0.18, 1.0, t);
        float electricBands =
        band(t, 0.22, 0.035) * 0.25 +
        band(t, 0.46, 0.050) * 0.18 +
        band(t, 0.70, 0.070) * 0.11;

        vec3 deepBlue = vec3(0.10, 0.22, 1.00);
        vec3 violet = vec3(0.42, 0.20, 1.00);
        vec3 pale = vec3(0.72, 0.84, 1.00);

        vec3 color = mix(deepBlue, violet, smoothstep(0.15, 0.85, t));
        color = mix(color, pale, core * 0.48);
        color = mix(color, pale, electricBands);

        color *= 0.72 + 0.32 * Power;
        color *= 0.92 + micro * 0.18;

        alpha *= .72;
        alpha *= 0.72 + needle * 0.36;
        alpha *= 0.75 + electricBands * 0.35;

        if (alpha <= 0.003) discard;
        fragColor = vec4(color, alpha);
        return;
    }

    float cornerGlow = pow(side, 2.2);
    float core = 1.0 - smoothstep(0.0, 0.42, t);
    float exhaustBody = 1.0 - smoothstep(0.34, 1.0, t);

    float diamonds =
    band(t, 0.18, 0.030) * 0.95 +
    band(t, 0.34, 0.038) * 0.75 +
    band(t, 0.51, 0.052) * 0.48 +
    band(t, 0.68, 0.066) * 0.28;

    vec3 hotWhite = vec3(1.0, 0.96, 0.72);
    vec3 yellow = vec3(1.0, 0.72, 0.18);
    vec3 orange = vec3(1.0, 0.30, 0.035);
    vec3 red = vec3(0.78, 0.055, 0.012);
    vec3 smoke = vec3(0.13, 0.105, 0.085);

    vec3 fire = mix(red, orange, 1.0 - t);
    fire = mix(fire, yellow, core * 0.72);
    fire = mix(fire, hotWhite, core * 0.46);
    fire = mix(fire, hotWhite, diamonds * exhaustBody * 0.55);
    fire = mix(fire, smoke, smoothstep(0.66, 1.0, t) * 0.38);

    fire *= 0.84 + 0.26 * cornerGlow;
    fire *= shimmer;
    fire *= 0.75 + 0.45 * Power;

    alpha *= 0.78 + diamonds * 0.32;
    alpha *= 0.92 + 0.08 * Power;

    if (alpha <= 0.003) discard;
    fragColor = vec4(fire, alpha);
}