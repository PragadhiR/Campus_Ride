package com.campusride.util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Centralized Modern Dark Theme design system for CampusRide.
 * Provides colors, fonts, borders, and component styling.
 */
public class UITheme {
    // Brand Colors
    public static final Color BG_DARKEST = new Color(15, 23, 42);   // Slate 900
    public static final Color BG_DARK = new Color(24, 34, 53);      // Slate 850
    public static final Color BG_CARD = new Color(30, 41, 59);      // Slate 800
    public static final Color BG_INPUT = new Color(40, 53, 76);     // Slate 750
    public static final Color BG_SIDEBAR = new Color(17, 24, 39);   // Gray 900
    public static final Color BORDER_COLOR = new Color(51, 65, 85); // Slate 700

    // Accents
    public static final Color PRIMARY = new Color(14, 165, 233);    // Sky 500
    public static final Color PRIMARY_HOVER = new Color(2, 132, 199);
    public static final Color ACCENT_TEAL = new Color(20, 184, 166); // Teal 500
    public static final Color ACCENT_CYAN = new Color(6, 182, 212);  // Cyan 500

    // Text Colors
    public static final Color TEXT_PRIMARY = new Color(248, 250, 252);   // Slate 50
    public static final Color TEXT_SECONDARY = new Color(148, 163, 184); // Slate 400
    public static final Color TEXT_MUTED = new Color(100, 116, 139);     // Slate 500

    // Status Colors
    public static final Color STATUS_OPEN_BG = new Color(6, 78, 59);
    public static final Color STATUS_OPEN_FG = new Color(52, 211, 153);

    public static final Color STATUS_FULL_BG = new Color(120, 53, 15);
    public static final Color STATUS_FULL_FG = new Color(251, 191, 36);

    public static final Color STATUS_CANCELLED_BG = new Color(127, 29, 29);
    public static final Color STATUS_CANCELLED_FG = new Color(248, 113, 113);

    public static final Color STATUS_COMPLETED_BG = new Color(30, 58, 138);
    public static final Color STATUS_COMPLETED_FG = new Color(96, 165, 250);

    // Typography
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 17);
    public static final Font FONT_SUBHEADER = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_STAT_NUM = new Font("Segoe UI", Font.BOLD, 26);

    /**
     * Styles a standard JTable to match the sleek dark theme.
     */
    public static void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setFont(FONT_BODY);
        table.setRowHeight(36);
        table.setShowGrid(true);
        table.setGridColor(BORDER_COLOR);
        table.setSelectionBackground(new Color(45, 62, 88));
        table.setSelectionForeground(TEXT_PRIMARY);

        JTableHeader header = table.getTableHeader();
        header.setBackground(BG_DARK);
        header.setForeground(TEXT_SECONDARY);
        header.setFont(FONT_SUBHEADER);
        header.setPreferredSize(new Dimension(0, 40));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        // Center align table headers
        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) header.getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
    }

    /**
     * Styles standard input fields with dark backgrounds, light text, and clean borders.
     */
    public static void styleTextField(JTextField tf) {
        tf.setBackground(BG_INPUT);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(TEXT_PRIMARY);
        tf.setFont(FONT_BODY);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
    }

    /**
     * Styles standard combo boxes with dark background, crisp light text,
     * modern arrow button, dark-themed popup list with selection highlights,
     * and readable editor components.
     * Explicitly overrides paintCurrentValueBackground and cell renderer so the closed
     * combo box renders the selected item with a dark navy background and clearly visible light text.
     */
    public static <T> void styleComboBox(JComboBox<T> cb) {
        UIManager.put("ComboBox.background", BG_INPUT);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", PRIMARY);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);

        cb.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton("▼");
                btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                btn.setForeground(TEXT_SECONDARY);
                btn.setBackground(BG_INPUT);
                btn.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 8));
                btn.setContentAreaFilled(false);
                btn.setFocusPainted(false);
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                return btn;
            }

            @Override
            protected ComboPopup createPopup() {
                BasicComboPopup popup = (BasicComboPopup) super.createPopup();
                popup.getList().setBackground(BG_CARD);
                popup.getList().setForeground(TEXT_PRIMARY);
                popup.getList().setSelectionBackground(PRIMARY);
                popup.getList().setSelectionForeground(Color.WHITE);
                popup.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
                return popup;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                Color oldColor = g.getColor();
                g.setColor(comboBox.getBackground() != null ? comboBox.getBackground() : BG_INPUT);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                g.setColor(oldColor);
            }

            @Override
            protected ComboBoxEditor createEditor() {
                BasicComboBoxEditor editor = new BasicComboBoxEditor();
                if (editor.getEditorComponent() instanceof JTextField) {
                    styleTextField((JTextField) editor.getEditorComponent());
                }
                return editor;
            }
        });

        cb.setBackground(BG_INPUT);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFont(FONT_BODY);
        cb.setOpaque(true);
        cb.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(4, 8, 4, 4)
        ));

        if (cb.isEditable() && cb.getEditor() != null && cb.getEditor().getEditorComponent() instanceof JTextField) {
            styleTextField((JTextField) cb.getEditor().getEditorComponent());
        }
        cb.addPropertyChangeListener("editable", evt -> {
            if (cb.isEditable() && cb.getEditor() != null && cb.getEditor().getEditorComponent() instanceof JTextField) {
                styleTextField((JTextField) cb.getEditor().getEditorComponent());
            }
        });

        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setFont(FONT_BODY);
                l.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                String text = (value != null) ? value.toString() : "";
                l.setText(text);

                if (index == -1) {
                    // Closed combo box: dark navy background with clearly visible light text
                    l.setOpaque(true);
                    l.setBackground(cb.getBackground() != null ? cb.getBackground() : BG_INPUT);
                    l.setForeground(TEXT_PRIMARY);
                } else {
                    // Inside dropdown popup
                    l.setOpaque(true);
                    if (isSelected) {
                        l.setBackground(PRIMARY);
                        l.setForeground(Color.WHITE);
                    } else {
                        l.setBackground(BG_CARD);
                        l.setForeground(TEXT_PRIMARY);
                    }
                }
                return l;
            }
        });
    }

    /**
     * Creates a rounded card panel border with padding.
     */
    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(16, 16, 16, 16)
        );
    }
}
