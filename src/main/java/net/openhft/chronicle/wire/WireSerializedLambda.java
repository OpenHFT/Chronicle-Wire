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
import net.openhft.chronicle.core.util.ReadResolvable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to support the serialization of lambda expressions in Wire formats.
 * <p>
 * This class provides functionalities to check if a class is a serializable lambda and
 * serialize it using the Wire format. It uses Java's {@link SerializedLambda} mechanism
 * to capture details about the lambda and then writes those details to a Wire format.
 * </p>
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

    /**
     * Determines if the provided class is a serializable lambda.
     *
     * @param clazz The class to be checked.
     * @return {@code true} if the class is a serializable lambda, {@code false} otherwise.
     */
    public static boolean isSerializableLambda(@NotNull Class clazz) {
        return Serializable.class.isAssignableFrom(clazz) && Jvm.isLambdaClass(clazz);
    }

    /**
     * Serializes the given lambda to a Wire format using the provided {@link ValueOut} instance.
     * <p>
     * This method fetches the details of the lambda using the {@link SerializedLambda} mechanism
     * and then writes these details to the provided Wire format.
     * </p>
     *
     * @param <L> The type of the lambda to be serialized.
     * @param lambda The lambda instance to be serialized.
     * @param valueOut The {@link ValueOut} instance to which the lambda should be serialized.
     */
    public static <L> void write(@NotNull L lambda, @NotNull ValueOut valueOut) {
        try {
            Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
            Jvm.setAccessible(writeReplace);
            @NotNull SerializedLambda sl = (SerializedLambda) writeReplace.invoke(lambda);

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
