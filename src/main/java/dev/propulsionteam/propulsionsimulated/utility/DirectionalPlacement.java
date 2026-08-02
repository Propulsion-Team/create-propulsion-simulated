package dev.propulsionteam.propulsionsimulated.utility;

import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;

/** Resolves the direction a directional block should use from a placement context. */
public final class DirectionalPlacement {
    private DirectionalPlacement() {
    }

    public static Direction nearestLookingDirection(BlockPlaceContext context) {
        if (context.getPlayer() instanceof DeployerFakePlayer) {
            // A Deployer interacts with the face opposite its extension direction. Using that
            // face avoids relying on the fake player's horizontal look rotation.
            return context.getClickedFace().getOpposite();
        }
        return context.getNearestLookingDirection();
    }
}
