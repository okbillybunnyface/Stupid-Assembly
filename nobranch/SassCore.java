import java.util.Scanner;
import java.util.HashMap;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.nio.charset.Charset;

public class SassCore
{
	public static String accumulator = "";

	private static String[] code;
	private static HashMap<String,Integer> labels;
	private static int line;

	private SassCore(){}

	public static void initialize(String file) throws java.io.FileNotFoundException
	{
		line = 0;

		//Load file
		Scanner sc = new Scanner(new File(file));
		ArrayList<String> lines = new ArrayList<String>();
		while (sc.hasNextLine()) {
		  lines.add(sc.nextLine());
		}
		code = lines.toArray(new String[0]);
		sc.close();

		//Populate labels
		labels = new HashMap<String,Integer>();
		for(int i = 0; i < code.length; i++)
		{
			if(code[i].matches("^@[A-Za-z0-9_]+$"))
			{
				labels.put(code[i],new Integer(i+1));
			}
		}
	}

	public static void jump(String label)
	{
		line = labels.get(label).intValue();
	}

	public static ByteArrayInputStream next(boolean iterate)
	{
		if(line < code.length)
		{
			return new ByteArrayInputStream(Charset.forName("UTF-16").encode(code[iterate?line++:line]).array());
		}
		else System.exit(-1);

		return null;
	}
}