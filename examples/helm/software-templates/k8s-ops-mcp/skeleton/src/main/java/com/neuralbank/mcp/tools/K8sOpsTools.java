package com.neuralbank.mcp.tools;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.Route;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class K8sOpsTools {

    @Inject
    KubernetesClient kubeClient;

    @ConfigProperty(name = "k8s.target.namespaces")
    String targetNamespaces;

    private List<String> getNamespaces() {
        return Arrays.asList(targetNamespaces.split(","));
    }

    @Tool(description = "List all pods in the monitored namespaces. Shows pod name, status, restarts, and age for each namespace.")
    public String listPods(
            @ToolArg(description = "Namespace to list pods from. Leave empty for all monitored namespaces.", required = false) String namespace
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Pods:\n");
        sb.append("=====\n");
        List<String> namespaces = namespace != null && !namespace.isEmpty()
            ? Collections.singletonList(namespace) : getNamespaces();
        for (String ns : namespaces) {
            try {
                var pods = kubeClient.pods().inNamespace(ns.trim()).list().getItems();
                sb.append("\nNamespace: ").append(ns.trim()).append(" (").append(pods.size()).append(" pods)\n");
                sb.append("-".repeat(80)).append("\n");
                for (var pod : pods) {
                    String status = pod.getStatus().getPhase();
                    int restarts = pod.getStatus().getContainerStatuses() != null
                        ? pod.getStatus().getContainerStatuses().stream()
                            .mapToInt(cs -> cs.getRestartCount()).sum() : 0;
                    String ready = pod.getStatus().getContainerStatuses() != null
                        ? pod.getStatus().getContainerStatuses().stream()
                            .filter(cs -> Boolean.TRUE.equals(cs.getReady())).count() + "/" +
                          pod.getStatus().getContainerStatuses().size() : "0/0";
                    sb.append(String.format("  %-55s  %-12s  Ready: %-5s  Restarts: %d\n",
                        pod.getMetadata().getName(), status, ready, restarts));
                }
            } catch (Exception e) {
                sb.append("\nNamespace: ").append(ns.trim()).append(" - Error: ").append(e.getMessage()).append("\n");
            }
        }
        return sb.toString();
    }

    @Tool(description = "List all deployments in the monitored namespaces. Shows deployment name, ready replicas, and available replicas.")
    public String listDeployments(
            @ToolArg(description = "Namespace to list deployments from. Leave empty for all monitored namespaces.", required = false) String namespace
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Deployments:\n");
        sb.append("============\n");
        List<String> namespaces = namespace != null && !namespace.isEmpty()
            ? Collections.singletonList(namespace) : getNamespaces();
        for (String ns : namespaces) {
            try {
                var deployments = kubeClient.apps().deployments().inNamespace(ns.trim()).list().getItems();
                sb.append("\nNamespace: ").append(ns.trim()).append("\n");
                sb.append("-".repeat(80)).append("\n");
                for (var dep : deployments) {
                    int desired = dep.getSpec().getReplicas() != null ? dep.getSpec().getReplicas() : 0;
                    int ready = dep.getStatus().getReadyReplicas() != null ? dep.getStatus().getReadyReplicas() : 0;
                    int available = dep.getStatus().getAvailableReplicas() != null ? dep.getStatus().getAvailableReplicas() : 0;
                    sb.append(String.format("  %-50s  %d/%d ready  %d available\n",
                        dep.getMetadata().getName(), ready, desired, available));
                }
            } catch (Exception e) {
                sb.append("\nNamespace: ").append(ns.trim()).append(" - Error: ").append(e.getMessage()).append("\n");
            }
        }
        return sb.toString();
    }

    @Tool(description = "List all routes (external URLs) in the monitored namespaces. Shows the route name and its external hostname.")
    public String listRoutes(
            @ToolArg(description = "Namespace to list routes from. Leave empty for all monitored namespaces.", required = false) String namespace
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Routes (External URLs):\n");
        sb.append("======================\n");
        List<String> namespaces = namespace != null && !namespace.isEmpty()
            ? Collections.singletonList(namespace) : getNamespaces();
        for (String ns : namespaces) {
            try {
                var routes = kubeClient.resources(Route.class)
                    .inNamespace(ns.trim()).list().getItems();
                sb.append("\nNamespace: ").append(ns.trim()).append("\n");
                for (var route : routes) {
                    String host = route.getSpec().getHost();
                    String tls = route.getSpec().getTls() != null ? "https" : "http";
                    sb.append(String.format("  %-40s  %s://%s\n",
                        route.getMetadata().getName(), tls, host));
                }
            } catch (Exception e) {
                sb.append("\nNamespace: ").append(ns.trim()).append(" - Error: ").append(e.getMessage()).append("\n");
            }
        }
        return sb.toString();
    }

    @Tool(description = "Get the logs from a specific pod. Returns the last N lines of logs.")
    public String getPodLogs(
            @ToolArg(description = "Name of the pod") String podName,
            @ToolArg(description = "Namespace of the pod") String namespace,
            @ToolArg(description = "Number of log lines to retrieve (default: 50)", required = false) String lines
    ) {
        int tailLines = 50;
        try { tailLines = Integer.parseInt(lines); } catch (Exception ignored) {}
        try {
            String logs = kubeClient.pods().inNamespace(namespace)
                .withName(podName).tailingLines(tailLines).getLog();
            return "Logs for " + podName + " (last " + tailLines + " lines):\n" +
                   "=".repeat(60) + "\n" + logs;
        } catch (Exception e) {
            return "Error getting logs: " + e.getMessage();
        }
    }

    @Tool(description = "Get the health status of all services in a namespace. Checks pod readiness, deployment availability, and service endpoints.")
    public String getNamespaceHealth(
            @ToolArg(description = "Namespace to check health for") String namespace
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Health Status for namespace: ").append(namespace).append("\n");
        sb.append("=".repeat(50)).append("\n\n");
        try {
            // Pods health
            var pods = kubeClient.pods().inNamespace(namespace).list().getItems();
            long running = pods.stream().filter(p -> "Running".equals(p.getStatus().getPhase())).count();
            long failed = pods.stream().filter(p -> "Failed".equals(p.getStatus().getPhase())).count();
            long pending = pods.stream().filter(p -> "Pending".equals(p.getStatus().getPhase())).count();
            sb.append("Pods: ").append(pods.size()).append(" total");
            sb.append(" (").append(running).append(" running");
            if (failed > 0) sb.append(", ").append(failed).append(" FAILED");
            if (pending > 0) sb.append(", ").append(pending).append(" pending");
            sb.append(")\n\n");

            // Deployments health
            var deployments = kubeClient.apps().deployments().inNamespace(namespace).list().getItems();
            sb.append("Deployments:\n");
            for (var dep : deployments) {
                int desired = dep.getSpec().getReplicas() != null ? dep.getSpec().getReplicas() : 0;
                int ready = dep.getStatus().getReadyReplicas() != null ? dep.getStatus().getReadyReplicas() : 0;
                String healthIcon = ready >= desired ? "OK" : "DEGRADED";
                sb.append(String.format("  [%s] %-45s  %d/%d ready\n",
                    healthIcon, dep.getMetadata().getName(), ready, desired));
            }

            // Services
            var services = kubeClient.services().inNamespace(namespace).list().getItems();
            sb.append("\nServices: ").append(services.size()).append("\n");
            for (var svc : services) {
                String ports = svc.getSpec().getPorts().stream()
                    .map(p -> p.getPort() + "/" + p.getProtocol())
                    .collect(Collectors.joining(", "));
                sb.append(String.format("  %-45s  %s  ports: %s\n",
                    svc.getMetadata().getName(), svc.getSpec().getType(), ports));
            }
        } catch (Exception e) {
            sb.append("Error: ").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "Get events from a namespace. Shows recent Kubernetes events including warnings, errors, and status changes.")
    public String getNamespaceEvents(
            @ToolArg(description = "Namespace to get events from") String namespace,
            @ToolArg(description = "Maximum number of events to show (default: 20)", required = false) String maxEvents
    ) {
        int max = 20;
        try { max = Integer.parseInt(maxEvents); } catch (Exception ignored) {}
        try {
            List<Event> events = kubeClient.v1().events().inNamespace(namespace).list().getItems();
            events.sort((a, b) -> {
                String ta = a.getLastTimestamp() != null ? a.getLastTimestamp() : "";
                String tb = b.getLastTimestamp() != null ? b.getLastTimestamp() : "";
                return tb.compareTo(ta);
            });

            StringBuilder sb = new StringBuilder();
            sb.append("Events in namespace: ").append(namespace).append("\n");
            sb.append("=".repeat(60)).append("\n");
            int count = 0;
            for (var event : events) {
                if (count >= max) break;
                sb.append(String.format("[%s] %s - %s/%s: %s\n",
                    event.getType(),
                    event.getLastTimestamp() != null ? event.getLastTimestamp() : "unknown",
                    event.getInvolvedObject().getKind(),
                    event.getInvolvedObject().getName(),
                    event.getMessage()));
                count++;
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error getting events: " + e.getMessage();
        }
    }
}
