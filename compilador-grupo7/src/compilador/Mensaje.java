package compilador;

/**
 * Línea de salida del análisis sintáctico: una regla reconocida,
 * un resultado de APLICARDESCUENTO o un error.
 */
public class Mensaje {

    public static final int REGLA = 0;
    public static final int RESULTADO = 1;
    public static final int ERROR = 2;

    public final int tipo;
    public final int linea;
    public final String texto;

    public Mensaje(int tipo, int linea, String texto) {
        this.tipo = tipo;
        this.linea = linea;
        this.texto = texto;
    }

    public String comoTexto() {
        switch (tipo) {
            case REGLA:
                return String.format("Línea %-4d %s", linea, texto);
            case RESULTADO:
                return String.format("           %s", texto);
            default:
                return String.format("Línea %-4d %s", linea, texto);
        }
    }
}
