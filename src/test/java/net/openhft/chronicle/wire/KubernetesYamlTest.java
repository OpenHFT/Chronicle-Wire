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

    @Test
    public void testExample5() {
        doTest("example5.yaml",
                "{apiVersion=rbac.istio.io/v1alpha1, kind=ServiceRole, metadata={name=hello-viewer, namespace=default}, " +
                        "spec={rules=[{services=[hello.default.svc.cluster.local], methods=[GET, HEAD]}]}}",
                "{apiVersion=rbac.istio.io/v1alpha1, kind=ServiceRole, metadata={name=world-viewer, namespace=default}, " +
                        "spec={rules=[{services=[world.default.svc.cluster.local], methods=[GET, HEAD]}]}}",
                "{apiVersion=rbac.istio.io/v1alpha1, kind=ServiceRole, metadata={name=world-2-viewer, namespace=default}, " +
                        "spec={rules=[{services=[world-2.default.svc.cluster.local], methods=[GET, HEAD]}]}}",
                "{apiVersion=rbac.istio.io/v1alpha1, kind=ServiceRoleBinding, metadata={name=istio-ingress-binding, namespace=default}, " +
                        "spec={subjects=[{properties={source.namespace=istio-system}}], roleRef={kind=ServiceRole, name=hello-viewer}}}",
                "{apiVersion=rbac.istio.io/v1alpha1, kind=ServiceRoleBinding, metadata={name=hello-user-binding, namespace=default}, " +
                        "spec={subjects=[{user=cluster.local/ns/default/sa/hello}], roleRef={kind=ServiceRole, name=world-viewer}}}",
                "{apiVersion=rbac.istio.io/v1alpha1, kind=ServiceRoleBinding, metadata={name=world-user-binding, namespace=default}, " +
                        "spec={subjects=[{user=cluster.local/ns/default/sa/world}], roleRef={kind=ServiceRole, name=world-2-viewer}}}",
                "{apiVersion=v1, kind=ServiceAccount, metadata={name=hello}}",
                "{apiVersion=v1, kind=ServiceAccount, metadata={name=world}}",
                "{apiVersion=apps/v1, kind=Deployment, metadata={name=hello}, " +
                        "spec={replicas=1, selector={matchLabels={app=hello}}, template={metadata={labels={app=hello, version=v1}}, " +
                        "spec={serviceAccountName=hello, containers=[{name=hello, image=wardviaene/http-echo, env=[" +
                        "{name=TEXT, value=hello}, {name=NEXT, value=world:8080}], ports=[{name=http, containerPort=8080}]}]}}}}",
                "{apiVersion=v1, kind=Service, metadata={name=hello, labels={app=hello}}, spec={selector={app=hello}, ports=[{name=http, port=8080, targetPort=8080}]}}",
                "{apiVersion=apps/v1, kind=Deployment, metadata={name=world}, spec={replicas=1, selector={matchLabels={app=world}}, " +
                        "template={metadata={labels={app=world, version=v1}}, spec={serviceAccountName=world, containers=[" +
                        "{name=world, image=wardviaene/http-echo, env=[{name=TEXT, value=world}, {name=NEXT, value=world-2:8080}], " +
                        "ports=[{name=http, containerPort=8080}]}]}}}}",
                "{apiVersion=v1, kind=Service, metadata={name=world, labels={app=world}}, spec={selector={app=world}, " +
                        "ports=[{name=http, port=8080, targetPort=8080}]}}",
                "{apiVersion=apps/v1, kind=Deployment, metadata={name=world-2}, " +
                        "spec={replicas=1, selector={matchLabels={app=world-2}}, template={metadata={labels={app=world-2, version=v1}}, " +
                        "spec={containers=[{name=world-2, image=wardviaene/http-echo, env=[{name=TEXT, value=!!!}], ports=[{name=http, containerPort=8080}]}]}}}}",
                "{apiVersion=v1, kind=Service, metadata={name=world-2, labels={app=world-2}}, " +
                        "spec={selector={app=world-2}, ports=[{name=http, port=8080, targetPort=8080}]}}",
                "{apiVersion=networking.istio.io/v1alpha3, kind=Gateway, metadata={name=helloworld-gateway}, " +
                        "spec={selector={istio=ingressgateway}, servers=[{port={number=80, name=http, protocol=HTTP}, hosts=[*]}]}}",
                "{apiVersion=networking.istio.io/v1alpha3, kind=VirtualService, metadata={name=helloworld}, " +
                        "spec={hosts=[hello-rbac.example.com], gateways=[helloworld-gateway], " +
                        "http=[{route=[{destination={host=hello.default.svc.cluster.local, subset=v1, port={number=8080}}}]}]}}",
                "{apiVersion=networking.istio.io/v1alpha3, kind=DestinationRule, metadata={name=hello}, " +
                        "spec={host=hello.default.svc.cluster.local, trafficPolicy={tls={mode=ISTIO_MUTUAL}}, subsets=[{name=v1, labels={version=v1}}]}}");
    }

    @Test
    public void testExample6() {
        doTest("example6.yaml", "{apiVersion=v1, items=[{apiVersion=v1, kind=Service, metadata={annotations={" +
                "external-dns.alpha.kubernetes.io/cloudflare-proxied=false, external-dns.alpha.kubernetes.io/hostname=h.christine.website, external-dns.alpha.kubernetes.io/ttl=120}, " +
                "labels={app=hlang}, name=hlang, namespace=apps}, spec={ports=[{port=5000, targetPort=5000}], selector={app=hlang}, type=ClusterIP}}, " +
                "{apiVersion=apps/v1, kind=Deployment, metadata={name=hlang, namespace=apps}, spec={replicas=2, selector={matchLabels={app=hlang}}, " +
                "template={metadata={labels={app=hlang}, name=hlang}, spec={containers=[{image=xena/hlang:latest, imagePullPolicy=Always, name=web, ports=[{containerPort=5000}]}], imagePullSecrets=[{name=regcred}]}}}}, " +
                "{apiVersion=networking.k8s.io/v1beta1, kind=Ingress, metadata={annotations={certmanager.k8s.io/cluster-issuer=letsencrypt-prod, kubernetes.io/ingress.class=nginx}, " +
                "labels={app=hlang}, name=hlang, namespace=apps}, spec={rules=[{host=h.christine.website, http={paths=[{backend={serviceName=hlang, servicePort=5000}}]}}], " +
                "tls=[{hosts=[h.christine.website], secretName=prod-certs-hlang}]}}, " +
                "{apiVersion=v1, kind=Service, metadata={annotations={external-dns.alpha.kubernetes.io/cloudflare-proxied=false, external-dns.alpha.kubernetes.io/hostname=olin.within.website, external-dns.alpha.kubernetes.io/ttl=120}, " +
                "labels={app=olin}, name=olin, namespace=apps}, spec={ports=[{port=5000, targetPort=5000}], selector={app=olin}, type=ClusterIP}}, " +
                "{apiVersion=apps/v1, kind=Deployment, metadata={name=olin, namespace=apps}, spec={replicas=2, selector={matchLabels={app=olin}}, " +
                "template={metadata={labels={app=olin}, name=olin}, spec={containers=[{image=xena/olin:latest, imagePullPolicy=Always, name=web, ports=[{containerPort=5000}]}], imagePullSecrets=[{name=regcred}]}}}}, " +
                "{apiVersion=networking.k8s.io/v1beta1, kind=Ingress, metadata={annotations={certmanager.k8s.io/cluster-issuer=letsencrypt-prod, kubernetes.io/ingress.class=nginx}, " +
                "labels={app=olin}, name=olin, namespace=apps}, spec={rules=[{host=olin.within.website, http={paths=[{backend={serviceName=olin, servicePort=5000}}]}}], " +
                "tls=[{hosts=[olin.within.website], secretName=prod-certs-olin}]}}, " +
                "{apiVersion=v1, kind=Service, metadata={annotations={external-dns.alpha.kubernetes.io/cloudflare-proxied=false, external-dns.alpha.kubernetes.io/hostname=tulpaforce.xyz, external-dns.alpha.kubernetes.io/ttl=120}, " +
                "labels={app=tulpaforcexyz}, name=tulpaforcexyz, namespace=apps}, spec={ports=[{port=80, targetPort=80}], selector={app=tulpaforcexyz}, type=ClusterIP}}, " +
                "{apiVersion=apps/v1, kind=Deployment, metadata={name=tulpaforcexyz, namespace=apps}, spec={replicas=2, selector={matchLabels={app=tulpaforcexyz}}, " +
                "template={metadata={labels={app=tulpaforcexyz}, name=tulpaforcexyz}, spec={containers=[{image=xena/tulpaforce:20190906, imagePullPolicy=Always, name=web, ports=[{containerPort=80}]}], imagePullSecrets=[{name=regcred}]}}}}, " +
                "{apiVersion=networking.k8s.io/v1beta1, kind=Ingress, metadata={annotations={certmanager.k8s.io/cluster-issuer=letsencrypt-prod, kubernetes.io/ingress.class=nginx}, " +
                "labels={app=tulpaforcexyz}, name=tulpaforcexyz, namespace=apps}, spec={rules=[{host=tulpaforce.xyz, http=" +
                "{paths=[{backend={serviceName=tulpaforcexyz, servicePort=80}}]}}], tls=[{hosts=[tulpaforce.xyz], secretName=prod-certs-tulpaforcexyz}]}}, " +
                "{apiVersion=v1, kind=Service, metadata={annotations={external-dns.alpha.kubernetes.io/cloudflare-proxied=false, external-dns.alpha.kubernetes.io/hostname=within.website, external-dns.alpha.kubernetes.io/ttl=120}, " +
                "labels={app=withinwebsite}, name=withinwebsite, namespace=apps}, spec={ports=[{port=5000, targetPort=5000}], selector={app=withinwebsite}, type=ClusterIP}}, " +
                "{apiVersion=apps/v1, kind=Deployment, metadata={name=withinwebsite, namespace=apps}, spec={replicas=2, selector={matchLabels={app=withinwebsite}}, " +
                "template={metadata={labels={app=withinwebsite}, name=withinwebsite}, spec={containers=[{image=xena/within.website:013120201402, imagePullPolicy=Always, name=web, ports=[{containerPort=5000}]}], imagePullSecrets=[{name=regcred}]}}}}, " +
                "{apiVersion=networking.k8s.io/v1beta1, kind=Ingress, metadata={annotations={certmanager.k8s.io/cluster-issuer=letsencrypt-prod, kubernetes.io/ingress.class=nginx}, labels={app=withinwebsite}, name=withinwebsite, namespace=apps}, " +
                "spec={rules=[{host=within.website, http={paths=[{backend={serviceName=withinwebsite, servicePort=5000}}]}}], tls=[{hosts=[within.website], secretName=prod-certs-withinwebsite}]}}], kind=List}");
    }

    @Test
    public void testExample7() {
        doTest("example7.yaml", "{containers=[{env=[{name=POD_ID, valueFrom=null}, {name=LOG_PATH, value=/var/log/mycompany/$(POD_ID)/logs}]}]}");
    }
}
