package compilador;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * IDE del compilador: editor de código con resaltado de sintaxis, carga y
 * guardado de archivos, y compilación (análisis léxico) con salida de tokens,
 * errores y tabla de símbolos.
 */
public class Ventana extends JFrame {

    private static final String TITULO = "Compilador - Grupo 7 (APLICARDESCUENTO)";

    /* ------------------- Paleta (tomada del logo) ------------------- */

    private static final Color TINTA = new Color(0x012620);       /* contornos y texto */
    private static final Color TURQUESA = new Color(0x0ED2B3);
    private static final Color NARANJA = new Color(0xFE7D43);
    private static final Color FONDO = new Color(0xE4ECF4);        /* fondo de la ventana */
    private static final Color PAPEL = new Color(0xFCFDFE);        /* fondo del editor */
    private static final Color MARGEN = new Color(0xD3F3EC);       /* números de línea */
    private static final Color FILA_PAR = new Color(0xF1F6FA);

    /* Colores del resaltado de sintaxis */
    private static final Color COLOR_RESERVADA = new Color(0x078A75);
    private static final Color COLOR_NUMERO = new Color(0x1E63B5);
    private static final Color COLOR_STRING = new Color(0xB8501C);
    private static final Color COLOR_COMENTARIO = new Color(0x7A8C89);
    private static final Color COLOR_ERROR = new Color(0xC62828);
    private static final Color FONDO_ERROR = new Color(0xFDE0DC);

    private static final Font FUENTE_CODIGO = elegirFuenteCodigo();
    private static final Font FUENTE_INTERFAZ = new Font(Font.SANS_SERIF, Font.PLAIN, 13);

    private static final Set<String> RESERVADAS = new HashSet<String>(Arrays.asList(
            "DECLARE_SECTION", "ENDDECLARE_SECTION", "PROGRAM_SECTION", "ENDPROGRAM_SECTION",
            "WHILE", "ENDWHILE", "IF", "ELSE", "ENDIF", "WRITE", "INT", "FLOAT", "STRING",
            "AND", "OR", "APLICARDESCUENTO"));

    /* ---------------------- Estilos de texto ------------------------ */

    private final SimpleAttributeSet estiloNormal = estilo(TINTA, false, false, null);
    private final SimpleAttributeSet estiloReservada = estilo(COLOR_RESERVADA, true, false, null);
    private final SimpleAttributeSet estiloNumero = estilo(COLOR_NUMERO, false, false, null);
    private final SimpleAttributeSet estiloString = estilo(COLOR_STRING, false, false, null);
    private final SimpleAttributeSet estiloComentario = estilo(COLOR_COMENTARIO, false, true, null);
    private final SimpleAttributeSet estiloError = estilo(COLOR_ERROR, true, false, FONDO_ERROR);
    private final SimpleAttributeSet estiloErrorSalida = estilo(COLOR_ERROR, true, false, null);
    private final SimpleAttributeSet estiloLinea = estilo(COLOR_COMENTARIO, false, false, null);
    private final SimpleAttributeSet estiloResumen = estilo(TINTA, true, false, null);

    /* ------------------------ Componentes --------------------------- */

    private final JTextPane editor = new PanelSinAjuste();
    private final JTextArea numerosDeLinea = new JTextArea("1");
    private final JTextPane salida = new PanelSinAjuste();
    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[] { "NOMBRE", "TOKEN", "TIPO", "VALOR", "LONG" }, 0) {
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel panelSalida = new JPanel(tarjetas);
    private final BotonPlano pestaniaSalida = new BotonPlano("Tokens y errores", BotonPlano.PESTANIA);
    private final BotonPlano pestaniaTabla = new BotonPlano("Tabla de símbolos", BotonPlano.PESTANIA);
    private final JLabel estado = new JLabel("Listo");
    private final JFileChooser selector = new JFileChooser(new File("."));

    private File archivoActual = null;
    private boolean resaltadoPendiente = false;

    public Ventana() {
        super(TITULO);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1040, 760);
        setMinimumSize(new Dimension(680, 520));
        setLocationRelativeTo(null);
        setIconImages(cargarIconos());
        getContentPane().setBackground(FONDO);

        selector.setFileFilter(new FileNameExtensionFilter("Archivos de texto (*.txt)", "txt"));

        setJMenuBar(crearMenu());
        add(crearBarra(), BorderLayout.NORTH);
        add(crearPaneles(), BorderLayout.CENTER);
        add(crearBarraDeEstado(), BorderLayout.SOUTH);
    }

    /* ========================= Construcción ========================= */

    private static Font elegirFuenteCodigo() {
        Set<String> disponibles = new HashSet<String>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        String[] preferidas = { "Cascadia Mono", "Consolas", "Menlo", "DejaVu Sans Mono" };
        for (String nombre : preferidas) {
            if (disponibles.contains(nombre)) {
                return new Font(nombre, Font.PLAIN, 14);
            }
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, 14);
    }

    private static SimpleAttributeSet estilo(Color color, boolean negrita, boolean cursiva, Color fondo) {
        SimpleAttributeSet atributos = new SimpleAttributeSet();
        StyleConstants.setForeground(atributos, color);
        StyleConstants.setBold(atributos, negrita);
        StyleConstants.setItalic(atributos, cursiva);
        StyleConstants.setBackground(atributos, fondo != null ? fondo : PAPEL);
        return atributos;
    }

    private List<Image> cargarIconos() {
        List<Image> iconos = new ArrayList<Image>();
        int[] medidas = { 16, 24, 32, 48, 64, 128, 256 };
        for (int medida : medidas) {
            URL recurso = Ventana.class.getResource("/compilador/recursos/icono_" + medida + ".png");
            if (recurso != null) {
                try {
                    iconos.add(ImageIO.read(recurso));
                } catch (IOException e) {
                    /* si falta un tamaño se usan los demás */
                }
            }
        }
        return iconos;
    }

    private JMenuBar crearMenu() {
        JMenu archivo = new JMenu("Archivo");
        archivo.add(item("Nuevo", KeyEvent.VK_N, new ActionListener() {
            public void actionPerformed(ActionEvent e) { nuevo(); }
        }));
        archivo.add(item("Abrir...", KeyEvent.VK_O, new ActionListener() {
            public void actionPerformed(ActionEvent e) { abrir(); }
        }));
        archivo.add(item("Guardar", KeyEvent.VK_S, new ActionListener() {
            public void actionPerformed(ActionEvent e) { guardar(); }
        }));
        archivo.addSeparator();
        archivo.add(item("Salir", KeyEvent.VK_Q, new ActionListener() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        }));

        JMenu compilador = new JMenu("Compilador");
        JMenuItem compilar = new JMenuItem("Compilar (análisis léxico)");
        compilar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        compilar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { compilar(); }
        });
        compilador.add(compilar);

        JMenuBar barra = new JMenuBar();
        barra.add(archivo);
        barra.add(compilador);
        return barra;
    }

    private JMenuItem item(String texto, int tecla, ActionListener accion) {
        JMenuItem item = new JMenuItem(texto);
        item.setAccelerator(KeyStroke.getKeyStroke(tecla, getToolkit().getMenuShortcutKeyMask()));
        item.addActionListener(accion);
        return item;
    }

    private JPanel crearBarra() {
        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        izquierda.setOpaque(false);
        izquierda.add(boton("Nuevo", BotonPlano.NORMAL, new ActionListener() {
            public void actionPerformed(ActionEvent e) { nuevo(); }
        }));
        izquierda.add(boton("Abrir", BotonPlano.NORMAL, new ActionListener() {
            public void actionPerformed(ActionEvent e) { abrir(); }
        }));
        izquierda.add(boton("Guardar", BotonPlano.NORMAL, new ActionListener() {
            public void actionPerformed(ActionEvent e) { guardar(); }
        }));

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        derecha.setOpaque(false);
        derecha.add(boton("Compilar  (F5)", BotonPlano.PRINCIPAL, new ActionListener() {
            public void actionPerformed(ActionEvent e) { compilar(); }
        }));

        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(FONDO);
        barra.setBorder(BorderFactory.createEmptyBorder(10, 4, 10, 12));
        barra.add(izquierda, BorderLayout.WEST);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    private BotonPlano boton(String texto, int tipo, ActionListener accion) {
        BotonPlano boton = new BotonPlano(texto, tipo);
        boton.addActionListener(accion);
        return boton;
    }

    private JSplitPane crearPaneles() {
        /* ---- Editor con números de línea ---- */
        editor.setFont(FUENTE_CODIGO);
        editor.setBackground(PAPEL);
        editor.setForeground(TINTA);
        editor.setCaretColor(TINTA);
        editor.setSelectionColor(new Color(0xB6EFE4));
        editor.setMargin(new Insets(6, 8, 6, 8));

        numerosDeLinea.setFont(FUENTE_CODIGO);
        numerosDeLinea.setEditable(false);
        numerosDeLinea.setFocusable(false);
        numerosDeLinea.setBackground(MARGEN);
        numerosDeLinea.setForeground(COLOR_RESERVADA);
        numerosDeLinea.setMargin(new Insets(6, 10, 6, 10));

        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { textoModificado(); }
            public void removeUpdate(DocumentEvent e) { textoModificado(); }
            public void changedUpdate(DocumentEvent e) { /* cambios de estilo: se ignoran */ }
        });

        JScrollPane desplazamientoEditor = new JScrollPane(editor);
        desplazamientoEditor.setRowHeaderView(numerosDeLinea);
        desplazamientoEditor.setBorder(BorderFactory.createLineBorder(TINTA, 2));
        desplazamientoEditor.getViewport().setBackground(PAPEL);

        JPanel panelEditor = new JPanel(new BorderLayout(0, 6));
        panelEditor.setOpaque(false);
        panelEditor.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
        panelEditor.add(titulo("Código fuente"), BorderLayout.NORTH);
        panelEditor.add(desplazamientoEditor, BorderLayout.CENTER);

        /* ---- Salida: tokens y errores ---- */
        salida.setFont(FUENTE_CODIGO);
        salida.setEditable(false);
        salida.setBackground(PAPEL);
        salida.setForeground(TINTA);
        salida.setMargin(new Insets(6, 8, 6, 8));
        JScrollPane desplazamientoSalida = new JScrollPane(salida);
        desplazamientoSalida.setBorder(null);
        desplazamientoSalida.getViewport().setBackground(PAPEL);

        /* ---- Tabla de símbolos ---- */
        JTable tabla = new JTable(modeloTabla);
        tabla.setFont(FUENTE_CODIGO);
        tabla.setForeground(TINTA);
        tabla.setRowHeight(24);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setFillsViewportHeight(true);
        tabla.setBackground(PAPEL);
        tabla.setSelectionBackground(new Color(0xB6EFE4));
        tabla.setSelectionForeground(TINTA);
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                    boolean foco, int fila, int columna) {
                Component c = super.getTableCellRendererComponent(t, valor, seleccionada, false, fila, columna);
                if (!seleccionada) {
                    c.setBackground(fila % 2 == 0 ? PAPEL : FILA_PAR);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        });
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                    boolean foco, int fila, int columna) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(t, valor, false, false, fila, columna);
                c.setOpaque(true);
                c.setBackground(TINTA);
                c.setForeground(Color.WHITE);
                c.setFont(FUENTE_INTERFAZ.deriveFont(Font.BOLD));
                c.setHorizontalAlignment(SwingConstants.LEFT);
                c.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                return c;
            }
        });
        JScrollPane desplazamientoTabla = new JScrollPane(tabla);
        desplazamientoTabla.setBorder(null);
        desplazamientoTabla.getViewport().setBackground(PAPEL);

        panelSalida.add(desplazamientoSalida, "salida");
        panelSalida.add(desplazamientoTabla, "tabla");
        panelSalida.setBorder(BorderFactory.createLineBorder(TINTA, 2));

        pestaniaSalida.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { mostrarPestania(true); }
        });
        pestaniaTabla.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { mostrarPestania(false); }
        });
        JPanel pestanias = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pestanias.setOpaque(false);
        pestanias.add(pestaniaSalida);
        pestanias.add(javax.swing.Box.createHorizontalStrut(6));
        pestanias.add(pestaniaTabla);
        mostrarPestania(true);

        JPanel panelInferior = new JPanel(new BorderLayout(0, 6));
        panelInferior.setOpaque(false);
        panelInferior.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        panelInferior.add(pestanias, BorderLayout.NORTH);
        panelInferior.add(panelSalida, BorderLayout.CENTER);

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelEditor, panelInferior);
        division.setResizeWeight(0.58);
        division.setDividerLocation(390);
        division.setDividerSize(6);
        division.setBorder(null);
        division.setOpaque(false);
        division.setBackground(FONDO);
        if (division.getUI() instanceof javax.swing.plaf.basic.BasicSplitPaneUI) {
            javax.swing.plaf.basic.BasicSplitPaneDivider divisor =
                    ((javax.swing.plaf.basic.BasicSplitPaneUI) division.getUI()).getDivider();
            divisor.setBorder(null);
            divisor.setBackground(FONDO);
        }
        return division;
    }

    private JLabel titulo(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(FUENTE_INTERFAZ.deriveFont(Font.BOLD, 14f));
        etiqueta.setForeground(TINTA);
        return etiqueta;
    }

    private JPanel crearBarraDeEstado() {
        estado.setFont(FUENTE_INTERFAZ);
        estado.setForeground(Color.WHITE);
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(TINTA);
        barra.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        barra.add(estado, BorderLayout.CENTER);
        return barra;
    }

    private void mostrarPestania(boolean salidaVisible) {
        tarjetas.show(panelSalida, salidaVisible ? "salida" : "tabla");
        pestaniaSalida.setSeleccionado(salidaVisible);
        pestaniaTabla.setSeleccionado(!salidaVisible);
    }

    /* ==================== Editor: números y colores ================== */

    private String textoDelEditor() {
        try {
            /* se lee del documento para que los saltos de línea sean siempre \n */
            return editor.getDocument().getText(0, editor.getDocument().getLength());
        } catch (BadLocationException e) {
            return "";
        }
    }

    private void textoModificado() {
        if (resaltadoPendiente) {
            return;
        }
        resaltadoPendiente = true;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                resaltadoPendiente = false;
                actualizarNumeros();
                resaltar();
            }
        });
    }

    private void actualizarNumeros() {
        String texto = textoDelEditor();
        int lineas = 1;
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) == '\n') {
                lineas++;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lineas; i++) {
            sb.append(i);
            if (i < lineas) {
                sb.append('\n');
            }
        }
        numerosDeLinea.setText(sb.toString());
    }

    /** Colorea el código usando el mismo analizador léxico que la compilación. */
    private void resaltar() {
        String texto = textoDelEditor();
        StyledDocument documento = editor.getStyledDocument();
        int total = texto.length();
        try {
            Analizador.Resultado resultado = Analizador.analizar(texto);
            documento.setCharacterAttributes(0, total, estiloNormal, true);
            for (int[] rango : resultado.comentarios) {
                int fin = Math.min(rango[1], total);
                documento.setCharacterAttributes(rango[0], fin - rango[0], estiloComentario, true);
            }
            for (Token t : resultado.elementos) {
                SimpleAttributeSet estilo = null;
                if (t.esError) {
                    estilo = estiloError;
                } else if (RESERVADAS.contains(t.nombre)) {
                    estilo = estiloReservada;
                } else if (t.nombre.equals("CTE_E") || t.nombre.equals("CTE_F")) {
                    estilo = estiloNumero;
                } else if (t.nombre.equals("CTE_STR")) {
                    estilo = estiloString;
                }
                if (estilo != null && t.posicion + t.largo <= total) {
                    documento.setCharacterAttributes(t.posicion, t.largo, estilo, true);
                }
            }
        } catch (IOException e) {
            /* sin resaltado: el texto queda con el color normal */
        }
    }

    /* ============================ Acciones =========================== */

    private void nuevo() {
        editor.setText("");
        salida.setText("");
        modeloTabla.setRowCount(0);
        archivoActual = null;
        setTitle(TITULO);
        estado.setForeground(Color.WHITE);
        estado.setText("Nuevo archivo");
    }

    private void abrir() {
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = selector.getSelectedFile();
        try {
            String contenido = new String(Files.readAllBytes(archivo.toPath()), StandardCharsets.UTF_8);
            contenido = contenido.replace("\r\n", "\n").replace('\r', '\n');
            editor.getDocument().remove(0, editor.getDocument().getLength());
            editor.getDocument().insertString(0, contenido, estiloNormal);
            editor.setCaretPosition(0);
            archivoActual = archivo;
            setTitle(TITULO + " - " + archivo.getName());
            estado.setForeground(Color.WHITE);
            estado.setText("Archivo cargado: " + archivo.getAbsolutePath());
        } catch (IOException e) {
            mostrarError("No se pudo abrir el archivo:\n" + e.getMessage());
        } catch (BadLocationException e) {
            mostrarError("No se pudo cargar el archivo en el editor.");
        }
    }

    private void guardar() {
        if (archivoActual == null) {
            if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            archivoActual = selector.getSelectedFile();
            setTitle(TITULO + " - " + archivoActual.getName());
        }
        try {
            Files.write(archivoActual.toPath(), textoDelEditor().getBytes(StandardCharsets.UTF_8));
            estado.setForeground(Color.WHITE);
            estado.setText("Archivo guardado: " + archivoActual.getAbsolutePath());
        } catch (IOException e) {
            mostrarError("No se pudo guardar el archivo:\n" + e.getMessage());
        }
    }

    private void compilar() {
        try {
            Analizador.Resultado resultado = Analizador.analizar(textoDelEditor());
            escribirSalida(resultado);

            modeloTabla.setRowCount(0);
            for (TablaSimbolos.Simbolo s : resultado.tabla.getSimbolos()) {
                modeloTabla.addRow(new Object[] { s.nombre, s.token, s.tipo, s.valor, s.longitud });
            }

            /* ts.txt se guarda junto al archivo abierto, o en la carpeta de trabajo */
            File carpeta = (archivoActual != null) ? archivoActual.getAbsoluteFile().getParentFile()
                    : new File(".").getAbsoluteFile();
            File ts = new File(carpeta, "ts.txt");
            resultado.tabla.guardar(ts.getPath());

            mostrarPestania(true);
            estado.setForeground(resultado.cantidadErrores > 0 ? new Color(0xFFB59A) : TURQUESA);
            estado.setText("Compilación finalizada: " + resultado.cantidadTokens + " tokens, "
                    + resultado.cantidadErrores + " errores. Tabla de símbolos en "
                    + ts.getCanonicalPath());
        } catch (IOException e) {
            mostrarError("Error durante la compilación:\n" + e.getMessage());
        }
    }

    /** Escribe tokens y errores en orden; los errores van en rojo. */
    private void escribirSalida(Analizador.Resultado resultado) {
        salida.setText("");
        StyledDocument documento = salida.getStyledDocument();
        try {
            for (Token t : resultado.elementos) {
                if (t.esError) {
                    agregar(documento, String.format("Línea %-4d ERROR LÉXICO: %s%n", t.linea, t.lexema),
                            estiloErrorSalida);
                } else {
                    agregar(documento, String.format("Línea %-4d ", t.linea), estiloLinea);
                    agregar(documento, String.format("%-20s ", t.nombre), estiloReservada);
                    agregar(documento, t.lexema + "\n", estiloNormal);
                }
            }
            agregar(documento, String.format("%nAnálisis léxico finalizado: %d tokens reconocidos, ",
                    resultado.cantidadTokens), estiloResumen);
            agregar(documento, resultado.cantidadErrores + " errores.\n",
                    resultado.cantidadErrores > 0 ? estiloErrorSalida : estiloResumen);
        } catch (BadLocationException e) {
            salida.setText(resultado.comoTexto());
        }
        salida.setCaretPosition(0);
    }

    private void agregar(StyledDocument documento, String texto, SimpleAttributeSet estilo)
            throws BadLocationException {
        documento.insertString(documento.getLength(), texto.replace("\r\n", "\n"), estilo);
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /* ====================== Componentes propios ====================== */

    /** Panel de texto con estilos que no corta las líneas largas. */
    private static class PanelSinAjuste extends JTextPane {
        public boolean getScrollableTracksViewportWidth() {
            return getParent() == null
                    || getUI().getPreferredSize(this).width <= getParent().getSize().width;
        }
    }

    /** Botón plano con el contorno grueso del logo. */
    private static class BotonPlano extends JButton {

        static final int NORMAL = 0;
        static final int PRINCIPAL = 1;
        static final int PESTANIA = 2;

        private final int tipo;
        private boolean seleccionado = false;

        BotonPlano(String texto, int tipo) {
            super(texto);
            this.tipo = tipo;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(FUENTE_INTERFAZ.deriveFont(Font.BOLD));
            setForeground(TINTA);
            if (tipo == PRINCIPAL) {
                setBorder(BorderFactory.createEmptyBorder(9, 22, 9, 22));
            } else {
                setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            }
        }

        void setSeleccionado(boolean seleccionado) {
            this.seleccionado = seleccionado;
            setForeground(seleccionado ? Color.WHITE : TINTA);
            repaint();
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean encima = getModel().isRollover();
            boolean presionado = getModel().isPressed();

            Color relleno;
            if (tipo == PRINCIPAL) {
                relleno = presionado ? new Color(0xE5682F) : (encima ? new Color(0xFF9160) : NARANJA);
            } else if (tipo == PESTANIA) {
                relleno = seleccionado ? TINTA : (encima ? MARGEN : PAPEL);
            } else {
                relleno = presionado ? TURQUESA : (encima ? MARGEN : PAPEL);
            }
            int arco = 12;
            g2.setColor(relleno);
            g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arco, arco);
            g2.setColor(TINTA);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arco, arco);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
