/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
 * Helper class to support the serialisation of lambda expressions in Wire formats.
 * <p>
 * This allows lambdas that are {@link java.io.Serializable} to be written to and read
 * from a {@link Wire}, preserving their capturing arguments and target functional
 * interface method. It effectively mimics Java's built-in lambda serialisation
 * mechanism for Chronicle Wire.
 */
@SuppressWarnings("rawtypes")
public class WireSerializedLambda implements ReadMarshallable, ReadResolvable {

    // The class that captured the lambda
    private Class<?> capturingClass;
    // Fully qualified name of the functional interface
    private String functionalInterfaceClass;
    // Name of the functional interface method
    private String functionalInterfaceMethodName;
    // Method signature of the functional interface method
    private String functionalInterfaceMethodSignature;
    // Implementation class containing the lambda body
    private String implClass;
    // Name of the implementation method
    private String implMethodName;
    // Signature of the implementation method
    private String implMethodSignature;
    // Kind of the implementation method as defined by {@link SerializedLambda}
    private int implMethodKind;
    // Instantiated method type descriptor
    private String instantiatedMethodType;
    // List of arguments captured by the lambda
    @NotNull
    private List<Object> capturedArgs = new ArrayList<>();

    /**
     * Checks whether {@code clazz} represents a lambda class that also implements
     * {@link java.io.Serializable}.
     *
     * @param clazz class to check
     * @return {@code true} if {@code clazz} is a serialisable lambda
     */
    public static boolean isSerializableLambda(@NotNull Class<?> clazz) {
        return Serializable.class.isAssignableFrom(clazz) && Jvm.isLambdaClass(clazz);
    }

    /**
     * Writes a serialisable lambda to the supplied {@link ValueOut}.
     * <p>
     * The lambda's {@code writeReplace} method yields a {@link SerializedLambda},
     * which is written as a {@code WireSerializedLambda}.
     *
     * @param <L>      the type of the lambda expression
     * @param lambda   the serialisable lambda instance
     * @param valueOut target for the lambda's serialised form
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

    /**
     * Deserialises this {@code WireSerializedLambda} from the given wire.
     * All fields corresponding to {@link SerializedLambda} properties are read.
     */
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

    /**
     * Recreates the original lambda instance from the deserialised state.
     * Called after all fields have been populated via {@link #readMarshallable(WireIn)}.
     */
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
