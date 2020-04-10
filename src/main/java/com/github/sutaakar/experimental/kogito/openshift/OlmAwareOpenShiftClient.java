package com.github.sutaakar.experimental.kogito.openshift;

import java.io.IOException;

import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.openshift.api.model.ProjectRequest;
import io.fabric8.openshift.api.model.ProjectRequestBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;

public class OlmAwareOpenShiftClient extends DefaultOpenShiftClient {

    private CustomResourceDefinitionContext operatorGroupContext = new CustomResourceDefinitionContext.Builder().withGroup("operators.coreos.com")
                                                                                                                .withName("operatorgroup")
                                                                                                                .withPlural("operatorgroups")
                                                                                                                .withScope("Namespaced")
                                                                                                                .withVersion("v1")
                                                                                                                .build();
    private CustomResourceDefinitionContext suscriptionContext = new CustomResourceDefinitionContext.Builder().withGroup("operators.coreos.com")
                                                                                                              .withName("subscription")
                                                                                                              .withPlural("subscriptions")
                                                                                                              .withScope("Namespaced")
                                                                                                              .withVersion("v1alpha1")
                                                                                                              .build();
    private CustomResourceDefinitionContext kogitoContext = new CustomResourceDefinitionContext.Builder().withGroup("app.kiegroup.org")
                                                                                                              .withName("kogitoapp")
                                                                                                              .withPlural("kogitoapps")
                                                                                                              .withScope("Namespaced")
                                                                                                              .withVersion("v1alpha1")
                                                                                                              .build();

    public void createProject(String projectName) {
        ProjectRequest projectRequest = (new ProjectRequestBuilder().withNewMetadata().withName(projectName).endMetadata().build());
        projectrequests().create(projectRequest);
    }

    public void createOperatorGroup(String namespace, String name) {
        String operatorGroup = String.format("apiVersion: operators.coreos.com/v1\n" +
                                             "kind: OperatorGroup\n" +
                                             "metadata:\n" +
                                             "  name: %s\n" +
                                             "spec:\n" +
                                             "  targetNamespaces:\n" +
                                             "  - %s",
                                             name, namespace);
        try {
            customResource(operatorGroupContext).create(namespace, operatorGroup);
        } catch (IOException e) {
            throw new RuntimeException("Error while reading OperatorGroup YAML.", e);
        }
    }

    public void createSubscription(String namespace, String name, String channel, String source) {
        String subscription = String.format("apiVersion: operators.coreos.com/v1alpha1\n" +
                                            "kind: Subscription\n" +
                                            "metadata:\n" +
                                            "  name: %s\n" +
                                            "spec:\n" +
                                            "  channel: %s\n" +
                                            "  name: %s\n" +
                                            "  source: %s\n" +
                                            "  sourceNamespace: openshift-marketplace",
                                            name, channel, name, source);
        try {
            customResource(suscriptionContext).create(namespace, subscription);
        } catch (IOException e) {
            throw new RuntimeException("Error while reading Subscription YAML.", e);
        }
    }

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
    }
}
