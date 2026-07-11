package net.captersers.stattweaks.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload containing serialized configuration data (NBT) to sync from server to client.
 */
public class ConfigSyncPayload implements CustomPacketPayload {
    public static final Type<ConfigSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("stattweaks", "config_sync"));

    public static final StreamCodec<FriendlyByteBuf, ConfigSyncPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeNbt(payload.data),
            buf -> new ConfigSyncPayload(buf.readNbt())
    );

    private final CompoundTag data;

    public ConfigSyncPayload(CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data;
    }

    public CompoundTag getData() {
        return this.data;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

