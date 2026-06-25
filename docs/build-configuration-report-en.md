# Build Configuration Report

This report verifies that the build configurations of components match the standard TeamCity templates.
For each component, the parameters and steps of its configuration are compared with the templates
defined for the selected build stage (`BUILD`, `RELEASE_CANDIDATE`, `RELEASE`).

Possible component statuses in the report:

- `OK` - configuration found and matches the template
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
    "excludeComponents": ["legacy-service"]
  },
  "checks": {
    "buildStage": "BUILD",
    "parameters": ["XRAY", "SONAR"],
    "steps": ["Compile", "Test"]
  }
}
```

| Field                                      | Required              | Description                                                    |
|--------------------------------------------|-----------------------|----------------------------------------------------------------|
| `rootProjectId`                            | yes                   | Root TeamCity project where component subprojects are searched |
| `componentsFilter.includeSystems`          | no                    | Filter by systems. If empty - all components                   |
| `componentsFilter.excludeComponents`       | no                    | Component identifiers to exclude                               |
| `checks.buildStage`                        | no (default: `BUILD`) | Build stage: `BUILD`, `RELEASE_CANDIDATE`, `RELEASE`           |
| `checks.parameters`                        | no                    | TeamCity parameter names to check                              |
| `checks.steps`                             | no                    | TeamCity step names to check                                   |

If both `parameters` and `steps` are empty, an empty result is returned.

### Response

```json
{
  "rootProjectId": "MyRootProject",
  "result": [
    {
      "componentId": "my-service",
      "status": "OK",
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
      "status": "NO_BUILD_CONFIGURATION"
    }
  ]
}
```

| Field                                | Description                                            |
|--------------------------------------|--------------------------------------------------------|
| `rootProjectId`                      | Root project passed in the request                     |
| `result`                             | List of reports per component, sorted by `componentId` |
| `result[].componentId`               | Component identifier                                   |
| `result[].status`                    | `OK`, `NO_BUILD_CONFIGURATION`, `NO_PROJECT`           |
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
  --root-project-id=MyRootProject \
  --include-systems=SYSTEM_A \
  --build-stage=BUILD \
  --parameters=XRAY,SONAR \
  --steps=Compile,Test
```

For a full list of options: `generate-build-configuration-report --help`.

### Meta-Runner

`GenerateBuildConfigurationReport.xml` - encapsulates all CLI parameters,
allows running the check as a build step without writing scripts.

### Publishing to Confluence

When passing `--publish-to-wiki=true` and Confluence access parameters,
the report is published to the specified wiki page via the Confluence REST API.