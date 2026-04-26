package com.item_management.client.key;

import org.lwjgl.glfw.GLFW;

import com.item_management.Itemmanagement;
import com.item_management.client.gui.BlockedItemsScreen;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.itemmanagement";
    public static final KeyMapping OPEN_BLOCKED_ITEMS_SCREEN = new KeyMapping(
            "key.itemmanagement.open_blocked_items",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY);

    private ModKeyMappings() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BLOCKED_ITEMS_SCREEN);
    }

    @EventBusSubscriber(modid = Itemmanagement.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {}

        @SubscribeEvent
        static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.screen != null) {
                return;
            }

            while (OPEN_BLOCKED_ITEMS_SCREEN.consumeClick()) {
                minecraft.setScreen(new BlockedItemsScreen());
            }
        }
    }
}
