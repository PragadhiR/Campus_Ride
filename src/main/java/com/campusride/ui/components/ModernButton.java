package com.campusride.ui.components;

import com.campusride.util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modern flat rounded button with hover and press effects.
 */
public class ModernButton extends JButton {
    public enum Variant {
        PRIMARY, SECONDARY, SUCCESS, DANGER, OUTLINE
    }

    private final Variant variant;
    private boolean isHovered = false;
    private boolean isPressed = false;

    public ModernButton(String text) {
        this(text, Variant.PRIMARY);
    }

    public ModernButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setFont(UITheme.FONT_BODY_BOLD);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(8, 16, 8, 16));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bgColor;
        Color fgColor = UITheme.TEXT_PRIMARY;

        switch (variant) {
            case SUCCESS -> {
                bgColor = isPressed ? new Color(5, 150, 105) : (isHovered ? new Color(16, 185, 129) : new Color(5, 150, 105));
            }
            case DANGER -> {
                bgColor = isPressed ? new Color(185, 28, 28) : (isHovered ? new Color(220, 38, 38) : new Color(239, 68, 68));
            }
            case SECONDARY -> {
                bgColor = isPressed ? new Color(30, 41, 59) : (isHovered ? new Color(51, 65, 85) : new Color(40, 53, 76));
                fgColor = UITheme.TEXT_SECONDARY;
            }
            case OUTLINE -> {
                bgColor = isHovered ? new Color(30, 41, 59) : new Color(0, 0, 0, 0);
                fgColor = UITheme.PRIMARY;
            }
            default -> { // PRIMARY
                bgColor = isPressed ? UITheme.PRIMARY_HOVER : (isHovered ? new Color(2, 132, 199) : UITheme.PRIMARY);
            }
        }

        if (!isEnabled()) {
            bgColor = new Color(51, 65, 85);
            fgColor = UITheme.TEXT_MUTED;
        }

        // Fill rounded background
        g2.setColor(bgColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

        // Draw border for OUTLINE
        if (variant == Variant.OUTLINE) {
            g2.setColor(UITheme.PRIMARY);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        }

        // Draw Text
        g2.setFont(getFont());
        g2.setColor(fgColor);
        FontMetrics fm = g2.getFontMetrics();
        int textX = (getWidth() - fm.stringWidth(getText())) / 2;
        int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width + 24, 90), Math.max(d.height + 12, 38));
    }
}
