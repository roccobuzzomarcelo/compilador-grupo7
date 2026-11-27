package compilador;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Ejecuta el análisis sobre un texto y reúne el resultado:
 * tokens y errores léxicos en orden de aparición, reglas sintácticas
 * reconocidas y la tabla de símbolos.
 */
public class Analizador {

    public static class Resultado {
        public final List<Token> elementos = new ArrayList<Token>();
        public TablaSimbolos tabla;
        /** Rangos [inicio, fin) de los comentarios dentro del texto. */
        public List<int[]> comentarios = new ArrayList<int[]>();
        public int cantidadTokens;
        public int cantidadErrores;

        /** Análisis sintáctico: solo se ejecuta si no hubo errores léxicos. */
        public boolean sintacticoEjecutado = false;
        public final List<Mensaje> mensajes = new ArrayList<Mensaje>();
        public int cantidadReglas;
        public int erroresSemanticos;
        public ErrorSintactico errorSintactico;

        public boolean sintacticoCorrecto() {
            return sintacticoEjecutado && errorSintactico == null && erroresSemanticos == 0;
        }

        /** Texto aclaratorio con los tokens reconocidos y los errores léxicos. */
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

        /** Texto aclaratorio con las reglas sintácticas reconocidas. */
        public String reglasComoTexto() {
            StringBuilder sb = new StringBuilder();
            for (Mensaje m : mensajes) {
                sb.append(m.comoTexto()).append(String.format("%n"));
            }
            sb.append(String.format("%n%s%n", resumenSintactico()));
            return sb.toString();
        }

        public String resumenSintactico() {
            if (!sintacticoEjecutado) {
                return "No se realizó el análisis sintáctico: hay " + cantidadErrores + " errores léxicos.";
            }
            if (errorSintactico != null) {
                return "Análisis sintáctico interrumpido en la línea " + errorSintactico.linea
                        + " (" + cantidadReglas + " reglas reconocidas hasta el error).";
            }
            if (erroresSemanticos > 0) {
                return "Análisis sintáctico finalizado: " + cantidadReglas + " reglas reconocidas, "
                        + erroresSemanticos + " errores semánticos.";
            }
            return "Análisis sintáctico finalizado: programa correcto (" + cantidadReglas
                    + " reglas reconocidas).";
        }
    }

    /** Análisis léxico (lo usa también el editor para colorear el código). */
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

    /** Compilación: análisis léxico y, si no hubo errores léxicos, análisis sintáctico. */
    public static Resultado compilar(String codigo) throws IOException {
        Resultado resultado = analizar(codigo);
        if (resultado.cantidadErrores > 0) {
            return resultado;
        }
        Sintactico parser = new Sintactico(new LexicoCup(resultado.elementos));
        parser.setTabla(resultado.tabla);
        resultado.sintacticoEjecutado = true;
        try {
            parser.parse();
        } catch (ErrorSintactico e) {
            resultado.errorSintactico = e;
        } catch (Exception e) {
            resultado.errorSintactico = new ErrorSintactico(0, "ERROR SINTÁCTICO: " + e.getMessage());
            parser.mensajes.add(new Mensaje(Mensaje.ERROR, 0, resultado.errorSintactico.getMessage()));
        }
        resultado.mensajes.addAll(parser.mensajes);
        resultado.cantidadReglas = parser.cantidadReglas;
        resultado.erroresSemanticos = parser.erroresSemanticos;
        return resultado;
    }
}
