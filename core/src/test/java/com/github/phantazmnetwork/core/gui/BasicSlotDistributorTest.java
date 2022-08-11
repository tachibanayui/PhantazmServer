package com.github.phantazmnetwork.core.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BasicSlotDistributorTest {
    @Test
    void test3x3_1Item_padding0() {
        SlotDistributor slotDistributor = new BasicSlotDistributor(0);
        int[] slots = slotDistributor.distribute(3, 3, 1);

        assertArrayEquals(new int[] {4}, slots);
    }

    @Test
    void test3x3_2Items_padding0() {
        SlotDistributor slotDistributor = new BasicSlotDistributor(0);
        int[] slots = slotDistributor.distribute(3, 3, 2);

        assertArrayEquals(new int[] {3, 5}, slots);
    }

    @Test
    void test3x3_3Items_padding0() {
        SlotDistributor slotDistributor = new BasicSlotDistributor(0);
        int[] slots = slotDistributor.distribute(3, 3, 3);

        assertArrayEquals(new int[] {3, 4, 5}, slots);
    }

    @Test
    void test3x3_4Items_padding0() {
        SlotDistributor slotDistributor = new BasicSlotDistributor(0);
        int[] slots = slotDistributor.distribute(3, 3, 4);

        assertArrayEquals(new int[] {0, 1, 2, 7}, slots);
    }
}
