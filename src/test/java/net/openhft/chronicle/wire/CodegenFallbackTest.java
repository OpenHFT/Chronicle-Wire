/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.Invocation;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Fresh processes keep compiler initialisation, failed-class caches and property modes independent. */
public class CodegenFallbackTest {
    private static final String PROPERTY = "disableCodegenFallback";

    public interface Events {
        void event(int value);
    }

    public static class Receiver implements Events {
        int value;
        public void event(int value) { this.value = value; }
    }

    public static class InvalidSource implements GeneratingMethodReaderInterceptorReturns {
        public String generatorId() { return "InvalidSource"; }
        public String codeBeforeCall(Method method, String object, String[] arguments) {
            return "this is deliberately invalid Java;";
        }
        public String codeAfterCall(Method method, String object, String[] arguments) { return ""; }
        public Object intercept(Method method, Object object, Object[] arguments, Invocation invocation) {
            try {
                return invocation.invoke(method, object, arguments);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw net.openhft.chronicle.core.Jvm.rethrow(e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String scenario = args[0];
        boolean strict = net.openhft.chronicle.core.Jvm.getBoolean(PROPERTY);
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            Wire wire = new BinaryWire(bytes);
            Receiver receiver = new Receiver();
            VanillaMethodReaderBuilder builder = new VanillaMethodReaderBuilder(wire);
            if (scenario.equals("source"))
                builder.methodReaderInterceptorReturns(new InvalidSource());
            boolean explicit = scenario.equals("explicit");
            if (explicit)
                System.setProperty("disableReaderProxyCodegen", "true");
            boolean fails = scenario.equals("source") || scenario.equals("access");
            if (fails && strict) {
                IllegalStateException first = assertThrows(IllegalStateException.class, () -> builder.build(receiver));
                assertNotNull(first.getCause());
                assertTrue(first.getMessage(), first.getMessage().contains(PROPERTY));
                assertFalse(first.getMessage(), first.getMessage().contains("falling back"));
                IllegalStateException cached = assertThrows(IllegalStateException.class, () -> builder.build(receiver));
                assertTrue(cached.getMessage(), cached.getMessage().contains("previously"));
            } else {
                try (MethodReader reader = builder.build(receiver)) {
                    assertEquals(fails || explicit, reader instanceof VanillaMethodReader);
                    wire.writeDocument(false, w -> w.write("event").int32(7));
                    assertTrue(reader.readOne());
                    assertEquals(7, receiver.value);
                }
                if (fails) {
                    // A permissive build must not let its cached failure bypass a later strict build.
                    System.setProperty(PROPERTY, "true");
                    assertThrows(IllegalStateException.class, () -> builder.build(receiver));
                }
            }
            if (scenario.equals("source")) {
                // The invalid interceptor has a distinct generated name; valid generation remains usable.
                System.setProperty(PROPERTY, "true");
                try (MethodReader good = new VanillaMethodReaderBuilder(wire).build(receiver)) {
                    assertTrue(good instanceof AbstractGeneratedMethodReader);
                }
            }
            System.out.println("CHECK_PASSED");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void sourceFailureHonoursDefaultFalseTrueAndBareFlag() throws Exception {
        for (String value : new String[]{null, "false", "true", ""})
            run("source", value, true);
    }

    @Test
    public void missingCompilerAccessRefusesFallback() throws Exception {
        assumeTrue(javaVersion() >= 17);
        run("access", null, false);
        run("access", "true", false);
    }

    @Test
    public void validGenerationWorksWithFallbackDisabled() throws Exception {
        run("valid", "true", true);
    }

    @Test
    public void explicitlyRequestedProxyStillWorks() throws Exception {
        run("explicit", "true", true);
    }

    private static int javaVersion() {
        return Integer.parseInt(System.getProperty("java.specification.version").replace("1.", ""));
    }

    private static void run(String scenario, String value, boolean compilerAccess) throws Exception {
        Path directory = Files.createTempDirectory("codegen fallback ");
        Path log = directory.resolve("child.log");
        List<String> command = new ArrayList<>();
        command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
        if (javaVersion() >= 9) {
            for (String export : new String[]{"java.base/jdk.internal.ref", "java.base/sun.nio.ch", "jdk.unsupported/sun.misc"})
                command.add("--add-exports=" + export + "=ALL-UNNAMED");
            for (String open : new String[]{"java.base/java.lang", "java.base/java.lang.reflect", "java.base/java.io", "java.base/java.util"})
                command.add("--add-opens=" + open + "=ALL-UNNAMED");
            if (compilerAccess)
                command.add("--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED");
        }
        if (value != null)
            command.add("-D" + PROPERTY + (value.isEmpty() ? "" : "=" + value));
        command.add("-Dchronicle.analytics.disable=true");
        StringBuilder cp = new StringBuilder();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            // Failure must be observable even without an SLF4J provider.
            if (entry.contains("slf4j-simple")) continue;
            if (cp.length() > 0) cp.append(File.pathSeparator);
            cp.append(Paths.get(entry).toAbsolutePath());
        }
        command.addAll(Arrays.asList("-cp", cp.toString(), CodegenFallbackTest.class.getName(), scenario));
        Process child = null;
        try {
            ProcessBuilder process = new ProcessBuilder(command).directory(directory.toFile())
                    .redirectErrorStream(true).redirectOutput(log.toFile());
            for (String key : new String[]{"JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"})
                process.environment().remove(key);
            child = process.start();
            assertTrue("child timeout", child.waitFor(60, TimeUnit.SECONDS));
            String output = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
            assertEquals(output, 0, child.exitValue());
            assertTrue(output, Arrays.asList(output.split("\\R")).contains("CHECK_PASSED"));
        } finally {
            if (child != null && child.isAlive()) {
                child.destroyForcibly();
                child.waitFor(10, TimeUnit.SECONDS);
            }
            Files.deleteIfExists(log);
            Files.deleteIfExists(directory);
        }
    }
}
