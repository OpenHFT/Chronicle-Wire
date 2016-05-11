package net.openhft.chronicle.wire;

/**
 * Created by peter on 10/05/16.
 */
public interface SerializationStrategy<T> {
    default T read(ValueIn in, Class<T> type) {
        return readUsing(newInstance(type), in);
    }

    default T readUsing(T using, ValueIn in, Class<T> type) {
        if (using == null && type != null)
            using = newInstance(type);
        return readUsing(using, in);
    }

    T readUsing(T using, ValueIn in);

    T newInstance(Class<T> type);

    Class<T> type();

    BracketType bracketType();
}
