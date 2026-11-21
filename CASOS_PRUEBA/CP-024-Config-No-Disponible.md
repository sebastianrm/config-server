# CP-024 — Falla de configuración por Config Server no disponible

- **Tipo:** Error
- **Precondiciones:**
  - Config Server caído o inaccesible.

- **Pasos:**
  1. Iniciar servicio dependiente o consultar configuración.
  2. Validar manejo del fallo.

- **Postcondiciones:**
  - El servicio reporta error controlado o fallback según diseño.

- **Errores esperados:**
  - Error de conexión / indisponibilidad.
