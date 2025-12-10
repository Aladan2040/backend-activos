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
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CargaInicial {

    // Necesitamos inyectar el repositorio para guardar los datos
    private final ActivoRepository repository;

    public CargaInicial(ActivoRepository repository) {
        this.repository = repository;
    }

    // El @Bean debe estar activo para que se ejecute la carga al iniciar Render
    @Bean
    @Transactional
    // Usamos transacciones para el guardado por lotes
    CommandLineRunner iniciarCarga() {
        return args -> {
            long countBD = repository.count();
            if (countBD > 0) {
                System.out.println("ℹ️ La BD ya tiene datos (" + countBD + " registros). Omitiendo carga inicial.");
                return;
            }

            System.out.println("🚀 INICIANDO CARGA POR LOTES (MODO MAX. EFICIENCIA DE MEMORIA)...");

            // Apuntamos al archivo REAL con los 35k datos
            ClassPathResource resource = new ClassPathResource("Depreciacion.csv");

            // 1. LEER LÍNEAS CRUDAS Y RECONSTRUIR EN MEMORIA
            List<String> lineasReconstruidas;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                List<String> lineasCrudas = br.lines().collect(Collectors.toList());
                lineasReconstruidas = reconstruirRegistros(lineasCrudas);
            }

            System.out.println("🧩 Registros Lógicos reconstruidos: " + (lineasReconstruidas.size() - 1));

            // 2. LIMPIEZA QUÍMICA Y PREPARACIÓN
            String csvCompleto = String.join("\n", lineasReconstruidas);
            String csvSanitizado = csvCompleto.replace("\"", "");

            // 3. CARGA POR LOTES (Batch Loading)
            // Reducimos el lote para ser conservadores con la memoria heap de Render
            int batchSize = 1000;
            int totalRegistros = lineasReconstruidas.size() - 1;
            int registrosGuardados = 0;

            List<CsvException> excepcionesCapturadas = new ArrayList<>();

            // Usamos un StringReader para leer el CSV reconstruido
            try (StringReader sr = new StringReader(csvSanitizado)) {

                String cabecera = lineasReconstruidas.get(0);

                for (int i = 1; i < lineasReconstruidas.size(); i += batchSize) {

                    // a. Seleccionamos el lote de líneas (incluyendo la cabecera para el parser)
                    List<String> loteLineas = new ArrayList<>();
                    loteLineas.add(cabecera);

                    int end = Math.min(i + batchSize, lineasReconstruidas.size());
                    loteLineas.addAll(lineasReconstruidas.subList(i, end));

                    // b. Parseamos solo este lote
                    CsvToBean<Activo> csvToBean = new CsvToBeanBuilder<Activo>(new StringReader(String.join("\n", loteLineas)))
                            .withType(Activo.class)
                            .withSeparator(';')
                            .withQuoteChar('\0')
                            .withIgnoreLeadingWhiteSpace(true)
                            .withIgnoreQuotations(true)
                            .withThrowExceptions(false)
                            .build();

                    List<Activo> activosLote = new ArrayList<>();
                    for (Activo activo : csvToBean) {
                        // CRÍTICO: Registramos las excepciones para el reporte final
                        if (csvToBean.getCapturedExceptions() != null) {
                            excepcionesCapturadas.addAll(csvToBean.getCapturedExceptions());
                            // Limpiamos la lista interna para que no se dupliquen en el siguiente lote
                            csvToBean.getCapturedExceptions().clear();
                        }

                        // Solo guardamos si tiene código (validación mínima)
                        if (activo.getCodigo() != null && !activo.getCodigo().isEmpty()) {
                            activosLote.add(activo);
                        }
                    }

                    // c. Guardamos el lote
                    repository.saveAll(activosLote);
                    registrosGuardados += activosLote.size();
                    System.out.print("📦"); // Indicador de progreso

                    // CRÍTICO: Forzamos la recolección de basura para liberar el heap
                    System.gc();
                }
            }

            System.out.println("\n✅ CARGA FINALIZADA");

            // 4. REPORTE DE ERRORES DETALLADO
            if (!excepcionesCapturadas.isEmpty()) {
                System.err.println("🔴 ERRORES DE PARSING DETECTADOS: " + excepcionesCapturadas.size());
                System.err.println("---------------------------------------------------------");
                excepcionesCapturadas.stream().limit(10).forEach(e -> {
                    System.err.println("   - Fila original: " + e.getLineNumber() + " | Error: " + e.getMessage());
                });
                if (excepcionesCapturadas.size() > 10) {
                    System.err.println("   ... y " + (excepcionesCapturadas.size() - 10) + " errores más.");
                }
                System.err.println("---------------------------------------------------------");
            }

            long registrosGuardadosFinal = repository.count();
            long diferencia = totalRegistros - registrosGuardadosFinal;

            System.out.println("   - Esperados: " + totalRegistros);
            System.out.println("   - Guardados: " + registrosGuardadosFinal);

            if (diferencia == 0) {
                System.out.println("🎉 ¡ÉXITO TOTAL! Los datos cuadran.");
            } else {
                System.out.println("❌ ATENCIÓN: Se perdieron " + diferencia + " registros.");
                System.out.println("   Revisa el reporte de errores de parsing (arriba) para ver la causa.");
            }
        };
    }

    // Método de reconstrucción que ya teníamos
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
