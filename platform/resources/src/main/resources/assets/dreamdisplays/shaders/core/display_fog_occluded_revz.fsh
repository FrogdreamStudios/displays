#version 330

// Result = post + (display - pre) * T

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Fog {
    vec4 FogColor;
    float FogEnvironmentalStart;
    float FogEnvironmentalEnd;
    float FogRenderDistanceStart;
    float FogRenderDistanceEnd;
    float FogSkyEnd;
    float FogCloudsEnd;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler2; // Opaque-only depth snapshot
uniform sampler2D Sampler4; // Color before the translucent pass
uniform sampler2D Sampler5; // Color after the translucent pass

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

// Transmission of a translucent layer covering the display. Matches vanilla water closely enough that the
// recovered tint reads correctly; only ever applied where the colour actually changed across the pass.
const float TRANSLUCENT_TRANSMISSION = 0.75;

float linear_fog_value(float dist, float start, float end) {
    if (dist <= start) return 0.0;
    if (dist >= end) return 1.0;
    return (dist - start) / (end - start);
}

void main() {
    ivec2 px = ivec2(gl_FragCoord.xy);

    // Reversed depth (26.2+, near plane at 1.0): a larger snapshot value means opaque geometry is in front
    if (texelFetch(Sampler2, px, 0).r > gl_FragCoord.z) {
        discard;
    }

    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    if (color.a == 0.0) {
        discard;
    }
    color *= ColorModulator;

    float fogValue = max(
        linear_fog_value(sphericalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd),
        linear_fog_value(cylindricalVertexDistance, FogRenderDistanceStart, FogRenderDistanceEnd)
    ) * FogColor.a;
    float alpha = color.a * (1.0 - fogValue);

    vec3 pre = texelFetch(Sampler4, px, 0).rgb;
    vec3 post = texelFetch(Sampler5, px, 0).rgb;

    // Fade the display against what was already behind it, so partially transparent pixels stay correct
    vec3 display = mix(pre, color.rgb, alpha);

    // Only treat the pixel as covered where the translucent pass actually changed it
    float covered = smoothstep(0.002, 0.02, distance(post, pre));
    float transmission = mix(1.0, TRANSLUCENT_TRANSMISSION, covered);

    fragColor = vec4(post + (display - pre) * transmission, 1.0);
}
