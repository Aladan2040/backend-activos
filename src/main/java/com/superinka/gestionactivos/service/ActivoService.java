package com.superinka.gestionactivos.service;

import com.superinka.gestionactivos.Activo;
import com.superinka.gestionactivos.ActivoRepository;
import com.superinka.gestionactivos.dto.ResumenActivoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivoService {

    @Autowired
    private ActivoRepository repository;

    public List<ResumenActivoDTO> obtenerResumen() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    // CAMBIO: Ahora devuelve la lista calculada y NO es @Transactional (porque no guarda)
    public List<ResumenActivoDTO> calcularDepreciacionVisual(int mesObjetivo) {
        // 1. Traemos los datos "vírgenes" de la BD cada vez
        List<Activo> activos = repository.findAll(Sort.by(Sort.Direction.ASC, "id"));

        for (Activo activo : activos) {
            BigDecimal AA12_ValorHistorico = activo.getValorHistorico() != null ? activo.getValorHistorico() : BigDecimal.ZERO;

            // Normalización del porcentaje
            BigDecimal rawPercent = activo.getPorcentajeDepreciacion() != null ? activo.getPorcentajeDepreciacion() : BigDecimal.ZERO;
            BigDecimal AJ12_Porcentaje = rawPercent.compareTo(BigDecimal.ONE) > 0 ? rawPercent.divide(new BigDecimal(100)) : rawPercent;

            BigDecimal AL12_AcumInicio = activo.getDepreciacionAcumuladaInicio() != null ? activo.getDepreciacionAcumuladaInicio() : BigDecimal.ZERO;

            // 2. BUCLE ACUMULATIVO: Calculamos desde Enero (1) hasta el mes que pidió el usuario (mesObjetivo)
            // Esto asegura que si piden "Marzo", Enero y Febrero se calculen primero en memoria para afectar el acumulado correctamente.
            for (int m = 1; m <= mesObjetivo; m++) {

                BigDecimal cuotaTeorica = AA12_ValorHistorico.multiply(AJ12_Porcentaje)
                        .divide(new BigDecimal(12), 2, RoundingMode.HALF_UP);

                // 'sumarMesesAnteriores' leerá lo que acabamos de calcular en la vuelta anterior del bucle 'm'
                BigDecimal acumuladoHastaHoy = AL12_AcumInicio.add(sumarMesesAnteriores(activo, m));

                BigDecimal remanente = AA12_ValorHistorico.subtract(acumuladoHastaHoy);
                BigDecimal montoFinal;

                if (remanente.compareTo(BigDecimal.ZERO) > 0) {
                    if (cuotaTeorica.compareTo(remanente) < 0) {
                        montoFinal = cuotaTeorica;
                    } else {
                        montoFinal = remanente;
                    }
                } else {
                    montoFinal = BigDecimal.ZERO;
                }

                // Actualizamos el objeto EN MEMORIA (Java), no en la BD
                asignarDepreciacionAlMes(activo, m, montoFinal);
            }
        }

        // 3. Convertimos a DTO y devolvemos los datos calculados directamente
        // NO llamamos a repository.saveAll()
        return activos.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    private BigDecimal sumarMesesAnteriores(Activo a, int mesActual) {
        BigDecimal suma = BigDecimal.ZERO;
        // Suma acumulativa usando los getters (que leen lo que acabamos de setear en memoria)
        if (mesActual > 1 && a.getEne25() != null) suma = suma.add(a.getEne25());
        if (mesActual > 2 && a.getFeb25() != null) suma = suma.add(a.getFeb25());
        if (mesActual > 3 && a.getMar25() != null) suma = suma.add(a.getMar25());
        if (mesActual > 4 && a.getAbr25() != null) suma = suma.add(a.getAbr25());
        if (mesActual > 5 && a.getMay25() != null) suma = suma.add(a.getMay25());
        if (mesActual > 6 && a.getJun25() != null) suma = suma.add(a.getJun25());
        if (mesActual > 7 && a.getJul25() != null) suma = suma.add(a.getJul25());
        if (mesActual > 8 && a.getAgo25() != null) suma = suma.add(a.getAgo25());
        if (mesActual > 9 && a.getSet25() != null) suma = suma.add(a.getSet25());
        if (mesActual > 10 && a.getOct25() != null) suma = suma.add(a.getOct25());
        if (mesActual > 11 && a.getNov25() != null) suma = suma.add(a.getNov25());
        return suma;
    }

    private void asignarDepreciacionAlMes(Activo a, int mes, BigDecimal monto) {
        switch (mes) {
            case 1: a.setEne25(monto); break;
            case 2: a.setFeb25(monto); break;
            case 3: a.setMar25(monto); break;
            case 4: a.setAbr25(monto); break;
            case 5: a.setMay25(monto); break;
            case 6: a.setJun25(monto); break;
            case 7: a.setJul25(monto); break;
            case 8: a.setAgo25(monto); break;
            case 9: a.setSet25(monto); break;
            case 10: a.setOct25(monto); break;
            case 11: a.setNov25(monto); break;
            case 12: a.setDic25(monto); break;
        }
    }

    private ResumenActivoDTO convertirADTO(Activo a) {
        ResumenActivoDTO dto = new ResumenActivoDTO();
        dto.setCodigo(a.getCodigo());
        dto.setDescripcion(a.getDescripcion());
        dto.setValorHistorico(a.getValorHistorico());

        BigDecimal rawPercent = a.getPorcentajeDepreciacion() != null ? a.getPorcentajeDepreciacion() : BigDecimal.ZERO;
        if (rawPercent.compareTo(BigDecimal.ONE) > 0) {
            rawPercent = rawPercent.divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
        }
        dto.setPorcentajeDepreciacion(rawPercent);

        dto.setDepAcumuladaInicio(a.getDepreciacionAcumuladaInicio());

        dto.setEne(a.getEne25()); dto.setFeb(a.getFeb25()); dto.setMar(a.getMar25());
        dto.setAbr(a.getAbr25()); dto.setMay(a.getMay25()); dto.setJun(a.getJun25());
        dto.setJul(a.getJul25()); dto.setAgo(a.getAgo25()); dto.setSet(a.getSet25());
        dto.setOct(a.getOct25()); dto.setNov(a.getNov25()); dto.setDic(a.getDic25());

        BigDecimal sumaAnio = BigDecimal.ZERO;
        if(a.getEne25()!=null) sumaAnio = sumaAnio.add(a.getEne25());
        if(a.getFeb25()!=null) sumaAnio = sumaAnio.add(a.getFeb25());
        if(a.getMar25()!=null) sumaAnio = sumaAnio.add(a.getMar25());
        if(a.getAbr25()!=null) sumaAnio = sumaAnio.add(a.getAbr25());
        if(a.getMay25()!=null) sumaAnio = sumaAnio.add(a.getMay25());
        if(a.getJun25()!=null) sumaAnio = sumaAnio.add(a.getJun25());
        if(a.getJul25()!=null) sumaAnio = sumaAnio.add(a.getJul25());
        if(a.getAgo25()!=null) sumaAnio = sumaAnio.add(a.getAgo25());
        if(a.getSet25()!=null) sumaAnio = sumaAnio.add(a.getSet25());
        if(a.getOct25()!=null) sumaAnio = sumaAnio.add(a.getOct25());
        if(a.getNov25()!=null) sumaAnio = sumaAnio.add(a.getNov25());
        if(a.getDic25()!=null) sumaAnio = sumaAnio.add(a.getDic25());

        dto.setTotalDepreciacion2025(sumaAnio);

        BigDecimal inicio = a.getDepreciacionAcumuladaInicio() != null ? a.getDepreciacionAcumuladaInicio() : BigDecimal.ZERO;
        BigDecimal totalAcum = inicio.add(sumaAnio);
        dto.setTotalDepreciacionAcumulada(totalAcum);

        BigDecimal historico = a.getValorHistorico() != null ? a.getValorHistorico() : BigDecimal.ZERO;
        dto.setCostoNeto(historico.subtract(totalAcum));

        dto.setEstado(dto.getCostoNeto().compareTo(BigDecimal.ZERO) <= 0 ? "COMPLETADO" : "ACTIVO");

        return dto;
    }
}
