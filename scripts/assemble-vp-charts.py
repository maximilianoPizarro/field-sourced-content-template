#!/usr/bin/env python3
"""Assemble charts/all/* grouped Helm charts from examples/helm/components."""
from __future__ import annotations

import shutil
from pathlib import Path

ROOT = Path("/home/maximilianopizarro/field-sourced-content-template")
SRC = ROOT / "examples/helm/components"
DST = ROOT / "charts/all"

FLAT_GROUPS = {
    "platform": [
        ("namespaces", "namespaces.yaml"),
        ("oauth-users", "oauth-users.yaml"),
        ("console-links", "console-links.yaml"),
        ("openshift-gitops", "openshift-gitops.yaml"),
    ],
    "identity-scm": [
        ("rhbk", "rhbk.yaml"),
        ("gitea", "gitea.yaml"),
    ],
    "developer-experience": [
        ("developer-hub", "developer-hub.yaml"),
        ("applicationsets", "applicationsets.yaml"),
        ("showroom", "showroom.yaml"),
        ("workshop-registration", "workshop-registration.yaml"),
        ("devspaces", "devspaces.yaml"),
    ],
    "cdc-pipeline": [
        ("kafka", "kafka.yaml"),
        ("kafka-console", "kafka-console.yaml"),
        ("apicurio-registry", "apicurio-registry.yaml"),
        ("cdc-demo", "cdc-demo.yaml"),
        ("camel-cdc", "camel-cdc.yaml"),
        ("kogito-bpm", "kogito-bpm.yaml"),
        ("mailpit", "mailpit.yaml"),
        ("cleanup", "cleanup.yaml"),
    ],
    "mesh-observability": [
        ("servicemeshoperator3", "servicemeshoperator3.yaml"),
        ("observability", "observability.yaml"),
        ("kiali", "kiali.yaml"),
        ("opentelemetry", "opentelemetry.yaml"),
        ("rhcl-operator", "rhcl-operator.yaml"),
    ],
    "ai-ml": [
        ("openshift-ai", "openshift-ai.yaml"),
        ("openshift-lightspeed", "openshift-lightspeed.yaml"),
        ("mcp-gateway", "mcp-gateway.yaml"),
        ("neuroface-cv", "neuroface-cv.yaml"),
    ],
}

LITE_WRAP = {
    "developer-experience": ["devspaces.yaml"],
    "cdc-pipeline": ["kogito-bpm.yaml"],
}

CHART_META = {
    "platform": "Namespaces, OAuth htpasswd users, console links, GitOps tuning",
    "identity-scm": "RHBK plus Gitea Route/Job (upstream Gitea chart is a separate Application)",
    "developer-experience": "Developer Hub, ApplicationSets, Showroom, registration, DevSpaces",
    "cdc-pipeline": "Kafka CDC, Apicurio, Camel, Mailpit, Kogito, cleanup",
    "mesh-observability": "Service Mesh CRs, observability, Kiali, OpenTelemetry, Kuadrant CR",
    "ai-ml": "OpenShift AI, Lightspeed (LLM Secret), MCP gateway, NeuroFace CV",
    "industrial-edge": "MinIO, RHOAI DSC/DSP, data lake, TST, StormShift, pipelines",
    "litemaas": "Optional LiteMaaS stack (disabled by default)",
}

IE_SUBCHARTS = [
    "industrial-edge-minio",
    "industrial-edge-data-science-cluster",
    "industrial-edge-data-science-project",
    "industrial-edge-data-lake",
    "industrial-edge-tst",
    "industrial-edge-stormshift",
    "industrial-edge-pipelines",
]

IE_RENAME = {
    "industrial-edge-data-science-cluster": "industrial-edge-data-science-cluster",
    "industrial-edge-data-science-project": "industrial-edge-data-science-project",
    "industrial-edge-pipelines": "industrial-edge-pipelines",
    "industrial-edge-tst": "industrial-edge-tst",
}


def write_chart_yaml(path: Path, name: str, description: str, deps: list[dict] | None = None) -> None:
    lines = [
        "apiVersion: v2",
        f"name: {name}",
        f"description: {description}",
        "type: application",
        "version: 0.1.0",
        'appVersion: "1.0"',
    ]
    if deps:
        lines.append("dependencies:")
        for d in deps:
            lines.append(f"  - name: {d['name']}")
            lines.append(f"    version: {d['version']}")
            lines.append(f"    repository: \"{d['repository']}\"")
    path.write_text("\n".join(lines) + "\n")


def copy_component_template(comp: str, dest: Path) -> None:
    tdir = SRC / comp / "templates"
    if not tdir.is_dir():
        raise SystemExit(f"missing templates for {comp}")
    files = sorted(p for p in tdir.rglob("*") if p.is_file())
    yaml_files = [p for p in files if p.suffix in {".yaml", ".yml"}]
    helpers = [p for p in files if p.name == "_helpers.tpl"]
    if helpers:
        raise SystemExit(f"{comp} has _helpers.tpl; flatten is unsafe")
    if len(yaml_files) == 1:
        shutil.copy2(yaml_files[0], dest)
        return
    # Nested templates (industrial-edge is handled separately)
    sub = dest.with_suffix("")  # unused
    out_dir = dest.parent / dest.stem
    out_dir.mkdir(parents=True, exist_ok=True)
    for p in yaml_files:
        rel = p.relative_to(tdir)
        target = out_dir / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(p, target)


def wrap_lite(path: Path) -> None:
    body = path.read_text()
    if "global.profile" in body[:400] and "lite" in body[:400]:
        return
    path.write_text(
        "{{- $g := .Values.global | default dict }}\n"
        "{{- if ne ($g.profile | default \"full\") \"lite\" }}\n"
        + body
        + ("\n" if not body.endswith("\n") else "")
        + "{{- end }}\n"
    )


def namespaces_users_only(src: Path, dest: Path) -> None:
    text = src.read_text()
    marker = "# Per-User Namespaces"
    idx = text.find(marker)
    if idx < 0:
        raise SystemExit("per-user marker not found in namespaces chart")
    dest.write_text("# Dynamic per-user namespaces (static namespaces live in clusterGroup.namespaces)\n" + text[idx:])


def assemble_flat() -> None:
    for group, comps in FLAT_GROUPS.items():
        gdir = DST / group
        tdir = gdir / "templates"
        if tdir.exists():
            shutil.rmtree(tdir)
        tdir.mkdir(parents=True, exist_ok=True)
        write_chart_yaml(gdir / "Chart.yaml", group, CHART_META[group])
        values_bits = [
            "global:",
            "  profile: full",
            '  localClusterDomain: ""',
            '  hubClusterDomain: ""',
            "  lightspeed:",
            '    llmApiKey: "sk-no-key"',
            '    llmEndpoint: ""',
            '    model: "qwen25-7b-instruct"',
            "userCount: 30",
            'clusterDomain: ""',
        ]
        for comp, fname in comps:
            dest = tdir / fname
            if comp == "namespaces":
                namespaces_users_only(SRC / "namespaces" / "templates" / "all.yaml", dest)
            elif comp == "showroom":
                shutil.copy2(SRC / "showroom" / "templates" / "showroom.yaml", dest)
            else:
                copy_component_template(comp, dest)
            vfile = SRC / comp / "values.yaml"
            if vfile.exists():
                values_bits.append(f"\n# --- from {comp} ---")
                values_bits.append(vfile.read_text().rstrip())
        (gdir / "values.yaml").write_text("\n".join(values_bits) + "\n")
        for wrap in LITE_WRAP.get(group, []):
            wrap_lite(tdir / wrap)


def fix_chart_name(chart_yaml: Path, name: str) -> None:
    lines = []
    replaced = False
    for line in chart_yaml.read_text().splitlines():
        if line.startswith("name:") and not replaced:
            lines.append(f"name: {name}")
            replaced = True
        else:
            lines.append(line)
    chart_yaml.write_text("\n".join(lines) + "\n")


def assemble_industrial_edge() -> None:
    gdir = DST / "industrial-edge"
    if gdir.exists():
        shutil.rmtree(gdir)
    charts = gdir / "charts"
    charts.mkdir(parents=True)
    deps = []
    parent_values = [
        "userCount: 30",
        'clusterDomain: ""',
        "global:",
        "  imageregistry:",
        "    type: openshift-internal",
        '    hostname: ""',
        '    account: ""',
        '  localClusterDomain: ""',
        '  hubClusterDomain: ""',
        "  git:",
        "    account: gitea_admin",
        "    email: admin@example.com",
        "    dev_revision: main",
    ]
    for name in IE_SUBCHARTS:
        src = SRC / name
        dest = charts / name
        shutil.copytree(src, dest, ignore=shutil.ignore_patterns("docs", "*.md"))
        fix_chart_name(dest / "Chart.yaml", name)
        if name in {"industrial-edge-tst", "industrial-edge-stormshift"}:
            helper = dest / "templates" / "_helpers.tpl"
            if helper.exists():
                helper.unlink()
        ver = "0.1.0"
        for line in (dest / "Chart.yaml").read_text().splitlines():
            if line.startswith("version:"):
                ver = line.split(":", 1)[1].strip()
        deps.append({"name": name, "version": ver, "repository": f"file://charts/{name}"})
        vfile = dest / "values.yaml"
        if vfile.exists():
            parent_values.append(f"\n{name}:")
            for line in vfile.read_text().splitlines():
                parent_values.append(f"  {line}" if line else "")
    write_chart_yaml(gdir / "Chart.yaml", "industrial-edge", CHART_META["industrial-edge"], deps)
    (gdir / "values.yaml").write_text("\n".join(parent_values) + "\n")
    # placeholder so helm template has a templates dir
    (gdir / "templates").mkdir(exist_ok=True)
    (gdir / "templates" / "_notes.yaml").write_text(
        "# Industrial Edge group — resources come from subcharts\n"
    )


def assemble_litemaas() -> None:
    dest = DST / "litemaas"
    if dest.exists():
        shutil.rmtree(dest)
    shutil.copytree(SRC / "litemaas", dest, ignore=shutil.ignore_patterns("docs"))


def main() -> None:
    DST.mkdir(parents=True, exist_ok=True)
    assemble_flat()
    assemble_industrial_edge()
    assemble_litemaas()
    print("assembled", sorted(p.name for p in DST.iterdir() if p.is_dir()))


if __name__ == "__main__":
    main()
