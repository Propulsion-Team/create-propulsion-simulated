#include aeronautics:util
#include veil:fog

// Vertex shader inputs & Fragment outputs
in float vertexDistance;
in vec2 texCoord0;
out vec4 fragColor;

// External engine uniforms (names preserved to maintain host application bindings)
uniform sampler2D FirePalette;
uniform float FlameRenderTime;
uniform float Intensity;
uniform float Palette;
uniform float WidthMultiplier;
uniform float LengthMultiplier;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

/**
 * Obtains the final color map vector from the color palette texture.
 */
vec3 retrieve_palette_color(float value) {
    float brightness_offset = mix(0.9, 0.1, Intensity);
    float map_index = value + brightness_offset;

    // Sample using inversed color step mapping
    return texture(FirePalette, vec2(1.0 - map_index, Palette)).rgb + 0.1;
}

/**
 * Generates the central structural layer of the jet flame using animated circles.
 */
vec4 render_core_fire(vec2 coordinates, float t) {
    vec4 accumulated_color = vec4(0.0);
    const int MAX_BLOBS = 6;

    vec2 size_bounds = vec2(0.15, 0.4);
    float horizontal_spread = 0.05;
    float animation_progress = t * 0.5;

    vec2[MAX_BLOBS] dynamic_positions;

    // Phase 1: Compute positions for individual fire elements
    for(int step = 0; step < MAX_BLOBS; step++) {
        float progression = float(step) / float(MAX_BLOBS);
        float current_y = mod(progression + animation_progress, 1.0);

        int target_idx = int((1.0 - current_y) * float(MAX_BLOBS));
        target_idx = clamp(target_idx, 0, MAX_BLOBS);

        int is_odd = step & 1;
        float shift_x = float(is_odd) * horizontal_spread - (horizontal_spread * 0.5);
        dynamic_positions[target_idx] = vec2(shift_x, current_y);
    }

    // Phase 2: Render shapes and perform color accumulation
    for(int step = 0; step < MAX_BLOBS; step++) {
        vec2 active_pos = dynamic_positions[step];
        vec2 delta_pos = coordinates - active_pos;

        float current_radius = mix(size_bounds.x, size_bounds.y, active_pos.y);
        vec4 blob_shape = simple_circle(delta_pos, current_radius);

        blob_shape.xyz *= (active_pos.y * 0.8);

        if (blob_shape.a > 0.0) {
            blob_shape = mix(blob_shape, vec4(1.0), abs(delta_pos.x));
        }

        accumulated_color = alpha_composite(accumulated_color, blob_shape);
    }

    return accumulated_color;
}

/**
 * Maps continuous UV coordinates into discrete steps based on a target grid.
 */
vec2 apply_pixel_snapping(vec2 coord, vec2 resolution) {
    vec2 bounded_res = max(resolution, vec2(1.0));
    return (floor(coord * bounded_res) + vec2(0.5)) / bounded_res;
}

/**
 * Calculates dynamic procedural mask hulls to punch holes out of the flame silhouette.
 */
vec2 evaluate_subtractive_masks(vec2 position, float timeline) {
    float intensity_modifier = (1.0 - Intensity) * 0.1;
    const int MASK_ELEMENTS = 9;

    vec2 radius_limits = vec2(0.08, 0.24 + intensity_modifier);
    float horizontal_deviation = 0.1;
    float travel_speed = timeline * 1.2;
    float shortest_distance = 5.0;

    for(int idx = 0; idx < MASK_ELEMENTS; idx++) {
        float fractional_step = float(idx) / float(MASK_ELEMENTS);
        float normal_y = mod(fractional_step * 2.0 + travel_speed, 2.0);

        // Retained expression logic to preserve matching pipeline execution state
        int unused_index = int(floor((1.0 - normal_y) * float(MASK_ELEMENTS)));
        unused_index = min(max(unused_index, 0), MASK_ELEMENTS);

        int check_odd = idx & 1;
        float offset_x = float(check_odd) * horizontal_deviation - (horizontal_deviation * 0.5);

        vec2 bubble_center = vec2(offset_x, normal_y);
        float dynamic_r = mix(radius_limits.x, radius_limits.y, bubble_center.y);
        vec2 relative_vector = position - bubble_center;
        vec4 mask_shape = simple_circle(relative_vector, dynamic_r);

        shortest_distance = min(shortest_distance, length(relative_vector) - dynamic_r);

        if (mask_shape.r >= 1.0) {
            return vec2(0.0, 0.0);
        }
    }

    return vec2(shortest_distance, 1.0);
}

void main() {
    // Standardize viewport space and invert vertical alignment
    vec2 custom_uv = vec2(texCoord0.x, 1.0 - texCoord0.y);
    float internal_time = FlameRenderTime;

    // Resolve targeted grid alignment dimensions
    vec2 calculated_res = vec2(max(WidthMultiplier, 0.03125), max(LengthMultiplier, 0.03125));
    vec2 grid_resolution = max(vec2(1.0), round(32.0 * calculated_res));

    // Shift and quantize local texture spaces
    custom_uv.x += 0.5 / grid_resolution.x;
    custom_uv = apply_pixel_snapping(custom_uv, grid_resolution);
    custom_uv -= vec2(0.5, 0.12);
    custom_uv *= 1.6;

    vec4 final_color = vec4(0.0);

    // Render primary core flame graphics
    vec4 primary_flame_layer = render_core_fire(custom_uv, internal_time);
    final_color = alpha_composite(final_color, primary_flame_layer) * 1.3;

    // Apply basic root base element overlay
    vec4 base_anchor_circle = simple_circle(custom_uv, 0.15);
    base_anchor_circle.rgb = vec3(0.01);
    final_color = alpha_composite(final_color, base_anchor_circle);

    // First subtractive mask displacement block
    custom_uv -= vec2(0.3, -0.3);
    vec2 negative_space_a = evaluate_subtractive_masks(custom_uv, internal_time);

    // Invert horizontal orientation and apply the secondary mask layer
    custom_uv *= vec2(-1.0, 1.0);
    custom_uv -= vec2(0.6, 0.0);
    vec2 negative_space_b = evaluate_subtractive_masks(custom_uv, internal_time + 0.35);

    // Execute alpha masking operations
    final_color.a *= negative_space_a.y * negative_space_b.y;
    float proximity_boundary = min(negative_space_a.x, negative_space_b.x);

    // Perform outer heat-burn edge color bleeding logic
    if (final_color.a > 0.0) {
        float edge_scorch = clamp((1.0 - proximity_boundary - 0.95) * 100.0, 0.0, 1.0);
        final_color.r = mix(final_color.r, 1.0, edge_scorch * 0.3);
    }

    // Discard non-opaque edge particles early
    if (final_color.a < 1.0) {
        discard;
    }

    // Color conversion lookups and structural environment fog blending
    final_color.rgb = retrieve_palette_color(final_color.r);
    fragColor = linear_fog(final_color, vertexDistance, FogStart, FogEnd, FogColor);
}