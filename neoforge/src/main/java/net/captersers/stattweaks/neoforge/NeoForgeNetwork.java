package net.captersers.stattweaks.neoforge;

import net.captersers.stattweaks.STConfigReloader;
import net.captersers.stattweaks.network.ConfigSyncPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge network helper for configuration sync.
 */
public final class NeoForgeNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("NeoForgeNetwork");

    private NeoForgeNetwork() {}

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0.0");
        registrar.playToClient(
                ConfigSyncPayload.TYPE,
                ConfigSyncPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> STConfigReloader.applyClientSync(payload))
        );
    }

    public static void sendConfigToAll(MinecraftServer server, ConfigSyncPayload payload) {
        PacketDistributor.sendToAllPlayers(payload);
        LOGGER.debug("Sent config sync to all players");
    }
}

