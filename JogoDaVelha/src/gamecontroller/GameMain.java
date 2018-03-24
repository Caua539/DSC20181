package gamecontroller;

import java.awt.EventQueue;
import javax.swing.JFrame;





public class GameMain
  extends JFrame
{
  private static final long serialVersionUID = 1L;
  private Quadro quadro;
  private GameState currentState;
  private Seed currentPlayer;
  public static final int ROWS = 3;
  public static final int COLS = 3;
  
  public GameMain()
  {
    setResizable(false);
    setTitle("Jogo da Velha");
    setDefaultCloseOperation(3);
    setBounds(100, 100, 367, 420);
    
    quadro = new Quadro(this);
    
    iniciarJogo();
  }
  

  public void warden(Celula celula)
  {
    if (currentState == GameState.JOGANDO) {
      if (celula.getContent() == Seed.VAZIO) {
        celula.setContent(currentPlayer);
        updateGame(currentPlayer, celula.getRow(), celula.getCol());
      }
    } else {
      iniciarJogo();
    }
  }
  
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
  
  public void iniciarJogo()
  {
    quadro.init();
    currentState = GameState.JOGANDO;
    currentPlayer = Seed.X;
    quadro.setBottomLabel("Jogo em andamento...");
  }
  


  public static void main(String[] args)
  {
    EventQueue.invokeLater(new GameMain.1());
  }
}
