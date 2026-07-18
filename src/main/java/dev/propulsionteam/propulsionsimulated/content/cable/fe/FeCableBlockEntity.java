package dev.propulsionteam.propulsionsimulated.content.cable.fe;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.cable.CableNetworkManager;
import dev.propulsionteam.propulsionsimulated.content.cable.hub.CableHubBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.cable.relay.CableRelayBlockEntity;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.*;

public class FeCableBlockEntity extends SmartBlockEntity {
    private final IEnergyStorage storage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return 0;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    public FeCableBlockEntity(BlockPos pos, BlockState blockState) {
        super(PropulsionBlockEntities.FE_CABLE_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level != null) {
            CableNetworkManager.invalidateAround(level, worldPosition);
        }
    }

    @Override
    public void tick() {
        super.tick();
        CableNetworkManager.tick(level, worldPosition);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public IEnergyStorage getEnergyHandler(Direction side) {
        return storage;
    }

}
