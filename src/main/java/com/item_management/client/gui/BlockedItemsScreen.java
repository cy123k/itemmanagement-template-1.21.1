package com.item_management.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.item_management.client.ClientBlockedItemsViewState;
import com.item_management.data.BlockedItemsSavedData.BlockedEntryData;
import com.item_management.network.ModNetwork;
import com.item_management.network.SyncBlockedItemsPayload.BlockedEntryPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BlockedItemsScreen extends Screen {
    private static final Component LOADING = Component.translatable("screen.itemmanagement.loading");
    private static final Component SEARCH_HINT = Component.translatable("screen.itemmanagement.search_hint");
    private static final Component REPLACEMENT_HINT = Component.translatable("screen.itemmanagement.replacement_hint");
    private static final String AIR_ID = ResourceLocation.parse("minecraft:air").toString();
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 228;
    private static final int PANEL_PADDING = 12;
    private static final int LIST_HEIGHT = 96;
    private static final int SUGGESTION_LIMIT = 5;
    private static final int CHILD_INDENT = 16;
    private static final int GROUP_ICON_OFFSET = 14;
    private static final int ITEM_ICON_OFFSET = 2;

    private final List<DisplayEntry> displayEntries = new ArrayList<>();
    private final List<String> searchSuggestions = new ArrayList<>();
    private int searchMatchCount;
    private boolean initializedFromServer;
    private boolean replacementEditorVisible;

    private BlockedItemsList blockedItemsList;
    private EditBox searchBox;
    private EditBox replacementBox;
    private Button addMainHandButton;
    private Button exportDefaultsButton;
    private Button editReplacementButton;
    private Button applyReplacementButton;
    private Button cancelReplacementButton;
    private Button removeSelectedButton;
    private Button saveButton;

    public BlockedItemsScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        int panelLeft = getPanelLeft();
        int panelTop = getPanelTop();
        int searchTop = panelTop + 12;
        int buttonY = panelTop + PANEL_HEIGHT - 32;
        int leftColumnX = panelLeft + PANEL_PADDING;
        int smallButtonWidth = 96;
        int rowGap = 4;
        int topRowSecondX = leftColumnX + smallButtonWidth + rowGap;
        int topRowThirdX = topRowSecondX + smallButtonWidth + rowGap;
        int rightColumnX = panelLeft + PANEL_WIDTH - PANEL_PADDING - 146;

        this.searchBox = this.addRenderableWidget(new EditBox(this.font, leftColumnX, searchTop, PANEL_WIDTH - PANEL_PADDING * 2, 20, SEARCH_HINT));
        this.searchBox.setHint(SEARCH_HINT);
        this.searchBox.setResponder(value -> updateSearchSuggestions());
        this.searchBox.setMaxLength(120);

        this.blockedItemsList = this.addRenderableWidget(new BlockedItemsList(this.minecraft));

        this.editReplacementButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.itemmanagement.edit_replacement"), button -> openReplacementEditor())
                .bounds(leftColumnX, buttonY - 24, smallButtonWidth, 20)
                .build());
        this.exportDefaultsButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.itemmanagement.export_defaults"), button -> exportDefaultRules())
                .bounds(topRowSecondX, buttonY - 24, smallButtonWidth, 20)
                .build());
        this.addMainHandButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.itemmanagement.add_main_hand"), button -> addMainHandItem())
                .bounds(topRowThirdX, buttonY - 24, smallButtonWidth, 20)
                .build());

        this.replacementBox = this.addRenderableWidget(new EditBox(this.font, leftColumnX, buttonY - 24, 188, 20, REPLACEMENT_HINT));
        this.replacementBox.setHint(REPLACEMENT_HINT);
        this.replacementBox.setMaxLength(120);

        this.applyReplacementButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.itemmanagement.apply_replacement"), button -> applyReplacementToSelection())
                .bounds(leftColumnX + 192, buttonY - 24, 50, 20)
                .build());
        this.cancelReplacementButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> closeReplacementEditor())
                .bounds(leftColumnX + 246, buttonY - 24, 50, 20)
                .build());

        this.removeSelectedButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.itemmanagement.remove_selected"), button -> removeSelected())
                .bounds(leftColumnX, buttonY, 146, 20)
                .build());
        this.saveButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.itemmanagement.save"), button -> saveChanges())
                .bounds(rightColumnX, buttonY, 70, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(rightColumnX + 76, buttonY, 70, 20)
                .build());

        ClientBlockedItemsViewState.markLoading();
        ModNetwork.requestBlockedItems();
        updateSearchSuggestions();
        refreshButtons();
    }

    @Override
    public void tick() {
        super.tick();

        if (!initializedFromServer && ClientBlockedItemsViewState.isLoaded()) {
            displayEntries.clear();
            for (BlockedEntryData entry : ClientBlockedItemsViewState.copyEntries()) {
                displayEntries.add(DisplayEntry.fromSavedEntry(entry));
            }
            sortDisplayEntries();
            initializedFromServer = true;
            rebuildList();
            refreshButtons();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        if (!initializedFromServer) {
            guiGraphics.drawCenteredString(this.font, LOADING, this.width / 2, this.height / 2 - 8, 0xAAAAAA);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderSearchHint(guiGraphics);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (keyCode == 258) {
                applyFirstSuggestion();
                return true;
            }

            if (keyCode == 257 || keyCode == 335) {
                addFromSearch();
                return true;
            }
        }

        if (this.replacementBox != null && this.replacementBox.isVisible() && this.replacementBox.isFocused()) {
            if (keyCode == 258) {
                applyReplacementSuggestion();
                return true;
            }

            if (keyCode == 257 || keyCode == 335) {
                applyReplacementToSelection();
                return true;
            }

            if (keyCode == 256) {
                closeReplacementEditor();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void addMainHandItem() {
        Minecraft minecraft = this.minecraft;
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (mainHand.isEmpty()) {
            return;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(mainHand.getItem()).toString();
        if (!containsBlockedItem(itemId)) {
            displayEntries.add(DisplayEntry.single(itemId, AIR_ID));
            sortDisplayEntries();
            rebuildList();
            blockedItemsList.selectById(itemId);
            refreshButtons();
        }
    }

    private void addFromSearch() {
        String normalized = normalizeSearchValue(this.searchBox.getValue());
        if (normalized.isEmpty()) {
            if (!searchSuggestions.isEmpty()) {
                normalized = searchSuggestions.get(0);
            } else {
                return;
            }
        }

        if (isPatternQuery(normalized)) {
            List<String> patternMatches = findMatches(normalized);
            if (patternMatches.isEmpty()) {
                return;
            }

            List<String> newItems = new ArrayList<>();
            for (String itemIdString : patternMatches) {
                if (!containsBlockedItem(itemIdString)) {
                    newItems.add(itemIdString);
                }
            }

            if (!newItems.isEmpty()) {
                newItems.sort(String::compareTo);
                displayEntries.add(DisplayEntry.group(normalized, newItems, AIR_ID));
                sortDisplayEntries();
                rebuildList();
                blockedItemsList.selectGroup(normalized);
            }

            this.searchBox.setValue("");
            updateSearchSuggestions();
            refreshButtons();
            return;
        }

        ResourceLocation itemId = resolveItemId(normalized);
        if (itemId == null) {
            return;
        }

        String itemIdString = itemId.toString();
        if (!containsBlockedItem(itemIdString)) {
            displayEntries.add(DisplayEntry.single(itemIdString, AIR_ID));
            sortDisplayEntries();
            rebuildList();
            blockedItemsList.selectById(itemIdString);
        }

        this.searchBox.setValue("");
        updateSearchSuggestions();
        refreshButtons();
    }

    private void applyFirstSuggestion() {
        if (searchSuggestions.isEmpty()) {
            return;
        }

        this.searchBox.setValue(searchSuggestions.get(0));
        this.searchBox.moveCursorToEnd(false);
        updateSearchSuggestions();
    }

    private void applyReplacementSuggestion() {
        String query = normalizeSearchValue(this.replacementBox.getValue());
        if (query.isEmpty()) {
            return;
        }

        List<String> matches = findPlainMatches(query);
        if (matches.isEmpty()) {
            return;
        }

        this.replacementBox.setValue(matches.get(0));
        this.replacementBox.moveCursorToEnd(false);
    }

    private void openReplacementEditor() {
        BlockedItemsEntry selected = blockedItemsList.getSelected();
        if (selected == null) {
            return;
        }

        replacementEditorVisible = true;
        replacementBox.setValue(selected.entry.replacementId());
        replacementBox.moveCursorToEnd(false);
        replacementBox.setFocused(true);
        refreshButtons();
    }

    private void closeReplacementEditor() {
        replacementEditorVisible = false;
        replacementBox.setFocused(false);
        refreshButtons();
    }

    private void applyReplacementToSelection() {
        BlockedItemsEntry selected = blockedItemsList.getSelected();
        if (selected == null) {
            return;
        }

        ResourceLocation replacementId = resolveItemId(normalizeSearchValue(replacementBox.getValue()));
        if (replacementId == null) {
            replacementId = ResourceLocation.parse("minecraft:air");
        }

        String replacementIdString = replacementId.toString();
        if (selected.child) {
            updateChildReplacement(selected.entry, selected.childItemId, replacementIdString);
            rebuildListAndSelect(selected.childItemId, null);
        } else {
            selected.entry.setReplacementId(replacementIdString);
            rebuildListAndSelect(selected.entry.representativeItemId(), selected.entry.label);
        }

        closeReplacementEditor();
    }

    private void removeSelected() {
        BlockedItemsEntry selected = blockedItemsList.getSelected();
        if (selected == null) {
            return;
        }

        if (selected.child) {
            removeChildEntry(selected.entry, selected.childItemId);
        } else {
            displayEntries.remove(selected.entry);
            rebuildList();
        }

        refreshButtons();
    }

    private void saveChanges() {
        ModNetwork.updateBlockedItems(displayEntries.stream().map(DisplayEntry::toPayload).toList());
        onClose();
    }

    private void exportDefaultRules() {
        ModNetwork.exportDefaultRules();
    }

    private void rebuildList() {
        blockedItemsList.rebuild(displayEntries);
    }

    private void rebuildListAndSelect(String preferredItemId, String preferredGroupLabel) {
        blockedItemsList.rebuild(displayEntries);

        if (preferredItemId != null) {
            blockedItemsList.selectChildOrParentById(preferredItemId);
            if (blockedItemsList.getSelected() != null) {
                return;
            }
        }

        if (preferredGroupLabel != null) {
            blockedItemsList.selectGroup(preferredGroupLabel);
        }
    }

    private void refreshButtons() {
        boolean loaded = initializedFromServer;
        boolean hasSelection = loaded && blockedItemsList != null && blockedItemsList.getSelected() != null;

        searchBox.setEditable(loaded);
        replacementBox.setEditable(loaded);
        replacementBox.visible = loaded && replacementEditorVisible;
        applyReplacementButton.visible = loaded && replacementEditorVisible;
        cancelReplacementButton.visible = loaded && replacementEditorVisible;

        replacementBox.active = loaded && replacementEditorVisible;
        applyReplacementButton.active = loaded && replacementEditorVisible;
        cancelReplacementButton.active = loaded && replacementEditorVisible;

        addMainHandButton.visible = !replacementEditorVisible;
        addMainHandButton.active = loaded && !replacementEditorVisible;
        exportDefaultsButton.visible = !replacementEditorVisible;
        exportDefaultsButton.active = loaded && !replacementEditorVisible;
        editReplacementButton.visible = !replacementEditorVisible;
        editReplacementButton.active = hasSelection && !replacementEditorVisible;

        removeSelectedButton.active = hasSelection;
        saveButton.active = loaded;
    }

    private void updateSearchSuggestions() {
        searchSuggestions.clear();
        searchMatchCount = 0;

        String query = normalizeSearchValue(this.searchBox.getValue());
        if (query.isEmpty()) {
            refreshButtons();
            return;
        }

        List<String> matches = findMatches(query);
        searchMatchCount = matches.size();
        searchSuggestions.addAll(matches.subList(0, Math.min(SUGGESTION_LIMIT, matches.size())));
        refreshButtons();
    }

    private void renderSearchHint(GuiGraphics guiGraphics) {
        String query = normalizeSearchValue(this.searchBox.getValue());
        int left = this.searchBox.getX() + 2;
        int top = this.searchBox.getY() + this.searchBox.getHeight() + 6;

        if (query.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.itemmanagement.search_inline_hint"), left, top, 0xB7C7DA, false);
            return;
        }

        if (searchSuggestions.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.itemmanagement.search_no_match"), left, top, 0xD88F8F, false);
            return;
        }

        if (isPatternQuery(query)) {
            guiGraphics.drawString(this.font, Component.translatable("screen.itemmanagement.search_pattern_match", searchMatchCount), left, top, 0xE5EEF9, false);
            return;
        }

        guiGraphics.drawString(this.font, Component.translatable("screen.itemmanagement.search_best_match", searchSuggestions.get(0)), left, top, 0xE5EEF9, false);
    }

    private String normalizeSearchValue(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isPatternQuery(String query) {
        return query.indexOf('*') >= 0;
    }

    private List<String> findMatches(String query) {
        if (isPatternQuery(query)) {
            return findPatternMatches(query);
        }

        return findPlainMatches(query);
    }

    private List<String> findPlainMatches(String query) {
        String lowercaseQuery = query.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (ResourceKey<Item> key : BuiltInRegistries.ITEM.registryKeySet()) {
            String itemId = key.location().toString();
            if (itemId.toLowerCase(Locale.ROOT).contains(lowercaseQuery)) {
                matches.add(itemId);
            }
        }

        matches.sort(Comparator
                .comparing((String itemId) -> !itemId.toLowerCase(Locale.ROOT).startsWith(lowercaseQuery))
                .thenComparing(String::compareTo));
        return matches;
    }

    private List<String> findPatternMatches(String patternQuery) {
        Pattern pattern = compileWildcardPattern(patternQuery);
        if (pattern == null) {
            return List.of();
        }

        List<String> matches = new ArrayList<>();
        for (ResourceKey<Item> key : BuiltInRegistries.ITEM.registryKeySet()) {
            String itemId = key.location().toString();
            if (pattern.matcher(itemId).matches()) {
                matches.add(itemId);
            }
        }

        matches.sort(String::compareTo);
        return matches;
    }

    private ResourceLocation resolveItemId(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return ResourceLocation.parse("minecraft:air");
        }

        ResourceLocation itemId = ResourceLocation.tryParse(rawValue);
        if (itemId != null && BuiltInRegistries.ITEM.containsKey(itemId)) {
            return itemId;
        }

        List<String> matches = findPlainMatches(rawValue);
        if (matches.isEmpty()) {
            return null;
        }

        ResourceLocation matched = ResourceLocation.tryParse(matches.get(0));
        return matched != null && BuiltInRegistries.ITEM.containsKey(matched) ? matched : null;
    }

    private boolean containsBlockedItem(String itemId) {
        for (DisplayEntry entry : displayEntries) {
            if (entry.itemIds.contains(itemId)) {
                return true;
            }
        }
        return false;
    }

    private void sortDisplayEntries() {
        displayEntries.sort(Comparator.comparing(entry -> entry.label));
    }

    private void removeChildEntry(DisplayEntry entry, String childItemId) {
        int removedIndex = entry.indexOf(childItemId);
        if (removedIndex < 0) {
            return;
        }

        entry.removeItem(childItemId);
        if (entry.itemCount() == 0) {
            displayEntries.remove(entry);
            rebuildList();
            return;
        }

        if (entry.itemCount() == 1) {
            String remainingItemId = entry.representativeItemId();
            int entryIndex = displayEntries.indexOf(entry);
            DisplayEntry replacement = DisplayEntry.single(remainingItemId, entry.replacementId());
            displayEntries.set(entryIndex, replacement);
            sortDisplayEntries();
            rebuildListAndSelect(remainingItemId, null);
            return;
        }

        String nextItemId = entry.itemIdAt(Math.min(removedIndex, entry.itemCount() - 1));
        rebuildListAndSelect(nextItemId, entry.label);
    }

    private void updateChildReplacement(DisplayEntry entry, String childItemId, String replacementId) {
        int childIndex = entry.indexOf(childItemId);
        if (childIndex < 0) {
            return;
        }

        if (entry.replacementId().equals(replacementId)) {
            return;
        }

        entry.removeItem(childItemId);
        if (entry.itemCount() == 0) {
            displayEntries.remove(entry);
        } else if (entry.itemCount() == 1) {
            int entryIndex = displayEntries.indexOf(entry);
            displayEntries.set(entryIndex, DisplayEntry.single(entry.representativeItemId(), entry.replacementId()));
        }

        displayEntries.add(DisplayEntry.single(childItemId, replacementId));
        sortDisplayEntries();
    }

    private Pattern compileWildcardPattern(String patternQuery) {
        StringBuilder builder = new StringBuilder("^");
        for (int index = 0; index < patternQuery.length(); index++) {
            char current = patternQuery.charAt(index);
            if (current == '*') {
                builder.append(".*");
            } else {
                builder.append(Pattern.quote(String.valueOf(current)));
            }
        }
        builder.append("$");

        try {
            return Pattern.compile(builder.toString(), Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException exception) {
            return null;
        }
    }

    private int getPanelLeft() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int getPanelTop() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    private final class BlockedItemsList extends ObjectSelectionList<BlockedItemsEntry> {
        private BlockedItemsList(Minecraft minecraft) {
            super(minecraft, PANEL_WIDTH - PANEL_PADDING * 2, LIST_HEIGHT, getPanelTop() + 54, 20);
            this.setX(getPanelLeft() + PANEL_PADDING);
        }

        @Override
        public int getRowWidth() {
            return this.width - 10;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width - 6;
        }

        @Override
        protected void renderListBackground(GuiGraphics guiGraphics) {
            int left = this.getX();
            int top = this.getY();
            int right = this.getRight();
            int bottom = this.getBottom();

            guiGraphics.fill(left, top, right, bottom, 0xFF141A22);
            guiGraphics.fill(left + 1, top + 1, right - 1, bottom - 1, 0xFF0E131A);
            guiGraphics.fill(left + 2, top + 2, right - 2, bottom - 2, 0xCC1B2430);
        }

        @Override
        protected void renderListSeparators(GuiGraphics guiGraphics) {
            int left = this.getX();
            int right = this.getRight();
            guiGraphics.fill(left, this.getY() - 1, right, this.getY(), 0xFF3C4A5E);
            guiGraphics.fill(left, this.getBottom(), right, this.getBottom() + 1, 0xFF3C4A5E);
        }

        @Override
        protected void renderSelection(GuiGraphics guiGraphics, int top, int width, int height, int outerColor, int innerColor) {
            int left = this.getRowLeft() - 2;
            int right = this.getScrollbarPosition() - 4;
            guiGraphics.fill(left, top - 1, right, top + height + 1, 0xFFDEE6F3);
            guiGraphics.fill(left + 1, top, right - 1, top + height, 0xFF2A3444);
        }

        @Override
        protected void renderItem(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int index, int left, int top, int width, int height) {
            if (!this.isSelectedItem(index)) {
                int background = index % 2 == 0 ? 0x44212A36 : 0x44303B4A;
                guiGraphics.fill(left - 2, top, this.getScrollbarPosition() - 4, top + height, background);
            }

            super.renderItem(guiGraphics, mouseX, mouseY, partialTick, index, left, top, width, height);
        }

        private void rebuild(List<DisplayEntry> entries) {
            this.clearEntries();
            for (DisplayEntry entry : entries) {
                this.addEntry(new BlockedItemsEntry(entry));
                if (entry.grouped && entry.expanded) {
                    for (String childItemId : entry.itemIds) {
                        this.addEntry(new BlockedItemsEntry(entry, childItemId));
                    }
                }
            }

            if (!entries.isEmpty()) {
                this.setSelected(this.getEntry(0));
            }
        }

        private void selectById(String itemId) {
            for (BlockedItemsEntry entry : this.children()) {
                if (!entry.child && entry.entry.itemIds.contains(itemId)) {
                    this.setSelected(entry);
                    this.ensureVisible(entry);
                    return;
                }
            }
        }

        private void selectChildOrParentById(String itemId) {
            for (BlockedItemsEntry entry : this.children()) {
                if (entry.child && itemId.equals(entry.childItemId)) {
                    this.setSelected(entry);
                    this.ensureVisible(entry);
                    return;
                }
            }

            selectById(itemId);
        }

        private void selectGroup(String pattern) {
            for (BlockedItemsEntry entry : this.children()) {
                if (!entry.child && entry.entry.grouped && entry.entry.label.equals(pattern)) {
                    this.setSelected(entry);
                    this.ensureVisible(entry);
                    return;
                }
            }
        }

        @Override
        public void setSelected(BlockedItemsEntry selected) {
            super.setSelected(selected);
            refreshButtons();
        }
    }

    private final class BlockedItemsEntry extends ObjectSelectionList.Entry<BlockedItemsEntry> {
        private final DisplayEntry entry;
        private final boolean child;
        private final String childItemId;
        private final ItemStack previewStack;

        private BlockedItemsEntry(DisplayEntry entry) {
            this(entry, null);
        }

        private BlockedItemsEntry(DisplayEntry entry, String childItemId) {
            this.entry = entry;
            this.child = childItemId != null;
            this.childItemId = childItemId;
            this.previewStack = createPreviewStack(this.child ? childItemId : entry.representativeItemId());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (child) {
                blockedItemsList.setSelected(this);
                return true;
            }

            int toggleLeft = blockedItemsList.getRowLeft() + 2;
            if (entry.grouped && mouseX >= toggleLeft && mouseX <= toggleLeft + 10) {
                entry.expanded = !entry.expanded;
                rebuildList();
                blockedItemsList.selectGroup(entry.label);
            } else {
                blockedItemsList.setSelected(this);
            }
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.literal(child ? childItemId : entry.displayLabel());
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int iconLeft = left + (child ? CHILD_INDENT + ITEM_ICON_OFFSET : entry.grouped ? GROUP_ICON_OFFSET : ITEM_ICON_OFFSET);
            if (!child && entry.grouped) {
                guiGraphics.drawString(font, entry.expanded ? "v" : ">", left + 2, top + 6, 0xD9E3F0, false);
            }

            guiGraphics.renderItem(previewStack, iconLeft, top + 1);
            int textLeft = (!child && entry.grouped ? left + GROUP_ICON_OFFSET : iconLeft) + 22;
            guiGraphics.drawString(font, Component.literal(child ? childItemId : entry.displayLabel()), textLeft, top + 6, child ? 0xB9C6D8 : 0xFFFFFF, false);
        }

        private ItemStack createPreviewStack(String itemId) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
            if (resourceLocation == null) {
                return new ItemStack(Items.BARRIER);
            }

            Item item = BuiltInRegistries.ITEM.get(resourceLocation);
            if (item == Items.AIR) {
                return new ItemStack(Items.BARRIER);
            }

            return new ItemStack(item);
        }
    }

    private static final class DisplayEntry {
        private final String label;
        private final List<String> itemIds;
        private final boolean grouped;
        private boolean expanded;
        private String replacementId;

        private DisplayEntry(String label, List<String> itemIds, boolean grouped, String replacementId) {
            this.label = label;
            this.itemIds = new ArrayList<>(itemIds);
            this.grouped = grouped;
            this.replacementId = replacementId;
        }

        private static DisplayEntry single(String itemId, String replacementId) {
            return new DisplayEntry(itemId, List.of(itemId), false, replacementId);
        }

        private static DisplayEntry group(String pattern, List<String> itemIds, String replacementId) {
            return new DisplayEntry(pattern, itemIds, true, replacementId);
        }

        private static DisplayEntry fromSavedEntry(BlockedEntryData entry) {
            DisplayEntry displayEntry = new DisplayEntry(
                    entry.label(),
                    entry.itemIds().stream().map(ResourceLocation::toString).toList(),
                    entry.grouped(),
                    entry.replacementId().toString());
            displayEntry.expanded = entry.expanded();
            return displayEntry;
        }

        private String representativeItemId() {
            return itemIds.get(0);
        }

        private int itemCount() {
            return itemIds.size();
        }

        private int indexOf(String itemId) {
            return itemIds.indexOf(itemId);
        }

        private String itemIdAt(int index) {
            return itemIds.get(index);
        }

        private void removeItem(String itemId) {
            itemIds.remove(itemId);
        }

        private String replacementId() {
            return replacementId;
        }

        private void setReplacementId(String replacementId) {
            this.replacementId = replacementId;
        }

        private String displayLabel() {
            return grouped
                    ? label + " -> " + replacementId + " (" + itemIds.size() + " items)"
                    : label + " -> " + replacementId;
        }

        private BlockedEntryPayload toPayload() {
            return new BlockedEntryPayload(grouped, label, List.copyOf(itemIds), replacementId, expanded);
        }
    }
}
