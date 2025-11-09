import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;

public class RegistrationsPanel extends JPanel {
    private DefaultTableModel tableModel;
    private JTable table;
    private JComboBox<EventItem> eventCombo;
    private JTextField searchField;
    private JButton searchButton;
    private JList<String> suggestionList;
    private JPopupMenu suggestionPopup;

    public RegistrationsPanel() {
        setLayout(new BorderLayout());

        // Table setup
        tableModel = new DefaultTableModel(new Object[]{"Student ID", "Name", "Email", "Event Title"}, 0);
        table = new JTable(tableModel);

        // Event combo box and search button
        eventCombo = new JComboBox<>();
        eventCombo.addActionListener(e -> loadRegistrations());

        searchField = new JTextField(15);
        searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchStudentEvents());

        // Suggestion list popup for search
        suggestionList = new JList<>();
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                String selected = suggestionList.getSelectedValue();
                if (selected != null) {
                    searchField.setText(selected.split(" \\(ID:")[0]);
                    suggestionPopup.setVisible(false);
                    searchStudentEvents();
                }
            }
        });

        suggestionPopup = new JPopupMenu();
        suggestionPopup.setFocusable(false);
        suggestionPopup.add(new JScrollPane(suggestionList));

        // Document listener for search field to show suggestions
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateSuggestions(); }
            public void removeUpdate(DocumentEvent e) { updateSuggestions(); }
            public void insertUpdate(DocumentEvent e) { updateSuggestions(); }
        });

        // Buttons for actions
        JButton registerBtn = new JButton("Register");
        JButton refreshBtn = new JButton("Refresh");
        JButton deleteBtn = new JButton("Delete Registration");

        registerBtn.addActionListener(e -> registerStudent());
        refreshBtn.addActionListener(e -> refresh());
        deleteBtn.addActionListener(e -> deleteRegistration());

        // Top panel with controls
        JPanel top = new JPanel(new FlowLayout());
        top.add(new JLabel("Select Event:"));
        top.add(eventCombo);
        top.add(new JLabel("Search Name:"));
        top.add(searchField);
        top.add(searchButton);
        top.add(registerBtn);
        top.add(refreshBtn);
        top.add(deleteBtn);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        refresh();  // Initial data load
    }

    private void refresh() {
        eventCombo.removeAllItems();
        try (Connection conn = DB.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM events ORDER BY title");
            while (rs.next()) {
                eventCombo.addItem(new EventItem(rs.getInt("event_id"), rs.getString("title")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        loadRegistrations();
    }

    private void loadRegistrations() {
        tableModel.setRowCount(0);
        EventItem selected = (EventItem) eventCombo.getSelectedItem();
        if (selected == null) return;

        try (Connection conn = DB.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT s.student_id, s.name, s.email, e.title FROM students s " +
                "JOIN registrations r ON s.student_id = r.student_id " +
                "JOIN events e ON r.event_id = e.event_id WHERE r.event_id = ? ORDER BY s.name");
            stmt.setInt(1, selected.id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("student_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("title")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateSuggestions() {
        String query = searchField.getText().toLowerCase();
        if (query.isEmpty()) {
            suggestionPopup.setVisible(false);
            return;
        }

        ArrayList<String> suggestions = new ArrayList<>();
        try (Connection conn = DB.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT student_id, name FROM students WHERE LOWER(name) LIKE ?");
            stmt.setString(1, "%" + query + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                suggestions.add(rs.getString("name") + " (ID: " + rs.getInt("student_id") + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (!suggestions.isEmpty()) {
            suggestionList.setListData(suggestions.toArray(new String[0]));
            suggestionPopup.show(searchField, 0, searchField.getHeight());
        } else {
            suggestionPopup.setVisible(false);
        }
    }

    private void searchStudentEvents() {
        tableModel.setRowCount(0);
        String query = searchField.getText().toLowerCase();
        if (query.isEmpty()) return;

        try (Connection conn = DB.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT s.student_id, s.name, s.email, e.title FROM students s " +
                "JOIN registrations r ON s.student_id = r.student_id " +
                "JOIN events e ON r.event_id = e.event_id " +
                "WHERE LOWER(s.name) LIKE ? ORDER BY e.title");
            stmt.setString(1, "%" + query + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("student_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("title")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void registerStudent() {
        EventItem selected = (EventItem) eventCombo.getSelectedItem();
        if (selected == null) return;

        DefaultComboBoxModel<StudentItem> studentModel = new DefaultComboBoxModel<>();
        try (Connection conn = DB.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT student_id, name FROM students WHERE student_id NOT IN " +
                "(SELECT student_id FROM registrations WHERE event_id = ?)");
            stmt.setInt(1, selected.id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                studentModel.addElement(new StudentItem(rs.getInt("student_id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (studentModel.getSize() == 0) {
            JOptionPane.showMessageDialog(this, "All students already registered.");
            return;
        }

        JComboBox<StudentItem> studentCombo = new JComboBox<>(studentModel);
        JPanel panel = new JPanel();
        panel.add(new JLabel("Student:"));
        panel.add(studentCombo);

        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Register Student",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            StudentItem student = (StudentItem) studentCombo.getSelectedItem();
            try (Connection conn = DB.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO registrations (student_id, event_id) VALUES (?, ?)");
                stmt.setInt(1, student.id);
                stmt.setInt(2, selected.id);
                stmt.executeUpdate();
                loadRegistrations();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteRegistration() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a registration to delete.");
            return;
        }

        int studentId = (int) tableModel.getValueAt(row, 0);
        EventItem selected = (EventItem) eventCombo.getSelectedItem();
        if (selected == null) return;

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this registration?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DB.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM registrations WHERE student_id = ? AND event_id = ?");
                stmt.setInt(1, studentId);
                stmt.setInt(2, selected.id);
                stmt.executeUpdate();
                loadRegistrations();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Helper classes
    private static class EventItem {
        int id;
        String title;
        EventItem(int id, String title) {
            this.id = id;
            this.title = title;
        }
        public String toString() {
            return title;
        }
    }

    private static class StudentItem {
        int id;
        String name;
        StudentItem(int id, String name) {
            this.id = id;
            this.name = name;
        }
        public String toString() {
            return name + " (ID: " + id + ")";
        }
    }
}