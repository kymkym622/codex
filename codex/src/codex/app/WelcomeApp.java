package codex.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class WelcomeApp {
    private WelcomeApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WelcomeApp::showWindow);
    }

    private static void showWindow() {
        UIManager.put("OptionPane.messageFont", new Font("Dialog", Font.PLAIN, 16));

        JFrame frame = new JFrame("환영 GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(420, 280));

        JPanel content = new JPanel(new BorderLayout(0, 28));
        content.setBackground(new Color(241, 245, 255));
        content.setBorder(BorderFactory.createEmptyBorder(42, 48, 42, 48));

        JLabel title = new JLabel("반갑습니다", SwingConstants.CENTER);
        title.setForeground(new Color(31, 41, 67));
        title.setFont(new Font("Dialog", Font.BOLD, 30));

        JButton welcomeButton = new JButton("환영 메시지 보기");
        welcomeButton.setFont(new Font("Dialog", Font.BOLD, 16));
        welcomeButton.setPreferredSize(new Dimension(210, 52));
        welcomeButton.setFocusPainted(false);
        welcomeButton.addActionListener(event -> JOptionPane.showMessageDialog(
                frame,
                "환영합니다!",
                "환영 메시지",
                JOptionPane.INFORMATION_MESSAGE));

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.add(welcomeButton);

        content.add(title, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);

        frame.setContentPane(content);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
