import java.util.Scanner;

public class IO
{
	private static Scanner scan = new Scanner(System.in);

	private IO(){}

	public static String in()
	{
		return scan.next();
	}

	public static void out(String msg)
	{
		System.out.println(msg);
	}
}