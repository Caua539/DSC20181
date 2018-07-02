package br.ufg.inf.es.dsc20181.jogodavelhaonline.model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import javax.swing.SwingWorker;
import br.ufg.inf.es.dsc20181.jogodavelhaonline.controller.GameController;

public class LstnUDP extends SwingWorker<Void, String>  {
    private GameController mainController;    
    private String apelidoLocal;         
    private DatagramSocket udpSocket;
    private int porta;
    private InetAddress addrLocal;

    public LstnUDP(GameController mainController, int porta,
                     String apelidoLocal, InetAddress addr) throws SocketException
    {
        this.mainController = mainController;
        this.porta = porta;
        this.apelidoLocal = apelidoLocal;
        this.addrLocal = addr;
        udpSocket = new DatagramSocket(porta, addr);
        udpSocket.setReuseAddress(true);

    }

    @Override
    protected Void doInBackground() throws Exception {
         
        String msg;

        while (true)
        {
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

             
            try {
                udpSocket.receive(packet);
            } catch (IOException ex)
            {
                mainController.mostraMensagem(GameController.MSG_IN,
                        GameController.MSG_PROTO_UDP,
                        packet.getAddress().getHostAddress(),
                        packet.getPort(), ex.getMessage());
                continue;
            }
            
             
            msg = new String(packet.getData()).trim();
            
             
            mainController.mostraMensagem(GameController.MSG_IN,
                        GameController.MSG_PROTO_UDP,
                        packet.getAddress().getHostAddress(),
                        packet.getPort(), msg);
            
             
            if (msg.length() < 5)
            {
                mainController.mostraMensagem(GameController.MSG_IN,
                        GameController.MSG_PROTO_UDP,
                        packet.getAddress().getHostAddress(),
                        packet.getPort(), "Mensagem inválida [" + msg + "]");
                continue;
            }
            
             
            int tam = Integer.parseInt(msg.substring(2, 5));
            if (msg.length() != tam)
            {
                mainController.mostraMensagem(GameController.MSG_IN,
                        GameController.MSG_PROTO_UDP,
                        packet.getAddress().getHostAddress(),
                        packet.getPort(),
                        "Erro: tamanho da mensagem [" + msg + "]");
                continue;
            }

             
            if(packet.getAddress().equals(addrLocal))
                continue;
            
            String complemento = "";
            if (tam > 5)
                complemento = msg.substring(5);
            
            int nMsg = Integer.parseInt(msg.substring(0, 2));
            switch(nMsg)
            {
                case 1:
                case 2:
                    mainController.adicionaJogador(nMsg, complemento, packet.getAddress()); break;
                    
                case 3:
                    mainController.removeJogador(complemento);
                    break;
                    
                case 4:
                    mainController.jogadorMeConvidou(complemento, packet.getAddress());
                    break;
                    
                case 5:
                    mainController.respostaConvite(complemento, packet.getAddress());
                    break;
                    
                case 6:
                    mainController.jogadorRemotoConfirmou(packet.getAddress());
                    break;
                    
                default:
                    mainController.mostraMensagem(GameController.MSG_IN,
                                GameController.MSG_PROTO_UDP,
                                packet.getAddress().getHostAddress(),
                                packet.getPort(),
                                "Mensagem inválida [" + msg + "]");
            }
        }
    }
    
    public void encerraConexao()
    {
        if (udpSocket.isConnected())
            udpSocket.disconnect();
        
        udpSocket.close();
    }
}
