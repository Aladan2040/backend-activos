package com.superinka.gestionactivos.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResumenActivoDTO {

    //Identificacion
    private String codigo;

    //Detalle del activo fijo
    private String descripcion;

    //Valor historico ACTIVO FIJO AL 09/2025
    private BigDecimal valorHistorico;

    //Porcentaje de depreciacion % DEPRE
    private BigDecimal porcentajeDepreciacion;

    //Depreciacion ACUM al inicio 2025 01/01/2025
    private BigDecimal depAcumuladaInicio;

    //Meses del 2025
    private BigDecimal ene;
    private BigDecimal feb;
    private BigDecimal mar;
    private BigDecimal abr;
    private BigDecimal may;
    private BigDecimal jun;
    private BigDecimal jul;
    private BigDecimal ago;
    private BigDecimal set;
    private BigDecimal oct;
    private BigDecimal nov;
    private BigDecimal dic;

    //Totales calculados
    //Depreciacion 2025 (Suma de meses)
    private BigDecimal totalDepreciacion2025;

    //Total depreciacion acumulada al 2025 (Inicio + Depreciacion 2025)
    private BigDecimal totalDepreciacionAcumulada;

    //"Costo Neto" Valor historico - Total Acumulado
    private BigDecimal costoNeto;

    //Estado visual par el fronend
    private String estado;
}
