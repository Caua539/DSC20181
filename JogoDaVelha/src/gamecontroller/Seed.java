package gamecontroller;

public enum Seed {
  VAZIO(" "),  X("X"),  BOLA("O");
  
  private String valorseed;
  
  private Seed(String valorseed) {
    this.valorseed = valorseed;
  }
  
  public String getValorseed() {
    return valorseed;
  }
}
