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
        PlumeShape shape
) {
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