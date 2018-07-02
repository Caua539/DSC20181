package br.ufg.inf.es.dsc20181.jogodavelhaonline.model;

import br.ufg.inf.es.dsc20181.jogodavelhaonline.controller.GameController;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import javax.swing.SwingWorker;


public class CnctTCP extends SwingWorker<Boolean, String> {
    private final GameController mainController;
    private final Socket socket;
    
     
    private InputStream entrada;  
    private InputStreamReader inr;  
    private BufferedReader bfr;
    
     
    private OutputStream saida;  
    private OutputStreamWriter outw;  
    private BufferedWriter bfw;      
    
    public Socket getSocket()
    {
        return socket;
    }
    
    public CnctTCP (GameController mainController, Socket socket)
    {
        this.mainController = mainController;
        this.socket = socket;
        try
        {
            entrada  = this.socket.getInputStream();
            inr = new InputStreamReader(entrada, "ISO-8859-1");
            bfr = new BufferedReader(inr);
            
            saida =  this.socket.getOutputStream();
            outw = new OutputStreamWriter(saida, "ISO-8859-1");
            bfw = new BufferedWriter(outw); 
        }
        catch (IOException e)
        {
            mainController.mostraMensagem(GameController.MSG_ERRO,
                            GameController.MSG_PROTO_TCP,
                            socket.getRemoteSocketAddress().toString(),
                            socket.getPort(),
                            "Erro: criação da nova conexão");
        } 
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        String msg;
        while(true)
        {
            try
            {
                msg = (String)bfr.readLine();
                if (msg != null)
                {
                     
                    if (msg.length() < 5)
                    {
                        mainController.mostraMensagem(GameController.MSG_IN,
                                GameController.MSG_PROTO_TCP,
                                socket.getRemoteSocketAddress().toString(),
                                socket.getPort(),
                                "Mensagem inválida [" + msg + "]");
                        continue;
                    }

                     
                    int tam = Integer.parseInt(msg.substring(2, 5));
                    if (msg.length() != tam)
                    {
                        mainController.mostraMensagem(GameController.MSG_IN,
                                GameController.MSG_PROTO_TCP,
                                socket.getRemoteSocketAddress().toString(),
                                socket.getPort(),
                                "Erro: tamanho da mensagem [" + msg + "]");
                        continue;
                    }

                     
                    mainController.mostraMensagem(GameController.MSG_IN,
                                GameController.MSG_PROTO_TCP,
                                socket.getRemoteSocketAddress().toString(),
                                socket.getPort(), msg);

                     
                    String complemento = "";
                    if(tam > 5)
                        complemento = msg.substring(5);

                    int pos;
                    int nMsg = Integer.parseInt(msg.substring(0, 2));
                    switch(nMsg)
                    {
                        case 7:

                            pos = Integer.parseInt(complemento);
                            if((pos == 1) || (pos == 2))
                                mainController.quemIniciaJogo(pos);
                            break;
                        
                        case 8:
                             
                             
                            pos = Integer.parseInt(complemento);
                            if((pos > 0) && (pos < 10))
                                mainController.marcaPosicao(GameController.JOGADOR_REMOTO, pos);
                            break;
                        
                        case 9:
                             
                            mainController.jogadorRemotoIniciaNovoJogo();
                            break;
                        
                        case 10: 
                            mainController.encerrarConexaoTCP(GameController.JOGADOR_DESISTIU);
                            break;
                            
                        default:
                            mainController.mostraMensagem(GameController.MSG_IN,
                                    GameController.MSG_PROTO_TCP,
                                    socket.getRemoteSocketAddress().toString(),
                                    socket.getPort(),
                                    "Mensagem inválida [" + msg + "]");
                    }
                }
                else
                {
                     
                    bfr.close();
                    inr.close();  
                    entrada.close();  
                    bfw.close();
                    outw.close();  
                    saida.close();  
                    socket.close();
                    
                    mainController.encerrarConexaoTCP(GameController.CONEXAO_CAIU);
                    
                    Thread.currentThread().stop();
                }
            }
            catch(IOException ex)
            {
                 
                mainController.mostraMensagem(GameController.MSG_IN,
                        GameController.MSG_PROTO_TCP,
                        socket.getRemoteSocketAddress().toString(),
                        socket.getPort(), ex.getMessage());
                return false;
            }
        }
    }
    
    public boolean enviarMensagemTCP(int numero, String compl)
    {
        String msg = "";
        try
        {
            if((compl == null) || compl.isEmpty())
                msg = String.format("%02d005", numero);
            else
                msg = String.format("%02d%03d%s", numero, 5 + compl.length(),
                                    compl);

            outw.write(msg + "\n");
            outw.flush();
            
             
            mainController.mostraMensagem(GameController.MSG_OUT,
                            GameController.MSG_PROTO_TCP,
                            socket.getRemoteSocketAddress().toString(),
                            socket.getPort(), msg);
            
            return true;
        }catch(IOException ex)
        {
            mainController.mostraMensagem(GameController.MSG_OUT,
                    GameController.MSG_PROTO_TCP,
                    socket.getRemoteSocketAddress().toString(),
                    socket.getPort(),
                    "Erro: envio da mensagem [" + msg + "]");
            return false;
        }
    }}
