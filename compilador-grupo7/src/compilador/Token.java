package compilador;

/**
 * Componente léxico devuelto por el analizador.
 * Si esError es true, el campo lexema contiene el mensaje de error.
 * posicion y largo ubican el lexema dentro del texto analizado.
 */
public class Token {

    public final String nombre;
    public final String lexema;
    public final int linea;
    public final int columna;
    public final int posicion;
    public final int largo;
    public final boolean esError;

    public Token(String nombre, String lexema, int linea, int columna,
            int posicion, int largo, boolean esError) {
        this.nombre = nombre;
        this.lexema = lexema;
        this.linea = linea;
        this.columna = columna;
        this.posicion = posicion;
        this.largo = largo;
        this.esError = esError;
    }
}
