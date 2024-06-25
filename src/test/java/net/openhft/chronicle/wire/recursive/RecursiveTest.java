package net.openhft.chronicle.wire.recursive;

import org.junit.Test;

public class RecursiveTest {

    @Test
    public void referToBaseClass() {
        test(new ReferToBaseClass(), new ReferToBaseClass());
    }

    @Test
    public void referToSameClass() {
        test(new ReferToSameClass(), new ReferToSameClass());
    }

    private void test(Base from, Base to) {
        from.copyTo(to);
    }
}