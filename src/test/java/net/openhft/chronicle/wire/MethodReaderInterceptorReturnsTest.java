/*
 * Copyright 2016-2020 Chronicle Software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.Invocation;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.*;

public class MethodReaderInterceptorReturnsTest {
    /**
     * Checks that regular {@link MethodReaderInterceptorReturns} is supported in generated method reader.
     */
    @Test
    public void testInterceptorSupportedInGeneratedCode() {
        BinaryWire binaryWire = new BinaryWire(Bytes.allocateElasticOnHeap(128));

        InterceptedInterface writer = binaryWire.methodWriter(InterceptedInterface.class);

        final StringBuilder interceptedMethodNames = new StringBuilder();

        final MethodReaderInterceptorReturns interceptor = (m, o, args, invocation) -> {
            interceptedMethodNames.append(m.getName()).append(Arrays.toString(args)).append("*");

            return invocation.invoke(m, o, args);
        };

        final InterceptedInterface impl = new InterceptedInterfaceImpl(new StringBuilder(), false);

        final MethodReader reader = binaryWire.methodReaderBuilder()
                .methodReaderInterceptorReturns(interceptor)
                .build(impl);

        assertFalse(reader instanceof VanillaMethodReader);

        writer.noArgs();
        writer.oneArg(2);
        writer.twoArgs("dd", 3).end();

        assertTrue(reader.readOne());
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());

        assertEquals("noArgs[]*oneArg[2]*twoArgs[dd, 3]*end[]*", interceptedMethodNames.toString());
    }

    /**
     * Covers {@link GeneratingMethodReaderInterceptorReturns}, which allows to intervene in method reader's logic
     * without having to make reflexive calls.
     */
    @Test
    public void testGeneratingAggregatingInfoInterceptor() {
        BinaryWire binaryWire = new BinaryWire(Bytes.allocateElasticOnHeap(128));

        InterceptedInterface writer = binaryWire.methodWriter(InterceptedInterface.class);

        final AggregatingInfoInterceptor interceptor = new AggregatingInfoInterceptor();

        final InterceptedInterfaceImpl impl = new InterceptedInterfaceImpl(new StringBuilder(), false);

        final MethodReader reader = binaryWire.methodReaderBuilder()
                .methodReaderInterceptorReturns(interceptor)
                .build(impl);

        assertFalse(reader instanceof VanillaMethodReader);

        writer.noArgs();
        writer.oneArg(2);
        writer.twoArgs("dd", 3).end();

        assertTrue(reader.readOne());
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());

        assertEquals("someCall*twoArgs[dd, 3]*someCall*", impl.info());
    }

    /**
     * Covers {@link GeneratingMethodReaderInterceptorReturns}, which allows to intervene in method reader's logic
     * without having to make reflexive calls.
     */
    @Test
    public void testGeneratingSkippingInterceptor() {
        BinaryWire binaryWire = new BinaryWire(Bytes.allocateElasticOnHeap(128));

        InterceptedInterface writer = binaryWire.methodWriter(InterceptedInterface.class);

        final SkippingInterceptor interceptor = new SkippingInterceptor();

        final InterceptedInterfaceImpl impl = new InterceptedInterfaceImpl(new StringBuilder(), true);

        final MethodReader reader = binaryWire.methodReaderBuilder()
                .methodReaderInterceptorReturns(interceptor)
                .build(impl);

        assertFalse(reader instanceof VanillaMethodReader);

        writer.twoArgs("dd", 3).end();
        writer.twoArgs(null, 4).end();

        assertTrue(reader.readOne());
        assertTrue(reader.readOne());

        // Calls where first arg is null are skipped according to the logic of SkippingInterceptor
        assertEquals("twoArgs[dd, 3]*", impl.info());
    }

    interface InterceptedInterface {
        void noArgs();

        void oneArg(int x);

        InterceptedChained twoArgs(String s, int x);
    }

    interface InterceptedChained {
        void end();
    }

    interface AggregatingInfo {
        void appendInfo(String info);

        String info();
    }

    static class InterceptedInterfaceImpl implements InterceptedInterface, AggregatingInfo {
        private final StringBuilder info;
        private final boolean addInfoOnTwoArgs;

        public InterceptedInterfaceImpl(StringBuilder info, boolean addInfoOnTwoArgs) {
            this.info = info;
            this.addInfoOnTwoArgs = addInfoOnTwoArgs;
        }

        @Override
        public void appendInfo(String info) {
            this.info.append(info);
        }

        @Override
        public String info() {
            return info.toString();
        }

        @Override
        public void noArgs() {
        }

        @Override
        public void oneArg(int x) {
        }

        @Override
        public InterceptedChained twoArgs(String s, int x) {
            if (addInfoOnTwoArgs)
                info.append("twoArgs[").append(s).append(", ").append(x).append("]*");

            return new InterceptedChainedImpl(info);
        }
    }


    static class InterceptedChainedImpl implements InterceptedChained, AggregatingInfo {
        private final StringBuilder info;

        public InterceptedChainedImpl(StringBuilder info) {
            this.info = info;
        }

        @Override
        public void appendInfo(String info) {
            this.info.append(info);
        }

        @Override
        public String info() {
            return info.toString();
        }

        @Override
        public void end() {
        }
    }

    static class AggregatingInfoInterceptor implements GeneratingMethodReaderInterceptorReturns {
        @Override
        public String generatorId() {
            return "aggregatingInfo";
        }

        @Override
        public String codeBeforeCall(Method m, String objectName, String[] argumentNames) {
            if (m.getName().equals("oneArg")) {
                return String.format("if (%s == 2)\n" +
                        " return true;\n", argumentNames[0]);
            }

            return null;
        }

        @Override
        public String codeAfterCall(Method m, String objectName, String[] argumentNames) {
            if (m.getName().equals("twoArgs")) {
                String infoCode = String.format("\"%s[\" + %s + \", \" + %s + \"]*\"",
                        m.getName(), argumentNames[0], argumentNames[1]);

                return "((net.openhft.chronicle.wire.MethodReaderInterceptorReturnsTest.AggregatingInfo)" +
                        objectName + ")." + "appendInfo(" + infoCode + ");";
            } else {
                return "((net.openhft.chronicle.wire.MethodReaderInterceptorReturnsTest.AggregatingInfo)" +
                        objectName + ")." + "appendInfo(\"someCall*\");";
            }
        }

        @Override
        public Object intercept(Method m, Object o, Object[] args, Invocation invocation) {
            throw new UnsupportedOperationException("intercept shouldn't be called in generating interceptor");
        }
    }

    static class SkippingInterceptor implements GeneratingMethodReaderInterceptorReturns {
        @Override
        public String generatorId() {
            return "skipping";
        }

        @Override
        public String codeBeforeCall(Method m, String objectName, String[] argumentNames) {
            if (m.getName().equals("twoArgs"))
                return "if (" + argumentNames[0] + " != null) {";
            else
                return "";
        }

        @Override
        public String codeAfterCall(Method m, String objectName, String[] argumentNames) {
            if (m.getName().equals("twoArgs"))
                return "}";
            else
                return "";
        }

        @Override
        public Object intercept(Method m, Object o, Object[] args, Invocation invocation) {
            throw new UnsupportedOperationException("intercept shouldn't be called in generating interceptor");
        }
    }
}
