---
name: nixend-code-audit
description: Проводить аудит відповідності стандарту на основі diff.
---

# NIXEND Code Audit Skill

Аудитуй саме змінені ділянки та необхідний навколишній контекст.

Перевір:
- Kotlin-first policy та виправданість змін Java;
- 2 пробіли, 140 символів, перенесення `else`/`catch`/`finally`;
- `my` / `our` prefixes;
- Apache 2.0 header і contributor name у нових файлах;
- `.editorconfig`, shared code style, Reformat Code;
- тести для нових функцій і багфіксів;
- shared Inspection Profile warnings;
- KDoc/Javadoc для нових OpenAPI;
- `rethrowControlFlowException` першим у catch для Throwable/Exception;
- відсутність ручного catch `CancellationException`;
- зворотну сумісність, непотрібні rename/move;
- відсутність незавершеного коду/TODO та сторонніх `.idea/*` змін;
- Kotlin build-script правила.

Формат результату:
1. **Blocker** — порушення, що заважають прийняти зміну.
2. **Major** — суттєвий ризик/порушення.
3. **Minor** — незначна невідповідність.
4. **Перевірки** — лише фактично виконані.
5. **Висновок** — відповідає / відповідає з зауваженнями / не відповідає.

Не вигадуй дефекти. Для кожного висновку наведи файл і рядок, якщо вони доступні.
