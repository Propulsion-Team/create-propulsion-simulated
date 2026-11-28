package dev.propulsionteam.propulsionsimulated.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import dev.propulsionteam.propulsionsimulated.content.thruster.PropulsionCoreBehaviour;
import dev.propulsionteam.propulsionsimulated.content.thruster.rcs_thruster.RcsOrientation;
import dev.propulsionteam.propulsionsimulated.content.thruster.rcs_thruster.RcsThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.ponder.instructions.FollowTextInstruction;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionItems;
import dev.simulated_team.simulated.ponder.SmoothMovementUtils;
import dev.simulated_team.simulated.ponder.instructions.CustomAnimateWorldSectionInstruction;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class VectorCoreScenes {
    private VectorCoreScenes() {
    }

    public static void vectorCore(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("vector_core", "Vector Core");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos casingPos = util.grid().at(2, 3, 2);
        BlockPos north = util.grid().at(2, 3, 1);
        BlockPos west = util.grid().at(1, 3, 2);
        BlockPos east = util.grid().at(3, 3, 2);
        BlockPos south = util.grid().at(2, 3, 3);
        Direction[] normals = {Direction.NORTH, Direction.WEST, Direction.EAST, Direction.SOUTH};
        BlockPos[] thrusters = {north, west, east, south};

        for (int i = 0; i < thrusters.length; i++) {
            setUpThrust(scene, thrusters[i], normals[i], 0f);
        }

        ElementLink<WorldSectionElement> ship = scene.world()
            .showIndependentSection(util.select().fromTo(1, 3, 1, 3, 3, 3), Direction.DOWN);
        scene.world().configureCenterOfRotation(ship, util.vector().centerOf(casingPos));
        scene.addInstruction(new FollowTextInstruction(ship, 1800));
        scene.idle(15);

        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("vector_core.intro")
            .pointAt(util.vector().centerOf(casingPos))
            .placeNearTarget();
        scene.idle(100);

        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("vector_core.fail")
            .pointAt(util.vector().centerOf(north))
            .placeNearTarget();
        scene.idle(40);
        setUpThrust(scene, north, Direction.NORTH, 1f);
        setUpThrust(scene, west, Direction.WEST, 1f);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(ship, new Vec3(0, 0.8, 0), 55,
            SmoothMovementUtils.quadraticRiseOut()));
        scene.addInstruction(CustomAnimateWorldSectionInstruction.rotate(ship, new Vec3(160, 0, -160), 55,
            SmoothMovementUtils.quadraticRise()));
        scene.idle(60);
        setUpThrust(scene, north, Direction.NORTH, 0f);
        setUpThrust(scene, west, Direction.WEST, 0f);
        scene.idle(30);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.rotate(ship, new Vec3(-160, 0, 160), 40,
            SmoothMovementUtils.quadraticRiseInOut()));
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(ship, new Vec3(0, -0.8, 0), 40,
            SmoothMovementUtils.quadraticRiseInOut()));
        scene.idle(50);

        scene.overlay().showText(80)
            .attachKeyFrame()
            .sharedText("vector_core.install")
            .pointAt(util.vector().topOf(north))
            .placeNearTarget();
        scene.idle(20);
        installCore(scene, util, north, Direction.NORTH);
        installCore(scene, util, west, Direction.WEST);
        scene.idle(30);

        scene.overlay().showText(100)
            .attachKeyFrame()
            .sharedText("vector_core.center")
            .pointAt(util.vector().centerOf(casingPos))
            .placeNearTarget();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, "vector_core_center",
            new AABB(util.vector().centerOf(casingPos), util.vector().centerOf(casingPos)).inflate(0.25), 100);
        scene.idle(110);

        scene.overlay().showText(80)
            .sharedText("vector_core.success")
            .pointAt(util.vector().centerOf(north))
            .placeNearTarget();
        scene.idle(20);
        setUpThrust(scene, north, Direction.NORTH, 1f);
        setUpThrust(scene, west, Direction.WEST, 1f);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(ship, new Vec3(0, 1.4, 0), 60,
            SmoothMovementUtils.quadraticRise()));
        scene.idle(65);
        setUpThrust(scene, north, Direction.NORTH, 0f);
        setUpThrust(scene, west, Direction.WEST, 0f);
        scene.idle(10);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(ship, new Vec3(0, -1.4, 0), 40,
            SmoothMovementUtils.quadraticRiseInOut()));
        scene.idle(60);

        scene.overlay().showText(80)
            .attachKeyFrame()
            .sharedText("vector_core.single")
            .pointAt(util.vector().centerOf(north))
            .placeNearTarget();
        scene.idle(20);
        setUpThrust(scene, north, Direction.NORTH, 1f);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(ship, new Vec3(0, 1.0, 0), 60,
            SmoothMovementUtils.quadraticRise()));
        scene.idle(65);
        setUpThrust(scene, north, Direction.NORTH, 0f);
        scene.idle(10);
        scene.addInstruction(CustomAnimateWorldSectionInstruction.move(ship, new Vec3(0, -1.0, 0), 40,
            SmoothMovementUtils.quadraticRiseInOut()));
        scene.idle(60);

        scene.overlay().showText(110)
            .attachKeyFrame()
            .sharedText("vector_core.durability")
            .pointAt(util.vector().centerOf(north))
            .placeNearTarget();
        scene.idle(120);
        scene.markAsFinished();
    }

    private static void installCore(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, Direction normal) {
        ItemStack core = new ItemStack(PropulsionItems.VECTOR_CORE.get());
        scene.overlay().showControls(util.vector().blockSurface(pos, Direction.UP), Pointing.DOWN, 25)
            .rightClick()
            .withItem(core);
        scene.idle(30);
        scene.world().modifyBlockEntity(pos, RcsThrusterBlockEntity.class, be -> {
            PropulsionCoreBehaviour behaviour = be.getBehaviour(PropulsionCoreBehaviour.TYPE);
            if (behaviour != null) {
                behaviour.getItemHandler().setStackInSlot(0, core.copy());
            }
        });
        scene.effects().indicateSuccess(pos);
        scene.idle(15);
    }

    private static void setUpThrust(CreateSceneBuilder scene, BlockPos pos, Direction normal, float power) {
        int nozzle = nozzlePushingUp(normal);
        scene.world().modifyBlockEntity(pos, RcsThrusterBlockEntity.class,
            be -> be.applyNozzlePowerForScene(nozzle, power));
    }

    private static int nozzlePushingUp(Direction normal) {
        for (int nozzle = 0; nozzle < 4; nozzle++) {
            if (RcsOrientation.direction(RcsOrientation.exhaust(false, nozzle), normal) == Direction.DOWN) {
                return nozzle;
            }
        }
        return 0;
    }
}
