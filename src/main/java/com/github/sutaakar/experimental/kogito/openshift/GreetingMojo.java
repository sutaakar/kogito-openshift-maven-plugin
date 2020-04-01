package com.github.sutaakar.experimental.kogito.openshift;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.openshift.api.model.ProjectRequest;
import io.fabric8.openshift.api.model.ProjectRequestBuilder;
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

    private TimeUtils timeUtils = new TimeUtils(getLog());
    
    /**
     * The greeting to display.
     */
    @Parameter( property = "sayhi.greeting", defaultValue = "Hello World!" )
    private String greeting;

    public void execute() throws MojoExecutionException {
        getLog().info(greeting);
        
        try (OlmAwareOpenShiftClient client = new OlmAwareOpenShiftClient()) {
            String namespace = "ksuta-experiment";
            getLog().info(namespace);
            
            ProjectRequest projectRequest = (new ProjectRequestBuilder().withNewMetadata().withName(namespace).endMetadata().build());
            client.projectrequests().create(projectRequest);

            client.createOperatorGroup(namespace, "kogito-operator-group");
            client.createSubscription(namespace, "kogito-operator", "alpha", "community-operators");
            
            waitForKogitoOperatorRunning(client, namespace);
            getLog().info("Running");
            
            Path createTempFile = Files.createTempFile("test", ".zip");
            zipFolder(new File("target").toPath(), createTempFile);
            client.buildConfigs().withName("example-quarkus-binary").instantiateBinary().fromFile(createTempFile.toFile());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private void waitForKogitoOperatorRunning(OlmAwareOpenShiftClient client, String namespace) {
        timeUtils.wait(Duration.ofMinutes(5), Duration.ofSeconds(1), () -> {
            Deployment kogitoDeployment = client.apps().deployments().inNamespace(namespace).withName("kogito-operator").get();
            return kogitoDeployment != null && kogitoDeployment.getStatus() != null && kogitoDeployment.getStatus().getAvailableReplicas() != null && kogitoDeployment.getStatus().getAvailableReplicas().intValue() > 0;
        });
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
