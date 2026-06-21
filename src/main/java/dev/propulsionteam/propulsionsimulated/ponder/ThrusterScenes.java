package dev.propulsionteam.propulsionsimulated.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlockEntity;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterParticleType;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class ThrusterScenes {
    public static void normal(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("ponder_thruster_normal", "Thruster Fuel Inputs");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        Selection lowerCog = util.select().position(4, 1, 3);
        Selection upperCogs = util.select().fromTo(3, 1, 3, 2, 1, 3);
        Selection pump = util.select().position(2, 1, 2);
        Selection pipeLine = util.select().fromTo(0, 1, 2, 3, 1, 2);
        Selection lever = util.select().position(2, 1, 1);
        Selection thruster = util.select().position(3, 1, 1);
        Selection motor = util.select().position(4, 1, 3);

        scene.world().showSection(pipeLine, Direction.WEST);
        scene.world().showSection(lever, Direction.DOWN);
        scene.world().showSection(thruster, Direction.DOWN);
        scene.world().showSection(util.select().position(2, 1, 3), Direction.DOWN);
        scene.world().showSection(util.select().position(3, 1, 3), Direction.DOWN);
        scene.world().showSection(motor, Direction.WEST);
        scene.idle(20);

        scene.world().setKineticSpeed(lowerCog, 64);
        scene.world().setKineticSpeed(upperCogs, -64);
        scene.world().setKineticSpeed(pump, 64);

        scene.overlay().showText(80)
            .sharedText("thruster_normal.fuels_intro")
            .pointAt(util.vector().centerOf(util.grid().at(1, 1, 2)))
            .placeNearTarget();
        scene.idle(90);

        scene.overlay().showControls(util.vector().centerOf(util.grid().at(0, 1, 2)), Pointing.RIGHT, 70)
            .rightClick()
            .withItem(new ItemStack(Items.WATER_BUCKET));
        scene.overlay().showText(70)
            .colored(PonderPalette.RED)
            .sharedText("thruster_normal.invalid_fuel")
            .pointAt(util.vector().centerOf(util.grid().at(1, 1, 2)))
            .placeNearTarget();
        scene.idle(80);

        scene.overlay().showControls(util.vector().centerOf(util.grid().at(0, 1, 2)), Pointing.RIGHT, 80)
            .rightClick()
            .withItem(new ItemStack(Items.LAVA_BUCKET));
        scene.overlay().showText(80)
            .colored(PonderPalette.GREEN)
            .sharedText("thruster_normal.valid_fuels")
            .pointAt(util.vector().centerOf(util.grid().at(1, 1, 2)))
            .placeNearTarget();
        scene.idle(90);

        scene.world().modifyBlockEntityNBT(thruster, ThrusterBlockEntity.class, nbt -> {
            CompoundTag lava = new CompoundTag();
            lava.putString("FluidName", "minecraft:lava");
            lava.putString("Fluid", "minecraft:lava");
            lava.putString("id", "minecraft:lava");
            lava.putInt("Amount", 200);
            nbt.put("TankContent", lava);
            nbt.put("Tank", lava.copy());
            nbt.putInt("RedstoneInput", 15);
        });
        scene.effects().indicateRedstone(util.grid().at(3, 1, 1));
        scene.overlay().showText(90)
            .attachKeyFrame()
            .colored(PonderPalette.GREEN)
            .sharedText("thruster_normal.power_and_thrust")
            .pointAt(util.vector().centerOf(util.grid().at(3, 1, 1)))
            .placeNearTarget();
        emitManualThrusterBurst(scene, util, 3, 1, 1, 0.0, 0.0, 0.0, Direction.NORTH, 12);
        scene.idle(100);
        scene.markAsFinished();
    }

    public static void multiblock2x2(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("ponder_thruster_2x2", "2x2 Thruster Inputs");
        scene.configureBasePlate(0, 0, 6);
        scene.showBasePlate();
        scene.idle(10);

        Selection base = util.select().layersFrom(1);
        Selection thrusterCube = util.select().fromTo(3, 1, 2, 4, 2, 3);
        Selection rearInputNoClutch = util.select().fromTo(0, 4, 2, 3, 4, 2)
            .add(util.select().position(3, 4, 3));
        Selection topPowerTrain = util.select().position(3, 3, 3)
            .add(util.select().position(3, 3, 2))
            .add(util.select().position(3, 4, 3));
        Selection rearInputWithClutch = util.select().fromTo(0, 1, 3, 2, 1, 3)
            .add(util.select().position(1, 1, 2))
            .add(util.select().position(0, 1, 2));
        Selection clutch = util.select().position(1, 1, 2);
        Selection bottomPowerTrain = util.select().position(2, 1, 2)
            .add(util.select().position(2, 1, 3))
            .add(util.select().position(0, 1, 2));

        scene.world().showSection(base, Direction.DOWN);
        scene.world().modifyBlock(util.grid().at(1, 1, 2),
            state -> state.setValue(BlockStateProperties.POWERED, true), false);
        scene.world().setKineticSpeed(rearInputWithClutch, 0);
        scene.world().setKineticSpeed(bottomPowerTrain, 0);
        scene.idle(20);

        scene.overlay().showText(40)
            .sharedText("thruster_2x2.composed_of_blocks")
            .pointAt(util.vector().centerOf(util.grid().at(3, 2, 2)))
            .placeNearTarget();
        scene.effects().indicateSuccess(util.grid().at(3, 1, 2));
        scene.effects().indicateSuccess(util.grid().at(4, 1, 2));
        scene.effects().indicateSuccess(util.grid().at(3, 1, 3));
        scene.effects().indicateSuccess(util.grid().at(4, 1, 3));
        scene.effects().indicateSuccess(util.grid().at(3, 2, 2));
        scene.effects().indicateSuccess(util.grid().at(4, 2, 2));
        scene.effects().indicateSuccess(util.grid().at(3, 2, 3));
        scene.effects().indicateSuccess(util.grid().at(4, 2, 3));
        scene.idle(40);
        // Invalid top feed: running and pumping, but not into the rear intake.
        scene.world().setKineticSpeed(rearInputNoClutch, 64);
        scene.world().setKineticSpeed(topPowerTrain, 64);
        scene.overlay().showText(70)
            .attachKeyFrame()
            .colored(PonderPalette.RED)
            .sharedText("thruster_2x2.top_feed_invalid")
            .pointAt(util.vector().centerOf(util.grid().at(2, 4, 2)))
            .placeNearTarget();
        scene.overlay().showControls(util.vector().centerOf(util.grid().at(0, 4, 2)), Pointing.RIGHT, 45)
            .rightClick()
            .withItem(new ItemStack(Items.LAVA_BUCKET));
        scene.idle(70);
        scene.world().hideSection(rearInputNoClutch.add(topPowerTrain), Direction.UP);
        scene.idle(40);
        scene.world().modifyBlock(util.grid().at(1, 1, 2),
            state -> state.setValue(BlockStateProperties.POWERED, false), false);
        scene.world().setKineticSpeed(rearInputWithClutch, -64);
        scene.world().setKineticSpeed(bottomPowerTrain, 64);
        scene.effects().indicateRedstone(clutch.iterator().next());

        scene.overlay().showControls(util.vector().centerOf(util.grid().at(0, 1, 3)), Pointing.RIGHT, 45)
            .rightClick()
            .withItem(new ItemStack(Items.LAVA_BUCKET));
        scene.overlay().showText(80)
            .sharedText("thruster_2x2.valid_fuel_now")
            .pointAt(util.vector().centerOf(util.grid().at(1, 1, 3)))
            .placeNearTarget();
        scene.idle(90);

        scene.world().modifyBlockEntityNBT(thrusterCube, ThrusterBlockEntity.class, nbt -> {
            CompoundTag lava = new CompoundTag();
            lava.putString("FluidName", "minecraft:lava");
            lava.putString("Fluid", "minecraft:lava");
            lava.putString("id", "minecraft:lava");
            lava.putInt("Amount", 1200);
            nbt.put("TankContent", lava);
            nbt.put("Tank", lava.copy());
            nbt.putInt("RedstoneInput", 15);
        });
        scene.effects().indicateRedstone(util.grid().at(3, 1, 2));
        scene.overlay().showText(80)
            .colored(PonderPalette.GREEN)
            .sharedText("thruster_2x2.power_thruster")
            .pointAt(util.vector().centerOf(util.grid().at(3, 1, 2)))
            .placeNearTarget();
        emitManualThrusterBurst(scene, util, 3, 1, 1, 0.5, 0.0, 0.0, Direction.NORTH, 18);
        scene.idle(90);
        scene.markAsFinished();
    }

    public static void multiblock3x3(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("ponder_thruster_3x3", "3x3 Thruster Inputs");
        scene.configureBasePlate(0, 0, 6);
        scene.showBasePlate();
        scene.idle(10);

        Selection all = util.select().layersFrom(1);
        Selection thrusterCube = util.select().fromTo(3, 1, 1, 5, 3, 3);
        Selection topLine = util.select().fromTo(0, 5, 2, 3, 5, 2).add(util.select().position(3, 5, 3));
        Selection topPowerTrain = util.select().position(3, 4, 3).add(util.select().position(3, 4, 2)).add(util.select().position(3, 5, 3));
        Selection bottomLine = util.select().fromTo(0, 1, 3, 2, 1, 3).add(util.select().position(1, 1, 2));
        Selection clutch = util.select().position(1, 1, 2);
        Selection bottomPowerTrain = util.select().position(0, 1, 2).add(util.select().position(2, 1, 2)).add(util.select().position(2, 1, 3));
        scene.world().showSection(all, Direction.DOWN);
        scene.world().modifyBlock(util.grid().at(1, 1, 2),
            state -> state.setValue(BlockStateProperties.POWERED, true), false);
        scene.world().setKineticSpeed(bottomLine, 0);
        scene.world().setKineticSpeed(bottomPowerTrain, 0);
        scene.idle(20);


        scene.overlay().showText(90)
            .sharedText("thruster_3x3.composed_of_blocks")
            .pointAt(util.vector().centerOf(util.grid().at(4, 2, 2)))
            .placeNearTarget();
        for (int y = 1; y <= 3; y++) {
            for (int z = 1; z <= 3; z++) {
                for (int x = 3; x <= 5; x++) {
                    scene.effects().indicateSuccess(util.grid().at(x, y, z));
                    scene.idle(2);
                }
            }
        }
        scene.idle(30);

        scene.world().setKineticSpeed(topLine, 64);
        scene.world().setKineticSpeed(topPowerTrain, 64);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .colored(PonderPalette.RED)
            .sharedText("thruster_3x3.top_line_invalid")
            .pointAt(util.vector().centerOf(util.grid().at(1, 5, 2)))
            .placeNearTarget();
        scene.overlay().showControls(util.vector().centerOf(util.grid().at(0, 5, 2)), Pointing.RIGHT, 45)
            .rightClick()
            .withItem(new ItemStack(Items.LAVA_BUCKET));
        scene.idle(90);

        scene.world().hideSection(topLine.add(topPowerTrain), Direction.UP);
        scene.idle(20);

        scene.world().modifyBlock(util.grid().at(1, 1, 2),
            state -> state.setValue(BlockStateProperties.POWERED, false), false);
        scene.world().setKineticSpeed(bottomLine, -64);
        scene.world().setKineticSpeed(bottomPowerTrain, 64);
        scene.effects().indicateRedstone(clutch.iterator().next());
        scene.overlay().showControls(util.vector().centerOf(util.grid().at(1, 1, 2)), Pointing.DOWN, 35)
            .rightClick();
        scene.overlay().showText(80)
            .sharedText("thruster_3x3.back_layer_inputs")
            .pointAt(util.vector().centerOf(util.grid().at(1, 1, 3)))
            .placeNearTarget();
        scene.idle(90);

        scene.overlay().showControls(util.vector().centerOf(util.grid().at(0, 1, 3)), Pointing.RIGHT, 45)
            .rightClick()
            .withItem(new ItemStack(Items.LAVA_BUCKET));
        scene.overlay().showText(80)
            .sharedText("thruster_3x3.valid_fuel_now")
            .pointAt(util.vector().centerOf(util.grid().at(1, 1, 2)))
            .placeNearTarget();
        scene.idle(90);

        scene.world().modifyBlockEntityNBT(thrusterCube, ThrusterBlockEntity.class, nbt -> {
            CompoundTag lava = new CompoundTag();
            lava.putString("FluidName", "minecraft:lava");
            lava.putString("Fluid", "minecraft:lava");
            lava.putString("id", "minecraft:lava");
            lava.putInt("Amount", 2000);
            nbt.put("TankContent", lava);
            nbt.put("Tank", lava.copy());
            nbt.putInt("RedstoneInput", 15);
        });
        scene.effects().indicateRedstone(util.grid().at(3, 1, 1));
        scene.overlay().showText(80)
            .colored(PonderPalette.GREEN)
            .sharedText("thruster_3x3.power_thruster")
            .pointAt(util.vector().centerOf(util.grid().at(3, 1, 1)))
            .placeNearTarget();
        emitManualThrusterBurst(scene, util, 4, 2, 0, 0.0, 0.0, 0.0, Direction.NORTH, 30);
        scene.idle(90);
        scene.markAsFinished();
    }

    private static void emitManualThrusterBurst(CreateSceneBuilder scene, SceneBuildingUtil util, int x, int y, int z,
            double offsetX, double offsetY, double offsetZ, Direction exhaustDirection, int particlesPerTick) {
        scene.effects().emitParticles(util.vector().centerOf(util.grid().at(x, y, z)).add(offsetX, offsetY, offsetZ), (world, px, py, pz) -> {
            for (int i = 0; i < particlesPerTick; i++) {
                double lateral = 0.08;
                double ox = (world.random.nextDouble() - 0.5) * lateral;
                double oy = (world.random.nextDouble() - 0.5) * lateral;
                double oz = (world.random.nextDouble() - 0.5) * lateral;
                switch (exhaustDirection.getAxis()) {
                    case X -> ox = 0.0;
                    case Y -> oy = 0.0;
                    case Z -> oz = 0.0;
                }
                double vx = exhaustDirection.getStepX() * 0.55 + (world.random.nextDouble() - 0.5) * 0.02;
                double vy = exhaustDirection.getStepY() * 0.55 + (world.random.nextDouble() - 0.5) * 0.02;
                double vz = exhaustDirection.getStepZ() * 0.55 + (world.random.nextDouble() - 0.5) * 0.02;
                world.addParticle(ThrusterParticleType.PLUME.createParticleOptions(), px + ox, py + oy, pz + oz, vx, vy, vz);
            }
        }, 1, 60);
    }
}

