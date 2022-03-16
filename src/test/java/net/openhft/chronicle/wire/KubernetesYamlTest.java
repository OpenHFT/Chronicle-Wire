package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

import static net.openhft.chronicle.wire.WireType.TEXT;
import static net.openhft.chronicle.wire.WireType.YAML;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("rawtypes")
public class KubernetesYamlTest extends WireTestCommon {
    static String DIR = "/yaml/k8s/";

    public static void doTest(String file, String... expected) {
        Bytes b = Bytes.elasticByteBuffer();
        try {
            InputStream is = KubernetesYamlTest.class.getResourceAsStream(DIR + file);

            Scanner s = new Scanner(is).useDelimiter("\\A");
            Bytes bytes = Bytes.from(s.hasNext() ? s.next() : "");
            Stream<Object> stream = YAML.streamFromBytes(Object.class, bytes);
            Object[] objects = stream.toArray();
            assertEquals(expected.length, objects.length);

            for (int i = 0; i < objects.length; i++) {
                Object o = objects[i];
                String actual = o.toString();

                assertEquals(expected[i], actual);
            }
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

    @Test
    public void testExample3() {
        doTest("example3.yaml",
        "{apiVersion=apps/v1, kind=Deployment, metadata={name=nginx-deployment}, spec={selector={matchLabels={app=nginx}}, " +
                "replicas=2, template={metadata={labels={app=nginx}}, spec={containers=[{name=nginx, image=nginx:1.14.2, ports=[{containerPort=80}]}]}}}}");
    }

    @Test
    public void testExample4() {
        doTest("example4.yaml",
        "{apiVersion=source.toolkit.fluxcd.io/v1beta1, kind=GitRepository, metadata={name=rook-ceph-source, namespace=flux-system}, " +
                "spec={interval=10m, url=https://github.com/rook/rook.git, ref={tag=v1.5.5}, ignore=# exclude all\n/*\n# include deploy crds dir\n!/cluster/examples/kubernetes/ceph/crds.yaml\n}}",

                "{apiVersion=kustomize.toolkit.fluxcd.io/v1beta1, kind=Kustomization, metadata={name=rook-ceph-crds, namespace=flux-system}, spec={interval=5m, prune=false, sourceRef={kind=GitRepository, name=rook-ceph-source}, healthChecks=[" +
                "{apiVersion=apiextensions.k8s.io/v1, kind=CustomResourceDefinition, name=cephblockpools.ceph.rook.io}, " +
                "{apiVersion=apiextensions.k8s.io/v1, kind=CustomResourceDefinition, name=cephclients.ceph.rook.io}, " +
                "{apiVersion=apiextensions.k8s.io/v1, kind=CustomResourceDefinition, name=cephclusters.ceph.rook.io}, " +
                "{apiVersion=apiextensions.k8s.io/v1, kind=CustomResourceDefinition, name=cephfilesystems.ceph.rook.io}, " +
                "{apiVersion=apiextensions.k8s.io/v1, kind=CustomResourceDefinition, name=cephnfses.ceph.rook.io}, " +
                "{apiVersion=apiextensions.k8s.io/v1, kind=CustomResourceDefinition, name=cephobjectrealms.ceph.rook.io}, " +
                "{apiVersion=apiextensions.k8s.io/v1, kind=CustomResourceDefinition, name=cephobjectstores.ceph.rook.io}, " +
                "{apiVersion=apiextensions.k8s.io/v1, kind=CustomResourceDefinition, name=cephobjectstoreusers.ceph.rook.io}, " +
                "{apiVersion=apiextensions.k8s.io/v1, kind=CustomResourceDefinition, name=cephobjectzonegroups.ceph.rook.io}, " +
                "{apiVersion=apiextensions.k8s.io/v1, kind=CustomResourceDefinition, name=cephobjectzones.ceph.rook.io}, " +
                "{apiVersion=apiextensions.k8s.io/v1, kind=CustomResourceDefinition, name=cephrbdmirrors.ceph.rook.io}, " +
                "{apiVersion=apiextensions.k8s.io/v1, kind=CustomResourceDefinition, name=volumes.rook.io}]}}");
    }

}
