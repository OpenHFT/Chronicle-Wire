package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static net.openhft.chronicle.wire.WireType.TEXT;
import static net.openhft.chronicle.wire.WireType.YAML;

@SuppressWarnings("rawtypes")
public class KubernetesYamlTest extends WireTestCommon {
    static String DIR = "/yaml/k8s/";

    public static void doTest(String file, String expected) {
        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = KubernetesYamlTest.class.getResourceAsStream
                    (DIR + file);

            Scanner s = new Scanner(is).useDelimiter("\\A");
            Object o = YAML.fromString(s.hasNext() ? s.next() : "");
            Assert.assertNotNull(o);
            String actual = o.toString();

            Assert.assertEquals(expected, actual);
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testExample1() {
        doTest("example1.yaml",
                "{apiVersion=v1, kind=Pod, metadata={name=frontend}, spec={containers=[" +
                        "{name=app, image=images.my-company.example/app:v4, resources={requests={memory=64Mi, cpu=250m}, limits={memory=128Mi, cpu=500m}}}, " +
                        "{name=log-aggregator, image=images.my-company.example/log-aggregator:v6, resources={requests={memory=64Mi, cpu=250m}, limits={memory=128Mi, cpu=500m}}}]}}");
    }

    @Test
    public void testExample2() {
        doTest("example2.yaml",
        "{apiVersion=v1, kind=Pod, metadata={name=frontend}, spec={containers=[" +
                "{name=app, image=images.my-company.example/app:v4, resources={requests={ephemeral-storage=2Gi}, limits={ephemeral-storage=4Gi}}, volumeMounts=[{name=ephemeral, mountPath=/tmp}]}, " +
                "{name=log-aggregator, image=images.my-company.example/log-aggregator:v6, resources={requests={ephemeral-storage=2Gi}, limits={ephemeral-storage=4Gi}}, volumeMounts=[{name=ephemeral, mountPath=/tmp}]}], " +
                "volumes=[{name=ephemeral, emptyDir={}}]}}");
    }
}
