# ADR-001: Implementación de los diagramas C4 (específicamente en el de Componentes)

* **Fecha:** [2026-04-08]
* **Estado:** [Desarrollo]

## Contexto del Problema:

> Revisando el diagrama de componentes, se encontró que en el módulo del dominio faltó implementar el componente **"Enum Estado de Citas"**.

## Decisión:

> Se decidió implementar estos estados para apoyar el manejo de las citas en el **"Módulo de Citas"**, en el cual, por medio de una clase Enum, se facilitará su implementación en código.

## Consecuencias:

### Positivas:

> Ahora se puede visualizar de manera más sencilla el estado de las citas, ya sea aprobado o cancelado.

### Negativas:

> * Se requerirá modificar el código constantemente cuando aparezcan nuevos estados, como por ejemplo: citas pendientes, reprogramadas o en proceso.
> * Posibles problemas de compatibilidad al intentar usar esta clase en otras clases o al hacer uso de la misma.
