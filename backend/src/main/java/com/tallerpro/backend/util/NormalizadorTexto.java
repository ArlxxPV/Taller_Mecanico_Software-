package com.tallerpro.backend.util;

import java.util.Locale;

/**
 * Normaliza como se guardan los campos de texto, sin importar como los haya
 * capturado quien llena el formulario (Fase 2: "cada campo se guarda con la
 * primera letra de cada palabra en mayuscula y el resto en minuscula").
 */
public final class NormalizadorTexto {

    private NormalizadorTexto() {
    }

    /** "juan PÉREZ   lopez" -> "Juan Pérez Lopez". Colapsa espacios repetidos. */
    public static String aTitulo(String valor) {
        if (valor == null) {
            return null;
        }
        String[] palabras = valor.trim().toLowerCase(new Locale("es", "MX")).split("\\s+");
        StringBuilder resultado = new StringBuilder();
        for (String palabra : palabras) {
            if (palabra.isEmpty()) {
                continue;
            }
            if (!resultado.isEmpty()) {
                resultado.append(' ');
            }
            resultado.append(Character.toUpperCase(palabra.charAt(0))).append(palabra.substring(1));
        }
        return resultado.toString();
    }

    public static String aMinusculas(String valor) {
        return valor == null ? null : valor.trim().toLowerCase(new Locale("es", "MX"));
    }

    public static String aMayusculas(String valor) {
        return valor == null ? null : valor.trim().toUpperCase(new Locale("es", "MX"));
    }
}
