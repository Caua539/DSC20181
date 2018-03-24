package aula01;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Atividade00 {
	public static void main (String[] args) throws IOException {
		int inteiro = 2;
		FileOutputStream fos = new FileOutputStream(args[0]);
		DataOutputStream dos = new DataOutputStream(fos);
		dos.writeInt(inteiro);
		dos.close();

	}
}
