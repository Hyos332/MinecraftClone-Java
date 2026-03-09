package com.minecraftclone.inventory;

import com.minecraftclone.block.BlockType;

public final class HotbarInventory {
    public static final int SLOT_COUNT = 9;

    private final ItemStack[] slots = new ItemStack[SLOT_COUNT];
    private int selectedIndex;

    public HotbarInventory() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = new ItemStack();
        }
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        if (index < 0) {
            selectedIndex = 0;
            return;
        }
        if (index >= SLOT_COUNT) {
            selectedIndex = SLOT_COUNT - 1;
            return;
        }
        selectedIndex = index;
    }

    public void selectNext() {
        selectedIndex = (selectedIndex + 1) % SLOT_COUNT;
    }

    public void selectPrevious() {
        selectedIndex--;
        if (selectedIndex < 0) {
            selectedIndex = SLOT_COUNT - 1;
        }
    }

    public ItemStack slot(int index) {
        return slots[index];
    }

    public BlockType selectedBlockType() {
        ItemStack selected = slots[selectedIndex];
        return selected.isEmpty() ? null : selected.blockType();
    }

    public boolean consumeSelected(int amount) {
        if (amount <= 0) {
            return true;
        }

        ItemStack selected = slots[selectedIndex];
        if (selected.isEmpty() || selected.count() < amount) {
            return false;
        }

        selected.remove(amount);
        return true;
    }

    public int add(BlockType blockType, int amount) {
        if (blockType == null || blockType == BlockType.AIR || amount <= 0) {
            return 0;
        }

        int remaining = amount;

        for (ItemStack slot : slots) {
            if (slot.isEmpty()) {
                continue;
            }
            if (slot.blockType() != blockType) {
                continue;
            }
            remaining = slot.add(remaining);
            if (remaining == 0) {
                return 0;
            }
        }

        for (ItemStack slot : slots) {
            if (!slot.isEmpty()) {
                continue;
            }

            int toPlace = Math.min(ItemStack.MAX_STACK_SIZE, remaining);
            slot.set(blockType, toPlace);
            remaining -= toPlace;

            if (remaining == 0) {
                return 0;
            }
        }

        return remaining;
    }

    public BlockType[] snapshotBlockTypes() {
        BlockType[] out = new BlockType[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            out[i] = slots[i].isEmpty() ? BlockType.AIR : slots[i].blockType();
        }
        return out;
    }

    public int[] snapshotCounts() {
        int[] out = new int[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            out[i] = slots[i].count();
        }
        return out;
    }

    public byte[] snapshotBlockIds() {
        byte[] out = new byte[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            out[i] = slots[i].isEmpty() ? BlockType.AIR.id() : slots[i].blockType().id();
        }
        return out;
    }

    public void load(byte[] blockIds, int[] counts) {
        clearAll();
        if (blockIds == null || counts == null) {
            return;
        }

        int limit = Math.min(Math.min(blockIds.length, counts.length), SLOT_COUNT);
        for (int i = 0; i < limit; i++) {
            BlockType type = BlockType.fromId(blockIds[i]);
            int count = Math.max(0, counts[i]);
            if (type == BlockType.AIR || count == 0) {
                continue;
            }
            slots[i].set(type, count);
        }
    }

    public void clearAll() {
        for (ItemStack slot : slots) {
            slot.clear();
        }
    }

    public void giveStarterKit() {
        clearAll();
        slots[0].set(BlockType.DIRT, 48);
        slots[1].set(BlockType.STONE, 48);
        slots[2].set(BlockType.WOOD, 32);
        slots[3].set(BlockType.SAND, 24);
        slots[4].set(BlockType.LEAVES, 24);
        slots[5].set(BlockType.GRASS, 16);
        slots[6].set(BlockType.COAL_ORE, 12);
        slots[7].set(BlockType.IRON_ORE, 10);
        slots[8].set(BlockType.WATER, 8);
        selectedIndex = 0;
    }
}
