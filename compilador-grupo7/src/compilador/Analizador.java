package compilador;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Ejecuta el análisis léxico sobre un texto y reúne el resultado:
 * tokens y errores en orden de aparición, y la tabla de símbolos.
 */
public class Analizador {

    public static class Resultado {
        public final List<Token> elementos = new ArrayList<Token>();
        public TablaSimbolos tabla;
        /** Rangos [inicio, fin) de los comentarios dentro del texto. */
        public List<int[]> comentarios = new ArrayList<int[]>();
        public int cantidadTokens;
        public int cantidadErrores;

        /** Texto aclaratorio con los tokens reconocidos y los errores encontrados. */
        public String comoTexto() {
            StringBuilder sb = new StringBuilder();
            for (Token t : elementos) {
                if (t.esError) {
                    sb.append(String.format("Línea %-4d ERROR LÉXICO: %s%n", t.linea, t.lexema));
                } else {
                    sb.append(String.format("Línea %-4d %-20s %s%n", t.linea, t.nombre, t.lexema));
                }
            }
            sb.append(String.format("%nAnálisis léxico finalizado: %d tokens reconocidos, %d errores.%n",
                    cantidadTokens, cantidadErrores));
            return sb.toString();
        }
    }

    public static Resultado analizar(String codigo) throws IOException {
        Lexico lexico = new Lexico(new StringReader(codigo));
        Resultado resultado = new Resultado();
        Token token = lexico.siguienteToken();
        while (token != null) {
            resultado.elementos.add(token);
            if (token.esError) {
                resultado.cantidadErrores++;
            } else {
                resultado.cantidadTokens++;
            }
            token = lexico.siguienteToken();
        }
        resultado.tabla = lexico.getTabla();
        resultado.comentarios = lexico.getComentarios();
        return resultado;
    }
}
