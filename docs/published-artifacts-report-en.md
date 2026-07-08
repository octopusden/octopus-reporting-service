# Published Artifacts Report

This report returns the list of artifacts published in Artifactory for the specified builds.
For each `buildName`, a search is performed via AQL using the `@build.name` and `@build.number` properties.
Artifacts are grouped by build name and sorted by file name within each group.

If an artifact has no `build.name` property, it is skipped with a `WARN` log entry.

## Report Format

The report is generated in JSON format and contains the total artifact count and a breakdown by build.

```json
{
  "size": 4,
  "builds": [
    {
      "buildName": "my-component_ee",
      "buildNumber": "1.0.0",
      "size": 3,
      "artifacts": [
        {
          "name": "my-component-1.0.0.jar",
          "url": "http://artifactory/artifactory/maven-release-local/org/example/my-component/1.0.0/my-component-1.0.0.jar",
          "repository": "maven-release-local",
          "path": "org/example/my-component/1.0.0",
          "created": "15.06.2026 10:30:00"
        },
        {
          "name": "my-component-1.0.0.pom",
          "url": "http://artifactory/artifactory/maven-release-local/org/example/my-component/1.0.0/my-component-1.0.0.pom",
          "repository": "maven-release-local",
          "path": "org/example/my-component/1.0.0",
          "created": "15.06.2026 10:30:00"
        },
        {
          "name": "my-component-1.0.0-sources.jar",
          "url": "http://artifactory/artifactory/deb-release-local/org/example/my-component/1.0.0/my-component-1.0.0-sources.jar",
          "repository": "maven-release-local",
          "path": "org/example/my-component/1.0.0",
          "created": "15.06.2026 10:30:00"
        }
      ]
    },
    {
      "buildName": "my-component_ee_rpm",
      "buildNumber": "1.0.0",
      "size": 1,
      "artifacts": [
        {
          "name": "my-component-1.0.0.x86_64.rpm",
          "url": "http://artifactory/artifactory/rpm-release-local/my-component/my-component-1.0.0.x86_64.rpm",
          "repository": "rpm-release-local",
          "path": "my-component",
          "created": "15.06.2026 11:00:00"
        }
      ]
    }
  ]
}
```

| Field                              | Description                                              |
|------------------------------------|----------------------------------------------------------|
| `size`                             | Total number of artifacts across all builds              |
| `builds`                           | List of builds                                           |
| `builds[].buildName`               | Build name (matches `--build-names`)                     |
| `builds[].buildNumber`             | Build number (matches `--build-number`)                  |
| `builds[].size`                    | Number of artifacts in this build                        |
| `builds[].artifacts`               | List of artifacts, sorted by file name                   |
| `builds[].artifacts[].name`        | Artifact file name                                       |
| `builds[].artifacts[].url`         | Full artifact URL in Artifactory                         |
| `builds[].artifacts[].repository`  | Artifactory repository                                   |
| `builds[].artifacts[].path`        | Path within the repository                               |
| `builds[].artifacts[].created`     | Creation date in `dd.MM.yyyy HH:mm:ss` format            |

## Automation

The report can be run from TeamCity via Meta-Runner or from the command line.

### CLI

```bash
java -jar automation.jar \
  --json-file=report.json \
  generate-published-artifacts-report \
  --artifactory-url=http://artifactory:8080 \
  --artifactory-user=ci \
  --artifactory-password=secret \
  --build-names=my-component_ee,my-component_ee_rpm \
  --build-number=1.0.0
```

### `generate-published-artifacts-report` command parameters

| Parameter                 | Required | Description                                           |
|---------------------------|----------|-------------------------------------------------------|
| `--artifactory-url`       | yes      | Artifactory URL                                       |
| `--artifactory-user`      | yes      | Artifactory username                                  |
| `--artifactory-password`  | yes      | Artifactory password                                  |
| `--build-names`           | yes      | Build names, comma-separated                          |
| `--build-number`          | yes      | Build number (same for all `--build-names`)           |

### Meta-Runner

`GeneratePublishedArtifactsReport.xml` - encapsulates all CLI parameters, enabling the report to be run as a build step without writing scripts.