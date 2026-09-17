package edu.seu.vcampus.client.view.shop;

import java.awt.Component;
import java.text.ParseException;
import javax.swing.AbstractCellEditor;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/** JTable 中可通过箭头或直接输入操作的非负整数微调框。 */
final class ShopQuantitySpinnerCell extends AbstractCellEditor
        implements TableCellEditor, TableCellRenderer {

    private static final long serialVersionUID = 1L;

    private final JSpinner editor = spinner();
    private final JSpinner renderer = spinner();
    private final JFormattedTextField editorField =
            ((JSpinner.DefaultEditor) editor.getEditor()).getTextField();
    private boolean adjusting;
    private boolean stopping;

    ShopQuantitySpinnerCell() {
        editor.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                if (!adjusting && !stopping) {
                    fireEditingStopped();
                }
            }
        });
        editorField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                commitTypedZero();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                commitTypedZero();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                commitTypedZero();
            }
        });
    }

    @Override
    public Object getCellEditorValue() {
        return editor.getValue();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean selected, int row, int column) {
        adjusting = true;
        try {
            editor.setValue(value);
        } finally {
            adjusting = false;
        }
        return editor;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean selected, boolean focused, int row, int column) {
        renderer.setValue(value);
        renderer.setEnabled(table.isEnabled() && table.isCellEditable(row, column));
        renderer.setBackground(selected
                ? table.getSelectionBackground() : table.getBackground());
        return renderer;
    }

    @Override
    public boolean stopCellEditing() {
        stopping = true;
        try {
            editor.commitEdit();
        } catch (ParseException e) {
            return false;
        } finally {
            stopping = false;
        }
        return super.stopCellEditing();
    }

    private void commitTypedZero() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!"0".equals(editorField.getText().trim())) {
                    return;
                }
                try {
                    editor.commitEdit();
                } catch (ParseException e) {
                    // The number formatter will keep the editor active for invalid input.
                }
            }
        });
    }

    private static JSpinner spinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(
                Integer.valueOf(1), Integer.valueOf(0),
                Integer.valueOf(Integer.MAX_VALUE), Integer.valueOf(1)));
        JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor) spinner.getEditor();
        spinnerEditor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
        return spinner;
    }
}
