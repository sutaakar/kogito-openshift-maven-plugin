package com.github.sutaakar.experimental.kogito.openshift;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Says "Hi" to the user.
 *
 */
@Mojo(name = "sayhi")
public class GreetingMojo extends AbstractMojo {

    /**
     * The greeting to display.
     */
    @Parameter( property = "sayhi.greeting", defaultValue = "Hello World!" )
    private String greeting;

    public void execute() throws MojoExecutionException {
        getLog().info(greeting);
        
        try (OpenShiftClient client = new DefaultOpenShiftClient()) {
            String namespace = client.getNamespace();
            getLog().info(namespace);
            
            installOperator(client);
            installSubscription(client);
            
            Path createTempFile = Files.createTempFile("test", ".zip");
            zipFolder(new File("target").toPath(), createTempFile);
            client.buildConfigs().withName("example-quarkus-binary").instantiateBinary().fromFile(createTempFile.toFile());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    

    private CustomResourceDefinitionContext customResourceDefinitionContext = new CustomResourceDefinitionContext.Builder()
      .withGroup("operators.coreos.com")
      .withName("operatorgroup")
      .withPlural("operatorgroups")
      .withScope("Namespaced")
      .withVersion("v1")
      .build();
    
    private void installOperator(OpenShiftClient client) {
        String operatorGroup = "apiVersion: operators.coreos.com/v1\n" +
                "kind: OperatorGroup\n" +
                "metadata:\n" +
                "  name: test\n" +
                "spec:\n" +
                "  targetNamespaces:\n" +
                "  - " + client.getNamespace();
        
              try {
                client.customResource(customResourceDefinitionContext).create(client.getNamespace(), operatorGroup);
            } catch (KubernetesClientException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }
    
    private CustomResourceDefinitionContext customResourceDefinitionContext2 = new CustomResourceDefinitionContext.Builder()
            .withGroup("operators.coreos.com")
            .withName("subscription")
            .withPlural("subscriptions")
            .withScope("Namespaced")
            .withVersion("v1alpha1")
            .build();
    
    private void installSubscription(OpenShiftClient client) {
      String subscription = "apiVersion: operators.coreos.com/v1alpha1\n" +
      "kind: Subscription\n" +
      "metadata:\n" +
      "  name: kogito-operator\n" +
      "spec:\n" +
      "  channel: alpha\n" +
      "  name: kogito-operator\n" +
      "  source: community-operators\n" +
      "  sourceNamespace: openshift-marketplace";
        
              try {
                client.customResource(customResourceDefinitionContext2).create(client.getNamespace(), subscription);
            } catch (KubernetesClientException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }
    
    

    private void zipFolder(Path sourceFolderPath, Path zipPath) {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            Files.walkFileTree(sourceFolderPath, new SimpleFileVisitor<Path>() {
    
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(new ZipEntry(sourceFolderPath.relativize(file).toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
