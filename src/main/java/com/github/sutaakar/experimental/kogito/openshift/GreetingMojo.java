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
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.ProjectRequest;
import io.fabric8.openshift.api.model.ProjectRequestBuilder;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Says "Hi" to the user.
 *
 */
@Mojo(name = "deploy")
public class GreetingMojo extends AbstractMojo {

    private TimeUtils timeUtils = new TimeUtils(getLog());
    
    public void execute() throws MojoExecutionException {
        try (OlmAwareOpenShiftClient client = new OlmAwareOpenShiftClient()) {
            String namespace = "kogito-" + UUID.randomUUID().toString().substring(0, 4);

            getLog().info("Creating namespace " + namespace);
            ProjectRequest projectRequest = (new ProjectRequestBuilder().withNewMetadata().withName(namespace).endMetadata().build());
            client.projectrequests().create(projectRequest);

            getLog().info("Installing Kogito operator");
            client.createOperatorGroup(namespace, "kogito-operator-group");
            client.createSubscription(namespace, "kogito-operator", "alpha", "community-operators");
            waitForKogitoOperatorRunning(client, namespace);

            getLog().info("Creating Kogito application");
            client.createKogitoApp(namespace, "kogito");
            waitForBuildConfig(client, namespace, "kogito-binary");

            getLog().info("Uploading content of 'target' directory");
            Path createTempFile = Files.createTempFile("test", ".zip");
            zipFolder(new File("target").toPath(), createTempFile);
            client.buildConfigs().inNamespace(namespace).withName("kogito-binary").instantiateBinary().fromFile(createTempFile.toFile());
        } catch (IOException e) {
            throw new MojoExecutionException("Error while deploying Kogito application to OpenShift", e);
        }
    }
    
    private void waitForKogitoOperatorRunning(OlmAwareOpenShiftClient client, String namespace) {
        timeUtils.wait(Duration.ofMinutes(5), Duration.ofSeconds(1), () -> {
            Deployment kogitoDeployment = client.apps().deployments().inNamespace(namespace).withName("kogito-operator").get();
            return kogitoDeployment != null && kogitoDeployment.getStatus() != null && kogitoDeployment.getStatus().getAvailableReplicas() != null && kogitoDeployment.getStatus().getAvailableReplicas().intValue() > 0;
        });
    }
    
    private void waitForBuildConfig(OlmAwareOpenShiftClient client, String namespace, String buildConfigName) {
        timeUtils.wait(Duration.ofMinutes(5), Duration.ofSeconds(1), () -> {
            BuildConfig buildConfig = client.buildConfigs().inNamespace(namespace).withName(buildConfigName).get();
            return buildConfig != null;
        });
    }
    
    private void zipFolder(Path sourceFolderPath, Path zipPath) throws IOException {
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
        }
    }
}
