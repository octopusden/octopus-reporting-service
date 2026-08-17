# Build Configuration Report

This report verifies that the build configurations of components match the standard TeamCity templates.
For each component, the parameters and steps of its configuration are compared with the templates
defined for the selected build stage (`BUILD`, `RELEASE_CANDIDATE`, `RELEASE`).

Possible component statuses in the report:

- `SUCCESS` - configuration found and matches the template
- `NO_BUILD_CONFIGURATION` - TeamCity project found, but no configuration inherits the standard template
- `NO_PROJECT` - TeamCity project not found

## API

```
POST /rest/api/1/reports/build-configuration
```

### Request

```json
{
  "rootProjectId": "MyRootProject",
  "componentsFilter": {
    "includeSystems": ["SYSTEM_A"],
    "includeComponents": ["my-service"],
    "excludeComponents": ["legacy-service"]
  },
  "checks": {
    "buildStage": "BUILD",
    "parameters": ["XRAY", "SONAR"],
    "steps": ["Compile", "Test"]
  }
}
```

| Field                                      | Required              | Description                                                                 |
|--------------------------------------------|-----------------------|-----------------------------------------------------------------------------|
| `rootProjectId`                            | yes                   | Root TeamCity project where component subprojects are searched              |
| `componentsFilter.includeSystems`          | no                    | Filter by systems. If empty - all components                                |
| `componentsFilter.includeComponents`       | no                    | Component identifiers to include (if set, only these will be in the report) |
| `componentsFilter.excludeComponents`       | no                    | Component identifiers to exclude                                            |
| `checks.buildStage`                        | no (default: `BUILD`) | Build stage: `BUILD`, `RELEASE_CANDIDATE`, `RELEASE`                        |
| `checks.parameters`                        | no                    | TeamCity parameter names to check                                           |
| `checks.steps`                             | no                    | TeamCity step names to check                                                |

If both `parameters` and `steps` are empty, an empty result is returned.

### Response

```json
{
  "request": {
    "rootProjectId": "MyRootProject",
    "componentsFilter": {
      "includeSystems": ["SYSTEM_A"],
      "includeComponents": [],
      "excludeComponents": []
    },
    "checks": {
      "buildStage": "BUILD",
      "parameters": ["XRAY"],
      "steps": ["Compile"]
    }
  },
  "result": [
    {
      "componentId": "my-service",
      "componentOwner": "team-a",
      "status": "SUCCESS",
      "buildConfigurationUrl": "http://teamcity/project.html?projectId=MyRootProject_MyService",
      "buildTypeId": "MyRootProject_MyService_Build",
      "checks": [
        {
          "checkType": "PARAMETER",
          "checkName": "XRAY",
          "actualValue": "true",
          "expectedValue": "true",
          "status": true
        },
        {
          "checkType": "STEP",
          "checkName": "Compile",
          "actualValue": "ENABLED",
          "expectedValue": "ENABLED",
          "status": true
        }
      ]
    },
    {
      "componentId": "another-service",
      "componentOwner": "team-b",
      "status": "NO_BUILD_CONFIGURATION"
    }
  ]
}
```

| Field                                | Description                                            |
|--------------------------------------|--------------------------------------------------------|
| `request`                            | Copy of the request used to generate the report        |
| `result`                             | List of reports per component, sorted by `componentId` |
| `result[].componentId`               | Component identifier                                   |
| `result[].componentOwner`            | Component owner                                        |
| `result[].status`                    | `SUCCESS`, `NO_BUILD_CONFIGURATION`, `NO_PROJECT`      |
| `result[].buildConfigurationUrl`     | TeamCity project URL (can be `null`)                   |
| `result[].buildTypeId`               | Build configuration identifier                         |
| `result[].checks`                    | Parameter and step check results                       |
| `result[].checks[].checkType`        | `PARAMETER` or `STEP`                                  |
| `result[].checks[].checkName`        | Name of the checked parameter or step                  |
| `result[].checks[].actualValue`      | Actual value                                           |
| `result[].checks[].expectedValue`    | Expected value from the template                       |
| `result[].checks[].status`           | `true` - values match                                  |

## Automation

The report can be run from TeamCity via Meta-Runner or from the command line.

### CLI

```bash
java -jar automation.jar \
  --json-file=report.json \
  generate-build-configuration-report \
  --reporting-service-url=http://reporting-service:8080 \
  --components-registry-url=http://components-registry:8080 \
  --root-project-id=MyRootProject \
  --include-systems=SYSTEM_A \
  --build-stage=BUILD \
  --parameters=XRAY,SONAR \
  --steps=Compile,Test
```

| Option                         | Required | Description                                                           |
|--------------------------------|----------|-----------------------------------------------------------------------|
| `--reporting-service-url`      | yes      | Reporting service URL                                                 |
| `--components-registry-url`    | yes      | Components Registry URL (used to build component links in the report) |
| `--root-project-id`            | yes      | Root TeamCity project                                                 |
| `--include-systems`            | no       | Systems to include (comma-separated)                                  |
| `--include-components`         | no       | Component identifiers to include (comma-separated)                    |
| `--exclude-components`         | no       | Component identifiers to exclude (comma-separated)                    |
| `--build-stage`                | no       | Build stage: `BUILD`, `RELEASE_CANDIDATE`, `RELEASE`                  |
| `--parameters`                 | no       | Parameters to check (comma-separated)                                 |
| `--steps`                      | no       | Steps to check (comma-separated)                                      |
| `--publish-to-wiki`            | no       | Publish to Confluence (`true`/`false`)                                |
| `--wiki-report-template`       | no       | Path to Velocity template for wiki                                    |
| `--wiki-page-id`               | no       | Confluence page ID                                                    |
| `--wiki-url`                   | no       | Confluence URL                                                        |
| `--wiki-user`                  | no       | Confluence username                                                   |
| `--wiki-password`              | no       | Confluence password                                                   |

### Meta-Runner

`GenerateBuildConfigurationReport.xml` - encapsulates all CLI parameters,
allows running the check as a build step without writing scripts.

### Publishing to Confluence

When passing `--publish-to-wiki=true` and Confluence access parameters,
the report is published to the specified wiki page via the Confluence REST API.