package org.alexdev.unlimitednametags.platform;

import org.bukkit.entity.Pose;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Bukkit pose translation for a temporary, owner-only presentation decision. */
public final class OwnNametagMovementPolicy {
    private OwnNametagMovementPolicy() {}

    public static boolean isHorizontalPose(Pose pose) {
        return pose == Pose.SWIMMING || pose == Pose.FALL_FLYING || pose == Pose.SPIN_ATTACK;
    }

    public static boolean isPreparingForHorizontalMovement(Player player) {
        if (player.isInWater()) {
            return true;
        }
        if (player.getInventory() == null) {
            return false;
        }
        ItemStack chestplate = player.getInventory().getChestplate();
        return chestplate != null && chestplate.getType() == Material.ELYTRA;
    }
}
