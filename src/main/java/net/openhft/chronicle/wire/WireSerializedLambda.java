/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.util.ReadResolvable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper calls to support serialization of lambdas in Wire formats.
 */
@SuppressWarnings("rawtypes")
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

    public static <L> void write(@NotNull L lambda, @NotNull ValueOut valueOut) {
        try {
            Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
            Jvm.setAccessible(writeReplace);
            @NotNull SerializedLambda sl = (SerializedLambda) writeReplace.invoke(lambda);
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
            valueOut.typePrefix("SerializedLambda");
            valueOut.marshallable(v ->
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
                    }));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        capturedArgs = new ArrayList<>();

        wire.read(() -> "cc").typeLiteral(this, (o, t) -> o.capturingClass = t)
                .read(() -> "fic").text(this, (o, s) -> o.functionalInterfaceClass = s)
                .read(() -> "fimn").text(this, (o, s) -> o.functionalInterfaceMethodName = s)
                .read(() -> "fims").text(this, (o, s) -> o.functionalInterfaceMethodSignature = s)
                .read(() -> "imk").int32(this, (o, i) -> o.implMethodKind = i)
                .read(() -> "ic").text(this, (o, s) -> o.implClass = s)
                .read(() -> "imn").text(this, (o, s) -> o.implMethodName = s)
                .read(() -> "ims").text(this, (o, s) -> o.implMethodSignature = s)
                .read(() -> "imt").text(this, (o, s) -> o.instantiatedMethodType = s)
                .read(() -> "ca").sequence(this, (o, v) -> {
            while (v.hasNextSequenceItem())
                capturedArgs.add(v.object(Object.class));
        });

    }

    @NotNull
    @Override
    public Object readResolve() {
        @NotNull SerializedLambda sl = new SerializedLambda(capturingClass, functionalInterfaceClass,
                functionalInterfaceMethodName, functionalInterfaceMethodSignature,
                implMethodKind, implClass, implMethodName, implMethodSignature,
                instantiatedMethodType, capturedArgs.toArray());
        try {
            Method readResolve = SerializedLambda.class.getDeclaredMethod("readResolve");
            Jvm.setAccessible(readResolve);
            return readResolve.invoke(sl);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
