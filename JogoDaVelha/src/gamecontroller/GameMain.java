package gamecontroller;

import java.awt.EventQueue;
import javax.swing.JFrame;

public class GameMain extends JFrame {
	
  private static final long serialVersionUID = 1L;
  private Quadro quadro;
  private GameState currentState;
  private Seed currentPlayer;
  public static final int ROWS = 3;
  public static final int COLS = 3;
  
  // CONSTRUTOR, CRIA A JANELA, INICIA O JOGO
  public GameMain() {
    setResizable(false);
    setTitle("Jogo da Velha");
    setDefaultCloseOperation(3);
    setBounds(100, 100, 367, 420);
    
    quadro = new Quadro(this);
    
    startGame();
  }
  
  // Controlador do jogo, recebe os cliques nas células e decide o que fazer com elas
  public void gameMaster(Celula celula) {
	  
    if (currentState == GameState.JOGANDO) {
      if (celula.getContent() == Seed.VAZIO) {
        celula.setContent(currentPlayer);
        updateGame(currentPlayer, celula.getRow(), celula.getCol());
      }
    } else {
      startGame();
    }
  }
  
  // Checa por condições de vitória ou empate
  public void updateGame(Seed theSeed, int row, int col) {
	  
    if (quadro.hasWon(theSeed, row, col)) {
      currentState = (theSeed == Seed.X ? GameState.X_GANHOU : GameState.BOLA_GANHOU);
      quadro.setBottomLabel("O jogador " + theSeed.getValorseed() + " ganhou! Clique para jogar novamente.");
    } else if (quadro.isDraw()) {
      currentState = GameState.EMPATE;
      quadro.setBottomLabel("Empate! Clique para jogar novamente.");
    } else {
      currentPlayer = (currentPlayer == Seed.X ? Seed.BOLA : Seed.X);
      quadro.setBottomLabel("Turno do jogador " + currentPlayer.getValorseed() + ".");
    }
  }
  
  //Inicia um novo jogo (X sempre começa)
  public void startGame() {
	  
    quadro.start();
    currentState = GameState.JOGANDO;
    currentPlayer = Seed.X;
    quadro.setBottomLabel("Jogo em andamento...");
  }
  

  //MAIN
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
		public void run() {
			try {
				GameMain frame = new GameMain();
				frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	});
  }
}
