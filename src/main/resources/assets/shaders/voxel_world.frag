#version 330 core

in vec3 vColor;
in float vDepth;

out vec4 fragColor;

void main() {
    float fogStart = 80.0;
    float fogEnd = 220.0;
    float fog = clamp((vDepth - fogStart) / (fogEnd - fogStart), 0.0, 1.0);

    vec3 fogColor = vec3(0.70, 0.81, 0.94);
    vec3 color = mix(vColor, fogColor, fog);
    fragColor = vec4(color, 1.0);
}
