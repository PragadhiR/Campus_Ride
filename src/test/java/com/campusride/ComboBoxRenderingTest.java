package com.campusride;

import com.campusride.model.Destination;
import com.campusride.util.UITheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ComboBox Rendering and Dark Theme Selection Tests")
public class ComboBoxRenderingTest {

    @BeforeAll
    public static void setUpLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Department combo box visibly displays selected text with primary light foreground")
    public void testDepartmentComboBoxRendering() {
        String[] departments = {
            "Computer Science", "Information Science", "Electronics & Comm",
            "Electrical Eng", "Mechanical Eng", "Civil Eng", "Biotech", "Business School"
        };
        JComboBox<String> cb = new JComboBox<>(departments);
        UITheme.styleComboBox(cb);

        cb.setSelectedIndex(0);
        assertEquals("Computer Science", cb.getSelectedItem());

        // Test closed combo box renderer (index -1)
        ListCellRenderer<? super String> renderer = cb.getRenderer();
        Component comp = renderer.getListCellRendererComponent(new JList<>(), (String) cb.getSelectedItem(), -1, false, false);
        assertInstanceOf(JLabel.class, comp);

        JLabel label = (JLabel) comp;
        assertEquals("Computer Science", label.getText());
        assertEquals(UITheme.TEXT_PRIMARY, label.getForeground());
        assertTrue(label.isOpaque(), "Closed combo renderer should be opaque with dark input background");
        assertEquals(UITheme.BG_INPUT, label.getBackground());
        assertEquals(UITheme.BG_INPUT, cb.getBackground());

        // Test changing selection to Mechanical Eng
        cb.setSelectedItem("Mechanical Eng");
        assertEquals("Mechanical Eng", cb.getSelectedItem());

        Component updatedComp = renderer.getListCellRendererComponent(new JList<>(), (String) cb.getSelectedItem(), -1, false, false);
        JLabel updatedLabel = (JLabel) updatedComp;
        assertEquals("Mechanical Eng", updatedLabel.getText());
        assertEquals(UITheme.TEXT_PRIMARY, updatedLabel.getForeground());
        assertEquals(UITheme.BG_INPUT, updatedLabel.getBackground());
    }

    @Test
    @DisplayName("Gender combo box visibly displays selected text with primary light foreground")
    public void testGenderComboBoxRendering() {
        String[] genders = {"Female", "Male", "Non-Binary", "Prefer not to say"};
        JComboBox<String> cb = new JComboBox<>(genders);
        UITheme.styleComboBox(cb);

        cb.setSelectedItem("Female");
        ListCellRenderer<? super String> renderer = cb.getRenderer();
        Component comp = renderer.getListCellRendererComponent(new JList<>(), (String) cb.getSelectedItem(), -1, false, false);
        JLabel label = (JLabel) comp;

        assertEquals("Female", label.getText());
        assertEquals(UITheme.TEXT_PRIMARY, label.getForeground());
        assertEquals(UITheme.BG_INPUT, label.getBackground());

        cb.setSelectedItem("Prefer not to say");
        Component comp2 = renderer.getListCellRendererComponent(new JList<>(), (String) cb.getSelectedItem(), -1, false, false);
        JLabel label2 = (JLabel) comp2;
        assertEquals("Prefer not to say", label2.getText());
        assertEquals(UITheme.TEXT_PRIMARY, label2.getForeground());
        assertEquals(UITheme.BG_INPUT, label2.getBackground());
    }

    @Test
    @DisplayName("Destination combo box correctly renders Destination model objects")
    public void testDestinationComboBoxRendering() {
        Destination dest = new Destination(1, "College Campus", "Campus", true);
        JComboBox<Destination> cb = new JComboBox<>(new Destination[]{dest});
        UITheme.styleComboBox(cb);

        cb.setSelectedIndex(0);
        ListCellRenderer<? super Destination> renderer = cb.getRenderer();
        Component comp = renderer.getListCellRendererComponent(new JList<>(), (Destination) cb.getSelectedItem(), -1, false, false);
        JLabel label = (JLabel) comp;

        assertEquals("College Campus", label.getText());
        assertEquals(UITheme.TEXT_PRIMARY, label.getForeground());
        assertEquals(UITheme.BG_INPUT, label.getBackground());
    }

    @Test
    @DisplayName("Popup items have appropriate dark background when unselected and primary when selected")
    public void testPopupItemsRendering() {
        JComboBox<String> cb = new JComboBox<>(new String[]{"Option 1", "Option 2"});
        UITheme.styleComboBox(cb);

        ListCellRenderer<? super String> renderer = cb.getRenderer();
        JList<String> dummyList = new JList<>();

        // Test selected popup item
        Component selectedComp = renderer.getListCellRendererComponent(dummyList, "Option 1", 0, true, true);
        JLabel selectedLabel = (JLabel) selectedComp;
        assertEquals(UITheme.PRIMARY, selectedLabel.getBackground());
        assertEquals(Color.WHITE, selectedLabel.getForeground());

        // Test unselected popup item
        Component unselectedComp = renderer.getListCellRendererComponent(dummyList, "Option 2", 1, false, false);
        JLabel unselectedLabel = (JLabel) unselectedComp;
        assertEquals(UITheme.BG_CARD, unselectedLabel.getBackground());
        assertEquals(UITheme.TEXT_PRIMARY, unselectedLabel.getForeground());
    }

    @Test
    @DisplayName("Editable combo box styles its editor text field with dark background and primary text")
    public void testEditableComboBoxStyling() {
        JComboBox<String> cb = new JComboBox<>(new String[]{"Test"});
        cb.setEditable(true);
        UITheme.styleComboBox(cb);

        Component editorComp = cb.getEditor().getEditorComponent();
        assertInstanceOf(JTextField.class, editorComp);
        JTextField tf = (JTextField) editorComp;
        assertEquals(UITheme.BG_INPUT, tf.getBackground());
        assertEquals(UITheme.TEXT_PRIMARY, tf.getForeground());
    }
}
