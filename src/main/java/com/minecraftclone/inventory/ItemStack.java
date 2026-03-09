package com.minecraftclone.inventory;

import com.minecraftclone.block.BlockType;

public final class ItemStack {
    public static final int MAX_STACK_SIZE = 64;

    private BlockType blockType;
    private int count;

    public ItemStack() {
        clear();
    }

    public ItemStack(BlockType blockType, int count) {
        set(blockType, count);
    }

    public boolean isEmpty() {
        return blockType == null || count <= 0;
    }

    public BlockType blockType() {
        return blockType;
    }

    public int count() {
        return count;
    }

    public int add(int amount) {
        if (amount <= 0) {
            return 0;
        }

        if (isEmpty()) {
            return amount;
        }

        int free = MAX_STACK_SIZE - count;
        if (free <= 0) {
            return amount;
        }

        int toAdd = Math.min(free, amount);
        count += toAdd;
        return amount - toAdd;
    }

    public int remove(int amount) {
        if (amount <= 0 || isEmpty()) {
            return 0;
        }

        int removed = Math.min(amount, count);
        count -= removed;
        if (count <= 0) {
            clear();
        }
        return removed;
    }

    public void set(BlockType type, int amount) {
        if (type == null || type == BlockType.AIR || amount <= 0) {
            clear();
            return;
        }

        this.blockType = type;
        this.count = Math.min(amount, MAX_STACK_SIZE);
    }

    public void clear() {
        this.blockType = null;
        this.count = 0;
    }
}
