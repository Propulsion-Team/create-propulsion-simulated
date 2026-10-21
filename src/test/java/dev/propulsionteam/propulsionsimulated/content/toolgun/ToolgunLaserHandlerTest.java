package dev.propulsionteam.propulsionsimulated.content.toolgun;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolgunLaserHandlerTest {
    @Test
    void muzzleUsesSuppliedModelCoordinates() {
        Vector3f muzzle = ToolgunLaserHandler.muzzlePosition(new Matrix4f(), new Matrix4f(),
            new Matrix4f(), new Matrix4f());
        assertVector(new Vector3f(15 / 16f, 2.5f / 16, 7 / 16f), muzzle);
    }

    @Test
    void muzzleFollowsHeldItemTransform() {
        Matrix4f model = new Matrix4f().translate(0.4f, -0.2f, -0.7f)
            .rotateY((float) Math.PI / 2).scale(0.88f).translate(-0.5f, -0.5f, -0.5f);
        Matrix4f view = new Matrix4f().rotateX(0.3f).rotateY(1.2f);
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(70), 16 / 9f, 0.05f, 256);
        Matrix4f inverseWorldClip = new Matrix4f(projection).mul(view).invert();
        Vector3f expected = model.transformPosition(new Vector3f(15 / 16f, 2.5f / 16, 7 / 16f));
        Vector3f actual = ToolgunLaserHandler.muzzlePosition(model, view, projection, inverseWorldClip);
        assertVector(expected, actual);
    }

    @Test
    void muzzleMatchesHandProjectionWhenWorldFovDiffers() {
        Matrix4f model = new Matrix4f().translate(0.4f, -0.3f, -1.5f).scale(0.88f);
        Matrix4f view = new Matrix4f().rotateY(0.7f);
        Matrix4f handProjection = new Matrix4f().perspective((float) Math.toRadians(70), 16 / 9f, 0.05f, 256);
        Matrix4f worldProjection = new Matrix4f().perspective((float) Math.toRadians(100), 16 / 9f, 0.05f, 256);
        Matrix4f inverseWorldClip = new Matrix4f(worldProjection).mul(view).invert();
        Vector3f muzzle = ToolgunLaserHandler.muzzlePosition(model, view, handProjection, inverseWorldClip);
        Vector3f worldScreen = new Matrix4f(worldProjection).mul(view).transformProject(muzzle);
        Vector3f handScreen = new Matrix4f(handProjection).mul(view).mul(model)
            .transformProject(new Vector3f(15 / 16f, 2.5f / 16, 7 / 16f));
        assertVector(handScreen, worldScreen);
    }

    @Test
    void boltTravelsAtConstantSpeedWithSubtickInterpolation() {
        assertEquals(0, ToolgunLaserHandler.travelDistance(0), 1e-8);
        assertEquals(1.5, ToolgunLaserHandler.travelDistance(0.25), 1e-8);
        assertEquals(6, ToolgunLaserHandler.travelDistance(1), 1e-8);
        assertEquals(120, ToolgunLaserHandler.travelDistance(20), 1e-8);
    }

    @Test
    void boltCannotTravelBackwardsOrBeyondRange() {
        assertEquals(0, ToolgunLaserHandler.travelDistance(-1), 1e-8);
        assertEquals(128, ToolgunLaserHandler.travelDistance(128 / 6.0), 1e-8);
        assertEquals(128, ToolgunLaserHandler.travelDistance(100), 1e-8);
    }

    @Test
    void boltIsAShortSegmentRatherThanAFullBeam() {
        assertEquals(0, ToolgunLaserHandler.tailDistance(0.25), 1e-8);
        assertEquals(0, ToolgunLaserHandler.tailDistance(1), 1e-8);
        assertEquals(0.5, ToolgunLaserHandler.tailDistance(2), 1e-8);
        assertEquals(78.5, ToolgunLaserHandler.tailDistance(80), 1e-8);
    }

    @Test
    void ordinaryWorldHitsDoNotChangeAim() {
        Vec3 direction = ToolgunLaserHandler.shotDirection(new Vec3(10, 64, 10),
            new Vec3(10, 64, 0), position -> position);
        assertDirection(direction, 0, 0, -1);
    }

    @Test
    void sublevelStorageCoordinatesAreProjectedBeforeAiming() {
        Vec3 direction = ToolgunLaserHandler.shotDirection(new Vec3(10, 64, 10),
            new Vec3(30000010, 64, 0), position -> position.add(-30000000, 0, 0));
        assertDirection(direction, 0, 0, -1);
    }

    @Test
    void rotatedSublevelHitsAimAtTheirWorldPosition() {
        Matrix4d pose = new Matrix4d().rotateY(Math.PI / 2).translate(-30000000, 0, 0);
        Vec3 direction = ToolgunLaserHandler.shotDirection(new Vec3(0, 64, 0),
            new Vec3(30000010, 64, 0), position -> {
                Vector3d world = pose.transformPosition(new Vector3d(position.x, position.y, position.z));
                return new Vec3(world.x(), world.y(), world.z());
            });
        assertDirection(direction, 0, 0, -1);
    }

    @Test
    void outlineStartsFadingImmediatelyAfterImpact() {
        assertEquals(1, ToolgunLaserHandler.outlineAlpha(0));
        assertEquals(0.9f, ToolgunLaserHandler.outlineAlpha(1));
        assertEquals(0.75f, ToolgunLaserHandler.outlineAlpha(2.5));
    }

    @Test
    void outlineDisappearsHalfASecondAfterImpact() {
        assertEquals(0.5f, ToolgunLaserHandler.outlineAlpha(5));
        assertEquals(0.25f, ToolgunLaserHandler.outlineAlpha(7.5));
        assertEquals(0, ToolgunLaserHandler.outlineAlpha(10));
        assertEquals(0, ToolgunLaserHandler.outlineAlpha(20));
    }

    private static void assertDirection(Vec3 actual, double x, double y, double z) {
        assertEquals(x, actual.x, 1e-8);
        assertEquals(y, actual.y, 1e-8);
        assertEquals(z, actual.z, 1e-8);
    }

    private static void assertVector(Vector3f expected, Vector3f actual) {
        assertEquals(expected.x(), actual.x(), 1e-5);
        assertEquals(expected.y(), actual.y(), 1e-5);
        assertEquals(expected.z(), actual.z(), 1e-5);
    }
}
