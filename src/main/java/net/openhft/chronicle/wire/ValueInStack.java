package net.openhft.chronicle.wire;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter.lawrey on 02/02/2016.
 */
class ValueInStack {
    final List<ValueInState> freeList = new ArrayList<>();
    int level = 0;

    public void reset() {
        level = 0;
    }

    public void push() {
        level++;
        if (freeList.size() > level) {
            freeList.get(level).reset();
        }
    }

    public void pop() {
        if (level < 0)
            throw new IllegalStateException();
        level--;
    }

    public ValueInState curr() {
        while (freeList.size() <= level)
            freeList.add(new ValueInState());
        return freeList.get(level);
    }
}
