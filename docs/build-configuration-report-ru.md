# Build Configuration Report

Данный отчет проверяет соответствие сборочных конфигураций компонентов стандартным шаблонам TeamCity.
Для каждого компонента сравниваются параметры и шаги его конфигурации с шаблонами,
определёнными для выбранного этапа сборки (`BUILD`, `RELEASE_CANDIDATE`, `RELEASE`).

Возможные статусы компонента в отчёте:

- `SUCCESS` - конфигурация найдена и соответствует шаблону
- `NO_BUILD_CONFIGURATION` - проект в TeamCity найден, но ни одна конфигурация не наследует стандартный шаблон
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

| Поле                                       | Обязательное            | Описание                                                          |
|--------------------------------------------|-------------------------|-------------------------------------------------------------------|
| `rootProjectId`                            | да                      | Корневой проект TeamCity, в котором ищутся подпроекты компонентов |
| `componentsFilter.includeSystems`          | нет                     | Фильтр по системам. Если пусто - все компоненты                   |
| `componentsFilter.includeComponents`       | нет                     | Идентификаторы компонентов для включения (если заданы, только они попадут в отчёт) |
| `componentsFilter.excludeComponents`       | нет                     | Идентификаторы компонентов для исключения                         |
| `checks.buildStage`                        | нет (default: `BUILD`)  | Этап сборки: `BUILD`, `RELEASE_CANDIDATE`, `RELEASE`              |
| `checks.parameters`                        | нет                     | Имена параметров TeamCity для проверки                            |
| `checks.steps`                             | нет                     | Имена шагов TeamCity для проверки                                 |

Если и `parameters`, и `steps` пусты - возвращается пустой результат.

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

| Поле                                 | Описание                                                     |
|--------------------------------------|--------------------------------------------------------------|
| `request`                            | Копия запроса, по которому был сформирован отчёт             |
| `result`                             | Список отчётов по компонентам, отсортирован по `componentId` |
| `result[].componentId`               | Идентификатор компонента                                     |
| `result[].componentOwner`            | Владелец компонента                                          |
| `result[].status`                    | `SUCCESS`, `NO_BUILD_CONFIGURATION`, `NO_PROJECT`            |
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
  --components-registry-url=http://components-registry:8080 \
  --root-project-id=MyRootProject \
  --include-systems=SYSTEM_A \
  --build-stage=BUILD \
  --parameters=XRAY,SONAR \
  --steps=Compile,Test
```

| Опция                          | Обязательное | Описание                                                                              |
|--------------------------------|--------------|---------------------------------------------------------------------------------------|
| `--reporting-service-url`      | да           | URL сервиса отчётов                                                                   |
| `--components-registry-url`    | да           | URL реестра компонентов (используется для формирования ссылок на компоненты в отчёте) |
| `--root-project-id`            | да           | Корневой проект TeamCity                                                              |
| `--include-systems`            | нет          | Системы для включения (через запятую)                                                 |
| `--include-components`         | нет          | Идентификаторы компонентов для включения (через запятую)                              |
| `--exclude-components`         | нет          | Идентификаторы компонентов для исключения (через запятую)                             |
| `--build-stage`                | нет          | Этап сборки: `BUILD`, `RELEASE_CANDIDATE`, `RELEASE`                                  |
| `--parameters`                 | нет          | Параметры для проверки (через запятую)                                                |
| `--steps`                      | нет          | Шаги для проверки (через запятую)                                                     |
| `--publish-to-wiki`            | нет          | Публиковать в Confluence (`true`/`false`)                                             |
| `--wiki-report-template`       | нет          | Путь к Velocity-шаблону для wiki                                                      |
| `--wiki-page-id`               | нет          | ID страницы Confluence                                                                |
| `--wiki-url`                   | нет          | URL Confluence                                                                        |
| `--wiki-user`                  | нет          | Имя пользователя Confluence                                                           |
| `--wiki-password`              | нет          | Пароль Confluence                                                                     |

### Meta-Runner

`GenerateBuildConfigurationReport.xml` - инкапсулирует все параметры CLI,
позволяет запускать проверку как шаг сборки без написания скриптов.

### Публикация в Confluence

При передаче `--publish-to-wiki=true` и параметров доступа к Confluence
отчёт публикуется на указанную wiki-страницу через Confluence REST API.