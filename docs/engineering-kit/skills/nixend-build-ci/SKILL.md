---
name: nixend-build-ci
description: Перевіряє збірку, Kotlin build scripts і CI-зміни.
---

# NIXEND Build and CI Skill

1. Визнач наявний build system і wrapper за файлами репозиторію; не припускай Gradle/Maven без перевірки.
2. Зберігай локальні build conventions і не змінюй версії плагінів/залежностей без причини.
3. У Kotlin build scripts дотримуйся стандарту:
   - `@JvmField` для internal data class fields;
   - віддавай перевагу `.put()` / `.get()` замість `[]`;
   - явні `HashMap`, `HashSet`, `LinkedHashMap` замість `mutableMapOf()` / `mutableSetOf()`.
4. Не додавай у patch незв'язані build-реформи.
5. Запускай доступні перевірки wrapper/build і повідомляй точні результати.
6. Стандарт вимагає будувати й тестувати проти останньої версії `intellij-community`; якщо це неможливо, зазнач причину.
