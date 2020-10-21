package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.UpdateInterceptor;
import net.openhft.chronicle.wire.utils.SourceCodeFormatter;

import java.util.ArrayList;
import java.util.List;

public class GenerateMethodBridge extends AbstractClassGenerator<GenerateMethodBridge.MethodBridgeMetaData> {

    private List<String> fnameList;

    protected GenerateMethodBridge() {
        super(new MethodBridgeMetaData());
    }

    protected void generateFields(SourceCodeFormatter mainCode) {
        MethodBridgeMetaData md = metaData();
        List<Class<?>> handlers = md.handlers;
        fnameList = new ArrayList<>();
        for (int i = 0; i < handlers.size(); i++) {
            Class handler = handlers.get(i);
            String fname = fieldCase(handler);
            if (fnameList.contains(fname))
                fname += fnameList.size();
            fnameList.add(fname);
            if (i == 0)
                withLineNumber(mainCode);
            mainCode.append("private final ").append(nameForClass(handler)).append(' ').append(fname).append(";\n");
        }
    }

    protected void generateConstructors(SourceCodeFormatter mainCode) {
        MethodBridgeMetaData md = metaData();
        withLineNumber(mainCode)
                .append("public ").append(className()).append("(").append(nameForClass(List.class)).append(" handlers");
        if (md.useUpdateInterceptor())
            mainCode.append(", ").append(nameForClass(UpdateInterceptor.class)).append(" updateInterceptor");

        mainCode.append("{\n");
        List<Class<?>> handlers = metaData().handlers;
        for (int i = 0; i < handlers.size(); i++) {
            Class handler = handlers.get(i);
            mainCode.append("this.").append(fieldCase(handler)).append(" = (").append(nameForClass(handler)).append(") handlers.get(").append(i).append(");\n");
        }
        if (md.useUpdateInterceptor())
            mainCode.append("this.updateInterceptor = updateInterceptor;\n");
        mainCode.append("}\n");
    }

    protected void generateMethod(String name, Class<?> returnType, Class<?>[] pts, StringBuilder params, List<String> paramList, SourceCodeFormatter mainCode) {
        MethodBridgeMetaData md = metaData();

        List<Class<?>> handlers = md.handlers;
        boolean first = true;
        for (int i = 0; i < handlers.size(); i++) {
            Class<?> handler = handlers.get(i);
            String fname = fnameList.get(i);
            try {
                handler.getMethod(name, pts);
                if (first)
                    withLineNumber(mainCode);
                first = false;
                mainCode.append("this.").append(fname).append(".").append(name).append("(").append(params).append(");\n");
            } catch (NoSuchMethodException e) {
                // skip.
            }
        }
    }

    static class MethodBridgeMetaData extends AbstractClassGenerator.MetaData<MethodBridgeMetaData> {
        private List<Class<?>> handlers = new ArrayList<>();

        public List<Class<?>> handlers() {
            return handlers;
        }

        public MethodBridgeMetaData handlers(List<Class<?>> handlers) {
            this.handlers = handlers;
            return this;
        }
    }

}
