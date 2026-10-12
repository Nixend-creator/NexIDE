# IntelliJ Platform Coding Guidelines — IDE Analog Development Standard

**Версія:** 1.0  
**Дата:** вересень 2026  
**Мови:** Kotlin (пріоритет), Java  
**Відступи:** 2 пробіли  
**Максимальна довжина рядка:** 140 символів  
**Ліцензія заголовків:** Apache 2.0  
**Префікси:** `my` (instance), `our` (static/class)

Цей документ нормалізує правила із завантаженого файлу користувача `IntelliJ Platform Coding Guidelines`, 3 сторінки. Не є офіційним документом JetBrains і не додає непідтверджених вимог до API.

## 1. Загальні архітектурні принципи

- Дотримуйся класичних принципів Java/Kotlin: Effective Java (Joshua Bloch) та ідіоматичного Kotlin.
- Нові файли — тільки Kotlin, крім випадків, коли потрібно модифікувати наявний Java-код.
- Використовуй data classes, sealed classes, extension functions і null-safety.
- Віддавай перевагу immutable-об'єктам, де можливо, та програмуванню через інтерфейси.
- Експериментальні API позначай `@ApiStatus.Experimental`.

## 2. Форматування коду

- Відступи: 2 пробіли; tabs заборонені.
- Максимальна довжина рядка: 140 символів.
- `else`, `catch`, `finally` — на новому рядку.
- Префікс `my` для instance-змінних.
- Префікс `our` для static/class-змінних.
- Кожен новий файл має містити copyright із ліцензією Apache 2.0 та ім'ям контриб'ютора.
- Використовуй `.editorconfig` і shared code style проєкту.
- Перед комітом завжди роби Reformat Code (Ctrl+Alt+L).

## 3. Тестування

- Функціональні тести мають покривати більшість функціоналу.
- При зміні наявної області обов'язково запускай відповідні тести.
- Для нових функцій і багфіксів пиши нові тести.
- Не допускай регресій.

## 4. Інспекції та якість коду

- Код не повинен містити попереджень із shared Inspection Profile проєкту.
- Не змішуй в одному PR/patch функціональні зміни, виправлення форматування та виправлення попереджень.

## 5. Документація

- Для всіх нових OpenAPI (інтерфейсів, класів, методів, extension points) обов'язковий Javadoc/KDoc із описом параметрів і призначення.
- Для внутрішнього коду документація бажана, але не обов'язкова.

## 6. Коміти та Pull Request

- Перед відправленням переглянь усі зміни, видали незавершений код і TODO.
- Не включай зміни до `.idea/*`, `codeStyleSettings.xml` тощо, якщо це не частина виправлення.
- Уникай rename/move, якщо це не потрібно; зворотна сумісність критична.
- Ідеальний PR — один коміт із чітким повідомленням.
- Локально можна комітити часто, але перед push — squash.

## 7. Обробка помилок

У `catch` для `Throwable` або `Exception` спочатку викликай `rethrowControlFlowException`, потім виконуй логування/обробку:

```kotlin
try {
  doWork()
}
catch (e: Throwable) {
  rethrowControlFlowException(e)
  LOG.error(e)
}
```

- Не перехоплюй `CancellationException` вручну.
- Використовуй `com.intellij.diagnostic.rethrowControlFlowException`.

## 8. Kotlin build scripts

- Використовуй `@JvmField` для internal data class fields.
- Надавай перевагу `.put()` / `.get()` замість оператора `[]`.
- Використовуй явні `HashMap` / `HashSet` / `LinkedHashMap` замість `mutableMapOf()` / `mutableSetOf()`.

## 9. Інші рекомендації

- Не ламай зворотну сумісність без крайньої необхідності.
- Будуй і тестуй проти останньої версії `intellij-community`.
- Дотримуйся shared Inspection Profile.

## Джерело

Завантажений користувачем документ `IntelliJ Platform Coding Guidelines — IDE Analog Development Standard`, версія 1.0, вересень 2026. Вихідний документ вказує, що він базується на рекомендаціях JetBrains.
