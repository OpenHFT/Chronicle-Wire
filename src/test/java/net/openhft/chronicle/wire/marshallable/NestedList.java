/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

import java.util.List;
import java.util.Objects;

public class NestedList extends SelfDescribingMarshallable {
    List<Item> items;

    public NestedList(List<Item> items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NestedList that = (NestedList) o;
        return Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(items);
    }

    public static class Item implements Marshallable {
        String name;
        int value;
        List<SubItem> subItems;
        String otherValue;

        public Item(String name, int value, List<SubItem> subItems, String otherValue) {
            this.name = name;
            this.value = value;
            this.subItems = subItems;
            this.otherValue = otherValue;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Item item = (Item) o;
            return value == item.value && Objects.equals(name, item.name) && Objects.equals(subItems, item.subItems);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value, subItems, otherValue);
        }
    }
    public static class SubItem extends SelfDescribingMarshallable {
        String subName;
        int subValue;

        public SubItem(String subName, int subValue) {
            this.subName = subName;
            this.subValue = subValue;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            SubItem subItem = (SubItem) o;
            return subValue == subItem.subValue && Objects.equals(subName, subItem.subName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(subName, subValue);
        }
    }
}
