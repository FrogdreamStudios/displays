#version 330

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

// I420 planes: Y at full resolution, U / V at half
uniform sampler2D Sampler0; // Y
uniform sampler2D Sampler1; // U
uniform sampler2D Sampler3; // V
uniform sampler2D Sampler2; // Opaque-only depth snapshot
uniform sampler2D Sampler4; // Color before the translucent pass
uniform sampler2D Sampler5; // Color after the translucent pass

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

// Transmission of a translucent layer covering the display
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

    // BT.709 limited range
    float y = (texture(Sampler0, texCoord0).r - 0.0625) * 1.164384;
    float u = texture(Sampler1, texCoord0).r - 0.5;
    float v = texture(Sampler3, texCoord0).r - 0.5;
    vec3 rgb = clamp(vec3(
        y + 1.792741 * v,
        y - 0.213249 * u - 0.532909 * v,
        y + 2.112402 * u
    ), 0.0, 1.0);

    vec4 color = vec4(rgb * vertexColor.rgb, vertexColor.a) * ColorModulator;

    float fogValue = max(
        linear_fog_value(sphericalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd),
        linear_fog_value(cylindricalVertexDistance, FogRenderDistanceStart, FogRenderDistanceEnd)
    ) * FogColor.a;
    float alpha = color.a * (1.0 - fogValue);

    vec3 pre = texelFetch(Sampler4, px, 0).rgb;
    vec3 post = texelFetch(Sampler5, px, 0).rgb;

    vec3 display = mix(pre, color.rgb, alpha);

    float covered = smoothstep(0.002, 0.02, distance(post, pre));
    float transmission = mix(1.0, TRANSLUCENT_TRANSMISSION, covered);

    fragColor = vec4(post + (display - pre) * transmission, 1.0);
}
