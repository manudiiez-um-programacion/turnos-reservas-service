# Imagen del servicio de turnos y reservas.
#
# Multi-stage: la etapa `build` necesita el JDK y Maven, pero nada de eso
# tiene que viajar a la imagen final, que lleva solo un JRE y el jar y corre
# con un usuario sin privilegios.
#
# Mismo esquema que la imagen del servicio de catalogo, a proposito: los dos
# backends se construyen y se despliegan igual.

# --- Etapa 1: compilacion -----------------------------------------------
FROM eclipse-temurin:25-jdk-alpine AS build

# El wrapper esta configurado con distributionType=only-script: baja Maven
# 3.9.15 en el primer uso. Eso necesita curl para descargar y unzip para
# descomprimir, que la imagen alpine no trae.
RUN apk add --no-cache curl unzip

WORKDIR /build

# Primero solo el wrapper y el pom. Mientras las dependencias no cambien,
# Docker reutiliza esta capa y no vuelve a bajar el repositorio entero en
# cada build. Si se copiara src/ antes, cualquier cambio en el codigo
# invalidaria la cache de dependencias.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp dependency:go-offline

# Ahora si el codigo. lombok.config entra porque el procesador de
# anotaciones lo lee en tiempo de compilacion: sin el, Lombok no marca su
# codigo generado y la imagen compilaria distinto que en local.
COPY lombok.config ./
COPY src/ src/

# Las pruebas no corren en el build de la imagen: para eso esta `./mvnw
# verify` en CI (#8). Construir la imagen no tiene que depender de que haya
# una base ni de cuanto tarden las pruebas.
RUN ./mvnw -B -ntp clean package -DskipTests

# --- Etapa 2: ejecucion -------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime

# Usuario sin privilegios: si alguien logra ejecutar codigo dentro del
# contenedor, no es root. El directorio de trabajo queda de root y el
# proceso solo lee de ahi, asi que no puede sobrescribir su propio jar.
RUN addgroup --system --gid 1001 turnos \
 && adduser --system --uid 1001 --ingroup turnos --no-create-home turnos

WORKDIR /app

# spring-boot-maven-plugin deja el jar ejecutable y, al lado, el original
# sin empaquetar (.jar.original), que el comodin no toma.
COPY --from=build /build/target/*.jar app.jar

USER turnos:turnos

# 8081, no 8080: el servicio de catalogo ya ocupa el 8080 y los dos
# backends tienen que poder correr a la vez.
EXPOSE 8081

# Sin shell intermedia: el JVM es el PID 1 y recibe directo el SIGTERM de
# `docker compose down`, asi que apaga ordenado en lugar de a los 10s.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
