/*
 * Copyright 2016-2022 chronicle.software
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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.utils.JsonSourceCodeFormatter;
import net.openhft.chronicle.wire.utils.SourceCodeFormatter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is the GenerateJsonSchemaMain class.
 * Its primary function is to generate a JSON schema based on the provided set of classes.
 * The generated JSON schema represents the structure and types of the specified classes.
 */
public class GenerateJsonSchemaMain {

    // A mapping of Java Classes to their corresponding JSON data types.
    final Map<Class<?>, String> aliases = new LinkedHashMap<>();

    // A mapping of Java Classes to their corresponding JSON schema definitions.
    final Map<Class<?>, String> definitions = new LinkedHashMap<>();

    // A sorted mapping of event names to their JSON schema representations.
    final Map<String, String> events = new TreeMap<>();

    // A set containing classes that represent events.
    final Set<Class<?>> eventClasses = new LinkedHashSet<>();

    /**
     * Default constructor initializing some common aliases for Java types to JSON types.
     */
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

    /**
     * Main entry point of the application.
     * For each provided class name argument, it generates a corresponding JSON schema and prints it.
     *
     * @param args Array of class names to generate JSON schema for.
     * @throws ClassNotFoundException if any of the provided class names is not found.
     */
    public static void main(String... args) throws ClassNotFoundException {
        final String json = main0(args);
        System.out.println(json);
    }

    /**
     * Processes the provided class names, generates the JSON schema for each, and returns the combined schema.
     *
     * @param args Array of class names to generate JSON schema for.
     * @return The combined JSON schema for all provided class names.
     * @throws ClassNotFoundException if any of the provided class names is not found.
     */
    static String main0(String... args) throws ClassNotFoundException {
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        for (String arg : args) {
            interfaces.add(Class.forName(arg));
        }
        GenerateJsonSchemaMain g = new GenerateJsonSchemaMain();
        for (Class<?> aClass : interfaces) {
            g.generateEventSchemaFor(aClass);
        }
        final String json = g.asJson();
        return json;
    }

    /**
     * Generates and returns a JSON-formatted schema string.
     * The schema is constructed based on the definitions and events stored in the instance.
     *
     * @return A string representation of the JSON schema.
     */
    String asJson() {
        SourceCodeFormatter sb = new JsonSourceCodeFormatter();
        String str = "{\n" +
                "\"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "\"$id\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "\"title\": \"Core schema meta-schema\",\n" +
                "\"definitions\": {\n";
        sb.append(str);
        String sep = "";
        for (Map.Entry<Class<?>, String> entry : definitions.entrySet()) {
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
        sb.append("}\n" +
                "}\n");
        return sb.toString();
    }

    /**
     * Generates a JSON schema representation for events based on the given type.
     * It processes the type's methods to derive the schema.
     * The generated schema is stored within the 'events' map, with the method name as the key
     * and the generated schema description as the value.
     *
     * @param type The class type for which the event schema is to be generated.
     */
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
                    desc.append("\"type\": \"constant\",\n" +
                            "\"value\": \"\""
                    );
                    break;
                case 1:
                    generateMethodDesc(desc, pTypes[0], pAnnotations[0]);
                    break;
                default:
                    Jvm.debug().on(getClass(), "Method ignored as more than 1 argument " + method);
                    break;
            }

            events.put(method.getName(), desc.toString());
        }
    }

    /**
     * Constructs and appends properties in JSON schema format to the provided StringBuilder.
     * The provided properties map holds the property names and their corresponding JSON definitions.
     * The resulting JSON schema structure will encapsulate these properties within a 'properties' object.
     *
     * @param properties A map of property names to their JSON schema representations.
     * @param sb         The StringBuilder to which the properties will be appended in JSON schema format.
     */
    private void addProperties(Map<String, String> properties, StringBuilder sb) {
        sb.append("\"properties\": {");
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

    /**
     * Generates a JSON schema representation for objects based on the given type.
     * This method processes the type's fields to derive the schema.
     * The generated schema is stored within the 'definitions' map with the class name as the key
     * and the generated schema description as the value.
     * If the type is already present in the 'aliases' map, the method will return without generating the schema.
     *
     * @param type The class type for which the object schema is to be generated.
     */
    void generateObjectSchemaFor(Class<?> type) {
        if (type.isArray())
            return;
        if (aliases.containsKey(type))
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
            sb.append("\"required\": [\n");
            sb.append(required.stream()
                    .map(s -> '"' + s + '"')
                    .collect(Collectors.joining(",\n")));
            sb.append("\n" +
                    "],\n");
        }
        Comment comment = Jvm.findAnnotation(type, Comment.class);
        if (comment != null)
            sb.append("\"description\": \"" + comment.value() + "\",\n");

        addProperties(properties, sb);
        definitions.put(type, sb.toString());
    }

    /**
     * Determines whether the provided array of annotations contains any annotation whose name ends with '.NotNull'.
     * This method is typically used to check if a field has a NotNull validation constraint.
     *
     * @param annotations The array of annotations to be checked.
     * @return true if an annotation with name ending in '.NotNull' is found, false otherwise.
     */
    private boolean hasNotNull(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().getName().endsWith(".NotNull"))
                return true;
        }
        return false;
    }

    /**
     * Generates a description for a method parameter for use in a JSON schema.
     * The description is based on the type and annotations associated with the parameter.
     *
     * @param desc The StringBuilder to which the description is appended.
     * @param pType The type of the method parameter.
     * @param annotations The array of annotations associated with the method parameter.
     */
    private void generateMethodDesc(StringBuilder desc, Class<?> pType, Annotation[] annotations) {
        addTypeForFieldOrParam(desc, pType, annotations);
    }

    /**
     * Adds a type descriptor to the given StringBuilder based on the type and annotations
     * of a field or method parameter. This method checks for specific annotations like
     * {@link LongConversion} to determine the type description.
     * If the field/parameter is of Collection type, it's identified as an "array", and if it's
     * of type Map, it's identified as an "object". For other types, the corresponding schema is generated.
     *
     * @param desc The StringBuilder to which the type descriptor is appended.
     * @param pType The type of the field or method parameter.
     * @param annotations The array of annotations associated with the field or method parameter.
     */
    private void addTypeForFieldOrParam(StringBuilder desc, Class<?> pType, Annotation[] annotations) {
        LongConversion lc = find(annotations, LongConversion.class);
        if (lc != null) {
            Class value = lc.value();
            if (value.getName().contains("Timestamp"))
                desc.append("\"type\": \"string\",\n" +
                        "\"format\": \"date-time\"");
            else
                desc.append("\"type\": \"string\"\n");
        } else if (Collection.class.isAssignableFrom(pType)) {
            desc.append("\"type\": \"array\"\n");
        } else if (Map.class.isAssignableFrom(pType)) {
            desc.append("\"type\": \"object\"\n");
        } else {
            generateObjectSchemaFor(pType);
            String alias = aliases.get(pType);
            String key = alias.startsWith("#") ? "$ref" : "type";
            desc.append("\"" + key + "\": \"" + alias + "\"\n");
        }
    }

    /**
     * Searches the provided array of annotations for an annotation of the specified class type.
     *
     * @param <T> The generic type parameter for the annotation.
     * @param annotations The array of annotations to search.
     * @param aClass The class type of the annotation to find.
     * @return The first found annotation of the specified class type, or null if not found.
     */
    private <T extends Annotation> T find(Annotation[] annotations, Class<T> aClass) {
        for (Annotation annotation : annotations) {
            if (aClass.isAssignableFrom(annotation.annotationType()))
                return (T) annotation;
        }
        return null;
    }
}
