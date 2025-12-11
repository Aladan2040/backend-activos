package com.superinka.gestionactivos.entity;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.superinka.gestionactivos.MoneyConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Data
@Table(name = "activos_fijos_2025")
public class Activo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CsvBindByName(column = "#")
    private String numeroFila;

    // --- CAMBIO CLAVE AQUÍ ---
    // Agregamos TEXT para permitir códigos gigantes o sucios como el de la fila 25974
    @CsvBindByName(column = "CODIGO RELACIONADO CON EL ACTIVO FIJO")
    @Column(columnDefinition = "TEXT")
    private String codigo;

    @CsvBindByName(column = "CUENTA CONTABLE DEL")
    private String cuentaContable;

    @CsvBindByName(column = "NOTA INFORME SEGÚN COSTO")
    @Column(columnDefinition = "TEXT")
    private String notaInformeCosto;

    @CsvBindByName(column = "NOTA INFORME SEGÚN DEPRE")
    @Column(columnDefinition = "TEXT")
    private String notaInformeDepre;

    @CsvBindByName(column = "CUENTA NETSUIE")
    @Column(columnDefinition = "TEXT")
    private String cuentaNetsuite;

    @CsvBindByName(column = "GRUPO CUENTA DETALLE NETSUIE")
    @Column(columnDefinition = "TEXT")
    private String grupoCuentaNetsuite;

    @CsvBindByName(column = "CTA DEP NETSUIE")
    @Column(columnDefinition = "TEXT")
    private String ctaDepNetsuite;

    @CsvBindByName(column = "CTA GASTO X DEPRE")
    @Column(columnDefinition = "TEXT")
    private String ctaGastoDepre;

    @CsvBindByName(column = "DETALLE DEP NETSUIE")
    @Column(columnDefinition = "TEXT")
    private String detalleDepNetsuite;

    @CsvBindByName(column = "CeCo")
    private String ceco;

    @CsvBindByName(column = "CeCo 2")
    private String ceco2;

    @CsvBindByName(column = "OC")
    private String oc;

    @CsvBindByName(column = "FACTURA")
    private String factura;

    @CsvBindByName(column = "Claster")
    private String cluster;

    @CsvBindByName(column = "SUB DIARIO")
    private String subDiario;

    @CsvBindByName(column = "NÚMERO CORRELATIVO")
    private String numeroCorrelativo;

    @CsvBindByName(column = "DETALLE DEL ACTIVO FIJO: DESCRIPCIÓN")
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @CsvBindByName(column = "DETALLE DEL ACTIVO FIJO: MARCA DEL ACTIVO FIJO")
    @Column(columnDefinition = "TEXT")
    private String marca;

    @CsvBindByName(column = "DETALLE DEL ACTIVO FIJO: MODELO DEL ACTIVO FIJO")
    @Column(columnDefinition = "TEXT")
    private String modelo;

    @CsvBindByName(column = "DETALLE DEL ACTIVO FIJO: NUMERO/SERIE Y/O PLACA ACTIVO FIJO")
    @Column(columnDefinition = "TEXT")
    private String seriePlaca;

    @CsvCustomBindByName(column = " SALDO INCIAL ", converter = MoneyConverter.class)
    private BigDecimal saldoInicial;

    @CsvCustomBindByName(column = " ADQUISICIONES ADICIONES ", converter = MoneyConverter.class)
    private BigDecimal adquisiciones;

    @CsvCustomBindByName(column = " MEJORAS ", converter = MoneyConverter.class)
    private BigDecimal mejoras;

    @CsvCustomBindByName(column = " RETIROS Y/O BAJAS ", converter = MoneyConverter.class)
    private BigDecimal retiros;

    @CsvCustomBindByName(column = " OTROS AJUSTES DEDUCCIONES ", converter = MoneyConverter.class)
    private BigDecimal otrosAjustes;

    @CsvCustomBindByName(column = " VALOR HISTORICO ACTIVO FIJO Al 09/2025 ", converter = MoneyConverter.class)
    private BigDecimal valorHistorico;

    @CsvCustomBindByName(column = "AJUSTADO X INFLACION AJUST X INFL", converter = MoneyConverter.class)
    private BigDecimal ajustadoInflacion;

    @CsvCustomBindByName(column = "VALOR AJUSTADO ACTIVO FIJO VAL AJUS A.FIJO", converter = MoneyConverter.class)
    private BigDecimal valorAjustado;

    @CsvBindByName(column = " FECHA ADQUISICION ")
    private String fechaAdquisicion;

    @CsvBindByName(column = " FECHA INICIO DE USO ACTIVO FIJO ")
    private String fechaInicioUso;

    @CsvBindByName(column = " DEPRECIACION METODO APLICADO ")
    @Column(columnDefinition = "TEXT")
    private String metodoDepreciacion;

    @CsvBindByName(column = " DEPRECIACION NRO. DOCUMENTO DE AUTORIZACIÓN ")
    private String nroDocumentoAutorizacion;

    @CsvCustomBindByName(column = " PORCENTAJE DE DEPRECIACION % DEPRE ", converter = MoneyConverter.class)
    private BigDecimal porcentajeDepreciacion;

    @CsvCustomBindByName(column = " VIDA UTIL EN AÑOS ", converter = MoneyConverter.class)
    private BigDecimal vidaUtilAnios;

    @CsvCustomBindByName(column = "DEPRECIACION ACUM AL INICIO 2025 01/01/2025", converter = MoneyConverter.class)
    private BigDecimal depreciacionAcumuladaInicio;

    @CsvCustomBindByName(column = "Ene-25", converter = MoneyConverter.class)
    private BigDecimal ene25;
    @CsvCustomBindByName(column = "Feb-25", converter = MoneyConverter.class)
    private BigDecimal feb25;
    @CsvCustomBindByName(column = "Mar-25", converter = MoneyConverter.class)
    private BigDecimal mar25;
    @CsvCustomBindByName(column = "Abr-25", converter = MoneyConverter.class)
    private BigDecimal abr25;
    @CsvCustomBindByName(column = "May-25", converter = MoneyConverter.class)
    private BigDecimal may25;
    @CsvCustomBindByName(column = "Jun-25", converter = MoneyConverter.class)
    private BigDecimal jun25;
    @CsvCustomBindByName(column = "Jul-25", converter = MoneyConverter.class)
    private BigDecimal jul25;
    @CsvCustomBindByName(column = "Ago-25", converter = MoneyConverter.class)
    private BigDecimal ago25;
    @CsvCustomBindByName(column = "Set-25", converter = MoneyConverter.class)
    private BigDecimal set25;
    @CsvCustomBindByName(column = "Oct-25", converter = MoneyConverter.class)
    private BigDecimal oct25;
    @CsvCustomBindByName(column = "Nov-25", converter = MoneyConverter.class)
    private BigDecimal nov25;
    @CsvCustomBindByName(column = "Dic-25", converter = MoneyConverter.class)
    private BigDecimal dic25;

    @CsvCustomBindByName(column = "DEPRECIACION 2025", converter = MoneyConverter.class)
    private BigDecimal totalDepreciacion2025;

    @CsvCustomBindByName(column = " TOTAL DEPRECIACIONACUMULADA 2025 ", converter = MoneyConverter.class)
    private BigDecimal totalDepreciacionAcumulada;

    @CsvCustomBindByName(column = " COSTO NETO ", converter = MoneyConverter.class)
    private BigDecimal costoNeto;
}
