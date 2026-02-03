# SegmentFit

Herramienta en Java para procesar archivos **Garmin .FIT** y extraer un segmento específico definido por coordenadas GPS (punto de inicio y punto de fin), generando un nuevo archivo `.FIT` válido con los records correspondientes a ese tramo.

## 🚴 Características

- Lee archivos `.FIT` generados por dispositivos Garmin
- Detecta un segmento por:
  - Punto inicio → punto fin (coordenadas GPS)
- Extrae únicamente los records del segmento
- Genera un nuevo archivo `.FIT` válido

## 📦 Requisitos

- Java 8 o superior
- Garmin **FIT SDK (Java)**
  - Probado con versión `21.188.0`

## ▶️ Uso

### Compilar con Maven

```bash
mvn clean package
```

El comando genera el artefacto en:

```text
target/segment-fit-1.0.0.jar
```

### Ejecutar

```bash
java -jar target/SegmentFit.jar actividad.fit \
  --start=lat,lon \
  --end=lat,lon
```

Ejemplo:

```bash
java -jar target/SegmentFit.jar salida.fit \
  --start=-34.6037,-58.3816 \
  --end=-34.6158,-58.4333
```bash
java -cp .:SegmentFit.jar SegmentFit salida.fit \
  --start=-34.6037,-58.3816 \
  --end=-34.6158,-58.4333
```

## 📥 Cómo obtener el FIT SDK

El **Garmin FIT SDK** no se distribuye con este repositorio y debe descargarse manualmente desde Garmin.

1. Ir a la página oficial del FIT SDK:
   https://developer.garmin.com/fit/overview/

2. Descargar el **FIT SDK (Java)**

3. Extraer el archivo descargado (por ejemplo `fit-21.188.0.zip`)

4. Usar el `.jar` del SDK como dependencia en el proyecto:
   - Si usás Maven, configurarlo en el `pom.xml` como dependencia local
   - O bien agregar el `.jar` al classpath al compilar/ejecutar

> Nota: Garmin y FIT son marcas registradas de Garmin Ltd.

## 📂 Estructura del proyecto

```
.
├── src
│   └── main
│       └── java
│           └── SegmentFit.java
├── LICENSE
├── pom.xml
└── README.md
```

## ⚠️ Notas

- El SDK de Garmin FIT **no se incluye** en este repositorio.
- El archivo `.FIT` generado conserva la estructura válida esperada por Garmin Connect y herramientas compatibles.

## 📜 Licencia

Este proyecto se distribuye bajo la **Licencia MIT**.

Ver el archivo [LICENSE](LICENSE) para más detalles.

---

© 2026 Daniel Sappa

