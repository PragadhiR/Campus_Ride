package com.campusride.ui.components;

import com.campusride.util.UITheme;

import javax.swing.*;
import java.awt.*;

/**
 * Modern metrics card for the dashboard displaying aggregate statistics.
 */
public class StatCard extends JPanel {
    private final JLabel lblTitle;
    private final JLabel lblValue;
    private final JLabel lblSubtitle;

    public StatCard(String title, String initialValue, String subtitle, Color accentColor) {
        setLayout(new BorderLayout(8, 8));
        setBackground(UITheme.BG_CARD);
        setBorder(UITheme.createCardBorder());
        setPreferredSize(new Dimension(210, 110));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(UITheme.FONT_SMALL);
        lblTitle.setForeground(UITheme.TEXT_MUTED);
        headerPanel.add(lblTitle, BorderLayout.WEST);

        // Small colored accent dot or bar
        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accentColor);
                g2.fillOval(0, 2, 8, 8);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(10, 12));
        dot.setOpaque(false);
        headerPanel.add(dot, BorderLayout.EAST);

        lblValue = new JLabel(initialValue);
        lblValue.setFont(UITheme.FONT_STAT_NUM);
        lblValue.setForeground(UITheme.TEXT_PRIMARY);

        lblSubtitle = new JLabel(subtitle);
        lblSubtitle.setFont(UITheme.FONT_SMALL);
        lblSubtitle.setForeground(UITheme.TEXT_SECONDARY);

        add(headerPanel, BorderLayout.NORTH);
        add(lblValue, BorderLayout.CENTER);
        add(lblSubtitle, BorderLayout.SOUTH);
    }

    public void setValue(String value) {
        lblValue.setText(value);
    }

    public void setSubtitle(String subtitle) {
        lblSubtitle.setText(subtitle);
    }
}
