package com.item_management.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class BlockedItemsSavedData extends SavedData {
    private static final String FILE_ID = "itemmanagement_blocked_items";
    private static final String BLOCKED_ITEMS_KEY = "blocked_items";
    private static final String CUSTOM_RULES_SET_KEY = "custom_rules_set";
    private static final String DISPLAY_ENTRIES_KEY = "display_entries";
    private static final String ENTRY_TYPE_KEY = "type";
    private static final String ENTRY_LABEL_KEY = "label";
    private static final String ENTRY_ITEMS_KEY = "items";
    private static final String ENTRY_REPLACEMENT_KEY = "replacement";
    private static final String ENTRY_EXPANDED_KEY = "expanded";
    private static final String TYPE_SINGLE = "single";
    private static final String TYPE_GROUP = "group";
    private static final ResourceLocation AIR_ID = ResourceLocation.parse("minecraft:air");
    private static final SavedData.Factory<BlockedItemsSavedData> FACTORY = new SavedData.Factory<>(
            BlockedItemsSavedData::new,
            BlockedItemsSavedData::load);

    private final Set<ResourceLocation> blockedItemIds = new LinkedHashSet<>();
    private final List<BlockedEntryData> displayEntries = new ArrayList<>();
    private boolean customRulesSet;

    public static BlockedItemsSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    private static BlockedItemsSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BlockedItemsSavedData data = new BlockedItemsSavedData();
        data.customRulesSet = tag.getBoolean(CUSTOM_RULES_SET_KEY);
        ListTag displayEntries = tag.getList(DISPLAY_ENTRIES_KEY, Tag.TAG_COMPOUND);
        if (!displayEntries.isEmpty()) {
            for (int index = 0; index < displayEntries.size(); index++) {
                CompoundTag entryTag = displayEntries.getCompound(index);
                String type = entryTag.getString(ENTRY_TYPE_KEY);
                String label = entryTag.getString(ENTRY_LABEL_KEY);
                boolean expanded = entryTag.getBoolean(ENTRY_EXPANDED_KEY);
                ResourceLocation replacementId = ResourceLocation.tryParse(entryTag.getString(ENTRY_REPLACEMENT_KEY));
                if (replacementId == null) {
                    replacementId = AIR_ID;
                }

                List<ResourceLocation> itemIds = new ArrayList<>();
                ListTag itemList = entryTag.getList(ENTRY_ITEMS_KEY, Tag.TAG_STRING);
                for (int itemIndex = 0; itemIndex < itemList.size(); itemIndex++) {
                    ResourceLocation itemId = ResourceLocation.tryParse(itemList.getString(itemIndex));
                    if (itemId != null) {
                        itemIds.add(itemId);
                        data.blockedItemIds.add(itemId);
                    }
                }

                if (!itemIds.isEmpty()) {
                    data.displayEntries.add(new BlockedEntryData(TYPE_GROUP.equals(type), label, itemIds, replacementId, expanded));
                }
            }
        } else {
            ListTag blockedItems = tag.getList(BLOCKED_ITEMS_KEY, Tag.TAG_STRING);
            for (int index = 0; index < blockedItems.size(); index++) {
                ResourceLocation itemId = ResourceLocation.tryParse(blockedItems.getString(index));
                if (itemId != null) {
                    data.blockedItemIds.add(itemId);
                    data.displayEntries.add(new BlockedEntryData(false, itemId.toString(), List.of(itemId), AIR_ID, false));
                }
            }
        }

        return data;
    }

    public Set<ResourceLocation> getBlockedItemIds() {
        return Set.copyOf(blockedItemIds);
    }

    public boolean hasCustomRulesSet() {
        return customRulesSet;
    }

    public List<BlockedEntryData> getDisplayEntries() {
        return List.copyOf(displayEntries);
    }

    public void setEntries(Collection<BlockedEntryData> entries) {
        blockedItemIds.clear();
        displayEntries.clear();

        for (BlockedEntryData entry : entries) {
            displayEntries.add(entry);
            blockedItemIds.addAll(entry.itemIds());
        }

        customRulesSet = true;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag blockedItems = new ListTag();
        ListTag serializedEntries = new ListTag();

        for (ResourceLocation itemId : blockedItemIds) {
            blockedItems.add(StringTag.valueOf(itemId.toString()));
        }

        for (BlockedEntryData entry : displayEntries) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(ENTRY_TYPE_KEY, entry.grouped() ? TYPE_GROUP : TYPE_SINGLE);
            entryTag.putString(ENTRY_LABEL_KEY, entry.label());
            entryTag.putString(ENTRY_REPLACEMENT_KEY, entry.replacementId().toString());
            entryTag.putBoolean(ENTRY_EXPANDED_KEY, entry.expanded());

            ListTag itemList = new ListTag();
            for (ResourceLocation itemId : entry.itemIds()) {
                itemList.add(StringTag.valueOf(itemId.toString()));
            }

            entryTag.put(ENTRY_ITEMS_KEY, itemList);
            serializedEntries.add(entryTag);
        }

        tag.putBoolean(CUSTOM_RULES_SET_KEY, customRulesSet);
        tag.put(BLOCKED_ITEMS_KEY, blockedItems);
        tag.put(DISPLAY_ENTRIES_KEY, serializedEntries);
        return tag;
    }

    public record BlockedEntryData(boolean grouped, String label, List<ResourceLocation> itemIds, ResourceLocation replacementId, boolean expanded) {
        public BlockedEntryData {
            itemIds = List.copyOf(itemIds);
            replacementId = replacementId == null ? AIR_ID : replacementId;
        }
    }
}
