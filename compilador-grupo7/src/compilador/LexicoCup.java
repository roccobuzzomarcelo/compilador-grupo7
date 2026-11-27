package compilador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java_cup.runtime.Symbol;

/**
 * Adaptador entre el analizador léxico y el sintáctico.
 * Entrega a CUP los tokens que ya reconoció Lexico, convertidos en Symbol.
 * El número de token se busca en la clase sym por nombre, así que cada
 * token de Lexico.flex debe estar declarado como terminal en Sintactico.cup.
 * Guarda los últimos tokens entregados para armar mensajes de error claros.
 */
public class LexicoCup implements java_cup.runtime.Scanner {

    private static final Map<String, Integer> NUMEROS = new HashMap<String, Integer>();

    static {
        for (java.lang.reflect.Field campo : sym.class.getFields()) {
            if (campo.getType() != int.class) {
                continue;   /* sym también trae terminalNames */
            }
            try {
                NUMEROS.put(campo.getName(), campo.getInt(null));
            } catch (IllegalAccessException e) {
                /* los campos de sym son públicos */
            }
        }
    }

    private final List<Token> tokens;
    private int siguiente = 0;
    private final List<Token> entregados = new ArrayList<Token>();

    public LexicoCup(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Symbol next_token() {
        if (siguiente >= tokens.size()) {
            entregados.add(null);
            return new Symbol(sym.EOF);
        }
        Token t = tokens.get(siguiente++);
        entregados.add(t);
        Integer numero = NUMEROS.get(t.nombre);
        if (numero == null) {
            throw new IllegalStateException("Token sin terminal en Sintactico.cup: " + t.nombre);
        }
        return new Symbol(numero, t.linea, t.columna, t.lexema);
    }

    /**
     * Token entregado hace "atras" lugares: 0 es el último (el que CUP está
     * mirando), 1 el anterior, etc. Devuelve null si no existe o si es el fin de archivo.
     */
    public Token anterior(int atras) {
        int i = entregados.size() - 1 - atras;
        return i >= 0 ? entregados.get(i) : null;
    }
}
