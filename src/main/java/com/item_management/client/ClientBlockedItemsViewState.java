package com.item_management.client;

import java.util.ArrayList;
import java.util.List;

import com.item_management.data.BlockedItemsSavedData;

public final class ClientBlockedItemsViewState {
    private static boolean loaded;
    private static List<BlockedItemsSavedData.BlockedEntryData> entries = List.of();

    private ClientBlockedItemsViewState() {}

    public static void markLoading() {
        loaded = false;
        entries = List.of();
    }

    public static void setEntries(List<BlockedItemsSavedData.BlockedEntryData> newEntries) {
        loaded = true;
        entries = List.copyOf(newEntries);
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static List<BlockedItemsSavedData.BlockedEntryData> copyEntries() {
        return new ArrayList<>(entries);
    }
}
