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
    @Transactional
    CommandLineRunner iniciarCarga() {
        return args -> {
            long countBD = repository.count();
            if (countBD > 0) {
                System.out.println("ℹ️ La BD ya tiene datos (" + countBD + " registros). Omitiendo carga inicial.");
                return;
            }

            System.out.println("🚀 INICIANDO CARGA (CORRECCIÓN DE COMILLAS + CECO)...");

            ClassPathResource resource = new ClassPathResource("Depreciacion.csv");

            // 1. LEER LÍNEAS CRUDAS
            List<String> lineasCrudas;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                lineasCrudas = br.lines().collect(Collectors.toList());
            }

            // 2. RECONSTRUCCIÓN (Unir líneas cortadas)
            List<String> lineasReconstruidas = reconstruirRegistros(lineasCrudas);
            System.out.println("🧩 Registros Lógicos reconstruidos: " + (lineasReconstruidas.size() - 1));

            // 3. LIMPIEZA QUÍMICA (CRÍTICO: Restaurado)
            String csvCompleto = String.join("\n", lineasReconstruidas);
            // Eliminamos TODAS las comillas para evitar que '24"' rompa el formato
            String csvSanitizado = csvCompleto.replace("\"", "");

            // 4. PARSEO
            CsvToBean<Activo> csvToBean = new CsvToBeanBuilder<Activo>(new StringReader(csvSanitizado))
                    .withType(Activo.class)
                    .withSeparator(';')
                    .withQuoteChar('\0') // Desactivar comillas
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreQuotations(true) // Ignorar comillas si quedaron
                    .withThrowExceptions(false)
                    .build();

            List<Activo> activosParsed = csvToBean.parse();
            List<Activo> activosValidos = new ArrayList<>();
            List<Activo> activosRechazados = new ArrayList<>();

            for (Activo activo : activosParsed) {
                // Validación suave: Solo exigimos que el código exista.
                // Si el CeCo viene vacío, el objeto tendrá ceco=null, pero SE GUARDARÁ igual.
                if (activo.getCodigo() != null && !activo.getCodigo().trim().isEmpty()) {
                    activosValidos.add(activo);
                } else {
                    activosRechazados.add(activo);
                }
            }

            // Reporte de errores si los hay
            if (csvToBean.getCapturedExceptions() != null && !csvToBean.getCapturedExceptions().isEmpty()) {
                System.out.println("⚠️  ERRORES DE PARSING: " + csvToBean.getCapturedExceptions().size());
            }

            System.out.println("💾 Guardando " + activosValidos.size() + " registros...");

            // Guardado por lotes
            int batchSize = 1000;
            for (int i = 0; i < activosValidos.size(); i += batchSize) {
                int end = Math.min(i + batchSize, activosValidos.size());
                guardarLoteTransaccional(activosValidos.subList(i, end));
                System.out.print(".");
                System.gc(); // Ayudar a la memoria
            }

            System.out.println("\n✅ CARGA COMPLETADA. Total en BD: " + repository.count());
        };
    }

    @Transactional
    public void guardarLoteTransaccional(List<Activo> activos) {
        repository.saveAll(activos);
        repository.flush();
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
