# ADR-002: Acerca de la configuración de seguridad

* **Fecha:** [2026-04-14]
* **Estado:** [Desarrollo]

## Contexto del Problema:

> Revisando la documentación del proyecto, se encontró que no se habló nada acerca de la configuración de seguridad.

## Decisión:

> Se decidió crearle un apartado en el archivo hablando acerca de esta decisión.

## Consecuencias:

### Positivas:

> * Ahora se tiene por escrito cómo se manejará la seguridad en el proyecto, evitando malentendidos más adelante.
> * Facilita que nuevos desarrolladores entiendan rápido qué medidas de seguridad se implementaron y por qué.
> * Ayuda a identificar posibles riesgos antes de que ocurran, ya que al tenerlo documentado se puede revisar con calma.

### Negativas:

> * Se requerirá modificar el archivo cada vez que se añadan nuevas reglas de seguridad o se actualicen las existentes.
> * Posibles problemas de compatibilidad si la configuración de seguridad documentada no coincide con lo que realmente se implementó en el código.
> * Puede que el apartado quede muy extenso si se detalla todo, lo cual dificultaría su lectura rápida.
