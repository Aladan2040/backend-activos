package com.superinka.gestionactivos;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

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

    @Bean
    CommandLineRunner iniciarCarga(ActivoRepository repository) {
        return args -> {
            // Verificación para no duplicar datos
            long countBD = repository.count();
            if (countBD > 0) {
                System.out.println("ℹ️ La BD ya tiene " + countBD + " registros. Si faltan datos, ejecuta: DROP TABLE activos_fijos_2025;");
                return;
            }

            System.out.println("🕵️‍♂️ INICIANDO CARGA CON RECONSTRUCCIÓN DE LÍNEAS...");

            ClassPathResource resource = new ClassPathResource("Depreciacion.csv");

            // 1. LEER LÍNEAS CRUDAS (Físicas)
            // Leemos el archivo como texto plano primero para arreglar los saltos de línea
            List<String> lineasCrudas;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                lineasCrudas = br.lines().collect(Collectors.toList());
            }

            System.out.println("📊 Líneas Físicas encontradas: " + lineasCrudas.size());

            // 2. RECONSTRUCCIÓN INTELIGENTE (Stitching)
            // Lógica: Si una línea NO empieza con un número ID (o #), es continuación de la anterior.
            List<String> lineasReconstruidas = new ArrayList<>();
            StringBuilder registroActual = new StringBuilder();

            // Regex: Busca líneas que empiecen con un número y punto y coma (ej: "35413;") o la cabecera ("#;")
            // Soporta comillas opcionales en el ID por si acaso.
            String patronInicioRegistro = "^\\s*\"?(\\d+|#)\"?\\s*;.*";

            for (String linea : lineasCrudas) {
                if (linea.trim().isEmpty()) continue; // Ignorar líneas vacías

                if (linea.matches(patronInicioRegistro)) {
                    // -> ES UN NUEVO REGISTRO
                    // Guardamos el anterior si existe
                    if (registroActual.length() > 0) {
                        lineasReconstruidas.add(registroActual.toString());
                    }
                    // Iniciamos el nuevo
                    registroActual = new StringBuilder(linea);
                } else {
                    // -> ES CONTINUACIÓN (El salto de línea estaba dentro de una celda)
                    // Lo pegamos al anterior reemplazando el enter por un espacio para aplanar el CSV
                    registroActual.append(" ").append(linea.trim());
                }
            }
            // Agregar el último registro pendiente
            if (registroActual.length() > 0) {
                lineasReconstruidas.add(registroActual.toString());
            }

            System.out.println("🧩 Registros Lógicos reconstruidos: " + (lineasReconstruidas.size() - 1)); // Restamos cabecera

            // 🔍 DIAGNÓSTICO: Verificar número de campos en cada línea
            String cabecera = lineasReconstruidas.get(0);
            int numCamposEsperados = cabecera.split(";", -1).length;
            System.out.println("📋 Número de campos en cabecera: " + numCamposEsperados);

            int lineasProblematicas = 0;
            for (int i = 1; i < Math.min(100, lineasReconstruidas.size()); i++) { // Verificar primeras 100 líneas como muestra
                int numCampos = lineasReconstruidas.get(i).split(";", -1).length;
                if (numCampos != numCamposEsperados) {
                    lineasProblematicas++;
                    if (lineasProblematicas <= 5) {
                        System.out.println("⚠️  Línea " + (i+1) + " tiene " + numCampos + " campos (esperados: " + numCamposEsperados + ")");
                        // Mostrar un preview de la línea problemática (primeros 150 caracteres)
                        String preview = lineasReconstruidas.get(i).substring(0, Math.min(150, lineasReconstruidas.get(i).length()));
                        System.out.println("   Preview: " + preview + "...");
                    }
                }
            }
            if (lineasProblematicas > 5) {
                System.out.println("   ... y " + (lineasProblematicas - 5) + " líneas problemáticas más en la muestra.");
            }

            // 📝 OPCIONAL: Guardar CSV reconstruido para debug
            // Descomenta estas líneas si necesitas ver exactamente cómo quedó el CSV reconstruido
            // try (java.io.FileWriter fw = new java.io.FileWriter("debug_reconstruido.csv")) {
            //     fw.write(String.join("\n", lineasReconstruidas));
            //     System.out.println("📝 CSV reconstruido guardado en: debug_reconstruido.csv");
            // } catch (Exception e) {
            //     System.out.println("⚠️  No se pudo guardar el CSV de debug: " + e.getMessage());
            // }

            // 3. PARSEO FINAL
            // Ahora convertimos la lista arreglada en un solo String y se la damos a OpenCSV
            // Usamos quoteChar '\0' (nulo) para que ignore las comillas de pulgadas (ej: 24") y no rompa nada.
            String csvCompleto = String.join("\n", lineasReconstruidas);

            CsvToBean<Activo> csvToBean = new CsvToBeanBuilder<Activo>(new StringReader(csvCompleto))
                    .withType(Activo.class)
                    .withSeparator(';')
                    .withQuoteChar('\0') // ¡Truco clave! Desactivar interpretación de comillas
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreQuotations(false)
                    .withThrowExceptions(false) // No lanzar excepciones, pero las capturaremos manualmente
                    .build();

            // 4. GUARDADO CON DIAGNÓSTICOS MEJORADOS
            List<Activo> activosParsed = csvToBean.parse();
            List<Activo> activosValidos = new ArrayList<>();
            List<Activo> activosRechazados = new ArrayList<>();

            System.out.println("🔍 Analizando " + activosParsed.size() + " registros parseados...");

            for (Activo activo : activosParsed) {
                // Validación MUY permisiva: solo rechazar si el objeto es null o está completamente vacío
                if (activo != null) {
                    // Aceptar el registro si tiene AL MENOS UNO de estos campos no vacío
                    boolean tieneAlgunDato =
                        (activo.getCodigo() != null && !activo.getCodigo().trim().isEmpty()) ||
                        (activo.getDescripcion() != null && !activo.getDescripcion().trim().isEmpty()) ||
                        (activo.getNumeroFila() != null && !activo.getNumeroFila().trim().isEmpty()) ||
                        (activo.getCuentaContable() != null && !activo.getCuentaContable().trim().isEmpty()) ||
                        (activo.getFechaAdquisicion() != null && !activo.getFechaAdquisicion().trim().isEmpty()) ||
                        (activo.getValorHistorico() != null && activo.getValorHistorico().compareTo(BigDecimal.ZERO) != 0);

                    if (tieneAlgunDato) {
                        activosValidos.add(activo);
                    } else {
                        activosRechazados.add(activo);
                    }
                } else {
                    activosRechazados.add(activo);
                }
            }

            // Mostrar errores de parsing capturados por OpenCSV
            if (csvToBean.getCapturedExceptions() != null && !csvToBean.getCapturedExceptions().isEmpty()) {
                System.out.println("⚠️  ERRORES DE PARSING DETECTADOS: " + csvToBean.getCapturedExceptions().size());
                int errorCount = 0;
                for (Exception ex : csvToBean.getCapturedExceptions()) {
                    errorCount++;
                    if (errorCount <= 10) { // Mostrar solo los primeros 10 errores
                        System.out.println("   Error #" + errorCount + ": " + ex.getMessage());
                    }
                }
                if (errorCount > 10) {
                    System.out.println("   ... y " + (errorCount - 10) + " errores más.");
                }
            }

            System.out.println("📊 ESTADÍSTICAS DE VALIDACIÓN:");
            System.out.println("   - Registros parseados: " + activosParsed.size());
            System.out.println("   - Registros válidos: " + activosValidos.size());
            System.out.println("   - Registros rechazados: " + activosRechazados.size());

            if (!activosRechazados.isEmpty()) {
                System.out.println("\n🚫 REGISTROS RECHAZADOS (muestra de " + Math.min(20, activosRechazados.size()) + " de " + activosRechazados.size() + "):");
                for (int i = 0; i < Math.min(20, activosRechazados.size()); i++) {
                    Activo a = activosRechazados.get(i);
                    if (a == null) {
                        System.out.println("   - Registro #" + (i+1) + ": NULL (objeto completamente nulo)");
                    } else {
                        String codigo = (a.getCodigo() != null && !a.getCodigo().isEmpty()) ? a.getCodigo().substring(0, Math.min(30, a.getCodigo().length())) : "vacío";
                        String desc = (a.getDescripcion() != null && !a.getDescripcion().isEmpty()) ? a.getDescripcion().substring(0, Math.min(40, a.getDescripcion().length())) : "vacío";
                        String fila = (a.getNumeroFila() != null && !a.getNumeroFila().isEmpty()) ? a.getNumeroFila() : "vacío";
                        String cuenta = (a.getCuentaContable() != null && !a.getCuentaContable().isEmpty()) ? a.getCuentaContable() : "vacío";
                        String fecha = (a.getFechaAdquisicion() != null && !a.getFechaAdquisicion().isEmpty()) ? a.getFechaAdquisicion() : "vacío";
                        BigDecimal valor = a.getValorHistorico();
                        String valorStr = (valor != null) ? valor.toString() : "null";

                        System.out.println("   - Reg #" + (i+1) + " | Fila: " + fila + " | Código: " + codigo +
                                         " | Desc: " + desc + " | Cuenta: " + cuenta +
                                         " | Fecha: " + fecha + " | Valor: " + valorStr);
                    }
                }
                System.out.println("\n💡 SUGERENCIA: Estos registros están vacíos o tienen todos los campos importantes nulos.");
            }

            System.out.println("\n💾 Guardando " + activosValidos.size() + " registros en base de datos...");

            int batchSize = 2000;
            for (int i = 0; i < activosValidos.size(); i += batchSize) {
                int end = Math.min(i + batchSize, activosValidos.size());
                repository.saveAll(activosValidos.subList(i, end));
                System.out.print(".");
            }

            long registrosGuardados = repository.count();
            System.out.println("\n✅ ¡CARGA MAESTRA COMPLETADA!");
            System.out.println("   - Líneas físicas CSV: " + lineasCrudas.size());
            System.out.println("   - Registros lógicos reconstruidos: " + (lineasReconstruidas.size() - 1));
            System.out.println("   - Registros parseados por OpenCSV: " + activosParsed.size());
            System.out.println("   - Registros validados como correctos: " + activosValidos.size());
            System.out.println("   - Registros guardados en BD: " + registrosGuardados);

            int diferencia = (lineasReconstruidas.size() - 1) - (int)registrosGuardados;
            if (diferencia > 0) {
                System.out.println("\n⚠️  ATENCIÓN: Se perdieron " + diferencia + " registros");
                System.out.println("   Revisa los errores de parsing mostrados arriba.");
            } else {
                System.out.println("\n🎉 ¡TODOS LOS REGISTROS SE CARGARON EXITOSAMENTE!");
            }
        };
    }
}
