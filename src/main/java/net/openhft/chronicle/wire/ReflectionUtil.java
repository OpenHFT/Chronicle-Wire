package net.openhft.chronicle.wire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReflectionUtil {
    /**
     * Get all interfaces implemented by all classes (up as far as but not including Object)
     * @param oClass class
     * @return interfaces
     */
    public static List<Class<?>> interfaces(Class<?> oClass) {
        List<Class<?>> list = new ArrayList<>();
        interfaces(oClass, list);
        return list;
    }

    private static void interfaces(Class<?> oClass, List<Class<?>> list) {
        Class<?> baseClass = oClass.getSuperclass();
        if (baseClass == null)
            return;
        list.addAll(Arrays.asList(oClass.getInterfaces()));
        interfaces(baseClass, list);
    }
}
