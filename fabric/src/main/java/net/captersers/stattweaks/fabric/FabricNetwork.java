package net.captersers.stattweaks.fabric;

import net.captersers.stattweaks.network.ConfigSyncPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric network helpers for sending configuration payloads to clients.
 */
public final class FabricNetwork {

    private FabricNetwork() {}

    public static void init() {
        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.CODEC);
    }

    public static void sendConfigToAll(MinecraftServer server, ConfigSyncPayload payload) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}

