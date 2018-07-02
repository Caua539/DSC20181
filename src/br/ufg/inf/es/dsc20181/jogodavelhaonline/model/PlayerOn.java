
package br.ufg.inf.es.dsc20181.jogodavelhaonline.model;

import java.net.InetAddress;

public class PlayerOn {
    private final String apelido;
    private final InetAddress addr;
    private int portaTCP;
    private boolean aindaOnline;     

    public PlayerOn(String apelido, InetAddress addr)
    {
        this.apelido = apelido;
        this.addr = addr;
        this.portaTCP = 0;
        this.aindaOnline = true;
    }
    
    public String getApelido()
    {
        return this.apelido;
    }
    
    public InetAddress getAddress()
    {
        return this.addr;
    }
    
    public int getPorta()
    {
        return this.portaTCP;
    }
    
    public void setPorta(int porta)
    {
        this.portaTCP = porta;
    }
    
    public void setAindaOnline(boolean b)
    {
        this.aindaOnline = b;
    }
    
    public boolean getAindaOnline()
    {
        return this.aindaOnline;
    }

    public boolean mesmoApelido(String apelido) {
        return (this.apelido.compareToIgnoreCase(apelido) == 0);
    }
}


