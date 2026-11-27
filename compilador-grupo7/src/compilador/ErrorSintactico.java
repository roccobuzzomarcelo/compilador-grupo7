package compilador;

/** Primer error sintáctico: corta la compilación. */
public class ErrorSintactico extends Exception {

    public final int linea;

    public ErrorSintactico(int linea, String mensaje) {
        super(mensaje);
        this.linea = linea;
    }
}
