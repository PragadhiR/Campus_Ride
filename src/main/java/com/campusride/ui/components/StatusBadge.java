package com.campusride.ui.components;

import com.campusride.util.UITheme;

import javax.swing.*;
import java.awt.*;

/**
 * Renders a compact pill badge for ride and booking statuses.
 */
public class StatusBadge extends JLabel {

    public StatusBadge(String status) {
        super(status != null ? status.toUpperCase() : "UNKNOWN", SwingConstants.CENTER);
        setFont(UITheme.FONT_SMALL);
        setOpaque(false);
        updateColors(status);
    }

    public void updateColors(String status) {
        setText(status != null ? status.toUpperCase() : "UNKNOWN");
        Color bg = UITheme.BG_CARD;
        Color fg = UITheme.TEXT_SECONDARY;

        if (status != null) {
            switch (status.trim().toUpperCase()) {
                case "OPEN", "CONFIRMED" -> {
                    bg = UITheme.STATUS_OPEN_BG;
                    fg = UITheme.STATUS_OPEN_FG;
                }
                case "FULL" -> {
                    bg = UITheme.STATUS_FULL_BG;
                    fg = UITheme.STATUS_FULL_FG;
                }
                case "CANCELLED" -> {
                    bg = UITheme.STATUS_CANCELLED_BG;
                    fg = UITheme.STATUS_CANCELLED_FG;
                }
                case "COMPLETED" -> {
                    bg = UITheme.STATUS_COMPLETED_BG;
                    fg = UITheme.STATUS_COMPLETED_FG;
                }
            }
        }
        setBackground(bg);
        setForeground(fg);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Rounded pill background
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

        // Text
        g2.setColor(getForeground());
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(getText())) / 2;
        int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(getText(), x, y);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int w = fm.stringWidth(getText()) + 16;
        int h = fm.getHeight() + 6;
        return new Dimension(Math.max(w, 65), Math.max(h, 22));
    }
}
