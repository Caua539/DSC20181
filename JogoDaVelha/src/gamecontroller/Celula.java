package gamecontroller;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@SuppressWarnings("serial")
public class Celula
  extends JLabel
{
  Seed content;
  int row;
  int col;
  
  public Celula(int row, int col, GameMain frame)
  {
    this.row = row;
    this.col = col;
    
    setForeground(Color.BLACK);
    setHorizontalAlignment(0);
    setFont(new Font("Tahoma", 0, 80));
    setBounds(122 * row, 122 * col, 121, 121);
    addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent arg0) {
        frame.warden(Celula.this);
      }
    });

    clear();
  }
  
  public void clear()
  {
    content = Seed.VAZIO;
    desenhar();
  }
  
  public void desenhar()
  {
    if (content == Seed.X) {
      setForeground(Color.RED);
      setText(content.getValorseed());
    } else if (content == Seed.BOLA) {
      setForeground(Color.BLUE);
      setText(content.getValorseed());
    } else {
      setText(" ");
    }
  }
  
  public Seed getContent() {
    return content;
  }
  
  public void setContent(Seed cont) {
    content = cont;
    desenhar();
  }
  
  public int getRow() {
    return row;
  }
  
  public int getCol() {
    return col;
  }
}
