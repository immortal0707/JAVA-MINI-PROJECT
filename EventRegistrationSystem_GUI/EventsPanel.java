import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class EventsPanel extends JPanel {
    private DefaultTableModel tableModel;
    private JTable eventsTable;

    public EventsPanel() {
        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(new Object[]{"Event ID", "Title", "Date", "Time", "Venue", "Capacity"}, 0);
        eventsTable = new JTable(tableModel);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addEventButton = new JButton("Add Event");
        addEventButton.addActionListener(e -> showAddEventDialog());

        buttonPanel.add(addEventButton);
        add(buttonPanel, BorderLayout.NORTH);
        add(new JScrollPane(eventsTable), BorderLayout.CENTER);

        refreshEvents();
    }

    private void refreshEvents() {
        tableModel.setRowCount(0);
        try (Connection conn = DB.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM events");

            List<Event> events = new ArrayList<>();
            while (rs.next()) {
                events.add(new Event(
                        rs.getInt("event_id"),
                        rs.getString("title"),
                        rs.getDate("event_date"),
                        rs.getTime("event_time"),
                        rs.getString("venue"),
                        rs.getInt("capacity")
                ));
            }

            for (Event event : events) {
                tableModel.addRow(new Object[]{
                        event.getEventId(),
                        event.getTitle(),
                        event.getDate(),
                        event.getTime(),
                        event.getVenue(),
                        event.getCapacity()
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAddEventDialog() {
        JTextField titleField = new JTextField(20);
        JTextField dateField = new JTextField(20);
        JTextField timeField = new JTextField(20);
        JTextField venueField = new JTextField(20);
        JTextField capacityField = new JTextField(5);

        JPanel panel = new JPanel(new GridLayout(5, 2));
        panel.add(new JLabel("Event Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Event Date (yyyy-MM-dd):"));
        panel.add(dateField);
        panel.add(new JLabel("Event Time (HH:mm:ss):"));
        panel.add(timeField);
        panel.add(new JLabel("Venue:"));
        panel.add(venueField);
        panel.add(new JLabel("Capacity:"));
        panel.add(capacityField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Event", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String title = titleField.getText().trim();
            String dateStr = dateField.getText().trim();
            String timeStr = timeField.getText().trim();
            String venue = venueField.getText().trim();
            String capacityStr = capacityField.getText().trim();

            if (title.isEmpty() || dateStr.isEmpty() || timeStr.isEmpty() || venue.isEmpty() || capacityStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int capacity = Integer.parseInt(capacityStr);
                java.sql.Date sqlDate = java.sql.Date.valueOf(dateStr);
                java.sql.Time sqlTime = java.sql.Time.valueOf(timeStr);

                try (Connection conn = DB.getConnection()) {
                    try (PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO events (title, event_date, event_time, venue, capacity) VALUES (?, ?, ?, ?, ?)")) {
                        insertStmt.setString(1, title);
                        insertStmt.setDate(2, sqlDate);
                        insertStmt.setTime(3, sqlTime);
                        insertStmt.setString(4, venue);
                        insertStmt.setInt(5, capacity);
                        insertStmt.executeUpdate();
                    }
                    JOptionPane.showMessageDialog(this, "Event added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    refreshEvents();
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Capacity must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error saving event: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    class Event {
        private final int eventId;
        private final String title;
        private final java.sql.Date date;
        private final java.sql.Time time;
        private final String venue;
        private final int capacity;

        public Event(int eventId, String title, java.sql.Date date, java.sql.Time time, String venue, int capacity) {
            this.eventId = eventId;
            this.title = title;
            this.date = date;
            this.time = time;
            this.venue = venue;
            this.capacity = capacity;
        }

        public int getEventId() {
            return eventId;
        }

        public String getTitle() {
            return title;
        }

        public java.sql.Date getDate() {
            return date;
        }

        public java.sql.Time getTime() {
            return time;
        }

        public String getVenue() {
            return venue;
        }

        public int getCapacity() {
            return capacity;
        }
    }
}