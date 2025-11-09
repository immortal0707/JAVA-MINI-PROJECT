import java.awt.*;
import javax.swing.*;

public class MainFrame extends JFrame {
    public MainFrame() {
        setTitle("Event Management System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Add panels to tabs
        tabbedPane.addTab("Events", new EventsPanel());
        tabbedPane.addTab("Registrations", new RegistrationsPanel());
        tabbedPane.addTab("Students", new StudentsPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}