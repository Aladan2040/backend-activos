package com.superinka.gestionactivos;

import com.opencsv.bean.AbstractBeanField;
import java.math.BigDecimal;

public class MoneyConverter extends AbstractBeanField<BigDecimal, String> {

    @Override
    protected Object convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 1. Limpieza básica
        String limpio = value.trim();

        // 2. Si es guion, es cero
        if (limpio.equals("-") || limpio.startsWith("- ")) {
            return BigDecimal.ZERO;
        }

        // 3. Quitar comas (miles) y símbolos de moneda/porcentaje
        limpio = limpio.replace(",", "");
        limpio = limpio.replace("%", "");
        limpio = limpio.replace("$", "");
        limpio = limpio.replace("S/", "");

        // 4. ¡CRÍTICO! Volver a limpiar espacios por si quedó "10 " después de quitar el "%"
        limpio = limpio.trim();

        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            // Log discreto para no llenar la consola
            // System.err.println("No se pudo convertir: " + value);
            return BigDecimal.ZERO;
        }
    }
}
