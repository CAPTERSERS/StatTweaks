package net.captersers.stattweaks.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.captersers.stattweaks.fabric.FabricClientNetwork;

public final class StattweaksFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricClientNetwork.init();
    }
}
