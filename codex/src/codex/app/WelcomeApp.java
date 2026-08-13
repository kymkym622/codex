package codex.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class WelcomeApp {
    private static final int MIN_COUNT = 2;
    private static final int MAX_COUNT = 20;

    private final JFrame frame = new JFrame("무작위 텍스트 뽑기");
    private final JPanel fieldsPanel = new JPanel();
    private final List<JTextField> textFields = new ArrayList<>();

    private WelcomeApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WelcomeApp().showWindow());
    }

    private void showWindow() {
        UIManager.put("OptionPane.messageFont", new Font("Dialog", Font.PLAIN, 16));

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(500, 420));

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(new Color(241, 245, 255));
        content.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        JComboBox<Integer> countSelector = new JComboBox<>(createCountOptions());
        countSelector.setFont(new Font("Dialog", Font.PLAIN, 15));
        countSelector.addActionListener(event -> {
            Integer selectedCount = (Integer) countSelector.getSelectedItem();
            if (selectedCount != null) {
                rebuildTextFields(selectedCount);
            }
        });

        JPanel selectorPanel = new JPanel(new BorderLayout(12, 0));
        selectorPanel.setOpaque(false);
        selectorPanel.add(new JLabel("입력칸 개수"), BorderLayout.WEST);
        selectorPanel.add(countSelector, BorderLayout.CENTER);

        fieldsPanel.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(fieldsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(241, 245, 255));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JButton pickButton = new JButton("무작위로 하나 뽑기");
        pickButton.setFont(new Font("Dialog", Font.BOLD, 16));
        pickButton.setPreferredSize(new Dimension(220, 52));
        pickButton.setFocusPainted(false);
        pickButton.addActionListener(event -> showRandomValue());

        content.add(selectorPanel, BorderLayout.NORTH);
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(pickButton, BorderLayout.SOUTH);

        rebuildTextFields(MIN_COUNT);
        frame.setContentPane(content);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static Integer[] createCountOptions() {
        Integer[] options = new Integer[MAX_COUNT - MIN_COUNT + 1];
        for (int index = 0; index < options.length; index++) {
            options[index] = MIN_COUNT + index;
        }
        return options;
    }

    private void rebuildTextFields(int count) {
        textFields.clear();
        fieldsPanel.removeAll();
        fieldsPanel.setLayout(new GridLayout(count, 1, 0, 10));

        for (int index = 0; index < count; index++) {
            JTextField textField = new JTextField();
            textField.setFont(new Font("Dialog", Font.PLAIN, 15));
            textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(196, 205, 226)),
                    BorderFactory.createEmptyBorder(9, 10, 9, 10)));
            textFields.add(textField);

            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);
            row.add(new JLabel((index + 1) + "."), BorderLayout.WEST);
            row.add(textField, BorderLayout.CENTER);
            fieldsPanel.add(row);
        }

        fieldsPanel.revalidate();
        fieldsPanel.repaint();
    }

    private void showRandomValue() {
        String selectedValue = selectRandomValue(textFields);

        JOptionPane.showMessageDialog(
                frame,
                selectedValue,
                "뽑기 결과",
                JOptionPane.INFORMATION_MESSAGE);
    }

    static String selectRandomValue(List<JTextField> fields) {
        int selectedIndex = ThreadLocalRandom.current().nextInt(fields.size());
        return fields.get(selectedIndex).getText();
    }
}
