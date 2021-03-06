package gamecontroller;

import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class Quadro extends JPanel {
	
  Celula[][] celulas;
  JLabel lblNewLabel;
  
  // CONSTRUTOR
  public Quadro(GameMain frame) {
	  
    celulas = new Celula[3][3];
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        celulas[row][col] = new Celula(row, col, frame);
      }
    }
    drawGame(frame);
  }
  
  // Limpa o quadro
  public void start() {
	  
    for (int row = 0; row < GameMain.ROWS; row++) {
      for (int col = 0; col < GameMain.COLS; col++) {
        celulas[row][col].clear();
      }
    }
  }
  
  // Teste pra empate
  public boolean isDraw() {
	  
    for (int row = 0; row < GameMain.ROWS; row++) {
      for (int col = 0; col < GameMain.COLS; col++) {
        if (celulas[row][col].content == Seed.VAZIO) {
          return false;
        }
      }
    }
    return true;
  }
  

  // Teste pra vit�ria
  public boolean hasWon(Seed seed, int seedRow, int seedCol) {
	  // Horizontal
	  if ((celulas[seedRow][0].content == seed) && 
			(celulas[seedRow][1].content == seed) && 
			(celulas[seedRow][2].content == seed)) {
		  
		  for (int i=0; i < GameMain.COLS; i++) {
			  celulas[seedRow][i].setWin();
		  }
		  return true;
	    // Vertical
	  } else if ((celulas[0][seedCol].content == seed) && 
			  		(celulas[1][seedCol].content == seed) && 
			  		(celulas[2][seedCol].content == seed)) {
			  
			  for (int i=0; i < GameMain.ROWS; i++) {
				  celulas[i][seedCol].setWin();
			  }
			  
			  return true;
		// Diagonal esquerda-direita
	  } else if ((seedRow == seedCol) && 
			  		(celulas[0][0].content == seed) && 
			  		(celulas[1][1].content == seed) && 
			  		(celulas[2][2].content == seed)) {
		  
		  for (int i=0; i < GameMain.ROWS; i++) {
			  celulas[i][i].setWin();
		  }
		  
		  return true;
		// Diagonal direita-esquerda
	  } else if ((seedRow + seedCol == 2) && 
			  (celulas[0][2].content == seed) && 
			  (celulas[1][1].content == seed) && 
			  (celulas[2][0].content == seed)) {
	  
		  for (int i=0; i < GameMain.ROWS; i++) {
			  for (int j=GameMain.COLS-1; j >= 0; j--) {
				  if (i + j == 2) {
					  celulas[i][j].setWin();
				  }

			  }
		  }
	  
		  return true;
	  }
	  
	  return false;
	  
  }


  // Desenha o quadro de jogo
  public void drawGame(JFrame frame) {
	  
    setBackground(Color.WHITE);
    setForeground(Color.GRAY);
    frame.setContentPane(this);
    setLayout(null);
    
    JPanel separadorV1 = new JPanel();
    separadorV1.setBackground(Color.BLACK);
    separadorV1.setBounds(121, 0, 3, 366);
    add(separadorV1);
    
    JPanel separadorV2 = new JPanel();
    separadorV2.setBackground(Color.BLACK);
    separadorV2.setBounds(242, 0, 3, 366);
    add(separadorV2);
    
    JPanel separadorH1 = new JPanel();
    separadorH1.setBackground(Color.BLACK);
    separadorH1.setBounds(0, 121, 366, 3);
    add(separadorH1);
    
    JPanel separadorH2 = new JPanel();
    separadorH2.setBackground(Color.BLACK);
    separadorH2.setBounds(0, 243, 366, 3);
    add(separadorH2);
    
    add(celulas[0][0]);
    
    add(celulas[0][1]);
    
    add(celulas[0][2]);
    
    add(celulas[1][0]);
    
    add(celulas[1][1]);
    
    add(celulas[1][2]);
    
    add(celulas[2][0]);
    
    add(celulas[2][1]);
    
    add(celulas[2][2]);
    
    lblNewLabel = new JLabel("Bem Vindo ao Jogo da Velha! X começa!");
    lblNewLabel.setOpaque(true);
    lblNewLabel.setHorizontalAlignment(0);
    lblNewLabel.setBackground(Color.LIGHT_GRAY);
    lblNewLabel.setBounds(0, 366, 366, 25);
    add(lblNewLabel);
  }
  
  // Muda o texto inferior
  public void setBottomLabel(String text) {
    lblNewLabel.setText(text);
  }
}
