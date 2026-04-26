package com.item_management;

import net.minecraft.world.level.GameRules;

public final class ModGameRules {
    public static final GameRules.Key<GameRules.BooleanValue> USE_PACK_DEFAULT_ITEM_RULES = GameRules.register(
            "itemmanagementUsePackDefaultRules",
            GameRules.Category.MISC,
            GameRules.BooleanValue.create(true));

    private ModGameRules() {}
}
