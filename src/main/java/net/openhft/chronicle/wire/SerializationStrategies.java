package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ObjectUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by peter on 10/05/16.
 */
public enum SerializationStrategies implements SerializationStrategy<Object> {
    MARSHALLABLE {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            ((ReadMarshallable) o).readMarshallable(in.wireIn());
            return o;
        }

        @Override
        public Class type() {
            return Marshallable.class;
        }
    },
    ANY_OBJECT {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            return in.objectWithInferredType(o, ANY_NESTED, null);
        }

        @Override
        public Class type() {
            return Object.class;
        }

        @Override
        public Boolean inBrackets() {
            return null;
        }
    },

    ANY_SCALAR {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            return in.objectWithInferredType(o, ANY_NESTED, null);
        }

        @Override
        public Class type() {
            return Object.class;
        }

        @Override
        public Boolean inBrackets() {
            return false;
        }
    },
    ANY_NESTED {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            Wires.readMarshallable(o, in.wireIn(), true);
            return o;
        }

        @Override
        public Class type() {
            return Object.class;
        }

        @Override
        public Boolean inBrackets() {
            return true;
        }
    },
    SERIALIZABLE {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            if (o instanceof Externalizable)
                EXTERNALIZABLE.readUsing(o, in);
            else
                ANY_OBJECT.readUsing(o, in);
            return o;
        }

        @Override
        public Class type() {
            return Serializable.class;
        }
    },
    EXTERNALIZABLE {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            try {
                ((Externalizable) o).readExternal(in.wireIn().objectInput());
            } catch (IOException | ClassNotFoundException e) {
                throw new IORuntimeException(e);
            }
            return o;
        }

        @Override
        public Class type() {
            return Externalizable.class;
        }
    },
    MAP {
        @Override
        public Object readUsing(Object o, ValueIn in) {
            Map<String, Object> map = (Map<String, Object>) o;
            final StringBuilder sb = Wires.acquireStringBuilder();
            long pos = in.wireIn().bytes().readPosition();
            while (in.hasNext()) {
                in.wireIn().readEventName(sb);
                String key = WireInternal.INTERNER.intern(sb);
                map.put(key, in.object());

                // make sure we are progressing.
                long pos2 = in.wireIn().bytes().readPosition();
                if (pos2 <= pos)
                    if (!Jvm.isDebug())
                        throw new IllegalStateException();
                pos = pos2;
            }
            return o;
        }

        @Override
        public Object newInstance(Class type) {
            return new LinkedHashMap<>();
        }

        @Override
        public Class type() {
            return Map.class;
        }
    };


    @Override
    public Object newInstance(Class type) {
        return ObjectUtils.newInstance(type);
    }

    @Override
    public Boolean inBrackets() {
        return true;
    }
}
