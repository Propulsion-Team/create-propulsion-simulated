package dev.propulsionteam.propulsionsimulated.ponder;

import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.cable.relay.CableRelayBlock;
import dev.propulsionteam.propulsionsimulated.content.platinum.CoralGeneratorBlock;
import dev.propulsionteam.propulsionsimulated.content.platinum.CoralGeneratorBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.ion_thruster.IonThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlocks;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionFluids;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionItems;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public final class CableRelayScenes {
    private CableRelayScenes() {
    }

    public static void cableRelay(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("cable_relay", "Cable Relay");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos leverPos = util.grid().at(0, 1, 1);
        BlockPos generatorPos = util.grid().at(1, 1, 3);
        BlockPos thrusterPos = util.grid().at(3, 1, 2);
        Selection relays = util.select().fromTo(0, 1, 2, 2, 1, 2);
        Selection lever = util.select().position(leverPos);
        Selection generator = util.select().position(generatorPos);
        Selection thruster = util.select().position(thrusterPos);
        int energyCapacity = PropulsionConfig.ION_THRUSTER_ENERGY_CAPACITY_FE.get();

        setThruster(scene, thruster, 0, 0);
        setLever(scene, lever, 0);
        setRelaysPowered(scene, relays, false);
        scene.world().setBlock(generatorPos, PropulsionBlocks.CORAL_GENERATOR.get().defaultBlockState()
            .setValue(CoralGeneratorBlock.FACING, Direction.NORTH), false);

        scene.world().showSection(relays, Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(thruster, Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(90)
            .attachKeyFrame()
            .sharedText("cable_relay.intro")
            .pointAt(util.vector().centerOf(util.grid().at(1, 1, 2)))
            .placeNearTarget();
        scene.idle(100);

        scene.world().showSection(generator, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(util.vector().blockSurface(generatorPos, Direction.EAST), Pointing.LEFT, 25)
            .rightClick()
            .withItem(new ItemStack(PropulsionItems.CORAL_BUCKET.get()));
        scene.idle(30);
        scene.world().modifyBlockEntity(generatorPos, CoralGeneratorBlockEntity.class, be -> {
            var handler = be.getFluidHandler(null);
            if (handler != null) {
                handler.fill(new FluidStack(PropulsionFluids.CORAL.get(), 1000), FluidAction.EXECUTE);
            }
        });
        scene.effects().indicateSuccess(generatorPos);
        scene.idle(15);

        scene.overlay().showText(100)
            .attachKeyFrame()
            .sharedText("cable_relay.power")
            .pointAt(util.vector().centerOf(util.grid().at(1, 1, 2)))
            .placeNearTarget();
        for (int x = 1; x >= 0; x--) {
            scene.effects().indicateRedstone(util.grid().at(x, 1, 2));
            scene.idle(4);
        }
        for (int x = 1; x <= 2; x++) {
            scene.effects().indicateRedstone(util.grid().at(x, 1, 2));
            scene.idle(4);
        }
        setThruster(scene, thruster, energyCapacity, 0);
        scene.effects().indicateSuccess(thrusterPos);
        scene.idle(80);

        scene.world().showSection(lever, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
            .attachKeyFrame()
            .sharedText("cable_relay.redstone")
            .pointAt(util.vector().centerOf(leverPos))
            .placeNearTarget();
        scene.idle(60);

        setLever(scene, lever, 9);
        setRelaysPowered(scene, relays, true);
        scene.effects().indicateRedstone(leverPos);
        scene.idle(10);
        for (int x = 0; x <= 2; x++) {
            scene.effects().indicateRedstone(util.grid().at(x, 1, 2));
            scene.idle(3);
        }
        setThruster(scene, thruster, energyCapacity, 9);
        scene.effects().indicateRedstone(thrusterPos);
        fireThruster(scene, util, 9, 110);
        scene.overlay().showText(100)
            .attachKeyFrame()
            .sharedText("cable_relay.lossless", 9)
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(110);

        setLever(scene, lever, 15);
        scene.effects().indicateRedstone(leverPos);
        setThruster(scene, thruster, energyCapacity, 15);
        scene.effects().indicateRedstone(thrusterPos);
        fireThruster(scene, util, 15, 100);
        scene.overlay().showText(90)
            .sharedText("cable_relay.lossless_full", 15)
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(100);

        setLever(scene, lever, 0);
        setRelaysPowered(scene, relays, false);
        setThruster(scene, thruster, energyCapacity, 0);
        scene.idle(30);
        scene.markAsFinished();
    }

    private static void setThruster(CreateSceneBuilder scene, Selection thruster, int energy, int signal) {
        scene.world().modifyBlockEntityNBT(thruster, IonThrusterBlockEntity.class, nbt -> {
            nbt.putInt("EnergyStored", energy);
            nbt.putInt("RedstoneInput", signal);
        });
    }

    private static void setLever(CreateSceneBuilder scene, Selection lever, int signal) {
        scene.world().modifyBlockEntityNBT(lever, AnalogLeverBlockEntity.class, nbt -> nbt.putInt("State", signal));
    }

    private static void setRelaysPowered(CreateSceneBuilder scene, Selection relays, boolean powered) {
        scene.world().modifyBlocks(relays, state -> state.hasProperty(CableRelayBlock.POWERED)
            ? state.setValue(CableRelayBlock.POWERED, powered) : state, false);
    }

    private static void fireThruster(CreateSceneBuilder scene, SceneBuildingUtil util, int signal, int durationTicks) {
        int particlesPerTick = Math.max(1, Math.round(10 * signal / 15f));
        VectorThrusterFeScenes.emitIonBurst(scene, util, 3, 1, 2, Direction.EAST, particlesPerTick, durationTicks);
    }
}
