package compilador;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada.
 * Sin argumentos abre el IDE. Con un archivo como argumento lo analiza
 * por consola y genera ts.txt (útil para probar sin interfaz).
 */
public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            String codigo = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
            Analizador.Resultado resultado = Analizador.analizar(codigo);
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
            System.out.print(resultado.comoTexto());
            resultado.tabla.guardar("ts.txt");
            System.out.println("Tabla de símbolos guardada en ts.txt");
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    /* se usa el aspecto por defecto */
                }
                new Ventana().setVisible(true);
            }
        });
    }
}
