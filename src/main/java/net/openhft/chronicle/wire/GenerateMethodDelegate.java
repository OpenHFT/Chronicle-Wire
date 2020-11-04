package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.utils.SourceCodeFormatter;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public class GenerateMethodDelegate extends AbstractClassGenerator<GenerateMethodDelegate.GMDMetaData> {
    public GenerateMethodDelegate() {
        super(new GMDMetaData());
    }

    @Override
    public synchronized Class acquireClass(ClassLoader classLoader) {
        metaData().interfaces().add(MethodDelegate.class);
        return super.acquireClass(classLoader);
    }

    @Override
    protected String generateGenericType() {
        return "OUT extends Object & " + metaData().interfaces().stream()
                .map(this::nameForClass)
                .map(s -> s.equals("MethodDelegate") ? "MethodDelegate<OUT>" : s)
                .collect(Collectors.joining(" & "));
    }

    @Override
    protected void generateFields(SourceCodeFormatter mainCode) {
        mainCode.append("private ").append(getDelegateType()).append(" delegate;\n");
    }

    protected String getDelegateType() {
        return "OUT";
    }

    @Override
    protected void generateConstructors(SourceCodeFormatter mainCode) {
    }

    @Override
    protected void generateMethod(Method method, SourceCodeFormatter mainCode) {
        String s = method.toString();
        if (s.equals("public abstract void net.openhft.chronicle.wire.MethodDelegate.delegate(java.lang.Object)")) {
            withLineNumber(mainCode)
                    .append("public void delegate(Object delegate) {\n" +
                            "this.delegate = (").append(getDelegateType()).append(") delegate;\n" +
                    "}\n");
        } else {
            super.generateMethod(method, mainCode);
        }
    }

    @Override
    protected void generateMethod(Method method, StringBuilder params, List<String> paramList, SourceCodeFormatter mainCode) {
        if (method.getReturnType() != void.class)
            mainCode.append("return ");
        getDelegate(mainCode, method)
                .append(".").append(method.getName()).append("(").append(params).append(");\n");
    }

    protected SourceCodeFormatter getDelegate(SourceCodeFormatter mainCode, Method method) {
        return mainCode.append("this.delegate");
    }

    public static class GMDMetaData extends AbstractClassGenerator.MetaData<GMDMetaData> {

    }
}
