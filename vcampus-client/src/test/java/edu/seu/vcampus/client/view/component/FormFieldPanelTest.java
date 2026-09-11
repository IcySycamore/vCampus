package edu.seu.vcampus.client.view.component;

import java.awt.GridBagLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 通用表单行测试。
 */
class FormFieldPanelTest {

    @Test
    void addsLabelAndFieldToForm() {
        JPanel form = new JPanel(new GridBagLayout());
        FormFieldPanel field = new FormFieldPanel("用户 ID", new JTextField(), 240);

        field.addTo(form, 0);

        assertEquals(2, field.getComponentCount());
        assertEquals(1, form.getComponentCount());
    }
}
