package dev.propulsionteam.propulsionsimulated.particles;

import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParticleCollisionMathTest {
    private static final double EPSILON = 1.0e-8d;

    @Test
    void fastParticlesCannotSkipAThinBlock() {
        var hit = ParticleCollisionMath.sweep(new Vector3d(-10, 0.5, 0.5), new Vector3d(20, 0, 0),
                0, 0, 0, 0.0625, 1, 1, 0.025);
        assertNotNull(hit);
        assertEquals(9.975d / 20.0d, hit.fraction(), EPSILON);
        assertEquals(new Vector3d(-1, 0, 0), hit.normal());
    }

    @Test
    void finiteRadiusHitsEvenWhenTheCenterMisses() {
        assertNotNull(ParticleCollisionMath.sweep(new Vector3d(-2, 1.05, 0.5), new Vector3d(4, 0, 0),
                0, 0, 0, 1, 1, 1, 0.1));
        assertNull(ParticleCollisionMath.sweep(new Vector3d(-2, 1.2, 0.5), new Vector3d(4, 0, 0),
                0, 0, 0, 1, 1, 1, 0.1));
    }

    @Test
    void radiusUsesTheTargetsScaleRatherThanTheEmittersScale() {
        Quaterniond rotation = new Quaterniond().rotateY(Math.PI / 3.0d).rotateZ(Math.PI / 5.0d);
        for (double targetScale : new double[]{0.0625d, 0.25d, 1.0d, 4.0d}) {
            Matrix4d pose = new Matrix4d().translation(8, 12, -3).rotate(rotation).scale(targetScale);
            Matrix4d inverse = new Matrix4d(pose).invert();
            Vector3d worldStart = pose.transformPosition(new Vector3d(-2, 0.5, 0.5));
            Vector3d worldMove = pose.transformDirection(new Vector3d(4, 0, 0));
            Vector3d localStart = inverse.transformPosition(new Vector3d(worldStart));
            Vector3d localMove = inverse.transformDirection(new Vector3d(worldMove));
            double worldRadius = 0.025d;
            var hit = ParticleCollisionMath.sweep(localStart, localMove,
                    0, 0, 0, 1, 1, 1, worldRadius / targetScale);
            assertNotNull(hit);
            assertEquals((2.0d - worldRadius / targetScale) / 4.0d, hit.fraction(), EPSILON);
            Vector3d normal = pose.transformDirection(new Vector3d(hit.normal())).normalize();
            assertEquals(1.0d, normal.length(), EPSILON);
            assertTrue(normal.distance(rotation.transform(new Vector3d(-1, 0, 0))) < EPSILON);
        }
    }

    @Test
    void expandingSublevelsPushStationaryParticlesOutward() {
        Matrix4d previous = new Matrix4d().scaling(0.25d);
        Matrix4d current = new Matrix4d().scaling(0.5d);
        Vector3d world = new Vector3d(0.4d, 0.125d, 0.125d);
        Vector3d start = new Matrix4d(previous).invert().transformPosition(new Vector3d(world));
        Vector3d end = new Matrix4d(current).invert().transformPosition(new Vector3d(world));
        Vector3d movement = new Vector3d(end).sub(start);
        var hit = ParticleCollisionMath.sweep(start, movement, 0, 0, 0, 1, 1, 1, 0.025d / 0.5d);
        assertNotNull(hit);
        Vector3d position = current.transformPosition(new Vector3d(start).fma(hit.fraction(), movement));
        assertEquals(0.525d, position.x, EPSILON);
        assertEquals(new Vector3d(1, 0, 0), hit.normal());
    }

    @Test
    void particlesCanLeaveAnOverlappingSpawn() {
        assertNull(ParticleCollisionMath.sweep(new Vector3d(0.5, 0.5, 0.5), new Vector3d(2, 0, 0),
                0, 0, 0, 1, 1, 1, 0.1));
    }

    @Test
    void touchingParticlesOnlyCollideWhenMovingIntoTheSurface() {
        var start = new Vector3d(-0.1, 0.5, 0.5);
        var hit = ParticleCollisionMath.sweep(start, new Vector3d(1, 0, 0), 0, 0, 0, 1, 1, 1, 0.1);
        assertNotNull(hit);
        assertEquals(0.0d, hit.fraction(), EPSILON);
        assertNull(ParticleCollisionMath.sweep(start, new Vector3d(-1, 0, 0), 0, 0, 0, 1, 1, 1, 0.1));
    }
}
