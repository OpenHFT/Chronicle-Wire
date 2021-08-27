package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.UpdateInterceptor;
import net.openhft.chronicle.wire.utils.SourceCodeFormatter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GenerateMethodBridge extends AbstractClassGenerator<GenerateMethodBridge.MethodBridgeMetaData> {

    private List<String> fnameList;

    public GenerateMethodBridge() {
        super(new MethodBridgeMetaData());
    }

    public static Object bridgeFor(Class<?> destType, List<Object> toInvoke, UpdateInterceptor ui) {
        GenerateMethodBridge gmb = new GenerateMethodBridge();
        MethodBridgeMetaData md = gmb.metaData();
        md.packageName(destType.getPackage().getName());
        md.baseClassName(destType.getSimpleName());
        md.invokes(toInvoke.stream().map(o -> findClass(o)).collect(Collectors.toList()));
        md.interfaces().add(destType);
        md.useUpdateInterceptor(ui != null);
        Class<?> aClass = gmb.acquireClass(destType.getClassLoader());
        try {
            return ui == null
                    ? aClass.getConstructor(List.class).newInstance(toInvoke)
                    : aClass.getConstructor(List.class, UpdateInterceptor.class).newInstance(toInvoke, ui);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Class<?> findClass(Object o) {
        Class<?> aClass = o.getClass();
        Class<?>[] interfaces = aClass.getInterfaces();
        if (interfaces.length > 0)
            return interfaces[0];
        return aClass;
    }

    @Override
    protected void generateFields(SourceCodeFormatter mainCode) {
        MethodBridgeMetaData md = metaData();
        List<Class<?>> handlers = md.invokes;
        fnameList = new ArrayList<>();
        for (int i = 0; i < handlers.size(); i++) {
            Class<?> handler = handlers.get(i);
            String fname = fieldCase(handler);
            if (fnameList.contains(fname))
                fname += fnameList.size();
            fnameList.add(fname);
            if (i == 0)
                withLineNumber(mainCode);
            mainCode.append("private final ").append(nameForClass(handler)).append(' ').append(fname).append(";\n");
        }
    }

    @Override
    protected void generateConstructors(SourceCodeFormatter mainCode) {
        MethodBridgeMetaData md = metaData();
        withLineNumber(mainCode)
                .append("public ").append(className()).append("(").append(nameForClass(List.class)).append(" handlers");
        if (md.useUpdateInterceptor())
            mainCode.append(", ").append(nameForClass(UpdateInterceptor.class)).append(" updateInterceptor");

        mainCode.append(") {\n");
        List<Class<?>> handlers = metaData().invokes;
        for (int i = 0; i < handlers.size(); i++) {
            Class<?> handler = handlers.get(i);
            mainCode.append("this.").append(fnameList.get(i)).append(" = (").append(nameForClass(handler)).append(") handlers.get(").append(i).append(");\n");
        }
        if (md.useUpdateInterceptor())
            mainCode.append("this.updateInterceptor = updateInterceptor;\n");
        mainCode.append("}\n");
    }

    protected void generateMethod(Method method, StringBuilder params, List<String> paramList, SourceCodeFormatter mainCode) {
        MethodBridgeMetaData md = metaData();
        String name = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();

        List<Class<?>> handlers = md.invokes;
        boolean first = true;
        for (int i = 0; i < handlers.size(); i++) {
            Class<?> handler = handlers.get(i);
            String fname = fnameList.get(i);
            try {
                handler.getMethod(name, parameterTypes);
                if (first)
                    withLineNumber(mainCode);
                first = false;
                mainCode.append("this.").append(fname).append(".").append(name).append("(").append(params).append(");\n");
            } catch (NoSuchMethodException e) {
                // skip.
            }
        }
    }

    static final class MethodBridgeMetaData extends AbstractClassGenerator.MetaData<MethodBridgeMetaData> {
        private List<Class<?>> invokes = new ArrayList<>();

        public List<Class<?>> invokes() {
            return invokes;
        }

        public MethodBridgeMetaData invokes(List<Class<?>> handlers) {
            this.invokes = handlers;
            return this;
        }
    }
}
