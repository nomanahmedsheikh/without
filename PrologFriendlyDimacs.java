import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class PrologFriendlyDimacs {

	public static void main(String[] args) throws IOException {
		
		if(args.length < 2) {
			System.out.println("PrologFriendlyDimacs <dimacs-file> <prolog-friendly-file>");
			System.exit(1);
		}
		
		FileInputStream fstream = new FileInputStream(args[0]);
		PrintWriter writer = new PrintWriter(args[1], "UTF-8");

		
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		// Read File Line By Line
		while ((strLine = br.readLine()) != null) {
			// Print the content on the console
			
			if(strLine.startsWith("c"))
				continue;
			
			String[] tokens = strLine.split(" ");
			
			if(strLine.startsWith("p")) {
				writer.print("p(");
				for (int i = 1; i < tokens.length - 1; i++) {
					if(tokens[i].trim().isEmpty())
						continue;
					writer.print(tokens[i] + ", ");
				}
				writer.println(tokens[tokens.length - 1] + ").");
			} else {
				writer.print("[");
				for (int i = 0; i < tokens.length - 2; i++) {
					if(tokens[i].trim().isEmpty())
						continue;
					writer.print(tokens[i] + ", ");
				}
				writer.println(tokens[tokens.length - 2] + "].");
			}
			
		}
		// Close the input stream
		in.close();
		writer.close();	

	}

}
