package com.github.sutaakar.experimental.kogito.openshift;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Says "Hi" to the user.
 *
 */
@Mojo(name = "deploy")
public class DeployKogitoApplicationMojo extends AbstractMojo {


    public void execute() throws MojoExecutionException {
        try (KogitoOpenShiftClient client = new KogitoOpenShiftClient()) {
            String namespace = "kogito-" + UUID.randomUUID().toString().substring(0, 4);

            getLog().info("Creating namespace " + namespace);
            client.createProject(namespace);

            getLog().info("Installing Kogito operator");
            client.installKogitoOperator(namespace);

            getLog().info("Creating Kogito application");
            client.createKogitoApp(namespace, "kogito");

            getLog().info("Uploading content of 'target' directory");
            Path createTempFile = Files.createTempFile("test", ".zip");
            zipFolder(new File("target").toPath(), createTempFile);
            client.buildConfigs().inNamespace(namespace).withName("kogito-binary").instantiateBinary().fromFile(createTempFile.toFile());
        } catch (IOException e) {
            throw new MojoExecutionException("Error while deploying Kogito application to OpenShift", e);
        } catch (InterruptedException e) {
            getLog().warn("Interrupted!", e);
            Thread.currentThread().interrupt();
        }
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
