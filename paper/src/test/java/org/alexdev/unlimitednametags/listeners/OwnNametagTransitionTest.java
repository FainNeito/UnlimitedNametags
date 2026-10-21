package org.alexdev.unlimitednametags.listeners;

import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.alexdev.unlimitednametags.TestInstances;
import org.alexdev.unlimitednametags.UnlimitedNameTags;
import org.alexdev.unlimitednametags.config.ConfigManager;
import org.alexdev.unlimitednametags.config.Settings;
import org.alexdev.unlimitednametags.nametags.NameTagManager;
import org.alexdev.unlimitednametags.packet.PacketNameTag;
import org.alexdev.unlimitednametags.packet.PaperNametagRow;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.mockito.MockedStatic;
import io.papermc.paper.registry.RegistryAccess;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OwnNametagTransitionTest {
    private static MockedStatic<RegistryAccess> registry;
    @BeforeAll static void installRegistry() { registry = TestInstances.potionRegistry(); }
    @AfterAll static void removeRegistry() { registry.close(); }
    private NameTagManager tags;
    private PlayerListener listener;
    private final Settings settings = new Settings();
    private final UUID id = UUID.randomUUID();
    private final Player owner = mock(Player.class);
    private final PacketNameTag packet = mock(PacketNameTag.class, withSettings().extraInterfaces(PaperNametagRow.class));
    private final PaperNametagRow row = (PaperNametagRow) packet;
    private final Map<UUID, Player> online = new ConcurrentHashMap<>();
    private final Set<UUID> hidden = ConcurrentHashMap.newKeySet();
    private final List<Pending> pending = new ArrayList<>();
    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private long tick;
    private record Pending(long due, Runnable action) {}

    @BeforeEach void setup() throws Exception {
        UnlimitedNameTags plugin = TestInstances.allocate(UnlimitedNameTags.class);
        tags = TestInstances.allocate(NameTagManager.class);
        listener = TestInstances.allocate(PlayerListener.class);
        ConfigManager config = mock(ConfigManager.class);
        when(config.getSettings()).thenReturn(settings);
        doAnswer(call -> {
            pending.add(new Pending(tick + call.getArgument(2, Long.class), call.getArgument(1)));
            return null;
        }).when(scheduler).runTaskLater(eq(owner), any(Runnable.class), anyLong());
        TestInstances.set(plugin, "configManager", config);
        TestInstances.set(plugin, "nametagManager", tags);
        TestInstances.set(plugin, "playerListener", listener);
        TestInstances.set(plugin, "taskScheduler", scheduler);
        TestInstances.set(tags, "plugin", plugin);
        TestInstances.set(tags, "ownMovementEntries", new ConcurrentHashMap<>());
        TestInstances.set(tags, "hideOwnFromSelf", hidden);
        TestInstances.set(tags, "nameTags", new ConcurrentHashMap<>(Map.of(id,
                new CopyOnWriteArrayList<>(List.of(packet)))));
        TestInstances.set(listener, "plugin", plugin);
        TestInstances.set(listener, "onlinePlayers", online);
        TestInstances.set(listener, "poseRetryRunIds", new ConcurrentHashMap<>());
        TestInstances.set(listener, "ownMovementWatchTasks", new ConcurrentHashMap<>());
        when(owner.getUniqueId()).thenReturn(id);
        when(owner.getPose()).thenReturn(Pose.STANDING);
        when(owner.isOnline()).thenReturn(true);
        when(owner.getGameMode()).thenReturn(GameMode.SURVIVAL);
        TestInstances.set(settings.getVisibility(), "showCurrentNameTag", true);
        online.put(id, owner);
    }

    private void advance(long ticks) {
        tick += ticks;
        List<Pending> due = pending.stream().filter(p -> p.due() <= tick).toList();
        pending.removeAll(due);
        due.forEach(p -> p.action().run());
    }

    private void swimEntry() { listener.onOwnTagSwim(new EntityToggleSwimEvent(owner, true)); }

    @Test void swimEntryBlocksRefreshBeforeServerFlagsChange() {
        swimEntry();
        assertTrue(tags.isOwnNametagSuppressedByMovement(owner));
        tags.reconcileOwnNametagVisibility(owner);
        tags.showToOwner(owner);
        verify(row, never()).showToPlayer(owner);
        verify(packet, never()).spawnForOwner();
        verify(packet, atLeastOnce()).hideFromViewer(id);
        assertTrue(hidden.isEmpty());
    }

    @Test void glideAndCreativeFlightEntryAlsoBlockBeforeFlagsChange() {
        listener.onOwnTagGlide(new EntityToggleGlideEvent(owner, true));
        assertTrue(tags.isOwnNametagSuppressedByMovement(owner));
        advance(5);
        listener.onOwnTagFlight(new PlayerToggleFlightEvent(owner, true));
        assertTrue(tags.isOwnNametagSuppressedByMovement(owner));
    }

    @Test void oneTickRecoveryCannotRevealAnUnsettledElytraStart() {
        listener.logicElytra(owner);
        advance(1);
        tags.reconcileOwnNametagVisibility(owner);
        assertTrue(tags.isOwnNametagSuppressedByMovement(owner));
        verify(row, never()).showToPlayer(owner);
    }

    @Test void failedStartRecoversAfterBoundedWindowWithoutChangingPreference() {
        swimEntry();
        advance(1);
        assertTrue(tags.isOwnNametagSuppressedByMovement(owner));
        advance(4);
        assertFalse(tags.isOwnNametagSuppressedByMovement(owner));
        verify(row).showToPlayer(owner);
        assertTrue(hidden.isEmpty());
    }

    @Test void olderEntryCallbackCannotReleaseANewerTransition() {
        swimEntry();
        advance(1);
        listener.logicElytra(owner);
        advance(4);
        assertTrue(tags.isOwnNametagSuppressedByMovement(owner));
        verify(row, never()).showToPlayer(owner);
        advance(1);
        assertFalse(tags.isOwnNametagSuppressedByMovement(owner));
        verify(row).showToPlayer(owner);
    }

    @Test void completedWindowStillHonoursAuthoritativeMovementAndDisabledPreference() {
        swimEntry();
        when(owner.isSwimming()).thenReturn(true);
        advance(5);
        assertTrue(tags.isOwnNametagSuppressedByMovement(owner));
        verify(row, never()).showToPlayer(owner);
        when(owner.isSwimming()).thenReturn(false);
        hidden.add(id);
        tags.reconcileOwnNametagVisibility(owner);
        verify(row, never()).showToPlayer(owner);
        assertEquals(Set.of(id), hidden);
    }

    @Test void replacedSessionCannotRecoverRowsFromOldCallback() {
        swimEntry();
        Player replacement = mock(Player.class);
        online.put(id, replacement);
        advance(5);
        verify(row, never()).showToPlayer(any());
    }

    @Test void disabledConfigDoesNotStartAnEntryGuard() throws Exception {
        TestInstances.set(settings.getVisibility(), "hideOwnNametagWhileSwimmingOrFlying", false);
        swimEntry();
        assertFalse(tags.isOwnNametagSuppressedByMovement(owner));
        verify(packet, never()).hideFromViewer(id);
    }

    @Test void poseEntryUsesTheSameSharedGuard() {
        listener.onPoseChange(new org.bukkit.event.entity.EntityPoseChangeEvent(owner, Pose.SWIMMING));
        assertTrue(tags.isOwnNametagSuppressedByMovement(owner));
        tags.reconcileOwnNametagVisibility(owner);
        verify(row, never()).showToPlayer(owner);
    }

    @Test void waterMoveBlocksBeforeWaterFlagAndCancelledMovesAreIgnoredByRegistration() throws Exception {
        org.bukkit.World world = mock(org.bukkit.World.class);
        org.bukkit.block.Block dry = mock(org.bukkit.block.Block.class);
        org.bukkit.block.Block water = mock(org.bukkit.block.Block.class);
        when(world.getBlockAt(any(org.bukkit.Location.class))).thenAnswer(call ->
                call.getArgument(0, org.bukkit.Location.class).getBlockX() == 1 ? water : dry);
        when(dry.getType()).thenReturn(org.bukkit.Material.AIR);
        when(water.getType()).thenReturn(org.bukkit.Material.WATER);
        listener.onOwnTagWaterEntry(new org.bukkit.event.player.PlayerMoveEvent(owner,
                new org.bukkit.Location(world, 0, 64, 0), new org.bukkit.Location(world, 1, 64, 0)));
        assertTrue(tags.isOwnNametagSuppressedByMovement(owner));
        assertTrue(PlayerListener.class.getMethod("onOwnTagWaterEntry", org.bukkit.event.player.PlayerMoveEvent.class)
                .getAnnotation(org.bukkit.event.EventHandler.class).ignoreCancelled());
    }

    private Runnable startWatch() {
        listener.startOwnMovementWatch(owner);
        var callback = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskTimer(eq(owner), callback.capture(), eq(1L), eq(1L));
        return callback.getValue();
    }

    @Test void watchHidesDisplayedRowsOnceAndRestoresAfterWaterExit() {
        Runnable watch = startWatch();
        when(owner.isInWater()).thenReturn(true);
        when(row.canPlayerSee(owner)).thenReturn(true, false);
        watch.run();
        watch.run();
        verify(packet, times(1)).hideFromViewer(id);
        when(owner.isInWater()).thenReturn(false);
        watch.run();
        verify(row).showToPlayer(owner);
        verify(packet, never()).remove();
        assertTrue(hidden.isEmpty());
    }

    @Test void watchRejectsOldSessionAndPreservesDisabledOwnPreference() {
        Runnable watch = startWatch();
        when(owner.isInWater()).thenReturn(true);
        watch.run();
        hidden.add(id);
        when(owner.isInWater()).thenReturn(false);
        watch.run();
        verify(row, never()).showToPlayer(owner);
        clearInvocations(packet, row);
        online.put(id, mock(Player.class));
        watch.run();
        verifyNoInteractions(packet);
    }

    @Test void replacingAWatchCancelsItsPreviousOwnerTask() {
        var task = mock(com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask.class);
        when(scheduler.runTaskTimer(eq(owner), any(Runnable.class), eq(1L), eq(1L))).thenReturn(task);
        listener.startOwnMovementWatch(owner);
        listener.startOwnMovementWatch(owner);
        verify(task).cancel();
    }

    @Test void watchWaitsForManagerDuringEnable() throws Exception {
        Runnable watch = startWatch();
        TestInstances.set(listener.getPlugin(), "nametagManager", null);
        assertDoesNotThrow(watch::run);
        TestInstances.set(listener.getPlugin(), "nametagManager", tags);
        when(owner.isInWater()).thenReturn(true);
        when(row.canPlayerSee(owner)).thenReturn(true);
        watch.run();
        verify(packet).hideFromViewer(id);
    }

    @Test void previousSessionEntryCannotBlockOrReleaseNewSessionsEntry() {
        UUID first = tags.beginOwnNametagMovementEntry(owner);
        Player replacement = mock(Player.class);
        when(replacement.getUniqueId()).thenReturn(id);
        when(replacement.getPose()).thenReturn(Pose.STANDING);
        assertFalse(tags.isOwnNametagSuppressedByMovement(replacement));
        UUID second = tags.beginOwnNametagMovementEntry(replacement);
        assertFalse(tags.endOwnNametagMovementEntry(owner, first));
        assertTrue(tags.isOwnNametagSuppressedByMovement(replacement));
        assertTrue(tags.endOwnNametagMovementEntry(replacement, second));
    }
}
