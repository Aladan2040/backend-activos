package com.superinka.gestionactivos;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.superinka.gestionactivos.entity.Activo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
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
                System.out.println("ℹ️ La BD ya tiene datos (" + countBD + "). Omitiendo carga.");
                return;
            }

            System.out.println("🚀 INICIANDO CARGA (MODO STREAMING PURO - MEMORIA ESTABLE)...");

            ClassPathResource resource = new ClassPathResource("Depreciacion.csv");

            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                // 1. Leer y guardar la cabecera (La necesitamos para cada mini-lote)
                String headerLine = br.readLine();
                if (headerLine == null) return; // Archivo vacío

                // Limpiamos comillas de la cabecera también por si acaso
                headerLine = headerLine.replace("\"", "");

                List<String> loteLineas = new ArrayList<>();
                StringBuilder registroActual = new StringBuilder();
                String patronInicioRegistro = "^\\s*\"?(\\d+|#)\"?\\s*;.*";

                String linea;
                int totalProcesados = 0;

                // 2. Bucle de lectura línea a línea (Streaming)
                while ((linea = br.readLine()) != null) {
                    if (linea.trim().isEmpty()) continue;

                    if (linea.matches(patronInicioRegistro)) {
                        // Es un nuevo registro: Procesamos el anterior si existe
                        if (registroActual.length() > 0) {
                            loteLineas.add(registroActual.toString());
                        }
                        registroActual = new StringBuilder(linea);
                    } else {
                        // Es continuación: Lo pegamos al actual
                        registroActual.append(" ").append(linea.trim());
                    }

                    // 3. Si el lote en memoria llega a 500, lo procesamos y vaciamos
                    if (loteLineas.size() >= 500) {
                        procesarLote(headerLine, loteLineas);
                        totalProcesados += loteLineas.size();
                        loteLineas.clear(); // ¡LIBERAR MEMORIA!
                        System.gc(); // Sugerencia agresiva al recolector de basura
                        System.out.print(".");
                    }
                }

                // Agregar el último registro pendiente del buffer
                if (registroActual.length() > 0) {
                    loteLineas.add(registroActual.toString());
                }

                // Procesar el remanente final
                if (!loteLineas.isEmpty()) {
                    procesarLote(headerLine, loteLineas);
                    totalProcesados += loteLineas.size();
                }

                System.out.println("\n✅ CARGA FINALIZADA EXITOSAMENTE.");
                System.out.println("   - Registros procesados: " + totalProcesados);
                System.out.println("   - Registros en BD: " + repository.count());
            }
        };
    }

    // Método auxiliar para procesar un pequeño lote de texto
    private void procesarLote(String header, List<String> lineas) {
        // Unimos el lote en un solo String grande
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n"); // Agregamos cabecera para que OpenCSV sepa mapear
        for (String l : lineas) {
            sb.append(l).append("\n");
        }

        // LIMPIEZA QUÍMICA: Quitamos comillas del lote entero
        String csvSanitizado = sb.toString().replace("\"", "");

        // Parseo
        CsvToBean<Activo> csvToBean = new CsvToBeanBuilder<Activo>(new StringReader(csvSanitizado))
                .withType(Activo.class)
                .withSeparator(';')
                .withQuoteChar('\0') // Ignorar comillas
                .withIgnoreLeadingWhiteSpace(true)
                .withIgnoreQuotations(true)
                .withThrowExceptions(false)
                .build();

        List<Activo> activosValidos = new ArrayList<>();
        for (Activo a : csvToBean) {
            // Validación mínima: Código no vacío
            if (a.getCodigo() != null && !a.getCodigo().trim().isEmpty()) {
                activosValidos.add(a);
            }
        }

        // Guardado Transaccional
        guardarEnBD(activosValidos);
    }

    @Transactional
    public void guardarEnBD(List<Activo> activos) {
        repository.saveAll(activos);
        repository.flush(); // Forzar escritura a disco para liberar RAM de Hibernate
    }
}
