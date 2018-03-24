package aula01;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class Atividade01 {
	public static void main (String[] args) throws IOException {
		FileInputStream fis = new FileInputStream(args[0]);
		DataInputStream dis = new DataInputStream(fis);
		
		int valor = dis.readInt();
		dis.close();
		System.out.println(Integer.toHexString(valor));
		
	}

}
