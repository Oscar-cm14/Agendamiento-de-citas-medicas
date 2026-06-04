# Cómo vender nuestro software como emprendedores
## Guía para presentar el Sistema de Agendamiento de Citas Médicas ante un cliente
**Red de Servicios Médicos de Piedrazul — Popayán**
*Universidad del Cauca — Ingeniería de Software III — 2026.1*

---

## Introducción

La Red de Servicios Médicos de Piedrazul, ubicada en el kilómetro 5 vía al Huila en Popayán, es una organización sin ánimo de lucro que atiende diariamente a numerosos pacientes en especialidades de medicina alternativa: terapia neural, quiropraxia y fisioterapia. Este documento orienta al equipo de desarrollo en la presentación profesional y comercial del sistema ante el cliente, siguiendo los principios de una propuesta de valor efectiva.

---

## 1. Empezamos hablando del problema, no de la tecnología

**¿Quién tiene el problema?**

Los médicos y funcionarios de Piedrazul dedican sus tardes (2:00 p.m. a 5:00 p.m.) a recibir llamadas y mensajes de WhatsApp para agendar citas una por una en un software de escritorio. Con el crecimiento del centro, este proceso se ha vuelto insostenible.

**¿Qué pierde actualmente Piedrazul por no resolverlo?**

- Los médicos pierden aproximadamente 3 horas diarias agendando citas manualmente en lugar de atender pacientes.
- El sistema de escritorio no permite que los pacientes agenden sus propias citas desde cualquier lugar.
- No existe control de acceso ni auditoría: cualquier persona puede modificar información sensible.
- La arquitectura monolítica impide escalar o agregar nuevas funcionalidades sin riesgo de romper el sistema.
- Las citas duplicadas y los errores humanos generan conflictos en la agenda diaria.

**¿Por qué es importante resolverlo ahora?**

Piedrazul ya supera los 50 pacientes diarios por médico. Seguir con el modelo actual significa que cada nuevo paciente agrega carga administrativa al personal clínico, reduciendo la calidad de atención y la capacidad de crecimiento del centro.

---

## 2. Hablamos en términos de beneficios, no de tecnología

En lugar de decir:

> *"Usamos Spring Boot con arquitectura de microservicios, Angular 17, H2 y Keycloak."*

Decimos:

- Los pacientes agendan sus propias citas desde el celular o computador en menos de 2 minutos, sin llamar.
- Los médicos eliminan el tiempo de agendamiento en las tardes: el sistema lo hace automáticamente.
- El administrador controla quién accede a qué información, evitando fugas de datos clínicos.
- Las citas se exportan en un clic a CSV para impresión o planificación diaria.
- Los reportes estadísticos muestran cuántas citas tuvo cada médico por mes, por especialidad y por período.
- El historial clínico de cada paciente queda registrado, protegido y disponible solo para personal autorizado.

---

## 3. Demostración real del sistema

Mostramos el sistema funcionando, no solo diapositivas. La demostración sigue este flujo:

1. **Situación inicial:** El agendador recibe un WhatsApp. Mostramos el proceso manual antiguo para que el cliente recuerde el problema.
2. **Cómo se usa:** El paciente ingresa al portal web, selecciona especialidad, médico y horario disponible en menos de 2 minutos, sin ayuda de nadie.
3. **Resultado:** La cita aparece registrada en el sistema, el médico la ve en su agenda y el paciente tiene su confirmación.

También demostramos: agendamiento manual por el agendador, reagendamiento, exportación CSV, registro de historia clínica y reporte estadístico por mes y médico.

---

## 4. Retorno de inversión (ROI)

**¿Qué ahorra Piedrazul con nuestro sistema?**

- **Tiempo médico liberado:** ~3 horas/día × 2 médicos = 6 horas diarias que se redirigen a atención de pacientes.
- **Reducción de errores:** las citas duplicadas y los conflictos de horario se eliminan con validación automática.
- **Sin papel:** la exportación digital reemplaza los listados impresos diarios.

**¿Qué riesgos disminuye?**

- Accesos no autorizados a historias clínicas: control de roles con Keycloak.
- Pérdida de datos: arquitectura en la nube con respaldo automático (Render).
- Crecimiento incontrolado: la arquitectura de módulos permite agregar nuevas especialidades o sedes sin rediseñar el sistema.

---

## 5. Conocemos a nuestro cliente: Piedrazul

**¿A quién le presentamos?**

A una organización sin ánimo de lucro del sector salud, con personal médico y administrativo de bajo perfil técnico, que valora la simplicidad, la confiabilidad y el bajo costo operativo.

**¿Qué le preocupa a Piedrazul?**

- Que el sistema sea difícil de usar para el personal y los pacientes.
- Que los datos clínicos de los pacientes queden expuestos.
- Que el sistema falle durante la jornada de atención.
- Que el costo de mantenimiento sea elevado.

**¿Qué valoraría más?**

- Que un paciente sin experiencia técnica pueda agendar su cita solo.
- Que los médicos vean su agenda actualizada en tiempo real.
- Que el administrador tenga control total sobre usuarios y permisos.

---

## 6. Nuestra propuesta de valor

> **"Nuestro sistema ayuda a la Red de Servicios Médicos de Piedrazul a eliminar el agendamiento manual de citas y liberar el tiempo de sus médicos, mediante una plataforma web donde los pacientes reservan su propia cita en minutos, con control de acceso, auditoría y reportes integrados."**

---

## 7. ¿Por qué elegir nuestro sistema?

Un cliente puede preguntar: ¿por qué no seguir con el software de escritorio o usar una solución genérica?

- **Diseñado específicamente para Piedrazul:** respeta sus flujos, especialidades y horarios reales.
- **Autogestión del paciente:** nadie más tiene que agendarle la cita; el sistema lo guía en el proceso.
- **Seguridad clínica real:** Keycloak gestiona la autenticación y los roles; las historias clínicas solo las ven los autorizados.
- **Disponible desde cualquier dispositivo:** celular, tablet o computador, sin instalar nada.
- **Fácil de mantener y evolucionar:** arquitectura modular que permite agregar nuevas sedes o especialidades.
- **Trazabilidad total:** cada acción queda registrada con usuario, fecha y hora.

---

## 8. Preguntas difíciles que anticipamos

### ¿Cuánto cuesta?
El sistema es un desarrollo académico de la Universidad del Cauca. El costo de operación actual es mínimo: el backend está desplegado en Render y el frontend en Vercel (ambos con planes gratuitos). Para producción escalable, estimamos costos de hosting desde $20 USD/mes en la nube.

### ¿Qué pasa si el sistema falla?
El sistema tiene una disponibilidad objetivo del 99%. En caso de fallo, los registros persisten en la base de datos y el servicio se recupera automáticamente. El agendamiento manual por el personal siempre está disponible como respaldo.

### ¿Quién da soporte?
El equipo de desarrollo de la Universidad del Cauca durante el período académico. El código fuente está en GitHub y la documentación técnica está incluida en el repositorio, lo que permite que cualquier desarrollador tome el proyecto.

### ¿Cómo protegen los datos de los pacientes?
Las contraseñas se almacenan cifradas en Keycloak (nunca en texto plano). El acceso a historias clínicas está restringido por rol. Toda consulta o modificación queda registrada en el sistema de auditoría. Las comunicaciones viajan por HTTPS.

### ¿Cuánto tarda la implementación?
El sistema ya está funcional y desplegado. La migración de datos del software de escritorio y la capacitación al personal tomarían aproximadamente una semana.

---

## 9. Cómo nos presentamos

- Llegamos puntualmente y con el sistema desplegado y funcionando antes de iniciar.
- Usamos lenguaje sencillo y evitamos tecnicismos ante el personal médico y administrativo.
- Las diapositivas son limpias: máximo 5 líneas por slide, sin leer el texto en voz alta.
- Traemos un escenario de demo preparado con datos reales de Piedrazul (especialidades, médicos, horarios).
- Asignamos roles en el equipo: un presentador, uno que maneja la demo en vivo, uno que responde preguntas técnicas.

---

## 10. Cómo cerramos la presentación

No decimos:

> *"Eso era todo, muchas gracias."*

Decimos:

> *"Nuestro sistema permite que Piedrazul elimine el agendamiento manual, libere el tiempo de sus médicos y brinde a sus pacientes la comodidad de reservar su cita desde casa en minutos. Los datos clínicos están protegidos, la agenda siempre está actualizada y el sistema está listo para crecer con ustedes. Estamos disponibles para implementarlo y acompañarlos en el proceso."*

---

*Sistema de Agendamiento de Citas Médicas — Red de Servicios Médicos de Piedrazul*
*Universidad del Cauca — Ingeniería de Software III — 2026.1*
