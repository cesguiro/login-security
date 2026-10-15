---
description: Documento con las reglas y directrices de desarrollo para todos los agentes de IA (Claude, Cursor, Codex, Gemini, etc.) en este proyecto.
alwaysApply: true
---

## 1. Principios Fundamentales

- **Pasos pequeños**: Trabaja siempre en pasos cortos y progresivos. No avances más de un paso a la vez.
- **Cambios incrementales**: Prioriza modificaciones pequeñas y enfocadas frente a cambios grandes y complejos.
- **Claridad en la nomenclatura**: Utiliza nombres claros y descriptivos para variables, funciones y clases.
- **Cuestionar suposiciones**: No dar nada por sentado. Si falta información o un requerimiento es ambiguo, preguntar o contemplar los casos límite.
- **Detección de código duplicado**: Identificar y señalar bloques de código repetidos para promover la reutilización (creación de funciones, bucles, etc.).

## 2. Idioma y Estándares de Redacción

- **Inglés (exclusivamente para el código)**:
    - Nombres de variables, funciones, clases y métodos.
    - Mensajes de commit en Git.
    - Tests (nombres y descripciones de las pruebas).
- **Español (para todo lo demás)**:
    - Comentarios en el código explicativos.
    - Documentación, archivos README y respuestas del agente.
    - Mensajes de error y logs proyectados.

## 3. Estándares Específicos

Para consultar guías detalladas según la parte del proyecto, revisa:

- [Estándares de Backend](./backend-standards.md) - Desarrollo de APIs, bases de datos y buenas prácticas.
- [Estándares de Frontend](./frontend-standards.md) - Arquitectura y componentes de la interfaz.
- [Estándares de Documentación](./documentation-standards.md) - Estructura y mantenimiento de la documentación técnica.

## 4. Habilidades del Proyecto (Skills)

- Las habilidades del proyecto se encuentran en la carpeta `skills`.
- Cuando una solicitud coincida con una habilidad, carga y sigue el archivo `SKILL.md` correspondiente antes de continuar.

## 5. TDD

- **Test-Driven Development (TDD)**: Antes de implementar una nueva funcionalidad, escribe primero los tests que validen su comportamiento esperado.
- **Cobertura de pruebas**: Asegúrate de que cada función y módulo tenga pruebas unitarias y de integración adecuadas.
- **Revisión de tests**: Antes de finalizar cualquier desarrollo, revisa que todos los tests pasen correctamente y que no se introduzcan errores en funcionalidades existentes.
- **Documentación de tests**: Mantén una documentación clara sobre qué se está probando y cómo ejecutar los tests.