/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.UpdateInterceptor;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.utils.SourceCodeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// Generator for simple classes based on SimpleMetaData
class SimpleClassGenerator extends AbstractClassGenerator<SimpleMetaData> {

    // Constructor initializing generator with default metadata and setting Callable as its interface
    SimpleClassGenerator() {
        super(new SimpleMetaData());
        metaData().interfaces().add(Callable.class);
    }

    // Overridden method to generate the content for a method
    @Override
    protected void generateMethod(Method method, StringBuilder params, List<String> paramList, SourceCodeFormatter mainCode) {
        // Ensure the method's name is "call"
        assertEquals("call", method.getName(), "Expected Callable method name");
        // Add the generated source code line
        withLineNumber(mainCode)
                .append("return \"").append(metaData().message).append("\";\n");
    }
}

// Generator for UI classes based on SimpleMetaData
class UIClassGenerator extends AbstractClassGenerator<SimpleMetaData> {

    // Constructor initializing generator with default metadata and setting Consumer as its interface
    UIClassGenerator() {
        super(new SimpleMetaData());
        metaData().interfaces().add(Consumer.class);
    }

    // Overridden method to generate constructors for the class
    @Override
    protected void generateConstructors(SourceCodeFormatter mainCode) {
        super.generateConstructors(mainCode);
        mainCode.append("public ").append(className()).append('(').append(nameForClass(UpdateInterceptor.class)).append(" updateInterceptor) {\n")
                .append("this.updateInterceptor = updateInterceptor;\n")
                .append("}\n");
    }

    // Overridden method to generate the content for a method
    @Override
    protected void generateMethod(Method method, StringBuilder params, List<String> paramList, SourceCodeFormatter mainCode) {
        // Ensure the method's name is "accept"
        assertEquals("accept", method.getName(), "Expected Consumer method name");
        // Add the generated source code line
        withLineNumber(mainCode)
                .append("((").append(nameForClass(MyTypes.class)).append(')').append(params)
                .append(").text().append('-').append(\"").append(metaData().message).append("\");\n");
    }
}

// Metadata specific for SimpleClassGenerator
class SimpleMetaData extends AbstractClassGenerator.MetaData<SimpleMetaData> {
    public String message;  // Message to be used in generated methods
}

// Tests for the AbstractClassGenerator's functionality
public class AbstractClassGeneratorTest extends WireTestCommon {

    // Test case to validate the SimpleClassGenerator's functionality
    @Test
    @DisplayName("Generates simple callable class from metadata")
    public void simpleGenerator() throws Exception {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip simple generator test");

        assertEquals("Hello World", doTest("Hello World"), "Simple generator should echo hello message");
        assertEquals("Bye now", doTest("Bye now"), "Simple generator should echo bye message");
        String theTimeIs = "The time is " + LocalDateTime.now();
        assertEquals(theTimeIs, doTest(theTimeIs), "Simple generator should echo time message");
    }

    // Helper method to perform the test with a given message
    private String doTest(String message) throws Exception {
        SimpleClassGenerator scg = new SimpleClassGenerator();
        scg.metaData()
                .packageName(Jvm.getPackageName(getClass()))
                .baseClassName("ACGT")
                .message = message;
        Class<Callable<String>> aClass = scg.acquireClass(getClass().getClassLoader());
        Callable<String> callable = aClass.getDeclaredConstructor().newInstance();
        // break point on the next line to be able to debug the generated class.
        return callable.call();
    }

    // Test Cases
    // Test case to validate the interceptor's functionality in the UIClassGenerator
    @Test
    @DisplayName("Generates UI class with update interceptor")
    public void useInterceptor() throws Exception {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip interceptor generator test");

        // StringWriter to capture the interceptor's output
        StringWriter sw = new StringWriter();

        // Define an interceptor for method updates
        UpdateInterceptor ui = (methodName, t) -> {
            sw.append(methodName).append(": ").append(String.valueOf(t));
            boolean block = !((MyTypes) t).text().toString().equals("block");
            sw.append("return: ").append(String.valueOf(block)).append("\n\n");
            return block;
        };

        // Test the interceptor with various messages
        doTest(ui, "Hello World");
        doTest(ui, "block");
        String theTimeIs = "The time is " + LocalDateTime.now();
        doTest(ui, theTimeIs);
        assertEquals("accept: !net.openhft.chronicle.wire.MyTypes {\n" +
                "  text: Hello World,\n" +
                "  flag: false,\n" +
                "  b: 0,\n" +
                "  s: 0,\n" +
                "  ch: \"\\0\",\n" +
                "  i: 0,\n" +
                "  f: 0.0,\n" +
                "  d: 0.0,\n" +
                "  l: 0\n" +
                "}\n" +
                "return: true\n" +
                "\n" +
                "accept: !net.openhft.chronicle.wire.MyTypes {\n" +
                "  text: block,\n" +
                "  flag: false,\n" +
                "  b: 0,\n" +
                "  s: 0,\n" +
                "  ch: \"\\0\",\n" +
                "  i: 0,\n" +
                "  f: 0.0,\n" +
                "  d: 0.0,\n" +
                "  l: 0\n" +
                "}\n" +
                "return: false\n" +
                "\n" +
                "accept: !net.openhft.chronicle.wire.MyTypes {\n" +
                "  text: \"" + theTimeIs + "\",\n" +
                "  flag: false,\n" +
                "  b: 0,\n" +
                "  s: 0,\n" +
                "  ch: \"\\0\",\n" +
                "  i: 0,\n" +
                "  f: 0.0,\n" +
                "  d: 0.0,\n" +
                "  l: 0\n" +
                "}\n" +
                "return: true\n" +
                "\n", sw.toString(), "Expected interceptor output for three messages");
    }

    // Helper method to test UIClassGenerator with a given message and update interceptor
    private void doTest(UpdateInterceptor ui, String message) throws Exception {
        UIClassGenerator scg = new UIClassGenerator();
        scg.metaData()
                .packageName(Jvm.getPackageName(getClass()))
                .baseClassName("ACGTUI")
                .useUpdateInterceptor(true)
                .message = message;
        Class<Consumer<MyTypes>> aClass = scg.acquireClass(getClass().getClassLoader());
        Consumer<MyTypes> callable = aClass.getDeclaredConstructor(UpdateInterceptor.class).newInstance(ui);
        // break point on the next line to be able to debug the generated class.
        MyTypes mt = new MyTypes().text(message);
        callable.accept(mt);

        // Define expected output based on input message and assert against the actual output
        String expected = message.equals("block") ? message : (message + '-' + message);
        assertEquals(expected, mt.text().toString(),
                "Expected text update to match generator output for message=" + message);
    }
}
