package org.alexdev.unlimitednametags.listeners;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import org.alexdev.unlimitednametags.TestInstances;
import org.alexdev.unlimitednametags.UnlimitedNameTags;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

class OwnMovementPacketInputTest {
    @Test void elytraInputCannotTargetADifferentPlayer() throws Exception {
        assertInputTarget(2, false);
    }

    @Test void elytraInputForTheSenderIsAccepted() throws Exception {
        assertInputTarget(1, true);
    }

    private void assertInputTarget(int targetEntityId, boolean accepted) throws Exception {
        UnlimitedNameTags plugin = TestInstances.allocate(UnlimitedNameTags.class);
        PlayerListener players = mock(PlayerListener.class);
        TestInstances.set(plugin, "playerListener", players);
        Player sender = mock(Player.class);
        when(sender.getEntityId()).thenReturn(1);
        when(players.getPlayerFromEntityId(targetEntityId)).thenReturn(Optional.of(sender));
        PacketReceiveEvent event = mock(PacketReceiveEvent.class);
        doReturn(sender).when(event).getPlayer();
        when(event.getPacketType()).thenReturn(PacketType.Play.Client.ENTITY_ACTION);
        try (var construction = mockConstruction(WrapperPlayClientEntityAction.class, (packet, context) -> {
            when(packet.getEntityId()).thenReturn(targetEntityId);
            when(packet.getAction()).thenReturn(WrapperPlayClientEntityAction.Action.START_FLYING_WITH_ELYTRA);
        })) {
            new PacketEventsListener(plugin).onPacketReceive(event);
        }
        if (accepted) {
            verify(players).logicElytra(sender);
        } else {
            verifyNoInteractions(players);
        }
    }
}
