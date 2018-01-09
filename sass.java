public class sass
{
	private static SassCore core;
	private static SassParser parser;
	public static void main(String[] args)
	{
		if(args.length != 1 && args.length != 2)
		{
			IO.out("Usage: java sass file.sass");
			return;
		}
		else
		{
			try
			{
				if(args.length == 2 && args[1] == "d")
				{
					core = new SassCore(args[0], true);
				}
				else core = new SassCore(args[0], false);
			}
			catch(java.io.FileNotFoundException e)
			{
				IO.out("File " + args[0] + " not found.");
				return;
			}
		}

		parser = new SassParser(core);
		executeLine();
	}

	private static void executeLine()
	{
		while(core.hasNextLine())
		{
			try
			{
				core.prepareNextLine();
				parser.parseLine();
			}
			catch (ParseException e)
			{
				IO.out(e.getMessage());
			}
		}
	}
}