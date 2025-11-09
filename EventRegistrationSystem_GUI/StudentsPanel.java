import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class StudentsPanel extends JPanel {
    private JTable studentsTable;
    private DefaultTableModel tableModel;

    public StudentsPanel() {
        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(new Object[]{"Student ID", "Name", "Email"}, 0);
        studentsTable = new JTable(tableModel);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Add Student");
        JButton refreshButton = new JButton("Refresh");

        addButton.addActionListener(e -> showAddStudentDialog());
        refreshButton.addActionListener(e -> refreshStudents());

        buttonPanel.add(addButton);
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);

        add(new JScrollPane(studentsTable), BorderLayout.CENTER);

        refreshStudents();
    }

    private void refreshStudents() {
        tableModel.setRowCount(0);
        try (Connection conn = DB.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM students");

            List<Student> students = new ArrayList<>();
            while (rs.next()) {
                students.add(new Student(
                        rs.getInt("student_id"),
                        rs.getString("name"),
                        rs.getString("email")
                ));
            }

            Collections.sort(students, (s1, s2) -> s1.getName().compareTo(s2.getName()));

            int newId = 1;
            for (Student student : students) {
                tableModel.addRow(new Object[]{
                        newId++,
                        student.getName(),
                        student.getEmail()
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAddStudentDialog() {
        JTextField nameField = new JTextField(20);
        JTextField emailField = new JTextField(20);

        JLabel nameLabel = new JLabel("Name:");
        JLabel emailLabel = new JLabel("Email:");
        JLabel emailHint = new JLabel("Enter valid email");
        emailHint.setForeground(Color.GRAY);
        emailHint.setFont(new Font("Arial", Font.ITALIC, 10));

        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(nameLabel);
        panel.add(nameField);
        panel.add(emailLabel);
        panel.add(emailField);
        panel.add(new JLabel()); // empty label for alignment
        panel.add(emailHint);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Student", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();

            if (name.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Regex validation
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                JOptionPane.showMessageDialog(this, "Invalid email format. Please enter a valid email.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try (Connection conn = DB.getConnection()) {
                PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM students WHERE email = ?");
                check.setString(1, email);
                ResultSet rs = check.executeQuery();
                rs.next();
                if (rs.getInt(1) > 0) {
                    JOptionPane.showMessageDialog(this, "Student with this email already exists.", "Duplicate Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                PreparedStatement stmt = conn.prepareStatement("INSERT INTO students (name, email) VALUES (?, ?)");
                stmt.setString(1, name);
                stmt.setString(2, email);
                stmt.executeUpdate();
                refreshStudents();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    class Student {
        private int studentId;
        private String name;
        private String email;

        public Student(int studentId, String name, String email) {
            this.studentId = studentId;
            this.name = name;
            this.email = email;
        }

        public int getStudentId() {
            return studentId;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }
}