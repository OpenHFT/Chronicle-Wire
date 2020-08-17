package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.utils.JsonSourceCodeFormatter;
import net.openhft.chronicle.wire.utils.SourceCodeFormatter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenerateJsonSchemaMain {
    final Map<Class, String> aliases = new LinkedHashMap<>();
    final Map<Class, String> definitions = new LinkedHashMap<>();
    final Map<String, String> events = new LinkedHashMap<>();
    final Set<Class> eventClasses = new LinkedHashSet<>();

    public GenerateJsonSchemaMain() {
        aliases.put(void.class, "null");
        aliases.put(Void.class, "null");
        aliases.put(String.class, "string");
        aliases.put(byte.class, "integer");
        aliases.put(short.class, "integer");
        aliases.put(int.class, "integer");
        aliases.put(long.class, "integer");
        aliases.put(float.class, "number");
        aliases.put(double.class, "number");
        aliases.put(boolean.class, "boolean");
    }

    public static void main(String... args) throws ClassNotFoundException {
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        for (String arg : args) {
            interfaces.add(Class.forName(arg));
        }
        GenerateJsonSchemaMain g = new GenerateJsonSchemaMain();
        for (Class<?> aClass : interfaces) {
            g.generateEventSchemaFor(aClass);
        }
        System.out.println(g.asJson());
    }

    String asJson() {
        SourceCodeFormatter sb = new JsonSourceCodeFormatter();
        String str = "{\n" +
                "\"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "\"$id\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "\"title\": \"Core schema meta-schema\",\n" +
                "\"definitions\": {\n";
        sb.append(str);
        String sep = "";
        for (Map.Entry<Class, String> entry : definitions.entrySet()) {
            sb.append(sep);
            sb.append("\"" + entry.getKey().getSimpleName() + "\": {\n");
            sb.append(entry.getValue());
            sb.append("}");
            sep = ",\n";
        }
        sb.append("\n");
        sb.append("},\n" +
                "\"properties\": {\n");
        for (Map.Entry<String, String> entry : events.entrySet()) {
            sb.append("\"" + entry.getKey() + "\": {\n");
            sb.append(entry.getValue());
            sb.append("},\n");
        }
        sb.append("" +
                "}\n" +
                "}\n");
        return sb.toString();
    }

    void generateEventSchemaFor(Class<?> type) {
        if (type.isArray())
            return;
        if (!eventClasses.add(type))
            return;
        for (Method method : type.getMethods()) {
            generateEventSchemaFor(method.getReturnType());
            Stream.of(method.getParameterTypes())
                    .forEach(this::generateObjectSchemaFor);
            StringBuilder desc = new StringBuilder();
            Class<?>[] pTypes = method.getParameterTypes();
            Annotation[][] pAnnotations = method.getParameterAnnotations();
            switch (pTypes.length) {
                case 0:
                    desc.append("" +
                            "\"type\": \"constant\",\n" +
                            "\"value\": \"\""
                    );
                    break;
                case 1:
                    generateMethodDesc(desc, pTypes[0], pAnnotations[0]);
                    break;
                default:
                    throw new UnsupportedOperationException(method.toString());
            }

            events.put(method.getName(), desc.toString());
        }
    }

    private void addProperties(Map<String, String> properties, StringBuilder sb) {
        sb.append("" +
                "\"properties\": {");
        String sep = "\n";
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            sb.append(sep);
            sb.append("\"" + entry.getKey() + "\": {\n");
            sb.append(entry.getValue());
            sb.append("}");
            sep = ",\n";
        }
        sb.append("\n" +
                "}\n");
    }

    void generateObjectSchemaFor(Class<?> type) {
        if (type.isArray())
            return;
        if (aliases.containsKey(type) || events.containsKey(type))
            return;
        aliases.put(type, "#/definitions/" + type.getSimpleName());
        Set<String> required = new LinkedHashSet<>();
        Map<String, String> properties = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder();
        Map<String, Field> fieldMap = new LinkedHashMap<>();
        WireMarshaller.getAllField(type, fieldMap);
        for (Map.Entry<String, Field> entry : fieldMap.entrySet()) {
            String name = entry.getKey();
            StringBuilder desc = new StringBuilder();
            Field field = entry.getValue();
            Class<?> fType = field.getType();
            Annotation[] annotations = field.getAnnotations();
            addTypeForFieldOrParam(desc, fType, annotations);
            if (fType.isPrimitive() || hasNotNull(annotations))
                required.add(name);
            properties.put(name, desc.toString());
        }
        sb.append("\"type\": \"object\",\n");
        if (!required.isEmpty()) {
            sb.append("\"required\": [\n" +
                    "");
            sb.append(required.stream()
                    .map(s -> '"' + s + '"')
                    .collect(Collectors.joining(",\n" +
                            "")));
            sb.append("\n" +
                    "],\n");
        }
        Comment comment = type.getAnnotation(Comment.class);
        if (comment != null)
            sb.append("\"description\": \"" + comment.value() + "\",\n");

        addProperties(properties, sb);
        definitions.put(type, sb.toString());
    }

    private boolean hasNotNull(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().getName().endsWith(".NotNull"))
                return true;
        }
        return false;
    }

    private void generateMethodDesc(StringBuilder desc, Class<?> pType, Annotation[] annotations) {
        addTypeForFieldOrParam(desc, pType, annotations);
    }

    private void addTypeForFieldOrParam(StringBuilder desc, Class<?> pType, Annotation[] annotations) {
        IntConversion ic = find(annotations, IntConversion.class);
        if (ic != null) {
            desc.append("" +
                    "\"type\": \"string\"\n");
        } else {
            LongConversion lc = find(annotations, LongConversion.class);
            if (lc != null) {
                Class<? extends LongConverter> value = lc.value();
                if (value.getName().contains("Timestamp"))
                    desc.append("" +
                            "\"type\": \"string\",\n" +
                            "\"format\": \"date-time\"");
                else
                    desc.append("" +
                            "\"type\": \"string\"\n");
            } else if (Collection.class.isAssignableFrom(pType)) {
                desc.append("" +
                        "\"type\": \"array\"\n");
            } else if (Map.class.isAssignableFrom(pType)) {
                desc.append("" +
                        "\"type\": \"object\"\n");
            } else {
                generateObjectSchemaFor(pType);
                String alias = aliases.get(pType);
                String key = alias.startsWith("#") ? "$ref" : "type";
                desc.append("" +
                        "\"" + key + "\": \"" + alias + "\"\n");
            }
        }
    }

    private <T extends Annotation> T find(Annotation[] annotations, Class<T> aClass) {
        for (Annotation annotation : annotations) {
            if (aClass.isAssignableFrom(annotation.annotationType()))
                return (T) annotation;
        }
        return null;
    }
}
