
package br.ufg.inf.es.dsc20181.jogodavelhaonline.model;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

public class PlayerListRender extends DefaultListCellRenderer  {
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof PlayerOn)
            setText(((PlayerOn)value).getApelido());

        return this;
    }
}