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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.*;
import net.openhft.chronicle.core.onoes.ChainedExceptionHandler;
import net.openhft.chronicle.core.onoes.ExceptionHandler;
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;
import net.openhft.chronicle.wire.utils.YamlAgitator;
import net.openhft.chronicle.wire.utils.YamlTester;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.*;

@SuppressWarnings({"rawtypes", "unchecked"})
/**
 * Provides a tester for methods with text input, primarily focused on YAML testing.
 * This tester is configurable and can simulate various situations, using specified exception handlers,
 * update interceptors, and other components.
 *
 * @param <T> The type of the output class.
 */
public class TextMethodTester<T> implements YamlTester {

    // Flags to determine if tests should include comments.
    private static final boolean TESTS_INCLUDE_COMMENTS = Jvm.getBoolean("tests.include.comments", true);
    // Flag to check if single-threaded tests are disabled.
    public static final boolean SINGLE_THREADED_CHECK_DISABLED = !Jvm.getBoolean("yaml.tester.single.threaded.check.enabled", false);
    // Flag to dump the tests.
    private static final boolean DUMP_TESTS = Jvm.getBoolean("dump.tests");

    // Default consumer for handling invocation target runtime exceptions.
    public static final Consumer<InvocationTargetRuntimeException> DEFAULT_INVOCATION_TARGET_RUNTIME_EXCEPTION_CONSUMER =
            e -> Jvm.warn().on(TextMethodTester.class, "Exception calling target method. Continuing", e);

    private final String input;  // The text input for the method.
    private final Class<T> outputClass;  // The class representing the type of output.

    // Set of additional classes to represent output.
    private final Set<Class> additionalOutputClasses = new LinkedHashSet<>();

    private final Function<WireOut, T> outputFunction;  // Function to generate the output based on wire data.
    private final String output;  // The text representation of the output.

    // Function to handle components within the tester.
    private final BiFunction<T, UpdateInterceptor, Object> componentFunction;

    // Flag to determine if the text should be treated as YAML.
    private final boolean TEXT_AS_YAML = Jvm.getBoolean("wire.testAsYaml");

    private Function<T, ExceptionHandler> exceptionHandlerFunction;  // Function to generate an exception handler.
    private BiConsumer<MethodReader, T> exceptionHandlerSetup;  // Setup for the exception handler.
    private String genericEvent;  // Generic event for the tester.
    private List<String> setups;  // List of setups for the tester.
    private Function<String, String> inputFunction;  // Function to process the input.
    private Function<String, String> afterRun;  // Function to execute after a run.
    private String expected;  // Expected text output.
    private String actual;  // Actual text output.
    private String[] retainLast;  // Strings to retain from the last run.
    private MethodReaderInterceptorReturns methodReaderInterceptorReturns;  // Interceptor for method reader returns.
    private long timeoutMS = 25;  // Timeout in milliseconds for the tester.
    private UpdateInterceptor updateInterceptor;  // Interceptor for updates.
    private Consumer<InvocationTargetRuntimeException> onInvocationException;  // Consumer for invocation exceptions.
    private boolean exceptionHandlerFunctionAndLog;  // Flag to determine if exception handler should log.
    private Predicate<String> testFilter = s -> true;  // Filter for tests.

    /**
     * Constructs a TextMethodTester with specified input, component function, output class, and output.
     *
     * @param input             The text input for the method.
     * @param componentFunction Function to handle components within the tester.
     * @param outputClass       The class representing the type of output.
     * @param output            The text representation of the output.
     */
    public TextMethodTester(String input, Function<T, Object> componentFunction, Class<T> outputClass, String output) {
        this(input, (out, ui) -> componentFunction.apply(out), outputClass, output);
    }

    /**
     * Constructs a TextMethodTester with specified input, component function, output class, and output.
     *
     * @param input             The text input for the method.
     * @param componentFunction BiFunction to handle components with an update interceptor.
     * @param outputClass       The class representing the type of output.
     * @param output            The text representation of the output.
     */
    public TextMethodTester(String input, BiFunction<T, UpdateInterceptor, Object> componentFunction, Class<T> outputClass, String output) {
        this(input, componentFunction, null, outputClass, output);
    }

    /**
     * Constructs a TextMethodTester with specified input, component function, output function, and output.
     *
     * @param input             The text input for the method.
     * @param componentFunction Function to handle components within the tester.
     * @param outputFunction    Function to generate the output based on wire data.
     * @param output            The text representation of the output.
     */
    public TextMethodTester(String input, Function<T, Object> componentFunction, Function<WireOut, T> outputFunction, String output) {
        this(input, (out, ui) -> componentFunction.apply(out), outputFunction, null, output);
    }

    /**
     * Primary private constructor for the TextMethodTester.
     */
    private TextMethodTester(String input, BiFunction<T, UpdateInterceptor, Object> componentFunction, Function<WireOut, T> outputFunction, Class<T> outputClass, String output) {
        this.input = input;
        this.componentFunction = componentFunction;
        this.outputFunction = outputFunction;
        this.outputClass = outputClass;
        this.output = output;

        this.setups = Collections.emptyList();
        this.onInvocationException = DEFAULT_INVOCATION_TARGET_RUNTIME_EXCEPTION_CONSUMER;
    }

    /**
     * Adds an output class to the tester.
     *
     * @param outputClass The additional output class to be added.
     * @return The current TextMethodTester instance.
     */
    public TextMethodTester<T> addOutputClass(Class<?> outputClass) {
        additionalOutputClasses.add(outputClass);
        return this;
    }

    /**
     * Checks if a given resource exists.
     *
     * @param resourceName The name of the resource.
     * @return true if the resource exists, otherwise false.
     */
    public static boolean resourceExists(String resourceName) {
        try {
            return new File(resourceName).exists() || IOTools.urlFor(TextMethodTester.class, resourceName) != null;
        } catch (FileNotFoundException ignored) {
            return false;
        }
    }

    /**
     * Gets the strings to be retained from the last run.
     *
     * @return An array of strings to be retained.
     */
    public String[] retainLast() {
        return retainLast;
    }

    /**
     * Specifies strings that should be retained from the last run.
     *
     * @param retainLast Strings to retain.
     * @return The current TextMethodTester instance.
     */
    @NotNull
    public TextMethodTester<T> retainLast(String... retainLast) {
        this.retainLast = retainLast;
        return this;
    }

    /**
     * Retrieves the single setup value. Throws an exception if there are none or multiple setups.
     *
     * @return The single setup string.
     * @throws IllegalStateException if there are no setups or more than one setup.
     */
    public String setup() {
        if (setups.size() != 1)
            throw new IllegalStateException();
        return setups.get(0);
    }

    /**
     * Specifies a single setup string for the method tester.
     *
     * @param setup The setup string to set.
     * @return The current TextMethodTester instance.
     */
    @NotNull
    public TextMethodTester<T> setup(@Nullable String setup) {
        this.setups = (setup == null) ? Collections.emptyList() : Collections.singletonList(setup);
        return this;
    }

    /**
     * Specifies a list of setup strings for the method tester.
     *
     * @param setups The list of setup strings.
     * @return The current TextMethodTester instance.
     */
    @NotNull
    public TextMethodTester<T> setups(@NotNull List<String> setups) {
        this.setups = setups;
        return this;
    }

    /**
     * Retrieves the after-run function.
     *
     * @return The function to execute after the run.
     */
    public Function<String, String> afterRun() {
        return afterRun;
    }

    /**
     * Specifies the function to execute after a run.
     *
     * @param afterRun The after-run function.
     * @return The current TextMethodTester instance.
     */
    @NotNull
    public TextMethodTester<T> afterRun(Function<String, String> afterRun) {
        this.afterRun = afterRun;
        return this;
    }

    /**
     * Retrieves the exception handler setup.
     *
     * @return The BiConsumer handling exception setup.
     */
    public BiConsumer<MethodReader, T> exceptionHandlerSetup() {
        return exceptionHandlerSetup;
    }

    /**
     * Specifies the BiConsumer for handling the exception setup.
     *
     * @param exceptionHandlerSetup The exception handler setup BiConsumer.
     * @return The current TextMethodTester instance.
     */
    public TextMethodTester<T> exceptionHandlerSetup(BiConsumer<MethodReader, T> exceptionHandlerSetup) {
        this.exceptionHandlerSetup = exceptionHandlerSetup;
        return this;
    }

    /**
     * Retrieves the generic event string.
     *
     * @return The generic event string.
     */
    public String genericEvent() {
        return genericEvent;
    }

    /**
     * Specifies the generic event string.
     *
     * @param genericEvent The generic event string.
     * @return The current TextMethodTester instance.
     */
    public TextMethodTester<T> genericEvent(String genericEvent) {
        this.genericEvent = genericEvent;
        return this;
    }

    /**
     * Retrieves the exception consumer for InvocationTargetRuntimeException.
     *
     * @return The exception consumer.
     */
    public Consumer<InvocationTargetRuntimeException> onInvocationException() {
        return onInvocationException;
    }

    /**
     * Specifies the consumer for InvocationTargetRuntimeException.
     *
     * @param onInvocationException The consumer for InvocationTargetRuntimeException.
     * @return The current TextMethodTester instance.
     */
    public TextMethodTester<T> onInvocationException(Consumer<InvocationTargetRuntimeException> onInvocationException) {
        this.onInvocationException = onInvocationException;
        return this;
    }

    /**
     * Executes the test based on the specified setup.
     *
     * @return The current TextMethodTester instance.
     * @throws IOException if there is an error in IO operations.
     */
    @NotNull
    public TextMethodTester<T> run() throws IOException {
        OnHeapBytes b = Bytes.allocateElasticOnHeap();
        b.singleThreadedCheckDisabled(SINGLE_THREADED_CHECK_DISABLED);
        Wire wireOut = createWire(b);

        T writer0;
        if (outputClass != null) {
            MethodWriterBuilder<T> methodWriterBuilder = wireOut.methodWriterBuilder(outputClass);
            additionalOutputClasses.forEach(((VanillaMethodWriterBuilder) methodWriterBuilder)::addInterface);
            if (updateInterceptor != null)
                methodWriterBuilder.updateInterceptor(updateInterceptor);

            if (genericEvent != null) methodWriterBuilder.genericEvent(genericEvent);

            writer0 = methodWriterBuilder.get();
        } else {
            writer0 = outputFunction.apply(wireOut);
        }
        T writer = retainLast == null
                ? writer0
                : cachedMethodWriter(writer0);
        Object component = componentFunction.apply(writer, updateInterceptor);
        Object[] components = component instanceof Object[]
                ? (Object[]) component
                : new Object[]{component};

        String setupNotFound = "";
        final Class<?> clazz = outputClass == null ? getClass() : outputClass;
        for (String setup : setups) {
            try {
                byte[] setupBytes = IOTools.readFile(clazz, setup);
                Wire wire0 = createWire(setupBytes);
                MethodReader reader0 = wire0.methodReaderBuilder()
                        .methodReaderInterceptorReturns(methodReaderInterceptorReturns)
                        .warnMissing(true)
                        .build(components);
                while (readOne(reader0, null)) {
                    wireOut.bytes().clear();
                }
                wireOut.bytes().clear();
            } catch (FileNotFoundException ignored) {
                setupNotFound = setup + " not found";
            }
        }

        // If the component implements PostSetup, trigger its postSetup method.
        if (component instanceof PostSetup)
            ((PostSetup) component).postSetup();

        // Dump test inputs if the DUMP_TESTS flag is enabled.
        if (DUMP_TESTS)
            System.out.println("input: " + input);

        // Process input data. If it starts with "=", treat it as a raw string; otherwise, load it from a resource file.
        byte[] inputBytes = input.startsWith("=")
                ? input.substring(1).trim().getBytes()
                : IOTools.readFile(clazz, input);

        // Create a wire from the input bytes.
        Wire wire = createWire(inputBytes);
        if (TESTS_INCLUDE_COMMENTS)
            wire.commentListener(wireOut::writeComment);

        // Determine the expected output. If the retainLast flag is set, load the last values; otherwise, process the output data.
        if (retainLast == null) {
            if (REGRESS_TESTS) {
                expected = "";
            } else {
                String outStr = output.startsWith("=")
                        ? output.substring(1)
                        : new String(IOTools.readFile(clazz, output), StandardCharsets.ISO_8859_1);
                expected = outStr.trim().replace("\r", "");
            }
        } else {
            ValidatableUtil.startValidateDisabled();
            try {
                expected = loadLastValues().toString().trim();
            } finally {
                ValidatableUtil.endValidateDisabled();
            }
        }
        String originalExpected = expected;
        boolean[] sepOnNext = {true};

        // Set up exception handling. If an exception handler function is provided, create a new exception handler instance.
        ExceptionHandler exceptionHandler = null;
        ExceptionHandler warn = Jvm.warn();
        ExceptionHandler error = Jvm.error();
        ExceptionHandler debug = Jvm.debug();
        if (exceptionHandlerFunction != null) {
            exceptionHandler = createExceptionHandler(writer0, warn, error);
        }

        // Build a MethodReader to read methods from the wire and invoke them on the components.
        MethodReader reader = wire.methodReaderBuilder()
                .methodReaderInterceptorReturns((Method m, Object o, Object[] args, net.openhft.chronicle.bytes.Invocation invocation) -> {
                    if (sepOnNext[0])
                        wireOut.bytes().append("---\n");
                    sepOnNext[0] = !(m.getReturnType().isInterface());
                    if (methodReaderInterceptorReturns == null)
                        return invocation.invoke(m, o, args);
                    return methodReaderInterceptorReturns.intercept(m, o, args, invocation);
                })
                .warnMissing(true)
                .build(components);

        // If there's a specific setup for the exception handler, run it
        if (exceptionHandlerSetup != null)
            exceptionHandlerSetup.accept(reader, writer);

        long pos = -1;
        boolean ok = false;
        try {
            // Reading from the wire and updating the output.
            while (readOne(reader, exceptionHandler)) {
                // Check for infinite looping if there's no advancement in reading.
                if (pos == wire.bytes().readPosition()) {
                    Jvm.warn().on(getClass(), "Bailing out of malformed message");
                    break;
                }
                // Format the output if needed.
                Bytes<?> bytes2 = wireOut.bytes();
                if (retainLast == null) {
                    if (bytes2.writePosition() > 0) {
                        int last = bytes2.peekUnsignedByte(bytes2.writePosition() - 1);
                        if (last >= ' ')
                            bytes2.append('\n');
                    }
                }
                pos = bytes2.readPosition();
            }
            ok = true;

            // Clear the wire output if the retainLast flag is set.
            if (retainLast != null)
                wireOut.bytes().clear();

            // If using cached invocations, make sure everything is written out.
            if (retainLast != null) {
                CachedInvocationHandler invocationHandler =
                        (CachedInvocationHandler) Proxy.getInvocationHandler(writer);
                try {
                    invocationHandler.flush();
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }

        } finally {
            // Restore original exception handlers if an exception handler function was used.
            if (exceptionHandlerFunction != null)
                Jvm.setExceptionHandlers(error, warn, debug);

            // If there was an error, print the problematic input for debugging.
            if (!ok)
                System.err.println("Unable to parse\n" + new String(inputBytes, StandardCharsets.UTF_8));

            // Close any components that implement Closeable.
            Closeable.closeQuietly(components);
        }

        // Capture the current state of wireOut as the actual output.
        actual = wireOut.toString().trim();

        // If running regression tests, overwrite expected with the actual output.
        if (REGRESS_TESTS && !output.startsWith("=")) {
            Jvm.pause(100);
            expected = actual = wireOut.toString().trim();
        } else {
            // If there's a mismatch, wait a bit in case there's a race condition affecting the output.
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() < start + timeoutMS) {
                if (actual.length() < expected.length())
                    Jvm.pause(25);
                else
                    break;
                actual = wireOut.toString().trim();
            }
        }

        // If there's an afterRun function, apply it to both the expected and actual output.
        if (afterRun != null) {
            expected = afterRun.apply(expected);
            actual = afterRun.apply(actual);
        }

        // If running on Windows, normalize newline characters.
        if (OS.isWindows()) {
            expected = expected.replace("\r\n", "\n");
            actual = actual.replace("\r\n", "\n");
        }

        // If in regression testing mode and the processed expected output isn't equal to
        // the original, update the output.
        if (REGRESS_TESTS && !originalExpected.equals(expected)) {
            updateOutput();
        }

        // If the actual and expected output don't match, and a setup file was missing,
        // log a warning.
        if (!expected.trim().equals(actual.trim()) && !setupNotFound.isEmpty())
            Jvm.warn().on(getClass(), setupNotFound);
        return this;
    }

    /**
     * Updates the expected output file with the latest result from the test.
     * If the expected output is too similar to previous results, it is dropped.
     * Otherwise, the expected output file is updated with the latest actual result.
     *
     * @throws IOException if there's an error during file operations.
     */
    private void updateOutput() throws IOException {
        // Replace the target path with the source path
        String output = replaceTargetWithSource(this.output);
        String output2;
        try {
            output2 = BytesUtil.findFile(output);
        } catch (FileNotFoundException fnfe) {
            File out2 = new File(this.output);
            File out = new File(out2.getParentFile(), "out.yaml");
            try {
                String output2dir = BytesUtil.findFile(replaceTargetWithSource(out.getPath()));
                output2 = new File(new File(output2dir).getParentFile(), out2.getName()).getPath();
            } catch (FileNotFoundException e2) {
                throw fnfe;
            }
        }
        String actual2 = actual.endsWith("\n") ? actual : (actual + "\n");
        if (!testFilter.test(actual2)) {
            System.err.println("The expected output for " + output2 + " has been drops as it is too similar to previous results");
            return;
        }
        System.err.println("The expected output for " + output2 + " has been updated, check your commits");

        try (FileWriter fw = new FileWriter(output2)) {
            if (OS.isWindows())
                actual2 = actual2.replace("\n", "\r\n");
            fw.write(actual2);
        }
    }

    /**
     * Creates and sets the exception handler based on the provided writer and existing handlers.
     * This method is designed to either chain exception handlers or set them individually,
     * depending on the value of {@code exceptionHandlerFunctionAndLog}.
     *
     * @param writer0 The writer object used for handling exceptions.
     * @param warn The existing warning exception handler.
     * @param error The existing error exception handler.
     * @return The constructed exception handler.
     */
    private ExceptionHandler createExceptionHandler(T writer0, ExceptionHandler warn, ExceptionHandler error) {
        ExceptionHandler exceptionHandler;
        exceptionHandler = exceptionHandlerFunction.apply(writer0);

        if (exceptionHandlerFunctionAndLog) {
            if (onInvocationException == DEFAULT_INVOCATION_TARGET_RUNTIME_EXCEPTION_CONSUMER) {
                ChainedExceptionHandler eh2 = new ChainedExceptionHandler(error, exceptionHandler);
                Consumer<InvocationTargetRuntimeException> invocationException =
                        er -> eh2.on(LoggerFactory.getLogger(classNameFor(er.getCause())), "Unhandled Exception", er.getCause());
                onInvocationException = invocationException;
            }

            Jvm.setExceptionHandlers(
                    new ChainedExceptionHandler(error, exceptionHandler),
                    new ChainedExceptionHandler(warn, exceptionHandler),
                    null);
        } else {
            if (onInvocationException == DEFAULT_INVOCATION_TARGET_RUNTIME_EXCEPTION_CONSUMER) {
                ExceptionHandler eh = exceptionHandler;
                Consumer<InvocationTargetRuntimeException> invocationException =
                        er -> eh.on(LoggerFactory.getLogger(classNameFor(er.getCause())), "Unhandled Exception", er.getCause());
                onInvocationException = invocationException;
            }

            // Set individual exception handlers
            Jvm.setExceptionHandlers(
                    exceptionHandler,
                    exceptionHandler,
                    null);
        }
        return exceptionHandler;
    }

    @Override
    public Map<String, String> agitate(YamlAgitator agitator) throws IORuntimeException {
        try {
            final Class<?> clazz = outputClass == null ? getClass() : outputClass;
            String yaml = input.startsWith("=")
                    ? input.substring(1)
                    : new String(IOTools.readFile(clazz, input), StandardCharsets.UTF_8);
            return agitator.generateInputs(yaml);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
     * Attempts to read a single message or event from the provided {@link MethodReader}.
     * Handles any exceptions thrown during the reading process by using the provided {@link ExceptionHandler} or the default {@code onInvocationException}.
     *
     * @param reader0 The MethodReader used to read the message or event.
     * @param exceptionHandler The handler to manage any exceptions thrown during the reading process.
     * @return true if the reading is successful or if an exception was caught and handled. Returns false if no messages were read and no exception occurred.
     */
    public boolean readOne(MethodReader reader0, ExceptionHandler exceptionHandler) {
        try {
            return reader0.readOne();
        } catch (InvocationTargetRuntimeException e) {
            this.onInvocationException.accept(e);

        } catch (Throwable t) {
            if (exceptionHandler == null)
                throw t;
            exceptionHandler.on(LoggerFactory.getLogger(classNameFor(t)), t.toString());
        }
        return true;
    }

    /**
     * Extracts the class name associated with the initial cause of a given {@link Throwable}.
     * If the stack trace is empty, returns a default class name "TextMethodTester".
     *
     * @param t The Throwable for which the class name is to be extracted.
     * @return The class name from the first element of the stack trace or "TextMethodTester" if the stack trace is empty.
     */
    @NotNull
    private static String classNameFor(Throwable t) {
        StackTraceElement[] stackTrace = t.getStackTrace();
        return stackTrace.length == 0 ? "TextMethodTester" : stackTrace[0].getClassName();
    }

    /**
     * Replaces certain substrings in a given path string to transform target paths to source paths.
     * Primarily used to convert paths from compiled classes to resource paths.
     *
     * @param replace The original path string.
     * @return The modified path string with target paths transformed to source paths.
     */
    private String replaceTargetWithSource(String replace) {
        return replace
                .replace('\\', '/')
                .replace("/target/test-classes/", "/src/test/resources/");
    }

    /**
     * Creates a {@link Wire} object from a given byte array.
     * This method provides flexibility by allowing input to be processed using the {@code inputFunction} if it's set, or using the byte array directly if it's not.
     *
     * @param byteArray The byte array from which the Wire object will be created.
     * @return A new Wire object constructed using the provided byte array or its processed content.
     */
    protected Wire createWire(byte[] byteArray) {
        final Bytes<?> bytes;
        if (inputFunction == null) {
            bytes = Bytes.wrapForRead(byteArray);
        } else {
            bytes = Bytes.from(inputFunction.apply(new String(byteArray, StandardCharsets.ISO_8859_1)));
        }
        return createWire(bytes);
    }

    /**
     * Creates a {@link Wire} instance based on the provided bytes.
     * The choice between creating a {@link YamlWire} or a {@link TextWire} is determined by the value of the TEXT_AS_YAML flag.
     * In both cases, text documents and timestamps are enabled.
     *
     * @param bytes The bytes from which the Wire instance will be created.
     * @return A new Wire instance, either YamlWire or TextWire based on the TEXT_AS_YAML flag.
     */
    protected Wire createWire(Bytes<?> bytes) {
        return TEXT_AS_YAML
                ? new YamlWire(bytes).useTextDocuments().addTimeStamps(true)
                : new TextWire(bytes).useTextDocuments().addTimeStamps(true);
    }

    /**
     * Loads the last values from the output wire file.
     * Reads events from the wire and stores them in a map. The events are stored in sorted order.
     *
     * @return A StringBuilder containing the extracted events from the wire.
     * @throws IOException If there's a problem reading the wire file.
     * @throws InvalidMarshallableException If the wire file contains invalid or unmarshallable data.
     */
    @NotNull
    protected StringBuilder loadLastValues() throws IOException, InvalidMarshallableException {
        Wire wireOut = createWire(BytesUtil.readFile(output));
        Map<String, String> events = new TreeMap<>();
        consumeDocumentSeparator(wireOut);
        while (wireOut.hasMore()) {
            StringBuilder event = new StringBuilder();
            long start = wireOut.bytes().readPosition();
            Map<String, Object> m = wireOut.read(event).marshallableAsMap(String.class, Object.class);
            assert m != null;
            StringBuilder key = new StringBuilder(event);
            for (String s : retainLast) {
                key.append(",").append(m.get(s));
            }
            long end = wireOut.bytes().readPosition();
            BytesStore<?, ?> bytesStore = wireOut.bytes().subBytes(start, end - start);
            events.put(key.toString(), bytesStore.toString().trim());
            bytesStore.releaseLast();
            consumeDocumentSeparator(wireOut);
        }
        StringBuilder expected2 = new StringBuilder();
        for (String s : events.values()) {
            expected2.append(s.replace("\r", "")).append("\n");
        }
        return expected2;
    }

    /**
     * Consumes the document separator (i.e., '---') from the wire if present.
     * This ensures the wire read position is set correctly for the next read operation.
     *
     * @param wireOut The wire from which the document separator is to be consumed.
     */
    private void consumeDocumentSeparator(@NotNull Wire wireOut) {
        if (wireOut.bytes().peekUnsignedByte() == '-') {
            wireOut.bytes().readSkip(3);
        }
    }

    /**
     * Creates a proxy instance that implements the outputClass interface, backed by a {@link CachedInvocationHandler}.
     * This proxy is used to cache method calls, allowing for potential optimizations or deferred operations.
     *
     * @param writer0 The original writer instance that the proxy will delegate to.
     * @return A cached method writer proxy instance.
     */
    @NotNull
    private T cachedMethodWriter(T writer0) {
        Class[] interfaces = {outputClass};
        return (T) Proxy.newProxyInstance(outputClass.getClassLoader(), interfaces, new CachedInvocationHandler(writer0));
    }

    /**
     * Retrieves the expected output result.
     * If the expected result is not yet available, it will attempt to run the test to compute it.
     *
     * @return The expected output string.
     * @throws IORuntimeException If there's an IO issue while running the test.
     */
    public String expected() {
        if (expected == null)
            try {
                run();
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        return expected;
    }

    /**
     * Retrieves the actual test output.
     * If the actual result is not yet available, it will attempt to run the test to compute it.
     *
     * @return The actual test output string.
     * @throws IORuntimeException If there's an IO issue while running the test.
     */
    public String actual() {
        if (actual == null)
            try {
                run();
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        return actual;
    }

    /**
     * Sets the {@link UpdateInterceptor} for the tester.
     * This allows for custom logic to be executed when updates are detected.
     *
     * @param updateInterceptor The update interceptor to set.
     * @return The current instance of TextMethodTester, allowing for method chaining.
     */
    public TextMethodTester<T> updateInterceptor(UpdateInterceptor updateInterceptor) {
        this.updateInterceptor = updateInterceptor;
        return this;
    }

    /**
     * Sets the {@link MethodReaderInterceptorReturns} for the tester.
     * This allows for custom interception of method reader return values.
     *
     * @param methodReaderInterceptorReturns The method reader interceptor to set.
     * @return The current instance of TextMethodTester, allowing for method chaining.
     */
    public TextMethodTester<T> methodReaderInterceptorReturns(MethodReaderInterceptorReturns methodReaderInterceptorReturns) {
        this.methodReaderInterceptorReturns = methodReaderInterceptorReturns;
        return this;
    }

    /**
     * Sets the timeout (in milliseconds) for the tester.
     *
     * @param timeoutMS The timeout in milliseconds to set.
     * @return The current instance of TextMethodTester, allowing for method chaining.
     */
    public TextMethodTester<T> timeoutMS(long timeoutMS) {
        this.timeoutMS = timeoutMS;
        return this;
    }

    /**
     * Sets the exception handling function for the tester.
     * This function determines how exceptions are handled during test execution.
     *
     * @param exceptionHandlerFunction The exception handling function to set.
     * @return The current instance of TextMethodTester, allowing for method chaining.
     */
    public TextMethodTester<T> exceptionHandlerFunction(Function<T, ExceptionHandler> exceptionHandlerFunction) {
        this.exceptionHandlerFunction = exceptionHandlerFunction;
        return this;
    }

    /**
     * Configures whether the tester should log the exception handling function.
     * This determines how exceptions are logged during test execution.
     *
     * @param exceptionHandlerFunctionAndLog Flag to determine if the exception handling function should be logged.
     * @return The current instance of TextMethodTester, allowing for method chaining.
     */
    public TextMethodTester<T> exceptionHandlerFunctionAndLog(boolean exceptionHandlerFunctionAndLog) {
        this.exceptionHandlerFunctionAndLog = exceptionHandlerFunctionAndLog;
        return this;
    }

    /**
     * Sets a filter for the tester to determine which tests should be executed.
     *
     * @param testFilter The filter predicate that accepts a test's name as a parameter.
     * @return The current instance of TextMethodTester, allowing for method chaining.
     */
    public TextMethodTester<T> testFilter(Predicate<String> testFilter) {
        this.testFilter = testFilter;
        return this;
    }

    /**
     * Configures the input transformation function for the tester.
     * This function allows for modification of input values before they're processed.
     *
     * @param inputFunction The function that transforms input values.
     * @return The current instance of TextMethodTester, allowing for method chaining.
     */
    public TextMethodTester<T> inputFunction(Function<String, String> inputFunction) {
        this.inputFunction = inputFunction;
        return this;
    }

    /**
     * Represents an action that should be performed after a setup procedure.
     * This can be used to perform any necessary additional configuration or initialization.
     */
    public interface PostSetup {
        void postSetup();
    }

    /**
     * Represents a method invocation, capturing the method and its arguments.
     *
     * @deprecated This class is deprecated and is used by a specific client. New implementations should avoid using it.
     */
    @Deprecated(/* used by one client*/)
    static class Invocation {
        Method method;
        Object[] args;

        /**
         * Creates a new Invocation instance with the provided method and arguments.
         *
         * @param method The method being invoked.
         * @param args The arguments passed to the method.
         */
        public Invocation(Method method, Object[] args) {
            this.method = method;
            this.args = args;
        }
    }

    /**
     * Provides a caching mechanism for method invocations. This handler captures and caches
     * invocations for specific methods and allows for their subsequent execution, ensuring that
     * the same operations can be repeated or deferred as needed.
     *
     * @deprecated This class is deprecated and is used by a specific client. New implementations should avoid using it.
     */
    @Deprecated(/* used by one client*/)
    class CachedInvocationHandler implements InvocationHandler {
        // Cache to store the method invocations, where the key is a combination of method name and arguments.
        private final Map<String, Invocation> cache = new TreeMap<>();
        private final T writer0;

        /**
         * Constructs a new CachedInvocationHandler with the provided writer.
         *
         * @param writer0 The writer to which the cached invocations will be applied when flushed.
         */
        public CachedInvocationHandler(T writer0) {
            this.writer0 = writer0;
        }

        @Nullable
        @Override
        public Object invoke(Object proxy, @NotNull Method method, @Nullable Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }

            if (args != null && args.length == 1 && args[0] instanceof Marshallable) {
                StringBuilder key = new StringBuilder();
                key.append(method.getName());
                Marshallable m = (Marshallable) args[0];
                try {
                    for (String s : retainLast) {
                        key.append(",").append(m.getField(s, Object.class));
                    }
                } catch (NoSuchFieldException e) {
                    // move on
                }
                args[0] = m.deepCopy();
                cache.put(key.toString(), new Invocation(method, args));
            } else {
                method.invoke(writer0, args);
            }
            return null;
        }

        /**
         * Executes all the cached invocations on the provided writer. This ensures that any deferred
         * operations are now completed.
         *
         * @throws InvocationTargetException if an invocation target exception occurs during method invocation.
         * @throws IllegalAccessException if the current method cannot access the underlying method.
         */
        public void flush() throws InvocationTargetException, IllegalAccessException {
            for (Invocation invocation : cache.values()) {
                invocation.method.invoke(writer0, invocation.args);
            }
        }
    }
}
