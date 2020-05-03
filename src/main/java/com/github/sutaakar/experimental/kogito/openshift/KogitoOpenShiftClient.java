package com.github.sutaakar.experimental.kogito.openshift;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.openshift.api.model.BuildConfig;
import org.awaitility.Awaitility;

public class KogitoOpenShiftClient extends OlmAwareOpenShiftClient {

    private static final String OPERATOR_GROUP = "kogito-operator-group";

    private static final String SUBSCRIPTION_NAME = "kogito-operator";
    private static final String CHANNEL = "alpha";
    private static final String CATALOG_SOURCE = "community-operators";

    private static final String KOGITO_OPERATOR_DEPLOYMENT_NAME = "kogito-operator";

    private CustomResourceDefinitionContext kogitoContext = new CustomResourceDefinitionContext.Builder().withGroup("app.kiegroup.org")
                                                                                                         .withName("kogitoapp")
                                                                                                         .withPlural("kogitoapps")
                                                                                                         .withScope("Namespaced")
                                                                                                         .withVersion("v1alpha1")
                                                                                                         .build();

    /**
     * Install Kogito operator into the specific namespace.
     *
     * @param namespace Where the Kogito operator will be installed to.
     */
    public void installKogitoOperator(String namespace) {
        createOperatorGroup(namespace, OPERATOR_GROUP);
        createSubscription(namespace, SUBSCRIPTION_NAME, CHANNEL, CATALOG_SOURCE);
        waitForDeploymentToBeAvailable(namespace, KOGITO_OPERATOR_DEPLOYMENT_NAME);
    }

    private void waitForDeploymentToBeAvailable(String namespace, String deploymentName) {
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(5, TimeUnit.MINUTES)
                  .until(() -> {
                      Deployment deployment = apps().deployments().inNamespace(namespace).withName(deploymentName).get();
                      return deployment != null && deployment.getStatus() != null && deployment.getStatus().getAvailableReplicas() != null && deployment.getStatus().getAvailableReplicas().intValue() > 0;
                  });
    }

    /**
     * Create new Kogito application.
     *
     * @param namespace Namespace where the Kogito application should be created in.
     * @param name Kogito application name.
     * @throws InterruptedException Thread is interrupted while waiting for Kogito application to be created.
     */
    public void createKogitoApp(String namespace, String name) {
        String kogitoApp = String.format("apiVersion: app.kiegroup.org/v1alpha1\n" +
                                         "kind: KogitoApp\n" +
                                         "metadata:\n" +
                                         "  name: %s\n" +
                                         "spec:\n" + 
                                         "  build: {}",
                                         name);
        try {
            customResource(kogitoContext).create(namespace, kogitoApp);
        } catch (IOException e) {
            throw new RuntimeException("Error while reading KogitoApp YAML.", e);
        }
        waitForBuildConfigToBeCreated(namespace, name + "-binary");
    }

    private void waitForBuildConfigToBeCreated(String namespace, String buildConfigName) {
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(5, TimeUnit.MINUTES)
                  .until(() -> {
                      BuildConfig buildConfig = buildConfigs().inNamespace(namespace).withName(buildConfigName).get();
                      return buildConfig != null;
                  });
    }
}
