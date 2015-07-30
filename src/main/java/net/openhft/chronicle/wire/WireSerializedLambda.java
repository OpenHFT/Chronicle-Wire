/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.ReadResolvable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 23/06/15.
 */
public class WireSerializedLambda implements ReadMarshallable, ReadResolvable {

    private Class<?> capturingClass;
    private String functionalInterfaceClass;
    private String functionalInterfaceMethodName;
    private String functionalInterfaceMethodSignature;
    private String implClass;
    private String implMethodName;
    private String implMethodSignature;
    private int implMethodKind;
    private String instantiatedMethodType;
    @NotNull
    private List<Object> capturedArgs = new ArrayList<>();

    public static boolean isSerializableLambda(@NotNull Class clazz) {
        return Serializable.class.isAssignableFrom(clazz) && clazz.getName().contains("$Lambda$");
    }

    public static <Lambda> void write(@NotNull Lambda lambda, @NotNull ValueOut valueOut) {
        try {
            Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda sl = (SerializedLambda) writeReplace.invoke(lambda);
/*
                public SerializedLambda(Class<?> capturingClass,
                            String functionalInterfaceClass,
                            String functionalInterfaceMethodName,
                            String functionalInterfaceMethodSignature,
                            int implMethodKind,
                            String implClass,
                            String implMethodName,
                            String implMethodSignature,
                            String instantiatedMethodType,
                            Object[] capturedArgs) {
             */
            valueOut.type("SerializedLambda");
            valueOut.marshallable(v -> {
                v.write(() -> "cc").typeLiteral(sl.getCapturingClass().replace('/', '.'))
                        .write(() -> "fic").text(sl.getFunctionalInterfaceClass())
                        .write(() -> "fimn").text(sl.getFunctionalInterfaceMethodName())
                        .write(() -> "fims").text(sl.getFunctionalInterfaceMethodSignature())
                        .write(() -> "imk").int32(sl.getImplMethodKind())
                        .write(() -> "ic").text(sl.getImplClass())
                        .write(() -> "imn").text(sl.getImplMethodName())
                        .write(() -> "ims").text(sl.getImplMethodSignature())
                        .write(() -> "imt").text(sl.getInstantiatedMethodType())
                        .write(() -> "ca").sequence(v2 -> {
                    for (int i = 0; i < sl.getCapturedArgCount(); i++)
                        v2.object(sl.getCapturedArg(i));
                });
            });
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        capturedArgs = new ArrayList<>();

        wire.read(() -> "cc").typeLiteral(t -> capturingClass = t)
                .read(() -> "fic").text(s -> functionalInterfaceClass = s)
                .read(() -> "fimn").text(s -> functionalInterfaceMethodName = s)
                .read(() -> "fims").text(s -> functionalInterfaceMethodSignature = s)
                .read(() -> "imk").int32(i -> implMethodKind = i)
                .read(() -> "ic").text(s -> implClass = s)
                .read(() -> "imn").text(s -> implMethodName = s)
                .read(() -> "ims").text(s -> implMethodSignature = s)
                .read(() -> "imt").text(s -> instantiatedMethodType = s)
                .read(() -> "ca").sequence(v2 -> {
            while (v2.hasNextSequenceItem())
                capturedArgs.add(v2.object(Object.class));
        });

    }

    @Override
    public Object readResolve() {
        SerializedLambda sl = new SerializedLambda(capturingClass, functionalInterfaceClass,
                functionalInterfaceMethodName, functionalInterfaceMethodSignature,
                implMethodKind, implClass, implMethodName, implMethodSignature,
                instantiatedMethodType, capturedArgs.toArray());
        try {
            Method readReplace = SerializedLambda.class.getDeclaredMethod("readResolve");
            readReplace.setAccessible(true);
            return readReplace.invoke(sl);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
