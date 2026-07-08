# Published Artifacts Report

Данный отчёт возвращает список артефактов, опубликованных в Artifactory для заданных сборок.
Для каждого `buildName` выполняется поиск через AQL по свойствам `@build.name` и `@build.number`.
Артефакты группируются по имени сборки, внутри группы сортируются по имени файла.

Если у артефакта отсутствует свойство `build.name`, он пропускается с записью `WARN` в лог.

## Формат отчёта

Отчёт формируется в формате JSON и содержит общее количество артефактов и разбивку по сборкам.

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

| Поле                               | Описание                                          |
|------------------------------------|---------------------------------------------------|
| `size`                             | Общее количество артефактов по всем сборкам       |
| `builds`                           | Список сборок                                     |
| `builds[].buildName`               | Имя сборки (совпадает с `--build-names`)          |
| `builds[].buildNumber`             | Номер сборки (совпадает с `--build-number`)       |
| `builds[].size`                    | Количество артефактов в сборке                    |
| `builds[].artifacts`               | Список артефактов, отсортированный по имени файла |
| `builds[].artifacts[].name`        | Имя файла артефакта                               |
| `builds[].artifacts[].url`         | Полный URL артефакта в Artifactory                |
| `builds[].artifacts[].repository`  | Репозиторий Artifactory                           |
| `builds[].artifacts[].path`        | Путь внутри репозитория                           |
| `builds[].artifacts[].created`     | Дата создания в формате `dd.MM.yyyy HH:mm:ss`     |

## Automation

Отчёт запускается из TeamCity через Meta-Runner или из командной строки.

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

### Параметры команды `generate-published-artifacts-report`

| Параметр                  | Обязательный | Описание                                           |
|---------------------------|--------------|----------------------------------------------------|
| `--artifactory-url`       | да           | URL Artifactory                                    |
| `--artifactory-user`      | да           | Имя пользователя Artifactory                       |
| `--artifactory-password`  | да           | Пароль Artifactory                                 |
| `--build-names`           | да           | Имена сборок через запятую                         |
| `--build-number`          | да           | Номер сборки (одинаковый для всех `--build-names`) |

### Meta-Runner

`GeneratePublishedArtifactsReport.xml` — инкапсулирует все параметры CLI, позволяет запускать генерацию отчёта как шаг сборки.