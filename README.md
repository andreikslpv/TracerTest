# Tracer: не загружается R8-маппинг при сборке с AGP 9.4.1

Минимальный проект для воспроизведения проблемы: при включённой R8-оптимизации release
Gradle-плагин Tracer не создаёт задачу загрузки маппинга и оставляет `tracer_mapping_uuid`
нулевым.

## Окружение

| Компонент | Версия |
|---|---|
| Android Gradle Plugin | 9.4.1 |
| Gradle | 9.8.2 (также проверено на 9.6.0) |
| Kotlin | 2.4.21 |
| Tracer Gradle Plugin | 1.6.0 |
| compileSdk / targetSdk | 37 |
| JDK | 21 (launcher), daemon по критерию Java 25 |

Проблема также воспроизводится с Tracer 1.4.0.

## Конфигурация

`app/build.gradle.kts`:

```kotlin
android {
    buildTypes {
        release {
            optimization {
                enable = true
            }
        }
    }
    buildFeatures {
        resValues = true
    }
}

tracer {
    create("defaultConfig") {
        appToken = "TEST_APP_TOKEN_0000000000000000000000000"
        pluginToken = "TEST_PLUGIN_TOKEN_0000000000000000000000"
        uploadMapping = true
    }
    create("debug") {
        isDisabled = true
    }
}

dependencies {
    implementation(platform("ru.ok.tracer:tracer-platform:1.6.0"))
    implementation("ru.ok.tracer:tracer-crash-report")
}
```

Токены фиктивные: задача загрузки не создаётся, поэтому ничего не отправляется в Tracer.
В `TracerTestApp` подключён `CrashReportConfiguration` со сбором ANR.

## Воспроизведение

```bash
./gradlew clean :app:bundleRelease --no-configuration-cache --info
```

## Ожидаемое поведение

- В Gradle появляется задача загрузки маппинга (`UploadMappingTask`).
- В сгенерированных ресурсах `tracer_mapping_uuid` содержит ненулевой UUID.
- Создаётся каталог `app/build/tracer` с данными для загрузки.

## Фактическое поведение

Проверено на Gradle 9.8.2/9.6.0 и Kotlin 2.4.21/2.2.10 — результат одинаковый.

- Задачи загрузки маппинга нет.
- `app/build/generated/res/resValues/release/values/gradleResValues.xml`:

```xml
<string name="tracer_mapping_uuid" translatable="false">00000000-0000-0000-0000-000000000000</string>
```

- Каталог `app/build/tracer` не создаётся.
- При этом R8 работает и маппинг генерируется:
  `app/build/outputs/mapping/release/mapping.txt` (~15 МБ).

## Диагностика

`probe.init.gradle` печатает тип Variant, который AGP передаёт в `androidComponents.onVariants`,
и фактическое значение `isMinifyEnabled`:

```bash
./gradlew :app:generateReleaseResValues -I probe.init.gradle --no-configuration-cache
```

Вывод:

```
PROBE debug class=com.android.build.api.component.analytics.AnalyticsEnabledApplicationVariant_Decorated minify=false
PROBE release class=com.android.build.api.component.analytics.AnalyticsEnabledApplicationVariant_Decorated minify=true
```

То есть AGP 9.4.1 передаёт плагинам обёрнутый Variant (`AnalyticsEnabledApplicationVariant`),
но минификация для release действительно включена.

В байткоде `TracerUploadMappingPlugin.hookVariant` признак минификации определяется как
`variant instanceof ApplicationVariantImpl`. Обёртка AGP не является `ApplicationVariantImpl`,
поэтому условие ложно, и плагин пропускает и генерацию UUID, и регистрацию `UploadMappingTask`.
Похоже на несовместимость с обёртками Variant в AGP 9.
