# Especificación del lenguaje – Grupo 7 (APLICARDESCUENTO)

Trabajo Práctico Integrador de **Teoría de la Computación I** – Universidad Nacional de Luján – 2026.

**Integrantes:** Ávila Tobías, Echeverría Crenna Gonzalo, López Becerra Tomás, Buzzo Rocco.

Este documento reúne todo lo definido para el compilador:

1. el **analizador léxico** (primera entrega, `Lexico.flex`);
2. la **gramática libre de contexto** (segunda entrega, base de `Sintactico.cup`), con las correcciones de la cátedra ya aplicadas.

---

# Parte 1 – Analizador léxico

## 1.1 Definiciones auxiliares

```
DIGITO       = [0-9]
LETRA        = [A-Za-z]
ALFANUMERICO = {LETRA} | {DIGITO}
```

## 1.2 Identificadores y constantes

```
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

- Un `ID` empieza con letra, puede llevar guion medio o guion bajo en el medio y termina en letra o dígito.
- `CARACTER_STR` son los caracteres permitidos dentro de un string: letras, dígitos, blancos,
  `, : ; + - * / < > = ! .`, `¡`, `ñ` y `Ñ`.
- El signo no forma parte de la constante: `-5` se reconoce como `OP_RESTA` seguido de `CTE_E`.

## 1.3 Palabras reservadas

| Lexema | Token | Lexema | Token |
| --- | --- | --- | --- |
| `DECLARE.SECTION` | `DECLARE_SECTION` | `WRITE` / `write` | `WRITE` |
| `ENDDECLARE.SECTION` | `ENDDECLARE_SECTION` | `INT` | `INT` |
| `PROGRAM.SECTION` | `PROGRAM_SECTION` | `FLOAT` | `FLOAT` |
| `ENDPROGRAM.SECTION` | `ENDPROGRAM_SECTION` | `STRING` | `STRING` |
| `WHILE` | `WHILE` | `AND` | `AND` |
| `ENDWHILE` | `ENDWHILE` | `OR` | `OR` |
| `IF` | `IF` | `THEN` | `THEN` |
| `ELSE` | `ELSE` | `APLICARDESCUENTO` | `APLICARDESCUENTO` |
| `ENDIF` | `ENDIF` | | |

En `Lexico.flex` estas reglas van antes que la de `ID`, para que tengan prioridad.

## 1.4 Operadores y delimitadores

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

## 1.5 Comentarios y blancos

Los comentarios se delimitan con `//*` y `*//` y admiten **un solo nivel de anidamiento**. Ni los
comentarios ni los blancos generan token.

```
//* comentario simple *//

//* comentario exterior
    //* comentario interior *//
    sigue el comentario exterior
*//
```

En `Lexico.flex` se implementan con dos estados léxicos exclusivos, `COMENTARIO` y `COMENTARIO_ANIDADO`.

## 1.6 Errores léxicos

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

Una constante con error no se registra en la tabla de símbolos.

## 1.7 Tabla de símbolos

Guarda identificadores y constantes, sin repetir y en orden de aparición. Se muestra en pantalla y se guarda en `ts.txt`.

| Columna | Contenido |
| --- | --- |
| NOMBRE | El lexema para los `ID`; `_` + lexema para las constantes |
| TOKEN | `ID`, `CTE_E`, `CTE_F` o `CTE_STR` |
| TIPO | El tipo declarado de cada `ID` (se completa en la segunda entrega) |
| VALOR | El valor de la constante; los `ID` no guardan valor |
| LONG | Cantidad de caracteres, solo para `CTE_STR` |

En el nombre de una constante string los espacios se reemplazan por guion bajo: `"HOLA MUNDO"` se registra como `_HOLA_MUNDO`.

```
NOMBRE       TOKEN     TIPO    VALOR       LONG
a1           ID        -       -           -
_55          CTE_E     -       55          -
_99.         CTE_F     -       99.         -
_HOLA_MUNDO  CTE_STR   -       HOLA MUNDO  10
```

---

# Parte 2 – Gramática libre de contexto

La gramática se escribe en notación BNF, como en el apunte de la cátedra: los no terminales van
entre `< >` y los terminales se escriben directamente; los terminales son los tokens de la Parte 1.

## 2.1 Definición formal

G = ( ENT , ET , S , R )

**ENT (no terminales)**

```
{ <programa>, <seccion_declaracion>, <lista_declaraciones>, <declaracion>,
  <asignacion_tipos>, <tipo>, <seccion_programa>, <bloque>, <sentencia>, <asignacion>,
  <salida>, <seleccion>, <iteracion>, <condicion>, <comparacion>, <comparador>,
  <expresion>, <termino>, <factor>, <aplicar_descuento>, <porcentaje>,
  <lista_precios>, <precio> }
```

**ET (terminales)**

```
{ DECLARE_SECTION, ENDDECLARE_SECTION, PROGRAM_SECTION, ENDPROGRAM_SECTION,
  WHILE, ENDWHILE, IF, ELSE, ENDIF, WRITE, INT, FLOAT, STRING, AND, OR, APLICARDESCUENTO,
  OP_ASIGNACION, OP_DECL_TIPO, OP_MENOR, OP_MENOR_IGUAL, OP_MAYOR, OP_MAYOR_IGUAL,
  OP_IGUAL, OP_DISTINTO, OP_SUMA, OP_RESTA, OP_MULT, OP_DIV,
  PAREN_IZQ, PAREN_DER, CORCHETE_IZQ, CORCHETE_DER, COMA,
  ID, CTE_E, CTE_F, CTE_STR }
```

**S (símbolo distinguido):** `<programa>`

## 2.2 Reglas (R)

### Estructura del programa

```
 1. <programa> ::= <seccion_declaracion> <seccion_programa>
 2. <programa> ::= <seccion_programa>
```

### Sección de declaraciones

```
 3. <seccion_declaracion> ::= DECLARE_SECTION <lista_declaraciones> ENDDECLARE_SECTION
 4. <lista_declaraciones> ::= <lista_declaraciones> <declaracion>
 5. <lista_declaraciones> ::= <declaracion>
 6. <declaracion> ::= CORCHETE_IZQ <asignacion_tipos> CORCHETE_DER
 7. <asignacion_tipos> ::= ID COMA <asignacion_tipos> COMA <tipo>
 8. <asignacion_tipos> ::= ID CORCHETE_DER OP_DECL_TIPO CORCHETE_IZQ <tipo>
 9. <tipo> ::= INT
10. <tipo> ::= FLOAT
11. <tipo> ::= STRING
```

La regla 7 es recursiva **al centro**: cada vez que se aplica agrega un `ID` a la izquierda y un
`<tipo>` a la derecha, y la regla 8 cierra con un `ID` y un `<tipo>`. Por eso la gramática solo
genera declaraciones con **la misma cantidad de variables que de tipos**; si sobra o falta uno,
se produce un error sintáctico.

### Sección de programa y sentencias

```
12. <seccion_programa> ::= PROGRAM_SECTION <bloque> ENDPROGRAM_SECTION
13. <bloque> ::= <bloque> <sentencia>
14. <bloque> ::= <sentencia>
15. <sentencia> ::= <asignacion>
16. <sentencia> ::= <salida>
17. <sentencia> ::= <seleccion>
18. <sentencia> ::= <iteracion>
19. <sentencia> ::= <aplicar_descuento>
```

### Asignación y salida

```
20. <asignacion> ::= ID OP_ASIGNACION <expresion>
21. <asignacion> ::= ID OP_ASIGNACION CTE_STR
22. <salida> ::= WRITE CTE_STR
23. <salida> ::= WRITE ID
24. <salida> ::= WRITE CTE_E
25. <salida> ::= WRITE CTE_F
```

`WRITE` solo escribe constantes y variables; una expresión como `write a + 1` da error sintáctico.

### Decisión y ciclo

```
26. <seleccion> ::= IF PAREN_IZQ <condicion> PAREN_DER <bloque> ENDIF
27. <seleccion> ::= IF PAREN_IZQ <condicion> PAREN_DER <bloque> ELSE <bloque> ENDIF
28. <iteracion> ::= WHILE PAREN_IZQ <condicion> PAREN_DER <bloque> ENDWHILE
```

### Condiciones

```
29. <condicion> ::= <comparacion>
30. <condicion> ::= PAREN_IZQ <comparacion> PAREN_DER AND PAREN_IZQ <comparacion> PAREN_DER
31. <condicion> ::= PAREN_IZQ <comparacion> PAREN_DER OR PAREN_IZQ <comparacion> PAREN_DER
32. <comparacion> ::= <expresion> <comparador> <expresion>
33. <comparador> ::= OP_MENOR
34. <comparador> ::= OP_MENOR_IGUAL
35. <comparador> ::= OP_MAYOR
36. <comparador> ::= OP_MAYOR_IGUAL
37. <comparador> ::= OP_IGUAL
38. <comparador> ::= OP_DISTINTO
```

### Expresiones aritméticas

Se usa la forma no ambigua del apunte (expresión / término / factor): `*` y `/` tienen
mayor precedencia que `+` y `-`, y la recursión a izquierda da asociatividad a izquierda.

```
39. <expresion> ::= <expresion> OP_SUMA <termino>
40. <expresion> ::= <expresion> OP_RESTA <termino>
41. <expresion> ::= <termino>
42. <termino> ::= <termino> OP_MULT <factor>
43. <termino> ::= <termino> OP_DIV <factor>
44. <termino> ::= <factor>
45. <factor> ::= ID
46. <factor> ::= CTE_E
47. <factor> ::= CTE_F
48. <factor> ::= PAREN_IZQ <expresion> PAREN_DER
```

### Tema especial: APLICARDESCUENTO

La función recibe un porcentaje de descuento y una lista de precios, por ejemplo
`APLICARDESCUENTO(27, [500, 305, 79.4, 10])`.

```
49. <aplicar_descuento> ::= APLICARDESCUENTO PAREN_IZQ <porcentaje> COMA CORCHETE_IZQ <lista_precios> CORCHETE_DER PAREN_DER
50. <porcentaje> ::= CTE_E
51. <porcentaje> ::= CTE_F
52. <lista_precios> ::= <lista_precios> COMA <precio>
53. <lista_precios> ::= <precio>
54. <precio> ::= CTE_E
55. <precio> ::= CTE_F
```

`<lista_precios>` tiene al menos un `<precio>`, así que `APLICARDESCUENTO(50, [])` da error sintáctico.
`<porcentaje>` es un no terminal aparte para que en CUP tenga su propia acción, donde se valida el rango de 0 a 100.

**Total: 55 reglas.**

## 2.3 Ejemplos de derivación (left-most)

### Declaración

Hilera: `[a1, b1, cont] := [FLOAT, INT, INT]`

```
<declaracion>
⇒ [ <asignacion_tipos> ]                                         (6)
⇒ [ a1 , <asignacion_tipos> , <tipo> ]                           (7)
⇒ [ a1 , b1 , <asignacion_tipos> , <tipo> , <tipo> ]             (7)
⇒ [ a1 , b1 , cont ] := [ <tipo> , <tipo> , <tipo> ]             (8)
⇒ [ a1 , b1 , cont ] := [ FLOAT , <tipo> , <tipo> ]              (10)
⇒ [ a1 , b1 , cont ] := [ FLOAT , INT , <tipo> ]                 (9)
⇒ [ a1 , b1 , cont ] := [ FLOAT , INT , INT ]                    (9)
```

Reglas aplicadas: 6 7 7 8 10 9 9

### APLICARDESCUENTO

Hilera: `APLICARDESCUENTO(27, [500, 305, 79.4, 10])`

```
<aplicar_descuento>
⇒ APLICARDESCUENTO ( <porcentaje> , [ <lista_precios> ] )                         (49)
⇒ APLICARDESCUENTO ( 27 , [ <lista_precios> ] )                                   (50)
⇒ APLICARDESCUENTO ( 27 , [ <lista_precios> , <precio> ] )                        (52)
⇒ APLICARDESCUENTO ( 27 , [ <lista_precios> , <precio> , <precio> ] )             (52)
⇒ APLICARDESCUENTO ( 27 , [ <lista_precios> , <precio> , <precio> , <precio> ] )  (52)
⇒ APLICARDESCUENTO ( 27 , [ <precio> , <precio> , <precio> , <precio> ] )         (53)
⇒ APLICARDESCUENTO ( 27 , [ 500 , <precio> , <precio> , <precio> ] )              (54)
⇒ APLICARDESCUENTO ( 27 , [ 500 , 305 , <precio> , <precio> ] )                   (54)
⇒ APLICARDESCUENTO ( 27 , [ 500 , 305 , 79.4 , <precio> ] )                       (55)
⇒ APLICARDESCUENTO ( 27 , [ 500 , 305 , 79.4 , 10 ] )                             (54)
```

Reglas aplicadas: 49 50 52 52 52 53 54 54 55 54

## 2.4 Decisiones de diseño

- **Sin ambigüedad.** Las expresiones siguen el esquema expresión / término / factor del apunte.
  El IF no tiene el problema del "else colgante" porque siempre cierra con `ENDIF`.
  La gramática se verificó como LALR(1) sin conflictos.
- **Sección de declaraciones opcional** (regla 2): un programa puede empezar directamente con `PROGRAM.SECTION`.
- **Declaraciones:** la recursión al centro de la regla 7 obliga a que haya tantos tipos como variables.
- **WRITE:** solo admite una constante (`CTE_STR`, `CTE_E`, `CTE_F`) o una variable (`ID`).
- **APLICARDESCUENTO:** la lista de precios no puede estar vacía.
- **Condiciones:** una comparación simple, o dos comparaciones entre paréntesis unidas por un `AND` o un `OR`,
  como en `IF ((a1 >= 10) AND (b1 < 100))`.
- **Strings:** una `CTE_STR` solo puede asignarse o mostrarse con `WRITE`; no participa de expresiones aritméticas.
- **Signo negativo:** no hay menos unario; el léxico separa `-5` en `OP_RESTA` y `CTE_E`, y esa secuencia no forma una expresión válida.
- **Tokens del léxico sin uso sintáctico:** `THEN`, `LLAVE_IZQ`, `LLAVE_DER` y `PUNTO_Y_COMA` no aparecen en ninguna regla,
  así que en un programa producen error sintáctico.

## 2.5 Controles para las acciones semánticas

La GLC no puede expresar estos controles; se resuelven en las acciones de CUP:

- **Tipo de cada variable en la tabla de símbolos (columna TIPO).** Por la recursión al centro, la regla 7
  empareja el primer `ID` con el último `<tipo>`. Para asignar cada tipo a su variable en orden, las acciones
  juntan los `ID` y los `<tipo>` en dos listas y al reducir la regla 6 se recorren juntas.
- El `<porcentaje>` de APLICARDESCUENTO debe estar entre 0 y 100.

## 2.6 Correcciones de la cátedra aplicadas

| Comentario | Corrección |
| --- | --- |
| La declaración no controlaba igual cantidad de tipos y variables | Reglas 6 a 8 con recursión al centro |
| `WRITE` solo debe escribir constantes y variables | Reglas 22 a 25; se quitó `WRITE <expresion>` |
| Eliminar la regla de lista vacía de APLICARDESCUENTO | Se quitó; `APLICARDESCUENTO(50, [])` da error sintáctico |
