# Compilador – Grupo 7 (APLICARDESCUENTO)

Trabajo Práctico Integrador de **Teoría de la Computación I** – Universidad Nacional de Luján – 2026.

**Primera entrega:** analizador léxico generado con JFlex, con una interfaz gráfica que funciona como IDE del compilador.

**Integrantes:** Ávila Tobías, Echeverría Crenna Gonzalo, López Becerra Tomás, Buzzo Rocco.

## Qué hace

- Permite escribir código en un editor o cargarlo desde un archivo y modificarlo.
- Colorea el código mientras se escribe: palabras reservadas, constantes, comentarios y errores léxicos.
- Al compilar, ejecuta el análisis léxico y muestra cada token reconocido con su línea y su lexema.
- Informa los errores léxicos con su número de línea, resaltados en rojo.
- Arma la tabla de símbolos con identificadores y constantes, la muestra en pantalla y la guarda en `ts.txt`.
- Los blancos y los comentarios se reconocen pero no generan salida.

## Cómo ejecutarlo

Requiere Java 8 o superior.

```
java -jar Compilador.jar
```

En la ventana:

1. Escribir el programa en el cuadro **Código fuente**, o cargarlo con **Abrir**.
2. Presionar **Compilar** (o F5).
3. Ver el resultado en la pestaña **Tokens y errores** y la tabla en **Tabla de símbolos**.

`ts.txt` se guarda en la carpeta del archivo abierto. Si no se abrió ninguno, se guarda en la carpeta desde donde se ejecutó el JAR.

También se puede analizar un archivo por consola, sin abrir la ventana:

```
java -jar Compilador.jar prueba.txt
```

## Contenido del repositorio

| Archivo | Descripción |
| --- | --- |
| `src/compilador/Lexico.flex` | Especificación JFlex del analizador léxico |
| `src/compilador/Lexico.java` | Analizador generado por JFlex (no se edita a mano) |
| `src/compilador/Token.java` | Componente léxico: nombre, lexema, línea y columna |
| `src/compilador/TablaSimbolos.java` | Tabla de símbolos y escritura de `ts.txt` |
| `src/compilador/Analizador.java` | Ejecuta el análisis y arma el texto de salida |
| `src/compilador/Ventana.java` | Interfaz gráfica (Swing) con resaltado de sintaxis |
| `src/compilador/Main.java` | Punto de entrada |
| `src/compilador/recursos/` | Ícono de la aplicación en varios tamaños |
| `prueba.txt` | Pruebas generales, incluye el tema APLICARDESCUENTO |
| `prueba_errores.txt` | Casos de error léxico |
| `ts.txt` | Tabla de símbolos generada al compilar `prueba.txt` |
| `Compilador.jar` | JAR ejecutable |
| `lib/jflex-full-1.9.1.jar` | JFlex 1.9.1, usado para generar el analizador |
| `build.sh`, `build.bat` | Scripts para regenerar el analizador, compilar y armar el JAR |

## Componentes léxicos

### Definiciones auxiliares

```
DIGITO       = [0-9]
LETRA        = [A-Za-z]
ALFANUMERICO = {LETRA} | {DIGITO}
```

### Identificadores y constantes

| Token | Expresión regular | Control por código |
| --- | --- | --- |
| `ID` | `{LETRA} {ALFANUMERICO}*` | Ninguno |
| `CTE_E` | `{DIGITO}+` | Rango de 16 bits: 0 a 32767 |
| `CTE_F` | `{DIGITO}+ "." {DIGITO}* \| "." {DIGITO}+` | Rango de un real de 32 bits |
| `CTE_STR` | `\" [^\"\r\n]* \"` | Máximo 30 caracteres, sin contar las comillas |

El signo no forma parte de la constante: `-5` se reconoce como `OP_RESTA` seguido de `CTE_E`.

### Palabras reservadas

`DECLARE.SECTION`, `ENDDECLARE.SECTION`, `PROGRAM.SECTION`, `ENDPROGRAM.SECTION`, `WHILE`, `ENDWHILE`, `IF`, `ELSE`, `ENDIF`, `WRITE` (también `write`), `INT`, `FLOAT`, `STRING`, `AND`, `OR`, `APLICARDESCUENTO`.

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
| `COMA` | `,` | `CORCHETE_DER` | `]` |

### Comentarios

Se delimitan con `//*` y `*//` y admiten un solo nivel de anidamiento. No generan token.

```
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
| Constante string sin comilla de cierre | `d ::= "sin cerrar` |
| Carácter no reconocido | `$`, `;`, un punto suelto |
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
| TIPO | Vacío en esta entrega; se completa en la segunda |
| VALOR | El valor de la constante; los ID no guardan valor |
| LONG | Cantidad de caracteres, solo para `CTE_STR` |

En el nombre de una constante string los espacios se reemplazan por guion bajo: `"HOLA MUNDO"` se registra como `_HOLA_MUNDO`.

Ejemplo de `ts.txt`:

```
NOMBRE       TOKEN     TIPO    VALOR       LONG
a1           ID        -       -           -
_55          CTE_E     -       55          -
_99.         CTE_F     -       99.         -
_HOLA_MUNDO  CTE_STR   -       HOLA MUNDO  10
```

## Tema especial: APLICARDESCUENTO

La función recibe un porcentaje de descuento y una lista de precios:

```
APLICARDESCUENTO(27, [500, 305, 79.4, 10])
APLICARDESCUENTO(50, [])
```

En el análisis léxico el tema incorpora una sola palabra reservada, `APLICARDESCUENTO`. El resto de la sentencia usa tokens comunes: paréntesis, corchetes, coma y constantes numéricas.

El rango de 0 a 100 del porcentaje y el caso de lista vacía son validaciones de la segunda entrega.

## Compilar desde el código fuente

Requiere un JDK 8 o superior en el PATH.

```
./build.sh      (Linux / macOS)
build.bat       (Windows)
```

El script hace tres pasos:

1. Genera `Lexico.java` a partir de `Lexico.flex` con JFlex.
2. Compila todas las clases.
3. Arma `Compilador.jar`.

## Próxima entrega

La segunda entrega (23/11/2026) agrega el analizador sintáctico con Java CUP (`Sintactico.cup`), la salida de las reglas sintácticas reconocidas y los tipos de datos en la tabla de símbolos.
