package dev.propulsionteam.propulsionsimulated.client.render.plume;

import net.minecraft.core.Direction;

public record PlumeRenderParams(
        Direction direction,
        float power,
        float length,
        float radius,
        float alpha,
        float red,
        float green,
        float blue,
        PlumeShape shape,
        Integer dyeColor,
        float capRadius,
        float flare,
        float settle
) {
    public PlumeRenderParams(Direction direction, float power, float length, float radius, float alpha,
                             float red, float green, float blue, PlumeShape shape, Integer dyeColor) {
        this(direction, power, length, radius, alpha, red, green, blue, shape, dyeColor, 0.0f, 0.0f, 1.0f);
    }

    public PlumeRenderParams(Direction direction, float power, float length, float radius, float alpha,
                             float red, float green, float blue, PlumeShape shape) {
        this(direction, power, length, radius, alpha, red, green, blue, shape, null, 0.0f, 0.0f, 1.0f);
    }

    public static PlumeRenderParams fire(Direction direction, float power, float length, float radius) {
        return new PlumeRenderParams(
                direction,
                power,
                length,
                radius,
                1.0f,
                1.0f,
                0.38f,
                0.045f,
                PlumeShape.SQUARE
        );
    }
}
