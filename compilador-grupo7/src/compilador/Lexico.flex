/* =====================================================================
   Teoría de la Computación I - UNLu
   Trabajo Práctico Integrador - Compilador 2026
   Grupo 7 - Tema especial: APLICARDESCUENTO
   Primera entrega: analizador léxico
   ===================================================================== */

package compilador;

%%

%public
%class Lexico
%unicode
%line
%column
%char
%type Token
%function siguienteToken

/* Estados exclusivos para los comentarios: uno por nivel de anidamiento */
%xstate COMENTARIO, COMENTARIO_ANIDADO

%{
    /* Tabla de símbolos que se va cargando durante el análisis */
    private TablaSimbolos tabla = new TablaSimbolos();

    /* Línea donde se abrió el comentario en curso (para informar si no se cierra) */
    private int lineaComentario = 0;

    /* Aperturas de más dentro de un comentario anidado (niveles no permitidos) */
    private int nivelesExtra = 0;

    private static final int MAX_ENTERO = 32767;   /* entero de 16 bits */
    private static final int MAX_STRING = 30;      /* caracteres, sin contar las comillas */

    /* Posición donde empieza el comentario en curso */
    private int inicioComentario = 0;

    /* Rangos [inicio, fin) de los comentarios: los usa el editor para colorearlos */
    private java.util.List<int[]> comentarios = new java.util.ArrayList<int[]>();

    public TablaSimbolos getTabla() {
        return tabla;
    }

    public java.util.List<int[]> getComentarios() {
        return comentarios;
    }

    private Token token(String nombre) {
        return new Token(nombre, yytext(), yyline + 1, yycolumn + 1,
                (int) yychar, yylength(), false);
    }

    private Token error(String mensaje) {
        return new Token("ERROR", mensaje, yyline + 1, yycolumn + 1,
                (int) yychar, yylength(), true);
    }

    /* Comentario que llega al fin del archivo sin cerrarse */
    private Token comentarioSinCerrar() {
        comentarios.add(new int[] { inicioComentario, Integer.MAX_VALUE });
        return new Token("ERROR", "Comentario sin cerrar (abierto en esta línea)",
                lineaComentario, 1, inicioComentario, 3, true);
    }
%}

/* ---------------------- Definiciones auxiliares ---------------------- */

DIGITO       = [0-9]
LETRA        = [A-Za-z]
ALFANUMERICO = {LETRA} | {DIGITO}

/* ------------------- Identificadores y constantes -------------------- */

/* ID: comienza con letra, puede llevar guión medio o guión bajo, pero debe terminar
   en letra o dígito. La alternancia con {LETRA} sola conserva los identificadores
   de un solo carácter. */
ID      = {LETRA} | {LETRA} ({LETRA} | {DIGITO} | "-" | "_")* ({LETRA} | {DIGITO})
CTE_E   = {DIGITO}+
CTE_F   = {DIGITO}+ "." {DIGITO}* | "." {DIGITO}+
/* Caracteres permitidos dentro de una constante string, según CHECK_REGEX:
   letras, dígitos, blancos (incluyendo saltos de línea), los signos , : ; ,
   los operadores + - * / < > = ! , el punto, ¡ y ñ Ñ. */
CARACTER_STR = [ \t\r\n,:;+\-*/<>=!.¡ñÑA-Za-z0-9]
CTE_STR      = \" {CARACTER_STR}* \"

/* String con caracteres no permitidos: se acota a una sola línea para no tragarse
   el resto del archivo cuando aparece un carácter inválido suelto. */
STR_INVALIDO = \" [^\"\r\n]* \"

/* String sin comilla de cierre antes del fin de línea */
STR_SIN_CERRAR = \" [^\"\r\n]*

/* ----------------------------- Blancos ------------------------------- */

BLANCOS = [ \t\r\n]+

%%

/* ============================ REGLAS ================================= */

<YYINITIAL> {

    /* ---- Palabras reservadas (van antes que ID para tener prioridad) ---- */
    "DECLARE.SECTION"       { return token("DECLARE_SECTION"); }
    "ENDDECLARE.SECTION"    { return token("ENDDECLARE_SECTION"); }
    "PROGRAM.SECTION"       { return token("PROGRAM_SECTION"); }
    "ENDPROGRAM.SECTION"    { return token("ENDPROGRAM_SECTION"); }
    "WHILE"                 { return token("WHILE"); }
    "ENDWHILE"              { return token("ENDWHILE"); }
    "IF"                    { return token("IF"); }
    "ELSE"                  { return token("ELSE"); }
    "ENDIF"                 { return token("ENDIF"); }
    "THEN"                  { return token("THEN"); }
    "WRITE" | "write"       { return token("WRITE"); }
    "INT"                   { return token("INT"); }
    "FLOAT"                 { return token("FLOAT"); }
    "STRING"                { return token("STRING"); }
    "AND"                   { return token("AND"); }
    "OR"                    { return token("OR"); }
    "APLICARDESCUENTO"      { return token("APLICARDESCUENTO"); }

    /* ---- Operadores ---- */
    "::="                   { return token("OP_ASIGNACION"); }
    ":="                    { return token("OP_DECL_TIPO"); }
    "<="                    { return token("OP_MENOR_IGUAL"); }
    "<"                     { return token("OP_MENOR"); }
    ">="                    { return token("OP_MAYOR_IGUAL"); }
    ">"                     { return token("OP_MAYOR"); }
    "=="                    { return token("OP_IGUAL"); }
    "!="                    { return token("OP_DISTINTO"); }
    "+"                     { return token("OP_SUMA"); }
    "-"                     { return token("OP_RESTA"); }
    "*"                     { return token("OP_MULT"); }
    "/"                     { return token("OP_DIV"); }

    /* ---- Delimitadores ---- */
    "("                     { return token("PAREN_IZQ"); }
    ")"                     { return token("PAREN_DER"); }
    "["                     { return token("CORCHETE_IZQ"); }
    "]"                     { return token("CORCHETE_DER"); }
    "{"                     { return token("LLAVE_IZQ"); }
    "}"                     { return token("LLAVE_DER"); }
    ","                     { return token("COMA"); }
    ";"                     { return token("PUNTO_Y_COMA"); }

    /* ---- Identificadores ---- */
    {ID}                    {
                                tabla.agregarId(yytext());
                                return token("ID");
                            }

    /* ---- Constante entera: control de rango de 16 bits por código ---- */
    {CTE_E}                 {
                                String lexema = yytext();
                                java.math.BigInteger valor = new java.math.BigInteger(lexema);
                                if (valor.compareTo(java.math.BigInteger.valueOf(MAX_ENTERO)) > 0) {
                                    return error("Constante entera fuera de rango (máximo "
                                            + MAX_ENTERO + "): " + lexema);
                                }
                                tabla.agregarConstante("_" + lexema, "CTE_E", lexema, null);
                                return token("CTE_E");
                            }

    /* ---- Constante real: control de rango de 32 bits por código ---- */
    {CTE_F}                 {
                                String lexema = yytext();
                                double valor = Double.parseDouble(lexema);
                                if (valor > Float.MAX_VALUE) {
                                    return error("Constante real fuera de rango (32 bits): " + lexema);
                                }
                                tabla.agregarConstante("_" + lexema, "CTE_F", lexema, null);
                                return token("CTE_F");
                            }

    /* ---- Constante string: control de longitud por código ---- */
    {CTE_STR}               {
                                String lexema = yytext();
                                String contenido = lexema.substring(1, lexema.length() - 1);
                                if (contenido.length() > MAX_STRING) {
                                    return error("Constante string demasiado larga ("
                                            + contenido.length() + " caracteres, máximo "
                                            + MAX_STRING + "): " + lexema);
                                }
                                tabla.agregarConstante("_" + contenido.replace(' ', '_'),
                                        "CTE_STR", contenido, Integer.valueOf(contenido.length()));
                                return token("CTE_STR");
                            }

    {STR_INVALIDO}          { return error("Constante string con caracteres no permitidos: " + yytext()); }

    {STR_SIN_CERRAR}        { return error("Constante string sin comilla de cierre: " + yytext()); }

    /* ---- Comentarios: no generan token ---- */
    "//*"                   {
                                lineaComentario = yyline + 1;
                                inicioComentario = (int) yychar;
                                nivelesExtra = 0;
                                yybegin(COMENTARIO);
                            }

    "*//"                   { return error("Cierre de comentario sin apertura: *//"); }

    /* ---- Blancos: no generan token ---- */
    {BLANCOS}               { /* se ignoran */ }

    /* ---- Cualquier otro carácter es un error léxico ---- */
    [^]                     { return error("Carácter no reconocido: " + yytext()); }
}

/* Dentro de un comentario (nivel exterior) */
<COMENTARIO> {
    "//*"                   { yybegin(COMENTARIO_ANIDADO); }
    "*//"                   {
                                comentarios.add(new int[] { inicioComentario, (int) yychar + 3 });
                                yybegin(YYINITIAL);
                            }
    [^]                     { /* se ignora */ }
    <<EOF>>                 {
                                yybegin(YYINITIAL);
                                return comentarioSinCerrar();
                            }
}

/* Dentro de un comentario anidado (único nivel de anidamiento permitido) */
<COMENTARIO_ANIDADO> {
    "//*"                   {
                                nivelesExtra++;
                                return error("Comentario anidado en más de un nivel");
                            }
    "*//"                   {
                                if (nivelesExtra > 0) {
                                    nivelesExtra--;
                                } else {
                                    yybegin(COMENTARIO);
                                }
                            }
    [^]                     { /* se ignora */ }
    <<EOF>>                 {
                                yybegin(YYINITIAL);
                                return comentarioSinCerrar();
                            }
}
