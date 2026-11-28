package dev.propulsionteam.propulsionsimulated.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.propulsionteam.propulsionsimulated.content.lidar.LidarBlockEntity;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.Vec3;

public final class LidarScenes {
    private static final double RAY_Y = 1.5, RAY_Z = 2.5, PIG_X = 1.5, PIG_HALF_WIDTH = 0.45;

    private LidarScenes() {
    }

    public static void lidar(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("lidar", "Detecting with the Lidar");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos lidarPos = util.grid().at(3, 1, 2);
        BlockPos lampPos = util.grid().at(4, 1, 2);
        BlockPos targetPos = util.grid().at(1, 1, 2);
        Selection lidar = util.select().position(lidarPos);
        Selection lamp = util.select().position(lampPos);
        Selection target = util.select().position(targetPos);
        Vec3 sensor = new Vec3(3.6, RAY_Y, RAY_Z);

        scene.world().showSection(lidar, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .sharedText("lidar.intro")
            .pointAt(util.vector().centerOf(lidarPos))
            .placeNearTarget();
        scene.idle(10);
        scene.overlay().showLine(PonderPalette.GREEN, sensor, new Vec3(0, RAY_Y, RAY_Z), 70);
        scene.idle(80);

        scene.world().showSection(lamp, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .sharedText("lidar.output")
            .pointAt(util.vector().centerOf(lampPos))
            .placeNearTarget();
        scene.idle(90);

        ElementLink<EntityElement> pig = scene.world().createEntity(level -> {
            Pig entity = EntityType.PIG.create(level);
            entity.setNoAi(true);
            entity.setPos(PIG_X, 1, 5.2);
            entity.xo = PIG_X;
            entity.yo = 1;
            entity.zo = 5.2;
            entity.setYRot(180);
            entity.yRotO = 180;
            entity.yBodyRot = 180;
            entity.yBodyRotO = 180;
            entity.yHeadRot = 180;
            entity.yHeadRotO = 180;
            return entity;
        });
        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("lidar.entities")
            .pointAt(new Vec3(PIG_X, RAY_Y, RAY_Z))
            .placeNearTarget();
        scene.idle(20);

        boolean hit = false;
        for (double z = 5.2; z > -0.6; z -= 0.1) {
            double pigZ = z;
            boolean crossing = Math.abs(pigZ - RAY_Z) < PIG_HALF_WIDTH;
            scene.world().modifyEntity(pig, entity -> walkTo(entity, PIG_X, pigZ));
            if (crossing != hit) {
                hit = crossing;
                setOutput(scene, lidar, lampPos, hit ? 15 : 0);
                if (hit) {
                    scene.overlay().showLine(PonderPalette.RED, sensor, new Vec3(PIG_X + PIG_HALF_WIDTH, RAY_Y, RAY_Z), 9);
                    scene.effects().indicateRedstone(lampPos);
                }
            }
            scene.idle(1);
        }
        scene.world().modifyEntity(pig, Entity::discard);
        scene.idle(20);

        scene.world().showSection(target, Direction.DOWN);
        scene.idle(10);
        setOutput(scene, lidar, lampPos, 15);
        scene.effects().indicateRedstone(lampPos);
        scene.overlay().showLine(PonderPalette.RED, sensor, new Vec3(2, RAY_Y, RAY_Z), 90);
        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("lidar.analog")
            .pointAt(util.vector().blockSurface(targetPos, Direction.EAST))
            .placeNearTarget();
        scene.idle(100);

        scene.overlay().showControls(util.vector().topOf(lidarPos), Pointing.DOWN, 40).rightClick();
        scene.idle(10);
        scene.overlay().showText(100)
            .attachKeyFrame()
            .sharedText("lidar.settings")
            .pointAt(util.vector().centerOf(lidarPos))
            .placeNearTarget();
        scene.idle(110);

        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("lidar.invert")
            .pointAt(util.vector().centerOf(lampPos))
            .placeNearTarget();
        scene.idle(30);
        setOutput(scene, lidar, lampPos, 0);
        scene.idle(20);
        scene.world().hideSection(target, Direction.UP);
        scene.idle(10);
        setOutput(scene, lidar, lampPos, 15);
        scene.effects().indicateRedstone(lampPos);
        scene.overlay().showLine(PonderPalette.GREEN, sensor, new Vec3(0, RAY_Y, RAY_Z), 40);
        scene.idle(50);

        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("lidar.computer")
            .pointAt(util.vector().centerOf(lidarPos))
            .placeNearTarget();
        scene.idle(100);
        scene.markAsFinished();
    }

    private static void walkTo(Entity entity, double x, double z) {
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();
        entity.setPos(x, 1, z);
        if (entity instanceof Pig pig) pig.walkAnimation.update(0.4f, 1);
    }

    private static void setOutput(CreateSceneBuilder scene, Selection lidar, BlockPos lamp, int signal) {
        scene.world().modifyBlockEntityNBT(lidar, LidarBlockEntity.class, nbt -> nbt.putInt("Signal", signal));
        scene.world().modifyBlock(lamp, state -> state.setValue(RedstoneLampBlock.LIT, signal > 0), false);
    }
}
