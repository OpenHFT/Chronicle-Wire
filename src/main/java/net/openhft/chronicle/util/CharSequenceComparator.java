package net.openhft.chronicle.util;

import java.util.Comparator;

public enum CharSequenceComparator implements Comparator<CharSequence> {
    INSTANCE;

    @Override
    public int compare(CharSequence o1, CharSequence o2) {
        int cmp = Integer.compare(o1.length(), o2.length());
        if (cmp != 0)
            return cmp;
        for (int i = 0, len = o1.length(); i < len; i++) {
            cmp = Character.compare(o1.charAt(i), o2.charAt(i));
            if (cmp != 0)
                return cmp;
        }
        return cmp;
    }
}
