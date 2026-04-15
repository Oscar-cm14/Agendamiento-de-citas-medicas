# ADR-003: Resolución de conflictos de persistencia en el mapeo de herencia

* **Fecha:** [2026-04-14]
* **Estado:** [Terminado]

## Contexto del Problema:

> Durante las pruebas de integración del backend, se detectó un fallo crítico (Error 500) al intentar registrar un nuevo Médico. 
  El sistema arrojaba la excepción duplicate key value violates unique constraint "person_pkey". Esto sucedía porque las entidades Person, Doctor y User no 
  tenían sincronizada la generación de sus identificadores, provocando que Hibernate intentara insertar IDs que ya existían o que no correspondían a la secuencia 
  correcta en la base de datos PostgreSQL.

## Decisión:

> Se decidió refactorizar el mapeo de las entidades utilizando la anotación @MapsId de JPA y una estrategia de herencia compartida. En lugar de que cada tabla 
  genere su propio ID, se estableció que la tabla Person es la dueña de la identidad, y tanto Doctor como User deben heredar y reutilizar ese mismo ID como su 
  llave primaria (PK) y llave foránea (FK) simultáneamente.

## Consecuencias:

### Positivas:

> * Se garantiza que un registro en la tabla doctors siempre tenga un correspondiente exacto en la tabla person con el mismo ID, eliminando errores de huérfanos.
> * Facilita los JOIN entre tablas, ya que la relación es 1:1 directa por ID, lo que mejora el rendimiento de las búsquedas de médicos y usuarios.
> * Se resolvió el conflicto de "llave duplicada", permitiendo que el flujo de registro sea continuo y exitoso (como se evidenció en los últimos logs de Hibernate).

### Negativas:

> * Las entidades ahora están estrictamente ligadas por sus identificadores; no se puede crear un Doctor sin antes haber persistido correctamente una Person
> * Requiere que el desarrollador gestione manualmente la asignación del objeto Person dentro de las otras entidades antes de guardar, para que JPA pueda mapear el ID correctamente.
> * Cambiar la estrategia de identificación en el futuro requeriría una migración de base de datos compleja, ya que todas las relaciones dependen de este ID compartido.
