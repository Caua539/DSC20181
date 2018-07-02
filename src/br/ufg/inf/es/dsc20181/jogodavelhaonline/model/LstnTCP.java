package br.ufg.inf.es.dsc20181.jogodavelhaonline.model;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.SwingWorker;
import br.ufg.inf.es.dsc20181.jogodavelhaonline.controller.GameController;

public class LstnTCP extends SwingWorker<Boolean, String> {
    private final GameController mainController;
    private final ServerSocket socket;
    private final InetAddress addrRemoto;
    
    public LstnTCP(GameController mainController, ServerSocket socket,
                     InetAddress addrRemoto)
    {
        this.mainController = mainController;
        this.socket = socket;
        this.addrRemoto = addrRemoto;
    }
    
    @Override
    protected Boolean doInBackground() throws Exception {
        try
        {
            while(true)
            {
                 
                Socket connection = socket.accept();
                
                 
                if (connection.getInetAddress().equals(addrRemoto) == false)
                {
                     
                    connection.close();
                    
                    mainController.mostraMensagem(GameController.MSG_IN,
                            GameController.MSG_PROTO_TCP,
                            connection.getRemoteSocketAddress().toString(),
                            connection.getPort(), 
                            "Tentativa de conexão na porta " + socket.getLocalPort());
                }
                else
                {
                     
                    CnctTCP novaConexao = new CnctTCP(mainController, connection); 
                    
                     
                    novaConexao.execute();
                    
                     
                    mainController.jogadorRemotoConectou(novaConexao);
                    
                     
                    return true;
                }
            }
        }catch (IOException ex)
        {
            return false;
        }
    }
    
    public void encerraConexao()
    {
        try
        {
            if (socket.isClosed() == false)
                socket.close();
        } catch (IOException ex)
        {
        }
    }
}
