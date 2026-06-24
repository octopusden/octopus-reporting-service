# Build Configuration Report

Данный отчет проверяет соответствие сборочных конфигураций компонентов эталонным шаблонам TeamCity.
Для каждого компонента сравниваются параметры и шаги его конфигурации с шаблонами,
определёнными для выбранного этапа сборки (`BUILD`, `RELEASE_CANDIDATE`, `RELEASE`).

Возможные статусы компонента в отчёте:

- `OK` - конфигурация найдена и соответствует шаблону
- `NO_BUILD_CONFIGURATION` - проект в TeamCity найден, но ни одна конфигурация не наследует эталонный шаблон
- `NO_PROJECT` - проект в TeamCity не найден

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

| Поле                                       | Обязательное            | Описание                                                          |
|--------------------------------------------|-------------------------|-------------------------------------------------------------------|
| `rootProjectId`                            | да                      | Корневой проект TeamCity, в котором ищутся подпроекты компонентов |
| `componentsFilter.includeSystems`          | нет                     | Фильтр по системам. Если пусто - все компоненты                   |
| `componentsFilter.excludeComponents`       | нет                     | Идентификаторы компонентов для исключения                         |
| `checks.buildStage`                        | нет (default: `BUILD`)  | Этап сборки: `BUILD`, `RELEASE_CANDIDATE`, `RELEASE`              |
| `checks.parameters`                        | нет                     | Имена параметров TeamCity для проверки                            |
| `checks.steps`                             | нет                     | Имена шагов TeamCity для проверки                                 |

Если и `parameters`, и `steps` пусты - возвращается пустой результат.

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

| Поле                                 | Описание                                                     |
|--------------------------------------|--------------------------------------------------------------|
| `rootProjectId`                      | Корневой проект, переданный в запросе                        |
| `result`                             | Список отчётов по компонентам, отсортирован по `componentId` |
| `result[].componentId`               | Идентификатор компонента                                     |
| `result[].status`                    | `OK`, `NO_BUILD_CONFIGURATION`, `NO_PROJECT`                 |
| `result[].buildConfigurationUrl`     | URL проекта TeamCity (может быть `null`)                     |
| `result[].buildTypeId`               | Идентификатор сборочной конфигурации                         |
| `result[].checks`                    | Результаты проверок параметров и шагов                       |
| `result[].checks[].checkType`        | `PARAMETER` или `STEP`                                       |
| `result[].checks[].checkName`        | Имя проверяемого параметра или шага                          |
| `result[].checks[].actualValue`      | Фактическое значение                                         |
| `result[].checks[].expectedValue`    | Ожидаемое значение из шаблона                                |
| `result[].checks[].status`           | `true` - значения совпадают                                  |

## Automation

Отчёт можно запустить из TeamCity через Meta-Runner или из командной строки.

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

Полный список опций: `generate-build-configuration-report --help`.

### Meta-Runner

`GenerateBuildConfigurationReport.xml` - инкапсулирует все параметры CLI,
позволяет запускать проверку как шаг сборки без написания скриптов.

### Публикация в Confluence

При передаче `--publish-to-wiki=true` и параметров доступа к Confluence
отчёт публикуется на указанную wiki-страницу через Confluence REST API.