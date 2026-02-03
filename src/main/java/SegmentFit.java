/*
 * SegmentFit.java
 *
 * Herramienta para:
 *  - Leer un archivo .FIT de Garmin
 *  - Detectar un segmento definido por GPS (punto inicio / punto fin)
 *  - Extraer solo ese tramo
 *  - Generar un nuevo archivo .FIT válido con los records del segmento
 *
 * Requisitos:
 *  - Java 8+
 *  - fit-java-sdk (probado con 21.188.0)
 *
 * Uso típico:
 *   java -cp .:fit-21.188.0.jar SegmentFit actividad.fit --start=lat,lon --end=lat,lon
 *
 * Autor: Daniel Sappa
 * Copyright (c) 2026 Daniel Sappa
 *
 * Licenciado bajo la Licencia MIT.
 * Ver el archivo LICENSE para más detalles.
 */

import com.garmin.fit.*;

import java.io.FileInputStream;
import java.io.File;
import java.util.*;
import java.time.Instant;

public class SegmentFit {

    /**
     * Conversión de semicircles FIT a grados.
     *
     * En FIT, lat/lon se almacenan como enteros en "semicircles".
     * Fórmula oficial:
     *   grados = semicircles * (180 / 2^31)
     */
    static final double DEG = 180.0 / Math.pow(2, 31);

    /**
     * Punto de track simplificado.
     * Contiene únicamente los campos que nos interesan
     * para reconstruir el segmento.
     */
    static class Point {
        DateTime ts;        // Timestamp FIT (tiempo absoluto)
        double lat;         // Latitud en grados
        double lon;         // Longitud en grados
        Short hr;           // Frecuencia cardíaca
        Float speed;        // Velocidad (m/s)
        Short cadence;      // Cadencia (rpm)
        Float altitude;     // Altitud (m)
    }

    /**
     * Distancia Haversine entre dos coordenadas GPS.
     *
     * Devuelve distancia en metros.
     * Se usa para encontrar el punto más cercano a start/end.
     */
    static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0; // Radio medio de la Tierra (m)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Devuelve el índice del punto más cercano a una coordenada dada.
     *
     * Recorre secuencialmente la lista completa de puntos.
     */
    static int nearestIndex(List<Point> pts, double lat, double lon) {
        int idx = 0;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < pts.size(); i++) {
            Point p = pts.get(i);
            double d = haversine(p.lat, p.lon, lat, lon);
            if (d < best) {
                best = d;
                idx = i;
            }
        }
        return idx;
    }

    public static void main(String[] args) throws Exception {

        /**
         * Validación básica de argumentos.
         */
        if (args.length < 3) {
            System.err.println("Uso:");
            System.err.println("java SegmentFit archivo.fit --start=lat,lon --end=lat,lon");
            System.exit(1);
        }

        String fitFile = args[0];
        double startLat = 0, startLon = 0, endLat = 0, endLon = 0;

        /**
         * Parseo de argumentos --start y --end
         */
        for (String a : args) {
            if (a.startsWith("--start=")) {
                String[] p = a.substring(8).split(",");
                startLat = Double.parseDouble(p[0]);
                startLon = Double.parseDouble(p[1]);
            }
            if (a.startsWith("--end=")) {
                String[] p = a.substring(6).split(",");
                endLat = Double.parseDouble(p[0]);
                endLon = Double.parseDouble(p[1]);
            }
        }

        /**
         * Lista donde se almacenarán todos los puntos del FIT original.
         */
        List<Point> points = new ArrayList<>();

        /**
         * Decoder FIT y broadcaster de mensajes.
         * El broadcaster permite registrar listeners por tipo de mensaje.
         */
        Decode decode = new Decode();
        MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

        /**
         * Listener para mensajes RecordMesg (trackpoints).
         * Se ejecuta por cada punto del archivo FIT.
         */
        broadcaster.addListener((RecordMesg m) -> {
            // Ignorar puntos sin posición GPS
            if (m.getPositionLat() == null || m.getPositionLong() == null)
                return;

            Point p = new Point();
            p.ts = m.getTimestamp();
            p.lat = m.getPositionLat() * DEG;
            p.lon = m.getPositionLong() * DEG;
            p.hr = m.getHeartRate();
            p.speed = m.getSpeed();
            p.cadence = m.getCadence();
            p.altitude = m.getAltitude();
            points.add(p);
        });

        /**
         * Lectura del archivo FIT original.
         */
        FileInputStream in = new FileInputStream(fitFile);
        decode.read(in, broadcaster);
        in.close();

        if (points.size() < 2) {
            throw new RuntimeException("No hay puntos suficientes");
        }

        /**
         * Encontrar índices de inicio y fin más cercanos
         * a las coordenadas indicadas.
         */
        int i0 = nearestIndex(points, startLat, startLon);
        int i1 = nearestIndex(points, endLat, endLon);

        // Asegurar orden correcto
        if (i0 > i1) {
            int tmp = i0; i0 = i1; i1 = tmp;
        }

        /**
         * Sublista que representa el segmento seleccionado.
         */
        List<Point> seg = points.subList(i0, i1 + 1);

        /**
         * Creación del nuevo archivo FIT.
         */
        String out = fitFile.replace(".fit", "_segmento.fit");
        FileEncoder encoder = new FileEncoder(new File(out));

        /**
         * Mensaje FileId obligatorio.
         */
        FileIdMesg fileId = new FileIdMesg();
        fileId.setType(com.garmin.fit.File.ACTIVITY);
        fileId.setManufacturer(Manufacturer.GARMIN);
        fileId.setTimeCreated(new DateTime(Date.from(Instant.now())));
        encoder.write(fileId);

        /**
         * Mensaje Sport.
         */
        SportMesg sport = new SportMesg();
        sport.setSport(Sport.CYCLING);
        encoder.write(sport);

        /**
         * Escritura de cada punto del segmento como RecordMesg.
         */
        for (Point p : seg) {
            RecordMesg r = new RecordMesg();
            r.setTimestamp(p.ts);
            r.setPositionLat((int)(p.lat / DEG));
            r.setPositionLong((int)(p.lon / DEG));
            if (p.hr != null) r.setHeartRate(p.hr);
            if (p.speed != null) r.setSpeed(p.speed);
            if (p.cadence != null) r.setCadence(p.cadence);
            if (p.altitude != null) r.setAltitude(p.altitude);
            encoder.write(r);
        }

        /**
         * Mensaje Session.
         * Marca el inicio del segmento como sesión independiente.
         */
        SessionMesg session = new SessionMesg();
        session.setSport(Sport.CYCLING);
        session.setStartTime(seg.get(0).ts);
        encoder.write(session);

        /**
         * Cierre del encoder (escribe CRC y footer FIT).
         */
        encoder.close();

        System.out.println("FIT generado: " + out);
        System.out.println("Puntos: " + seg.size());
    }
}

