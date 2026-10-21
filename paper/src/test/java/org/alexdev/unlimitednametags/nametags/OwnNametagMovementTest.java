package org.alexdev.unlimitednametags.nametags;

import org.alexdev.unlimitednametags.TestInstances;
import org.alexdev.unlimitednametags.UnlimitedNameTags;
import org.alexdev.unlimitednametags.config.ConfigManager;
import org.alexdev.unlimitednametags.config.Settings;
import org.alexdev.unlimitednametags.packet.PacketNameTag;
import org.alexdev.unlimitednametags.packet.PaperNametagRow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.Material;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OwnNametagMovementTest {
    private NameTagManager manager;
    private final Settings settings = new Settings();
    private final Player owner = mock(Player.class);
    private final UUID ownerId = UUID.randomUUID();
    private final PacketNameTag packet = mock(PacketNameTag.class, withSettings().extraInterfaces(PaperNametagRow.class));
    private final PaperNametagRow row = (PaperNametagRow) packet;
    private final Set<UUID> hiddenPreferences = ConcurrentHashMap.newKeySet();

    @BeforeEach void setup() throws Exception {
        manager = TestInstances.allocate(NameTagManager.class);
        UnlimitedNameTags plugin = TestInstances.allocate(UnlimitedNameTags.class);
        ConfigManager config = mock(ConfigManager.class);
        when(config.getSettings()).thenReturn(settings);
        TestInstances.set(plugin, "configManager", config);
        TestInstances.set(manager, "plugin", plugin);
        TestInstances.set(manager, "ownMovementEntries", new ConcurrentHashMap<>());
        TestInstances.set(manager, "hideOwnFromSelf", hiddenPreferences);
        var rows = new ConcurrentHashMap<UUID, CopyOnWriteArrayList<PacketNameTag>>();
        rows.put(ownerId, new CopyOnWriteArrayList<>(List.of(packet)));
        TestInstances.set(manager, "nameTags", rows);
        when(owner.getUniqueId()).thenReturn(ownerId);
        when(owner.getPose()).thenReturn(Pose.STANDING);
        TestInstances.set(settings.getVisibility(), "showCurrentNameTag", true);
    }

    private boolean suppressed() throws Exception {
        return (boolean) NameTagManager.class.getMethod("isOwnNametagSuppressedByMovement", Player.class).invoke(manager, owner);
    }
    private void reconcile() throws Exception {
        NameTagManager.class.getMethod("reconcileOwnNametagVisibility", Player.class).invoke(manager, owner);
    }

    @Test void swimmingProneGlidingAndSpinPoseSuppressOnlyOwnDisplay() throws Exception {
        for (Pose pose : List.of(Pose.SWIMMING, Pose.FALL_FLYING, Pose.SPIN_ATTACK)) {
            when(owner.getPose()).thenReturn(pose);
            assertTrue(suppressed(), pose.name());
        }
    }

    @Test void flightAndSwimmingFlagsSuppressEvenBeforePoseUpdates() throws Exception {
        when(owner.isFlying()).thenReturn(true);
        assertTrue(suppressed());
        when(owner.isFlying()).thenReturn(false);
        when(owner.isSwimming()).thenReturn(true);
        assertTrue(suppressed());
        when(owner.isSwimming()).thenReturn(false);
        when(owner.isGliding()).thenReturn(true);
        assertTrue(suppressed());
    }

    @Test void waterContactSuppressesBeforeSwimmingStarts() throws Exception {
        when(owner.isInWater()).thenReturn(true);
        assertTrue(suppressed());
        reconcile();
        verify(row, never()).showToPlayer(any());
        when(owner.isInWater()).thenReturn(false);
        reconcile();
        verify(row).showToPlayer(owner);
        assertTrue(hiddenPreferences.isEmpty());
    }

    @Test void wornElytraSuppressesWhileStandingAndRemovalRestores() throws Exception {
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(owner.getInventory()).thenReturn(inventory);
        ItemStack elytra = mock(ItemStack.class);
        when(elytra.getType()).thenReturn(Material.ELYTRA);
        when(inventory.getChestplate()).thenReturn(elytra);
        assertTrue(suppressed());
        reconcile();
        verify(row, never()).showToPlayer(any());
        when(inventory.getChestplate()).thenReturn(null);
        reconcile();
        verify(row).showToPlayer(owner);
        assertTrue(hiddenPreferences.isEmpty());
    }

    @Test void earlyHidingCanBeDisabledWithoutDisablingActualMovementHiding() throws Exception {
        TestInstances.set(settings.getVisibility(), "hideOwnNametagWhileInWaterOrWearingElytra", false);
        when(owner.isInWater()).thenReturn(true);
        assertFalse(suppressed());
        when(owner.isSwimming()).thenReturn(true);
        assertTrue(suppressed());
    }

    @Test void chestArmourDoesNotTriggerElytraHiding() throws Exception {
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(owner.getInventory()).thenReturn(inventory);
        ItemStack armour = mock(ItemStack.class);
        when(armour.getType()).thenReturn(Material.DIAMOND_CHESTPLATE);
        when(inventory.getChestplate()).thenReturn(armour);
        assertFalse(suppressed());
    }

    @Test void walkingSneakingAndSleepingKeepExistingPreference() throws Exception {
        for (Pose pose : List.of(Pose.STANDING, Pose.SNEAKING, Pose.SLEEPING)) {
            when(owner.getPose()).thenReturn(pose);
            assertFalse(suppressed(), pose.name());
            assertTrue(manager.isEffectiveShowOwnNametag(owner));
        }
    }

    @Test void hidingDoesNotPersistPreferenceOrTouchOtherViewers() throws Exception {
        when(owner.getPose()).thenReturn(Pose.SWIMMING);
        reconcile();
        verify(packet).hideFromViewer(ownerId);
        verify(packet, never()).hideForOwner();
        verify(packet, never()).remove();
        verify(row, never()).showToPlayer(any());
        assertTrue(hiddenPreferences.isEmpty());
        assertTrue(manager.isEffectiveShowOwnNametag(owner));
    }

    @Test void standingRestoresOwnTagWhenPreferenceIsEnabled() throws Exception {
        reconcile();
        verify(row).showToPlayer(owner);
        verify(packet, never()).showForOwner();
    }

    @Test void preferenceDisabledDuringMovementStaysDisabledAfterward() throws Exception {
        hiddenPreferences.add(ownerId);
        reconcile();
        verify(packet).hideFromViewer(ownerId);
        verify(row, never()).showToPlayer(any());
        assertEquals(Set.of(ownerId), hiddenPreferences);
    }

    @Test void directShowToOwnerRejectsMovement() {
        when(owner.isFlying()).thenReturn(true);
        manager.showToOwner(owner);
        verify(packet, never()).spawnForOwner();
    }

    @Test void configEscapeHatchRestoresPreviousMovementEligibility() throws Exception {
        TestInstances.set(settings.getVisibility(), "hideOwnNametagWhileSwimmingOrFlying", false);
        when(owner.getPose()).thenReturn(Pose.SWIMMING);
        assertFalse(suppressed());
    }
}
