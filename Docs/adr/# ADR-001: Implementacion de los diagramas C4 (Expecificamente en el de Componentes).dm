# ADR-001: Implementacion de los diagramas C4 (Expecificamente en el de Componentes)
- Fecha : [2026-04-08]
- Estado : [Desarrollo]
## Contexto del Problema:
>  Revisando el diagrama de componentes encontramos en el modulo del dominio falto implementar el componente "Enum Estado de Citas".
## Decision:
>  Decidimos implementar estos estados para ayudarnos en el manejo de las citas en la parte del "Modulo de Citas" en el cual por medio de una clase Enum facilitaremos su implementacion en codigo.
##Consecuensias:
- ##Positivas:
> Ahora se puede ver de manera mas sencilla el estado de las citas , ya sea aprovado o cancelado
- ##Negativas :
> - Tener que modificar el codigo costantemente cuando aparezcan nuevos estados como por ejemplo : citas pendientes , reprogramadas o en proceso.
  - Problemas de compatibilidad cuando se inente usar en otras clases o cuando se haga uso de esa clase.
