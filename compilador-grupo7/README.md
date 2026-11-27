# Compilador – Grupo 7 (APLICARDESCUENTO)

Trabajo Práctico Integrador de **Teoría de la Computación I** – Universidad Nacional de Luján – 2026.

**Primera entrega:** analizador léxico generado con JFlex, con una interfaz gráfica que funciona como IDE del compilador.

**Segunda entrega:** analizador sintáctico generado con Java CUP (`Sintactico.cup`), que muestra las reglas que va reconociendo el parser.

**Integrantes:** Ávila Tobías, Echeverría Crenna Gonzalo, López Becerra Tomás, Buzzo Rocco.

## Qué hace

- Permite escribir código en un editor o cargarlo desde un archivo y modificarlo.
- Colorea el código mientras se escribe: palabras reservadas, constantes, comentarios y errores léxicos.
- Al compilar, ejecuta el análisis léxico y muestra cada token reconocido con su línea y su lexema.
- Informa los errores léxicos con su número de línea, resaltados en rojo.
- Si no hay errores léxicos, ejecuta el análisis sintáctico y muestra cada regla reconocida con su número y su línea.
- El primer error sintáctico corta la compilación y se informa con su línea.
- Para APLICARDESCUENTO valida el porcentaje (0 a 100) y muestra cada precio con el descuento aplicado.
- Arma la tabla de símbolos con identificadores y constantes, con el tipo de cada uno; la muestra en pantalla y la guarda en `ts.txt`.
- Los blancos y los comentarios se reconocen pero no generan salida.

## Cómo ejecutarlo

Requiere Java 8 o superior.

```powershell
java -jar Compilador.jar
```

En la ventana:

1. Escribir el programa en el cuadro **Código fuente**, o cargarlo con **Abrir**.
2. Presionar **Compilar** (o F5).
3. Ver los tokens en **Tokens y errores**, las reglas en **Reglas sintácticas** y la tabla en **Tabla de símbolos**.

`ts.txt` se guarda en la carpeta del archivo abierto. Si no se abrió ninguno, se guarda en la carpeta desde donde se ejecutó el JAR.

También se puede analizar un archivo por consola, sin abrir la ventana:

```powershell
java -jar Compilador.jar prueba.txt
```

## Contenido del repositorio

| Archivo | Descripción |
| --- | --- |
| `src/compilador/Lexico.flex` | Especificación JFlex del analizador léxico |
| `src/compilador/Lexico.java` | Analizador generado por JFlex (no se edita a mano) |
| `src/compilador/Sintactico.cup` | Especificación Java CUP del analizador sintáctico |
| `src/compilador/Sintactico.java`, `sym.java` | Parser y tabla de terminales generados por CUP (no se editan a mano) |
| `src/compilador/LexicoCup.java` | Adaptador: entrega a CUP los tokens que reconoció `Lexico` |
| `src/compilador/Mensaje.java`, `ErrorSintactico.java` | Salida del análisis sintáctico y primer error sintáctico |
| `src/compilador/Token.java` | Componente léxico: nombre, lexema, línea y columna |
| `src/compilador/TablaSimbolos.java` | Tabla de símbolos y escritura de `ts.txt` |
| `src/compilador/Analizador.java` | Ejecuta el análisis léxico y el sintáctico y arma el texto de salida |
| `src/compilador/Ventana.java` | Interfaz gráfica (Swing) con resaltado de sintaxis |
| `src/compilador/Main.java` | Punto de entrada |
| `src/compilador/recursos/` | Ícono de la aplicación en varios tamaños |
| `prueba.txt` | Pruebas generales, incluye el tema APLICARDESCUENTO |
| `prueba_errores.txt` | Casos de error léxico |
| `pruebas_sintacticas/` | Un archivo por caso de error sintáctico o semántico (la compilación corta en el primer error) |
| `ts.txt` | Tabla de símbolos generada al compilar `prueba.txt` |
| `GLC.md`, `Especificacion_Grupo7.md` / `.pdf` | Gramática y especificación completa del lenguaje |
| `Compilador.jar` | JAR ejecutable (incluye el runtime de CUP) |
| `lib/jflex-full-1.9.1.jar` | JFlex 1.9.1, usado para generar el analizador léxico |
| `lib/java-cup-11b-20160615.jar`, `lib/java-cup-runtime-11b-20160615.jar` | Java CUP 11b: generador del parser y su runtime |
| `build.sh`, `build.bat` | Scripts para regenerar el analizador, compilar y armar el JAR |

## Componentes léxicos

### Definiciones auxiliares

```java
DIGITO       = [0-9]
LETRA        = [A-Za-z]
ALFANUMERICO = {LETRA} | {DIGITO}
```

### Identificadores y constantes

```java
ID      = {LETRA} | {LETRA} ({LETRA} | {DIGITO} | "-" | "_")* ({LETRA} | {DIGITO})
CTE_E   = {DIGITO}+
CTE_F   = {DIGITO}+ "." {DIGITO}* | "." {DIGITO}+
CTE_STR = \" {CARACTER_STR}* \"
```

| Token | Control por código |
| --- | --- |
| `ID` | Ninguno |
| `CTE_E` | Rango de 16 bits: 0 a 32767 |
| `CTE_F` | Rango de un real de 32 bits |
| `CTE_STR` | Máximo 30 caracteres, sin contar las comillas |

Un `ID` puede llevar guion medio o guion bajo en el medio, pero termina en letra o dígito. `CARACTER_STR` son los caracteres permitidos dentro de un string: letras, dígitos, blancos, `, : ; + - * / < > = ! .`, `¡`, `ñ` y `Ñ`.

El signo no forma parte de la constante: `-5` se reconoce como `OP_RESTA` seguido de `CTE_E`.

### Palabras reservadas

`DECLARE.SECTION`, `ENDDECLARE.SECTION`, `PROGRAM.SECTION`, `ENDPROGRAM.SECTION`, `WHILE`, `ENDWHILE`, `IF`, `ELSE`, `ENDIF`, `WRITE` (también `write`), `INT`, `FLOAT`, `STRING`, `AND`, `OR`, `THEN`, `APLICARDESCUENTO`.

En `Lexico.flex` estas reglas están antes que la de `ID`, para que tengan prioridad.

### Operadores y delimitadores

| Token | Lexema | Token | Lexema |
| --- | --- | --- | --- |
| `OP_ASIGNACION` | `::=` | `OP_SUMA` | `+` |
| `OP_DECL_TIPO` | `:=` | `OP_RESTA` | `-` |
| `OP_MENOR` | `<` | `OP_MULT` | `*` |
| `OP_MENOR_IGUAL` | `<=` | `OP_DIV` | `/` |
| `OP_MAYOR` | `>` | `PAREN_IZQ` | `(` |
| `OP_MAYOR_IGUAL` | `>=` | `PAREN_DER` | `)` |
| `OP_IGUAL` | `==` | `CORCHETE_IZQ` | `[` |
| `OP_DISTINTO` | `!=` | `CORCHETE_DER` | `]` |
| `COMA` | `,` | `LLAVE_IZQ` | `{` |
| `PUNTO_Y_COMA` | `;` | `LLAVE_DER` | `}` |

`THEN`, `{`, `}` y `;` se reconocen en el léxico pero no forman parte de la gramática: en un programa dan error sintáctico.

### Comentarios

Se delimitan con `//*` y `*//` y admiten un solo nivel de anidamiento. No generan token.

```java
//* comentario simple *//

//* comentario exterior
    //* comentario interior *//
    sigue el comentario exterior
*//
```

En `Lexico.flex` se implementan con dos estados léxicos exclusivos, `COMENTARIO` y `COMENTARIO_ANIDADO`, uno por nivel.

## Errores léxicos que se detectan

| Caso | Ejemplo |
| --- | --- |
| Constante entera fuera de rango | `a ::= 40000` |
| Constante real fuera de rango | un valor mayor al máximo de 32 bits |
| Constante string demasiado larga | más de 30 caracteres entre comillas |
| Constante string con caracteres no permitidos | `"contiene @ y % no validos"` |
| Constante string sin comilla de cierre | `d ::= "sin cerrar` |
| Carácter no reconocido | `$`, `#`, un punto suelto |
| Cierre de comentario sin apertura | `*//` fuera de un comentario |
| Comentario anidado en más de un nivel | tres `//*` abiertos a la vez |
| Comentario sin cerrar | `//*` sin su `*//` antes del fin del archivo |

Una constante con error no se registra en la tabla de símbolos. Todos los casos están en `prueba_errores.txt`.

## Tabla de símbolos

Guarda identificadores y constantes, sin repetir y en orden de aparición.

| Columna | Contenido |
| --- | --- |
| NOMBRE | El lexema para los ID; `_` + lexema para las constantes |
| TOKEN | `ID`, `CTE_E`, `CTE_F` o `CTE_STR` |
| TIPO | El tipo declarado de cada `ID` (lo carga el sintáctico); el de cada constante según su token |
| VALOR | El valor de la constante; los ID no guardan valor |
| LONG | Cantidad de caracteres, solo para `CTE_STR` |

En el nombre de una constante string los espacios se reemplazan por guion bajo: `"HOLA MUNDO"` se registra como `_HOLA_MUNDO`.

Ejemplo de `ts.txt`:

```txt
NOMBRE       TOKEN     TIPO    VALOR       LONG
a1           ID        FLOAT   -           -
cont         ID        INT     -           -
_55          CTE_E     INT     55          -
_99.         CTE_F     FLOAT   99.         -
_HOLA_MUNDO  CTE_STR   STRING  HOLA MUNDO  10
```

## Análisis sintáctico

La gramática completa (55 reglas, notación BNF) está en `GLC.md` y en `Especificacion_Grupo7.md` / `.pdf`. CUP la acepta sin conflictos.

- **Salida:** cada regla reconocida se muestra con su número, en el orden en que el parser la reduce (de abajo hacia arriba), y con la línea del código.
- **Declaraciones:** `[a, b, c] := [INT, FLOAT, STRING]`. La gramática obliga a que haya tantos tipos como variables; el tipo de cada una se guarda en la tabla de símbolos.
- **WRITE:** solo admite una constante o una variable.
- **Errores:** si hay errores léxicos no se hace el análisis sintáctico. El primer error sintáctico corta la compilación.

Ejemplo de salida:

```txt
Línea 59   Regla 50: <porcentaje> ::= CTE_E
Línea 59   Regla 54: <precio> ::= CTE_E
Línea 59   Regla 53: <lista_precios> ::= <precio>
...
Línea 59   Regla 49: <aplicar_descuento> ::= APLICARDESCUENTO PAREN_IZQ <porcentaje> COMA CORCHETE_IZQ <lista_precios> CORCHETE_DER PAREN_DER
           Precio 1: 500 - 500*27/100 = 365
```

## Tema especial: APLICARDESCUENTO

La función recibe un porcentaje de descuento y una lista de precios, y muestra cada precio con el descuento aplicado:

```java
APLICARDESCUENTO(27, [500, 305, 79.4, 10])
```

```txt
Precio 1: 500 - 500*27/100 = 365
Precio 2: 305 - 305*27/100 = 222.65
Precio 3: 79.4 - 79.4*27/100 = 57.96
Precio 4: 10 - 10*27/100 = 7.3
```

- El porcentaje puede ser una constante entera o real y debe estar entre 0 y 100; si no, se informa un **error semántico** y el análisis sigue.
- La lista de precios no puede estar vacía: `APLICARDESCUENTO(50, [])` da **error sintáctico** con el mensaje "lista vacía en APLICARDESCUENTO".
- Los precios finales se muestran con hasta dos decimales.

## Pruebas

| Archivo | Qué prueba |
| --- | --- |
| `prueba.txt` | Programa completo y correcto, con todos los usos de APLICARDESCUENTO |
| `prueba_errores.txt` | Errores léxicos (no llega al análisis sintáctico) |
| `pruebas_sintacticas/error_lista_vacia.txt` | `APLICARDESCUENTO(50, [])` |
| `pruebas_sintacticas/error_porcentaje_fuera_de_rango.txt` | Porcentaje mayor a 100 (error semántico) |
| `pruebas_sintacticas/error_declaracion_menos_tipos.txt` | Más variables que tipos |
| `pruebas_sintacticas/error_declaracion_mas_tipos.txt` | Más tipos que variables |
| `pruebas_sintacticas/error_write_expresion.txt` | `write total + 1` |
| `pruebas_sintacticas/error_falta_endif.txt` | IF sin ENDIF |
| `pruebas_sintacticas/error_token_sin_uso.txt` | `THEN`, token del léxico que no está en la gramática |
| `pruebas_sintacticas/error_falta_program_section.txt` | Sentencias fuera de `PROGRAM.SECTION` |

## Compilar desde el código fuente

Requiere un JDK 8 o superior en el PATH.

```powershell
./build.sh      (Linux / macOS)
build.bat       (Windows)
```

El script hace cuatro pasos:

1. Genera `Lexico.java` a partir de `Lexico.flex` con JFlex.
2. Genera `Sintactico.java` y `sym.java` a partir de `Sintactico.cup` con Java CUP.
3. Compila todas las clases.
4. Arma `Compilador.jar`, con el runtime de CUP adentro.
