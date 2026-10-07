package compilador;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tabla de símbolos: guarda identificadores y constantes sin repetir.
 * Columnas: NOMBRE, TOKEN, TIPO, VALOR, LONG.
 * En la primera entrega la columna TIPO queda vacía.
 */
public class TablaSimbolos {

    public static final String VACIO = "-";

    public static class Simbolo {
        public final String nombre;
        public final String token;
        public String tipo;
        public final String valor;
        public final String longitud;

        Simbolo(String nombre, String token, String tipo, String valor, String longitud) {
            this.nombre = nombre;
            this.token = token;
            this.tipo = tipo;
            this.valor = valor;
            this.longitud = longitud;
        }
    }

    /* LinkedHashMap: sin duplicados y en orden de aparición */
    private final Map<String, Simbolo> simbolos = new LinkedHashMap<String, Simbolo>();

    /** Los identificadores no guardan valor. */
    public void agregarId(String nombre) {
        if (!simbolos.containsKey(nombre)) {
            simbolos.put(nombre, new Simbolo(nombre, "ID", VACIO, VACIO, VACIO));
        }
    }

    /** Las constantes guardan su valor; las string, además, su longitud. */
    public void agregarConstante(String nombre, String token, String valor, Integer longitud) {
        if (!simbolos.containsKey(nombre)) {
            String lon = (longitud == null) ? VACIO : String.valueOf(longitud);
            simbolos.put(nombre, new Simbolo(nombre, token, VACIO, valor, lon));
        }
    }

    public List<Simbolo> getSimbolos() {
        return new ArrayList<Simbolo>(simbolos.values());
    }

    public boolean estaVacia() {
        return simbolos.isEmpty();
    }

    /** Devuelve la tabla como texto con columnas alineadas. */
    public String comoTexto() {
        int anchoNombre = "NOMBRE".length();
        int anchoValor = "VALOR".length();
        for (Simbolo s : simbolos.values()) {
            anchoNombre = Math.max(anchoNombre, s.nombre.length());
            anchoValor = Math.max(anchoValor, s.valor.length());
        }
        String formato = "%-" + anchoNombre + "s  %-8s  %-6s  %-" + anchoValor + "s  %s%n";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(formato, "NOMBRE", "TOKEN", "TIPO", "VALOR", "LONG"));
        for (Simbolo s : simbolos.values()) {
            sb.append(String.format(formato, s.nombre, s.token, s.tipo, s.valor, s.longitud));
        }
        return sb.toString();
    }

    /** Escribe la tabla en un archivo de texto (ts.txt). */
    public void guardar(String ruta) throws IOException {
        PrintWriter salida = new PrintWriter(ruta, "UTF-8");
        try {
            salida.print(comoTexto());
        } finally {
            salida.close();
        }
    }
}
