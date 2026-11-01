<!-- calidad:inicio -->
![Calidad](https://img.shields.io/badge/Calidad-sin%20c%C3%B3digo-lightgrey) ![Aprobar](https://img.shields.io/badge/Aprobar-no-red) ![Destacar](https://img.shields.io/badge/Destacar-no-lightgrey)

**Calidad (7 dimensiones):** sin código en `main` (no evaluable).
<!-- calidad:fin -->

# turnos-reservas-service

Servicio de turnos y reservas del proyecto integrador de Programación 2
(Universidad de Mendoza).

## Análisis de calidad: SonarCloud

[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=manudiiez-um-programacion_turnos-reservas-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=manudiiez-um-programacion_turnos-reservas-service)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=manudiiez-um-programacion_turnos-reservas-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=manudiiez-um-programacion_turnos-reservas-service)

Cada pull request y cada push se analiza en
[SonarCloud](https://sonarcloud.io/summary/new_code?id=manudiiez-um-programacion_turnos-reservas-service):
bugs potenciales, code smells, duplicación y problemas de seguridad, más la
cobertura que mide JaCoCo durante las pruebas.

Lo dispara **GitHub Actions**, al final del job de `build.yml` y después de que
`./mvnw verify` generó el reporte de cobertura. El *automatic analysis* de
SonarCloud está desactivado a propósito: tener las dos formas de análisis
compitiendo produce resultados contradictorios.

Qué esperar al leerlo:

- **El quality gate es informativo.** No se usa `sonar.qualitygate.wait`, así que
  un gate rojo no rompe el build ni bloquea el merge. El enunciado no exige un
  porcentaje de cobertura, y el ruleset de las ramas protegidas lo administra la
  cátedra: este repo no toca su configuración.
- **La cobertura solo se puede leer en `main`.** En otras ramas el análisis se
  procesa igual, pero la organización no tiene permitido exponer esos datos
  (limitación del plan). El número aparece cuando el código llega a `main`.
- **El código generado por Lombok no cuenta.** Lo resuelve `lombok.config`, no una
  exclusión de Sonar: Lombok marca lo que genera con `@lombok.Generated` y JaCoCo
  ignora por convención todo lo anotado "Generated".
- No hay exclusiones de análisis declaradas, y el quality gate es el "Sonar way"
  por defecto, sin reglas propias.

### Configurarlo en un clon propio

El análisis necesita un token que **no** está en el repositorio. Sin él, el paso
se omite con un aviso y el build sigue en verde, así que el proyecto se clona y
compila sin configurar nada.

Para habilitarlo hace falta ser dueño del repositorio y de la organización en
SonarCloud:

1. Entrar a [sonarcloud.io](https://sonarcloud.io) con la cuenta de GitHub y
   crear o usar la organización `manudiiez-um-programacion`.
2. Agregar el repositorio eligiendo **"With GitHub Actions"** como método de
   análisis (no *automatic analysis*). El project key queda
   `manudiiez-um-programacion_turnos-reservas-service`, igual que en el `pom.xml`.
3. Generar el token en *My Account → Security → Generate Token*.
4. Guardarlo como secret de repositorio:

   ```bash
   gh secret set SONAR_TOKEN
   ```

El token es el único secreto que usa la integración continua de este repo: las
pruebas no hablan con el servicio central ni necesitan PostgreSQL.
