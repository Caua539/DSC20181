package br.ufg.inf.es.dsc20181.jogodavelhaonline.controller;

import java.awt.Color;
import java.awt.Container;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import br.ufg.inf.es.dsc20181.jogodavelhaonline.view.GameBoard;
import java.util.Random;
import javax.swing.DefaultListModel;
import javax.swing.Timer;
import br.ufg.inf.es.dsc20181.jogodavelhaonline.model.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

public class GameController {
    
    private char[][] tabuleiro = new char[3][3];
    private GameBoard board;
    private boolean estaJogando;
    private boolean estaConectado;
    private boolean minhaVez;
    private boolean inicieiUltimoJogo;
    private boolean fuiConvidado;
    private ServerSocket servidorTCP;
    private CnctTCP conexaoTCP;
    private String apelidoLocal; 
    private DefaultListModel<PlayerOn> jogadores; 
    private final static Random numAleatorio = new Random();
    
    private final Color COR_LOCAL = new Color(51, 153, 0);
    private final Color COR_REMOTO = new Color(255, 0, 0);
    private final Color COR_EMPATE = new Color(255,255,0);
    
    private final int PORTA_UDP = 20181;
    
    private final int SEM_RESULTADO = -1;
    private final int EMPATE = 0;
    private final int VITORIA_LOCAL = 1;
    private final int VITORIA_REMOTO = 2;
    
    private final int SEM_GANHADOR = 0;
    private final int LINHA_1 = 1;
    private final int LINHA_2 = 2;
    private final int LINHA_3 = 3;
    private final int COLUNA_1 = 4;
    private final int COLUNA_2 = 5;
    private final int COLUNA_3 = 6;
    private final int DIAGONAL_PRINCIPAL = 7;
    private final int DIAGONAL_SECUNDARIA = 8;
    
    public final static int JOGADOR_LOCAL = 1;
    public final static int JOGADOR_REMOTO = 2;
    public final char SIMBOLO_VAZIO = ' ';
    public final char SIMBOLO_LOCAL = 'X';
    public final char SIMBOLO_REMOTO = 'O';
    
    public final static int CONEXAO_TIMEOUT = 0;
    public final static int CONEXAO_CAIU = 1;
    public final static int JOGADOR_DESISTIU = 2;
    public final static int FIM_JOGO = 3;
    
    public static final String MSG_IN = "IN";
    public static final String MSG_OUT = "OUT";
    public static final String MSG_ERRO = "ERRO";
    public static final String MSG_INFO = "INFO";
    public static final String MSG_PROTO_TCP = "TCP";
    public static final String MSG_PROTO_UDP = "UDP";
    public static final String MSG_PROTO_NENHUM = "";
    
    private int[] resultados = new int[5];
    private int jogoAtual;

    private LstnUDP udpEscutaThread;        
    private LstnTCP tcpEscutaThread;    
    private InetAddress addrLocal;         
    private InetAddress addrBroadcast;     
    private InetAddress addrJogadorRemoto; 
    private String apelidoRemoto;  
    private Timer quemEstaOnlineTimer;     
    private Timer timeoutQuemEstaOnlineTimer; 
    private Timer timeoutAguardandoJogadorRemoto;  
    

    private boolean esperandoConexao;
    private boolean esperandoInicioJogo;
    private boolean esperandoConfirmacao;
    private boolean esperandoJogadorRemoto;
    private boolean esperandoRespostaConvite;
    
    public GameController(GameBoard board){
        this.board = board;

        board.setTitle("Jogo da Velha Remoto");
        

        board.setLocationRelativeTo(null);
        

        estaJogando = estaConectado = false;
        servidorTCP = null;
        conexaoTCP = null;
        udpEscutaThread = null;
        tcpEscutaThread = null;
        addrLocal = null;
        esperandoConexao = esperandoInicioJogo = false;
        esperandoConfirmacao = esperandoJogadorRemoto = false;
        esperandoRespostaConvite = false;

        try {

            addrBroadcast = InetAddress.getByName("255.255.255.255");
        } catch (UnknownHostException ex) {
            JOptionPane.showMessageDialog(null,
                    "Não foi possível criar endereço para broadcasting.",
                    "Encerrando programa",
                    JOptionPane.ERROR_MESSAGE);
            encerraPrograma();
            return;
        }
        

        jogadores = new DefaultListModel<>();
        board.JogadoresOnlineList.setModel(jogadores);
        board.JogadoresOnlineList.setCellRenderer(new PlayerListRender());


        try
        {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets))
            {

                if (netint.isVirtual() || netint.isLoopback()){
                    continue;
                }


                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                if(inetAddresses.hasMoreElements())
                {
                    for (InetAddress inetAddress : Collections.list(inetAddresses))
                    {
                        if ((inetAddress instanceof Inet4Address) &&
                            inetAddress.isSiteLocalAddress())
                        {
                            board.adaptadoresComboBox.addItem(inetAddress.getHostAddress() +
                                    " - " + netint.getDisplayName());
                        }
                    }
                }
            }
        }catch(SocketException ex)
        {
        }
        

        ActionListener quemEstaOnlinePerformer = (ActionEvent evt) -> {
            for(int i = 0; i < jogadores.getSize(); ++i)
                jogadores.get(i).setAindaOnline(false);
            

            enviarMensagemUDP(addrBroadcast, 1, apelidoLocal);

            timeoutQuemEstaOnlineTimer.start();
        };
        quemEstaOnlineTimer = new Timer(180000, quemEstaOnlinePerformer);
        quemEstaOnlineTimer.setRepeats(true); 
        

        ActionListener timeoutQuemEstaOnlinePerformer = (ActionEvent evt) -> {
            atualizarListaJogadoresOnline();
        };
        timeoutQuemEstaOnlineTimer = new Timer(15000, timeoutQuemEstaOnlinePerformer);
        timeoutQuemEstaOnlineTimer.setRepeats(false); 

        

        ActionListener timeoutAguardandoJogadorRemotoPerformer = (ActionEvent evt) -> {
            if(esperandoRespostaConvite)
                encerrarConviteParaJogar(true);
            else
                encerrarConexaoTCP(CONEXAO_TIMEOUT);
        };
        
        timeoutAguardandoJogadorRemoto = new Timer(30000, timeoutAguardandoJogadorRemotoPerformer);
        timeoutAguardandoJogadorRemoto.setRepeats(false);
        
    }
                                           

    public void encerraPrograma()
    {
        jogoFechando();
        
        Container frame = board.SairBotao.getParent();
        do
        {
            frame = frame.getParent(); 
        }while (!(frame instanceof JFrame));  
        ((JFrame)frame).dispose();
    }
    
    public void jogoFechando() {                                   

        if(quemEstaOnlineTimer.isRunning())
            quemEstaOnlineTimer.stop();
        if(timeoutQuemEstaOnlineTimer.isRunning())
            timeoutQuemEstaOnlineTimer.stop();
        if(timeoutAguardandoJogadorRemoto.isRunning())
            timeoutAguardandoJogadorRemoto.stop();


        enviarMensagemUDP(addrBroadcast, 3, apelidoLocal);
        

        if (udpEscutaThread != null)
        {
            udpEscutaThread.encerraConexao();
            udpEscutaThread.cancel(true);
        }
        
        if (tcpEscutaThread != null)
        {
            tcpEscutaThread.encerraConexao();
            tcpEscutaThread.cancel(true);
        }
    }                                  
            
    private void desconectaJogadorLocal()
    {
        estaConectado = false;
        
        if(quemEstaOnlineTimer.isRunning())
            quemEstaOnlineTimer.stop();
        if(timeoutQuemEstaOnlineTimer.isRunning())
            timeoutQuemEstaOnlineTimer.stop();
        if(timeoutAguardandoJogadorRemoto.isRunning())
            timeoutAguardandoJogadorRemoto.stop();
        
        jogadores.clear();
        
        enviarMensagemUDP(addrBroadcast, 3, apelidoLocal);
        
        board.nomeJogadorLocal.setEnabled(true);
        board.adaptadoresComboBox.setEnabled(true);
        board.botaoConectar.setText("Conectar");
        board.playerNameLabel.setEnabled(false);
        
        board.playerNameLabel.setText(SIMBOLO_LOCAL + " - Local");
        
        if (udpEscutaThread != null)
        {
            udpEscutaThread.encerraConexao();
            udpEscutaThread.cancel(true);
        }
        
        if (tcpEscutaThread != null)
        {
            tcpEscutaThread.encerraConexao();
            tcpEscutaThread.cancel(true);
        }
       
        board.statusLabel.setText("");
        board.nomeJogadorLocal.requestFocus();
    }

    public void jogadorSelecionado(javax.swing.event.ListSelectionEvent evt) {                                            
        int idx = board.JogadoresOnlineList.getSelectedIndex();
        board.ConvidarBotao.setEnabled(idx >= 0);
    }                                           

    public void convidar(java.awt.event.ActionEvent evt) {                                                
        PlayerOn j = board.JogadoresOnlineList.getSelectedValue();
        if (j == null)
            return;
        
        apelidoRemoto = j.getApelido();
        addrJogadorRemoto = j.getAddress();
        
        board.statusLabel.setText("");
        String msg = "Convida " + apelidoRemoto + " para jogar?";
        int resp = JOptionPane.showConfirmDialog(board, msg, "Convite para jogar",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (resp == JOptionPane.NO_OPTION)
            return;

        enviarMensagemUDP(j.getAddress(), 4, apelidoLocal);
        esperandoRespostaConvite = true;
        board.statusLabel.setText("AGUARDANDO RESPOSTA");
        timeoutAguardandoJogadorRemoto.start();
    }                                               

    public void conectar(java.awt.event.ActionEvent evt) {                                                
        if(estaConectado)
        {
            desconectaJogadorLocal();
            return;
        }

        apelidoLocal = board.nomeJogadorLocal.getText().trim();
        if (apelidoLocal.isEmpty())
        {
            board.nomeJogadorLocal.requestFocus();
            return;
        }

        int nInterface = board.adaptadoresComboBox.getSelectedIndex();
        if(nInterface < 0)
        {
            board.adaptadoresComboBox.requestFocus();
            return;
        }

        addrLocal = obtemInterfaceRede();
        if(addrLocal == null)
        {
            JOptionPane.showMessageDialog(null,
                "Erro na obtenção da interface escolhida.",
                "Conexão do jogador local",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        try
        {
            udpEscutaThread = new LstnUDP(this, PORTA_UDP, apelidoLocal,
                addrLocal);
        }catch(SocketException ex)
        {
            JOptionPane.showMessageDialog(null,
                "Erro na criação do thread de leitura da porta "+ PORTA_UDP +
                ".\n" + ex.getMessage(),
                "Conexão do jogador local",
                JOptionPane.ERROR_MESSAGE);
            encerraPrograma();
            return;
        }

        estaConectado = true;

        board.nomeJogadorLocal.setEnabled(false);
        board.adaptadoresComboBox.setEnabled(false);
        board.botaoConectar.setText("Desconectar");
        board.playerNameLabel.setEnabled(true);

        board.playerNameLabel.setText(SIMBOLO_LOCAL + " - " + apelidoLocal);

        udpEscutaThread.execute();

        enviarMensagemUDP(addrBroadcast, 1, apelidoLocal);

        if(quemEstaOnlineTimer.isRunning() == false)
        quemEstaOnlineTimer.start();

    }                                               
                                            
                
    private void encerrarConviteParaJogar(boolean timeout)
    {
        esperandoRespostaConvite = false;
        board.statusLabel.setText("");
        
        String msg;
        if(timeout)
            msg = "Timeout: " + apelidoRemoto + " não respondeu convite.";
        else
            msg = apelidoRemoto + " recusou o convite.";
        JOptionPane.showMessageDialog(board, msg, "Convite para jogar",
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    
    public void respostaConvite(String msg, InetAddress addr)
    {
        
        String[] strPartes= msg.split("\\|");
        if(strPartes.length != 2)
            return;
        
        
        if(esperandoRespostaConvite == false)
            return;
        
        
        if ((addr.equals(addrJogadorRemoto) == false) ||
            apelidoRemoto.compareToIgnoreCase(strPartes[0]) != 0)
                return;

        
        esperandoRespostaConvite = false;
        if(timeoutAguardandoJogadorRemoto.isRunning())
            timeoutAguardandoJogadorRemoto.stop();

        int porta = Integer.parseInt(strPartes[1]);
        if(porta == 0)
        {
            
            encerrarConviteParaJogar(false);
            return;
        }
        
        
        enviarMensagemUDP(addr, 6, "Ok");
        
        
        try
        {
            
            Socket socket = new Socket(addr, porta);
            conexaoTCP = new CnctTCP(this, socket);
            
            
            conexaoTCP.execute();

            
            board.statusLabel.setText("AGUARDANDO INÍCIO");
            esperandoInicioJogo = true;
        } catch(IOException ex)
        {
            String erro = "Erro ao criar conexão TCP com jogador remoto\n" +
                         ex.getMessage();
            JOptionPane.showMessageDialog(board, erro,
                    "Conectar com jogador remoto", JOptionPane.INFORMATION_MESSAGE);
        }
    }
        
    
    public void escolhePosicao(int pos)
    {
        
        if (estaJogando == true && minhaVez == true)
            marcaPosicao(JOGADOR_LOCAL, pos);
    }

    public void marcaPosicao(int quemEscolheu, int pos)
    {
        
        if((pos < 1) || (pos > 9))
            return;
        
        
        int linha = (pos - 1) / 3;
        int coluna = (pos - 1) % 3;
        if (tabuleiro[linha][coluna] != SIMBOLO_VAZIO)
            return;
        
        Color cor;
        char marca;
        if(quemEscolheu == JOGADOR_LOCAL)
        {
            cor = COR_LOCAL;
            marca = SIMBOLO_LOCAL;
        }
        else
        {
            cor = COR_REMOTO;
            marca = SIMBOLO_REMOTO;
        }
        
        
        tabuleiro[linha][coluna] = marca;
        javax.swing.JLabel ctrl = null;
        switch(pos)
        {
            case 1: ctrl = board.jLabel00; break;
            case 2: ctrl = board.jLabel01; break;
            case 3: ctrl = board.jLabel02; break;
            case 4: ctrl = board.jLabel10; break;
            case 5: ctrl = board.jLabel11; break;
            case 6: ctrl = board.jLabel12; break;
            case 7: ctrl = board.jLabel20; break;
            case 8: ctrl = board.jLabel21; break;
            case 9: ctrl = board.jLabel22; break;
        }
        ctrl.setForeground(cor);
        ctrl.setText(Character.toString(marca));
        
        
        
        if(quemEscolheu == JOGADOR_LOCAL)
            conexaoTCP.enviarMensagemTCP(8, String.valueOf(pos));
        
        
        int ganhador = verificaGanhador();
        if(ganhador != SEM_GANHADOR)
        {
            resultados[jogoAtual - 1] = ganhador % 10;
            mostraResultadoPartida(ganhador);
            novaPartida(ganhador);
            return;
        }
        
        
        if (jogoEmpatou())
        {
            resultados[jogoAtual - 1] = EMPATE;
            mostraResultadoPartida(EMPATE);
            novaPartida(EMPATE);
            return;
        }
        
        
        if (quemEscolheu == JOGADOR_LOCAL)
        {
            minhaVez = false;
            board.statusLabel.setText("AGUARDANDO JOGADOR");
        }
        else
        {
            minhaVez = true;
            board.statusLabel.setText("SUA VEZ");
        }
    }

    
    public void quemIniciaJogo(int jogador)
    {
        
        esperandoInicioJogo = false;
        
        iniciarSerieJogos();
        
        
        if (jogador == 1)
        {
            
            minhaVez = inicieiUltimoJogo = true;
            board.statusLabel.setText("ESPERANDO JOGADOR");
        }
        else
        {
            
            minhaVez = inicieiUltimoJogo = true;
            board.statusLabel.setText("SUA VEZ");
        }
    }

    private void mostraResultadoPartida(int quemGanhou)
    {
        
        destacaResultadoTabuleiro(quemGanhou / 10);
        mostraResultados();
        String msg = "";
        switch(quemGanhou % 10)
        {
            case EMPATE: msg = "Partida empatou!"; break;
            case VITORIA_LOCAL: msg = "Você ganhou!"; break;
            case VITORIA_REMOTO: msg = "Você perdeu!"; break;
        }
        
        if (jogoAtual == 5)
        {
            int local = Integer.parseInt(board.pontuacaoYouLabel.getText());
            int remoto = Integer.parseInt(board.pontuacaoRemotoLabel.getText());
            msg += "\n\nPlacar final:" +
                   "\n    " + apelidoLocal + ": " + local +
                   "\n    " + apelidoRemoto + ": " + remoto +
                   "\n\n";
            if (local == remoto)
                msg += "Essa série ficou EMPATADA!";
            else
                if (local > remoto)
                    msg += "Você ganhou a série. Parabéns!";
                else
                    msg += apelidoRemoto + " ganhou a série!";
            
            msg += "\n\nPara jogar uma nova série,\njogador deverá ser convidado novamente.";
        }
        
        JOptionPane.showMessageDialog(board, msg, "Partida " + jogoAtual + " de 5.",
                                      JOptionPane.INFORMATION_MESSAGE);
    }
    
    
    
    private void destacaResultadoTabuleiro(int posicoesVencedoras)
    {
        boolean[][] destaca = {{false, false, false},
                               {false, false, false},
                               {false, false, false}};
        
        switch(posicoesVencedoras)
        {
            case LINHA_1:
                destaca[0][0] = destaca[0][1] = destaca[0][2] = true;
                break;
            case LINHA_2:
                destaca[1][0] = destaca[1][1] = destaca[1][2] = true;
                break;
            case LINHA_3:
                destaca[2][0] = destaca[2][1] = destaca[2][2] = true;
                break;
            case COLUNA_1:
                destaca[0][0] = destaca[1][0] = destaca[2][0] = true;
                break;
            case COLUNA_2:
                destaca[0][1] = destaca[1][1] = destaca[2][1] = true;
                break;
            case COLUNA_3:
                destaca[0][2] = destaca[1][2] = destaca[2][2] = true;
                break;
            case DIAGONAL_PRINCIPAL:
                destaca[0][0] = destaca[1][1] = destaca[2][2] = true;
                break;
            case DIAGONAL_SECUNDARIA:
                destaca[0][2] = destaca[1][1] = destaca[2][0] = true;
                break;
        }
        
        int linha, coluna;
        javax.swing.JLabel ctrl = null;
        for(int pos = 0; pos < 9; ++pos)
        {
            linha = pos / 3;
            coluna = pos % 3;
            switch(pos)
            {
                case 0: ctrl = board.jLabel00; break;
                case 1: ctrl = board.jLabel01; break;
                case 2: ctrl = board.jLabel02; break;
                case 3: ctrl = board.jLabel10; break;
                case 4: ctrl = board.jLabel11; break;
                case 5: ctrl = board.jLabel12; break;
                case 6: ctrl = board.jLabel20; break;
                case 7: ctrl = board.jLabel21; break;
                case 8: ctrl = board.jLabel22; break;
            }
            
            if (destaca[linha][coluna] == false)
                ctrl.setForeground(Color.DARK_GRAY);
        }
    }
    
    private int verificaGanhador()
    {
        
        for(int linha = 0; linha < 3; ++linha)
        {
            if((tabuleiro[linha][0] != SIMBOLO_VAZIO) &&
               (tabuleiro[linha][0] == tabuleiro[linha][1]) &&
               (tabuleiro[linha][1] == tabuleiro[linha][2]))
            {
                int resultado = 0;
                switch(linha)
                {
                    case 0: resultado = LINHA_1; break;
                    case 1: resultado = LINHA_2; break;
                    case 2: resultado = LINHA_3; break;
                }
                return 10 * resultado +
                       (tabuleiro[linha][0] == SIMBOLO_LOCAL ? JOGADOR_LOCAL : JOGADOR_REMOTO);
            }
        }
        
        
        for(int coluna = 0; coluna < 3; ++coluna)
        {
            if((tabuleiro[0][coluna] != SIMBOLO_VAZIO) &&
               (tabuleiro[0][coluna] == tabuleiro[1][coluna]) &&
               (tabuleiro[1][coluna] == tabuleiro[2][coluna]))
            {
                int resultado = 0;
                switch(coluna)
                {
                    case 0: resultado = COLUNA_1; break;
                    case 1: resultado = COLUNA_2; break;
                    case 2: resultado = COLUNA_3; break;
                }
                
                return 10 * resultado +
                       (tabuleiro[0][coluna] == SIMBOLO_LOCAL ? JOGADOR_LOCAL : JOGADOR_REMOTO);
            }
        }
        
        
        if((tabuleiro[0][0] != SIMBOLO_VAZIO) &&
           (tabuleiro[0][0] == tabuleiro[1][1]) &&
           (tabuleiro[1][1] == tabuleiro[2][2]))
                return 10 * DIAGONAL_PRINCIPAL +
                       (tabuleiro[0][0] == SIMBOLO_LOCAL ? JOGADOR_LOCAL : JOGADOR_REMOTO);
        
        
        if((tabuleiro[0][2] != SIMBOLO_VAZIO) &&
           (tabuleiro[0][2] == tabuleiro[1][1]) &&
           (tabuleiro[1][1] == tabuleiro[2][0]))
                return 10 * DIAGONAL_SECUNDARIA +
                       (tabuleiro[0][2] == SIMBOLO_LOCAL ? JOGADOR_LOCAL : JOGADOR_REMOTO);

        
        return SEM_GANHADOR;
    }
    
    
    private void novaPartida(int ultimoGanhador)
    {
        if (jogoAtual == 5)
        {
            
            encerrarConexaoTCP(FIM_JOGO);
            
            return;
        }
        
        
        limpaTabuleiro();
        
        
        ++jogoAtual;
        mostraResultados();
        
        
        
        
        
        if (ultimoGanhador != JOGADOR_LOCAL)
        {
            boolean enviaMensagem = true;
            if(ultimoGanhador == EMPATE)
                enviaMensagem = !inicieiUltimoJogo;
            
            if (enviaMensagem)
            {
                
                conexaoTCP.enviarMensagemTCP(9, null);
                
                minhaVez = inicieiUltimoJogo = true;
                board.statusLabel.setText("SUA VEZ");
            }
        }
        else
        {
            
            minhaVez = inicieiUltimoJogo = false;
            board.statusLabel.setText("AGUARDANDO INÍCIO");
            esperandoInicioJogo = true;
        }
    }
    
    public void jogadorRemotoIniciaNovoJogo()
    {
        esperandoInicioJogo = false;
        board.statusLabel.setText("AGUARDANDO JOGADOR");
    }
    
    
    private boolean jogoEmpatou()
    {
        for(int i = 0; i < 3; ++i)
            for(int j = 0; j < 3; ++j)
                if(tabuleiro[i][j] == SIMBOLO_VAZIO)
                    return false;
        
        return true;
    }
    
    public void enviarMensagemUDP(InetAddress addr, int numero,
                                  String compl)
    {
        String msg;
        if((compl == null) || compl.isEmpty())
            msg = String.format("%02d005", numero);
        else
            msg = String.format("%02d%03d%s", numero, 5 + compl.length(),
                                compl);
        
        DatagramPacket p = new DatagramPacket(msg.getBytes(),
                        msg.getBytes().length, addr, PORTA_UDP);
        
        DatagramSocket udpSocket = null;
        try {
            
            
            udpSocket = new DatagramSocket(0, addrLocal);
            udpSocket.setBroadcast(addr.equals(addrBroadcast));
            
            
            udpSocket.send(p);                    
            
            
            mostraMensagem(MSG_OUT, MSG_PROTO_UDP, addr.getHostAddress(),
                           PORTA_UDP, msg);
        } catch (IOException ex) {
            mostraMensagem(MSG_OUT, MSG_PROTO_UDP,
                           addr.getHostAddress(),
                           (udpSocket == null ? 0 : PORTA_UDP),
                           "Erro: Envio da mensagem [msg " + numero + "]");
        }
    }
    
    
    public void mostraMensagem(String inORout, String protocolo,
                               String endereco, int porta, String conteudo)
    {
        String mensagem = String.format("%s  |  %s  |  %s  |  %d  |  %s  ", inORout, protocolo, endereco, porta, conteudo);
        System.out.println(mensagem);
    }

    public void atualizarListaJogadoresOnline()
    {
        for(int i = 0; i < jogadores.size(); ++i)
        {
            if(jogadores.get(i).getAindaOnline() == false)
                jogadores.remove(i);
        }
    }
    
    
    
    public void adicionaJogador(int nMsg, String apelido, InetAddress addr)
    {
        PlayerOn j;
        PlayerOn novoJogador;
        
        
        for(int i = 0; i < jogadores.size(); ++i)
        {
            
            j = jogadores.get(i);
            
            
            if(j.mesmoApelido(apelido))
            {
                j.setAindaOnline(true); 
        
                
                if(nMsg == 1)
                    enviarMensagemUDP(addr, 2, apelidoLocal);
                
                return;
            }
            
            
            if (j.getApelido().compareToIgnoreCase(apelido) > 0)
            {
                novoJogador = new PlayerOn(apelido, addr);
                jogadores.add(i, novoJogador);
        
                
                if(nMsg == 1)
                    enviarMensagemUDP(addr, 2, apelidoLocal);
                
                return;
            }
        }
        
        
        novoJogador = new PlayerOn(apelido, addr);
        jogadores.addElement(novoJogador);
        
        
        if(nMsg == 1)
            enviarMensagemUDP(addr, 2, apelidoLocal);
    }
    
    
    public void removeJogador(String apelido)
    {
        if(estaJogando && (apelido.compareToIgnoreCase(apelidoRemoto) == 0))
            encerrarConexaoTCP(JOGADOR_DESISTIU);
        
        
        for(int i = 0; i < jogadores.size(); ++i)
        {
            
            if(jogadores.get(i).mesmoApelido(apelido))
            {
                jogadores.remove(i);
                return;
            }
        }
    }
    
    private InetAddress obtemInterfaceRede()
    {
        
        int nInterface = board.adaptadoresComboBox.getSelectedIndex();
        if(nInterface < 0)
            return null;

        
        String str = board.adaptadoresComboBox.getItemAt(nInterface);
        String[] strParts = str.split(" - ");
        InetAddress addr;
        try {
            addr = InetAddress.getByName(strParts[0]);
        } catch (UnknownHostException ex)
        {
            return null;
        }
        
        return addr;
    }
    
    public void jogadorMeConvidou(String apelido, InetAddress addr)
    {
        
        String msg;
        if(estaJogando)
        {
            mostraMensagem(MSG_INFO, MSG_PROTO_NENHUM, addr.getHostAddress(),
                    -1, "Convite recusado automaticamente");
            
            
            msg = apelido + "|0";
            enviarMensagemUDP(addr, 5, msg);
            
            return;
        }
        
        
        fuiConvidado = true;
        board.statusLabel.setText("");
        addrJogadorRemoto = null;
        
        
        msg = "O jogador " + apelido + " está te convidando para um jogo\nAceita?";
        int resp = JOptionPane.showConfirmDialog(board, msg, "Convite para jogar",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        
        
        if (resp == JOptionPane.NO_OPTION)
        {
            msg = apelidoLocal + "|0";
            enviarMensagemUDP(addr, 5, msg);
            mostraMensagem(MSG_INFO, MSG_PROTO_NENHUM, "", 0, "Convite não foi aceito");
            return;
        }
        
        
        
        servidorTCP = criarSocketTCP();
        if (servidorTCP == null)
        {
            JOptionPane.showMessageDialog(null,
                    "Erro na criação da conexão TCP.",
                    "Conexão do jogador remoto",
                    JOptionPane.ERROR_MESSAGE);
            
            
            msg = apelidoLocal + "|0";
            enviarMensagemUDP(addr, 5, msg);
            board.statusLabel.setText("");
            return;
        }
        
        
        
        addrJogadorRemoto = addr;
        apelidoRemoto = apelido;
        tcpEscutaThread = new LstnTCP(this, servidorTCP, addr);
        tcpEscutaThread.execute();
            
        
        msg = apelidoLocal + "|" + servidorTCP.getLocalPort();
        enviarMensagemUDP(addr, 5, msg);
        
        esperandoConexao = true;
        esperandoConfirmacao = true;
        esperandoInicioJogo = true;
        board.statusLabel.setText("AGUARDANDO CONEXÃO");
        timeoutAguardandoJogadorRemoto.start();
    }
    
    public void jogadorRemotoConectou(CnctTCP conexao)
    {
        esperandoConexao = false;
        this.conexaoTCP = conexao;
        servidorTCP = null;     
        iniciarSerieJogos();
    }
    
    public void jogadorRemotoConfirmou(InetAddress addr)
    {
        
        if (addr.equals(addrJogadorRemoto) == false)
            return;

        esperandoConfirmacao = false;
        
        iniciarSerieJogos();
    }
    
    
    private ServerSocket criarSocketTCP()
    {
        InetAddress addr = obtemInterfaceRede();
        if(addr == null)
            return null;

        ServerSocket socket;
        try
        {
            
            
            
            
            
            socket = new ServerSocket(0, 1, addr);
            socket.setReuseAddress(true);
        } catch (IOException e)
        {
            return null;
        }
        
        return socket;
    }
    
    /**
     * Encerra o socket e thread criados para gerenciar a
     * conexão TCP estabelecida entre os jogadores local
     * e remoto durante um jogo.
     * 
     * @param motivo
     * Indica o motivo do encerramento da conexão, a saber:
     *      <dl>
     *      <dt>0: timeout (jogador remoto não conectou)</dt>
     *      <dt>1: conexão caiu;</dt>
     *      <dt>2: jogador remoto desistiu do jogo;</dt>
     *      <dt>3: fim do jogo</dt>
     *      </dl>
     */
    
    public void encerrarConexaoTCP(int motivo)
    {
        
        if(estaJogando)
        {
            estaJogando = false;
            zeraResultados();
            limpaTabuleiro();
        }
        
        
        int portaRemota = 0;
        String enderecoRemoto = "";
        if ((conexaoTCP != null) && (conexaoTCP.getSocket() != null))
        {
            portaRemota = conexaoTCP.getSocket().getPort();
            if (conexaoTCP.getSocket().getRemoteSocketAddress() != null)
                enderecoRemoto = conexaoTCP.getSocket().getRemoteSocketAddress().toString();
        }
        
        
        try
        {
            if(servidorTCP != null)
                servidorTCP.close();
            
            if(tcpEscutaThread != null)
                tcpEscutaThread.cancel(true);
        } catch(IOException ex)
        {
        }
        servidorTCP = null;
        tcpEscutaThread = null;
        
        
        if (conexaoTCP != null)
            conexaoTCP.cancel(true);
        
        conexaoTCP = null;
        
        if(motivo == CONEXAO_TIMEOUT)
            JOptionPane.showMessageDialog(null,
                    "TIMEOUT: aguardando conexão remota.",
                    "Encerrar jogo",
                    JOptionPane.WARNING_MESSAGE);
        
        if(motivo == CONEXAO_CAIU)
            JOptionPane.showMessageDialog(null,
                    "Conexão com jogador remoto caiu.",
                    "Encerrar jogo",
                    JOptionPane.WARNING_MESSAGE);
        
        if (motivo == JOGADOR_DESISTIU)
            JOptionPane.showMessageDialog(null,
                    "Jogador remoto desistiu do jogo.",
                    "Encerrar jogo",
                    JOptionPane.WARNING_MESSAGE);
        
        esperandoConexao = esperandoInicioJogo = false;
        esperandoConfirmacao = esperandoJogadorRemoto = false;
        
        board.statusLabel.setText("");
        
        mostraMensagem(MSG_INFO, MSG_PROTO_TCP, enderecoRemoto, portaRemota, "Conexão foi encerrada.");
        mostraMensagem(MSG_INFO, MSG_PROTO_NENHUM, "", 0, "Fim do jogo");
    }
    
    private void iniciarSerieJogos()
    {
        if (esperandoConexao || esperandoConfirmacao)
            return;
        
        
        if (timeoutAguardandoJogadorRemoto.isRunning())
            timeoutAguardandoJogadorRemoto.stop();
        
        
        board.JogadorRemotoNameLabel.setText(SIMBOLO_REMOTO + " - " + apelidoRemoto);
        board.JogadorRemotoNameLabel.setEnabled(true);
        
        if (fuiConvidado)
        {
            
            int n = numAleatorio.nextInt(2) + 1;
            if (n == JOGADOR_LOCAL)
            {
                
                minhaVez = inicieiUltimoJogo = true;
                board.statusLabel.setText("SUA VEZ");
            }
            else
            {
                
                minhaVez = inicieiUltimoJogo = false;
                board.statusLabel.setText("ESPERANDO JOGADOR");
            }
            String compl = String.valueOf(n);
            conexaoTCP.enviarMensagemTCP(7, compl);
        }
        
        estaJogando = true;
        jogoAtual = 1;
        zeraResultados();
        
        board.pontuacaoYouLabel.setEnabled(true);
        board.pontuacaoRemotoLabel.setEnabled(true);
    }
    
    private void limpaTabuleiro()
    {
        
        int pos = 0;
        for(int i = 0; i < 3; ++i)
        {
            for(int j = 0; j < 3; ++j)
            {
                tabuleiro[i][j] = SIMBOLO_VAZIO;
                switch(pos)
                {
                    case 0: board.jLabel00.setText(""); break;
                    case 1: board.jLabel01.setText(""); break;
                    case 2: board.jLabel02.setText(""); break;
                    case 3: board.jLabel10.setText(""); break;
                    case 4: board.jLabel11.setText(""); break;
                    case 5: board.jLabel12.setText(""); break;
                    case 6: board.jLabel20.setText(""); break;
                    case 7: board.jLabel21.setText(""); break;
                    case 8: board.jLabel22.setText(""); break;
                }
                ++pos;
            }
        }
    }
    
    public void zeraResultados()
    {
        String nomeRemoto;
        Color corTabuleiro;
        if (estaJogando)
        {
            nomeRemoto = SIMBOLO_REMOTO + " - " + apelidoRemoto;
            corTabuleiro = Color.BLACK;
        }
        else
        {
            nomeRemoto = SIMBOLO_REMOTO + " - Jogador Remoto";
            corTabuleiro = Color.DARK_GRAY;
        }
        
        board.JogadorRemotoNameLabel.setText(nomeRemoto);
        
        
        for(int i = 0; i < 5; ++i)
            resultados[i] = SEM_RESULTADO;
        mostraResultados();
        
        
        limpaTabuleiro();
        
        board.JogadorRemotoNameLabel.setEnabled(estaJogando);
        board.pontuacaoYouLabel.setEnabled(estaJogando);
        board.pontuacaoRemotoLabel.setEnabled(estaJogando);
    }
    
    public void mostraResultados()
    {
        javax.swing.JLabel ctrlLabel = null;
        Color cor;
        int local = 0, remoto = 0;
        for(int i = 0; i < 5; ++i)
        {
            switch(i)
            {
                case 0: ctrlLabel = board.game1; break;
                case 1: ctrlLabel = board.game2; break;
                case 2: ctrlLabel = board.game3; break;
                case 3: ctrlLabel = board.game4; break;
                case 4: ctrlLabel = board.game5; break;
            }
            
            cor = Color.DARK_GRAY;
            if (estaJogando)
            {
                if(((i + 1) == jogoAtual) && (resultados[i] == SEM_RESULTADO))
                    cor = Color.BLACK;
                else
                {
                    switch (resultados[i])
                    {
                        case VITORIA_LOCAL:
                            ++local;
                            cor = COR_LOCAL;
                            break;
                        case VITORIA_REMOTO:
                            ++remoto;
                            cor = COR_REMOTO;
                            break;
                        default:
                            
                            cor = COR_EMPATE;
                            break;
                    }
                }
                
                ctrlLabel.setEnabled((i + 1) <= jogoAtual);
            }
            else
                ctrlLabel.setEnabled(false);
            ctrlLabel.setForeground(cor);
        }
        
        board.pontuacaoYouLabel.setText(String.valueOf(local));
        board.pontuacaoRemotoLabel.setText(String.valueOf(remoto));
    }
}
