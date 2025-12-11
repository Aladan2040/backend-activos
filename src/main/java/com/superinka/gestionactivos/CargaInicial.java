package com.superinka.gestionactivos;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CargaInicial {

    private final ActivoRepository repository;

    public CargaInicial(ActivoRepository repository) {
        this.repository = repository;
    }

    @Bean
    CommandLineRunner iniciarCarga() {
        return args -> {
            long countBD = repository.count();
            if (countBD > 0) {
                System.out.println("ℹ️ BD con datos (" + countBD + "). Omitiendo carga.");
                return;
            }

            System.out.println("🚀 INICIANDO CARGA (MODO STREAMING - ULTRA BAJO CONSUMO)...");

            ClassPathResource resource = new ClassPathResource("Depreciacion.csv");

            // 1. Lectura y Reconstrucción (Esto sí requiere cargar el texto, pero es "barato" en RAM)
            List<String> lineasReconstruidas;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                List<String> lineasCrudas = br.lines().collect(Collectors.toList());
                lineasReconstruidas = reconstruirRegistros(lineasCrudas);
            }

            // Liberamos la lista cruda inmediatamente (Hint al GC)
            System.out.println("🧩 Registros Lógicos: " + (lineasReconstruidas.size() - 1));

            String csvCompleto = String.join("\n", lineasReconstruidas);
            String csvSanitizado = csvCompleto.replace("\"", "");

            // Liberamos variables grandes
            lineasReconstruidas = null;
            csvCompleto = null;
            System.gc(); // Sugerir limpieza antes de empezar lo pesado

            // 2. PROCESAMIENTO POR STREAM (Iterador)
            try (StringReader sr = new StringReader(csvSanitizado)) {

                CsvToBean<Activo> csvToBean = new CsvToBeanBuilder<Activo>(sr)
                        .withType(Activo.class)
                        .withSeparator(';')
                        .withQuoteChar('\0')
                        .withIgnoreLeadingWhiteSpace(true)
                        .withIgnoreQuotations(true)
                        .withThrowExceptions(false)
                        .build();

                // CAMBIO CLAVE: Usamos iterator() en lugar de parse()
                // Esto lee registro por registro, sin cargar todo en memoria.
                Iterator<Activo> iterator = csvToBean.iterator();

                List<Activo> lote = new ArrayList<>();
                int batchSize = 500; // Lote pequeño
                int procesados = 0;

                while (iterator.hasNext()) {
                    Activo activo = iterator.next();

                    if (activo.getCodigo() != null && !activo.getCodigo().isEmpty()) {
                        lote.add(activo);
                    }

                    // Si el lote se llena, guardamos y limpiamos
                    if (lote.size() >= batchSize) {
                        guardarLoteTransaccional(lote);
                        procesados += lote.size();
                        System.out.print("."); // Feedback visual
                        lote.clear(); // ¡Vaciamos la lista para liberar RAM!
                        System.gc();  // Sugerir limpieza al GC
                    }
                }

                // Guardar el último lote si quedó algo pendiente
                if (!lote.isEmpty()) {
                    guardarLoteTransaccional(lote);
                    procesados += lote.size();
                }

                System.out.println("\n✅ CARGA FINALIZADA. Procesados: " + procesados);
                System.out.println("   - En BD: " + repository.count());
            }
        };
    }

    @Transactional
    public void guardarLoteTransaccional(List<Activo> activos) {
        repository.saveAll(activos);
        repository.flush(); // Forzar la escritura a BD inmediatamente
    }

    private List<String> reconstruirRegistros(List<String> lineasCrudas) {
        List<String> lineasReconstruidas = new ArrayList<>();
        StringBuilder registroActual = new StringBuilder();
        String patronInicioRegistro = "^\\s*\"?(\\d+|#)\"?\\s*;.*";

        for (String linea : lineasCrudas) {
            if (linea.trim().isEmpty()) continue;

            if (linea.matches(patronInicioRegistro)) {
                if (registroActual.length() > 0) {
                    lineasReconstruidas.add(registroActual.toString());
                }
                registroActual = new StringBuilder(linea);
            } else {
                registroActual.append(" ").append(linea.trim());
            }
        }
        if (registroActual.length() > 0) {
            lineasReconstruidas.add(registroActual.toString());
        }
        return lineasReconstruidas;
    }
}
