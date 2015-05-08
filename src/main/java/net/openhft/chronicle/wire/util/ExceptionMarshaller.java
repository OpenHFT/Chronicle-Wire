package net.openhft.chronicle.wire.util;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;

/**
 * Created by Rob Austin
 */
public class ExceptionMarshaller implements Marshallable {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionMarshaller.class);


    private final Exception exception;


    public ExceptionMarshaller(Exception throwable) {
        this.exception = throwable;
    }

    public ExceptionMarshaller() {
        exception = null;
    }


    @Override
    public void readMarshallable(WireIn wireIn) throws IllegalStateException {

        StringBuilder type = Wires.acquireStringBuilder();
        wireIn.getValueIn().type(type);

        final Class<? extends Exception> aClass;
        try {
            aClass = (Class) Class.forName(type.toString());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("unable to load an class of class=" + type.toString());
        }

        final Throwable exception;
        try {
            exception = MEMORY.allocateInstance(aClass);
        } catch (InstantiationException e) {
            throw new RuntimeException("unable to allocated a class=" + aClass);
        }


        wireIn.getValueIn().marshallable(m -> {

            final String message = m.read(() -> "message").text();
            if (message != null) {
                Field messageField = null;
                try {
                    messageField = Exception.class.getDeclaredField("detailMessage");
                    messageField.set(exception, message);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    LOG.error("", e);
                }

            }
        });


        try {

            final ValueIn stackTrace = wireIn.getValueIn();
                final List<StackTraceElement> stes = new ArrayList<>();

                while (stackTrace.hasNextSequenceItem()) {
                    stackTrace.sequence(c -> c.marshallable(r -> {


                        final String declaringClass = r.read(() -> "class").text();
                        final String methodName = r.read(() -> "method").text();
                        final String fileName = r.read(() -> "file").text();
                        final int lineNumber = r.read(() -> "lineNumber").int32();

                        stes.add(new StackTraceElement(declaringClass, methodName,
                                fileName, lineNumber));


                    }));
                }


                stes.add(new StackTraceElement("~ remote", "tcp ~", "", 0));
                StackTraceElement[] stackTrace2 = Thread.currentThread().getStackTrace();
                //noinspection ManualArrayToCollectionCopy
                for (int i = 4; i < stackTrace2.length; i++)
                    stes.add(stackTrace2[i]);

                Field stackTraceField = Throwable.class.getDeclaredField("stackTrace");
                stackTraceField.setAccessible(true);
                stackTraceField.set(exception, stes.toArray(new StackTraceElement[stes.size()]));
                Jvm.rethrow(exception);

            } catch (Exception e) {
                Jvm.rethrow(e);
            }

    }


    @Override
    public void writeMarshallable(WireOut wireOut) {

        final ValueOut valueOut0 = wireOut.getValueOut();
        valueOut0.type(exception.getClass().getName());
        valueOut0.marshallable(w -> w.write(() -> "message").text(exception.getMessage()));

        try {
            writeStackTraceElement(exception, valueOut0);
        } catch (Exception ignore) {
            LOG.error("", ignore);
        }

    }


    private void writeStackTraceElement(Exception e, ValueOut target) throws NoSuchFieldException,
            IllegalAccessException {

        LOG.info("", e);

        Field stackTrace = Throwable.class.getDeclaredField("stackTrace");
        stackTrace.setAccessible(true);
        List<StackTraceElement> stes = new ArrayList<>(Arrays.asList((StackTraceElement[]) stackTrace.get(e)));
        // prune the end of the stack.

        for (int i = stes.size() - 1; i > 0 && stes.get(i).getClassName().startsWith("Thread"); i--) {
            stes.remove(i);
        }

        for (StackTraceElement element : stes) {

            target.sequence(s -> s.marshallable(se -> {

                for (String[] f : new String[][]{
                        new String[]{"declaringClass", "class"},
                        new String[]{"methodName", "method"},
                        new String[]{"fileName", "file"}})

                    try {
                        final Field field = StackTraceElement.class
                                .getDeclaredField(f[0]);
                        field.setAccessible(true);
                        se.write(() -> f[1]).text(((String) field
                                .get(element)));
                    } catch (Exception e1) {
                        LOG.error("", e1);
                    }


                try {
                    final Field field = StackTraceElement.class
                            .getDeclaredField("lineNumber");
                    field.setAccessible(true);
                    se.write(() -> "lineNumber").int32((field
                            .getInt(element)));
                } catch (Exception e2) {
                    LOG.error("", e2);
                }

            }));
        }
    }

}
