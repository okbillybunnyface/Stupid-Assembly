import java.util.Scanner;
import java.util.HashMap;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Stack;
import java.util.Random;

public class SassCore implements TokenManager, SassParserConstants
{
	public static String accumulator = "";
	public static String[] register = new String[4];
	public static String[] memory = new String[10];
	public static int address = 0;

	private static String[] code;
	private static HashMap<String,Integer> labels;
	private static HashMap<String,Integer> pointers;
	private static int line;
	private static Token currentToken;
	private static Stack<String> memStack;
	private static Stack<Integer> progStack;
	private static Random random;
	private boolean debug = false;

	public static final String _pointer = "(?:&[a-zA-Z_][a-zA-Z0-9_]*)",
		_labelRef = "(?:@[a-zA-Z0-9_]+)",
		_labelDecl = "(?:^@[a-zA-Z0-9_]+$)",
		_string = "(?:\"(?:[^\\n\\r\"])*\")",
		_number = "(?:(?:\\-)?\\d+(?:\\.\\d+)?)",
		_memset = "(?:MSET)",
		_mem = "(?:MEM)",
		_address = "(?:ADDR)",
		_teqs = "(?:TEQS)",
		_teqn = "(?:TEQN)",
		_tnes = "(?:TNES)",
		_tnen = "(?:TNEN)",
		_tgts = "(?:TGTS)",
		_tgtn = "(?:TGTN)",
		_tges = "(?:TGES)",
		_tgen = "(?:TGEN)",
		_tlts = "(?:TLTS)",
		_tltn = "(?:TLTN)",
		_tles = "(?:TLES)",
		_tlen = "(?:TLEN)",
		_tnum = "(?:TNUM)",
		_jump = "(?:JUMP)",
		_jsr = "(?:JSR)",
		_ret = "(?:RET)",
		_acc = "(?:ACC)",
		_data = "(?:DATA)",
		_datb = "(?:DATB)",
		_datc = "(?:DATC)",
		_datd = "(?:DATD)",
		_push = "(?:PUSH)",
		_pop = "(?:POP)",
		_top = "(?:TOP)",
		_out = "(?:OUT)",
		_in = "(?:IN)",
		_move = "(?:MOVE|MOV)",
		_mult = "(?:MULT|MUL)",
		_div = "(?:DIV)",
		_sub = "(?:SUB)",
		_add = "(?:ADD)",
		_cat = "(?:CAT)",
		_rand = "(?:RAND)",
		_char = "(?:CHAR)",
		_length = "(?:LGTH)",
		_exit = "(?:EXIT)";

	private String regex = _pointer
		+ "|" + _labelRef
		+ "|" + _string
		+ "|" + _number
		+ "|" + _memset
		+ "|" + _mem
		+ "|" + _address
		+ "|" + _teqs
		+ "|" + _teqn
		+ "|" + _tnes
		+ "|" + _tnen
		+ "|" + _tgts
		+ "|" + _tgtn
		+ "|" + _tges
		+ "|" + _tgen
		+ "|" + _tlts
		+ "|" + _tltn
		+ "|" + _tles
		+ "|" + _tlen
		+ "|" + _tnum
		+ "|" + _jump
		+ "|" + _jsr
		+ "|" + _ret
		+ "|" + _acc
		+ "|" + _data
		+ "|" + _datb
		+ "|" + _datc
		+ "|" + _datd
		+ "|" + _push
		+ "|" + _pop
		+ "|" + _top
		+ "|" + _out
		+ "|" + _in
		+ "|" + _move
		+ "|" + _mult
		+ "|" + _div
		+ "|" + _sub
		+ "|" + _add
		+ "|" + _cat
		+ "|" + _rand
		+ "|" + _char
		+ "|" + _length
		+ "|" + _exit;

	public SassCore(String file, boolean debug) throws java.io.FileNotFoundException
	{
		line = 0;
		memStack = new Stack<String>();
		memSet(10);
		progStack = new Stack<Integer>();
		pointers = new HashMap<String,Integer>();
		random = new Random();
		for(int i = 0; i < register.length; i++)
		{
			register[i] = "";
		}
		this.debug = debug;
		//Load file
		Scanner sc = new Scanner(new File(file));
		ArrayList<String> lines = new ArrayList<String>();
		while (sc.hasNextLine()) {
		  lines.add(sc.nextLine().replaceAll("(?://.*)$","").replaceAll("(?:\\s*)$",""));
		}
		code = lines.toArray(new String[0]);
		sc.close();

		//Populate labels
		labels = new HashMap<String,Integer>();
		for(int i = 0; i < code.length; i++)
		{
			if(code[i].matches(_labelDecl))
			{
				labels.put(code[i],new Integer(i));
			}
		}
	}

	public static double random()
	{
		return random.nextDouble();
	}

	public static int getPointer(String key)
	{
		return pointers.get(key).intValue();
	}
	public static void setPointer(String key, int value)
	{
		pointers.put(key,new Integer(value));
	}

	public static void memSet(int size)
	{
		memory = new String[size];
		for(int i = 0; i < memory.length; i++)
		{
			memory[i] = "";
		}
	}

	public static void jump(String label)
	{
		line = labels.get(label).intValue();
	}

	public static void skipLine()
	{
		line++;
	}

	public static void beginSubroutine(String label)
	{
		progStack.push(new Integer(line));
		jump(label);
	}
	public static void endSubroutine()
	{
		line = progStack.pop().intValue();
	}

	public static void memPush(String a)
	{
		memStack.push(a);
	}
	public static String memPop()
	{
		return memStack.pop();
	}
	public static String memTop()
	{
		return memStack.peek();
	}

	public Token getNextToken()
	{
		Token temp = currentToken;
		currentToken = currentToken.next;
		return temp;
	}

	public boolean hasNextLine()
	{
		while(line < code.length && code[line].matches("(?:^\\s*$)|"+_labelDecl)) line++;
		return (line < code.length);
	}

	public void prepareNextLine() throws ParseException
	{
		if(!hasNextLine()) System.exit(-1);

		String[] candidates = getMatches(code[line]);
		ArrayList<Token> tokenList = new ArrayList<Token>();

		Token newToken;
		for(int i = 0; i < candidates.length; i++)
		{
			newToken = new Token();
			newToken.image = candidates[i];
			newToken.kind = getTokenKind(newToken.image);
			if(newToken.kind == -1) throw new ParseException("Parse Exception on line " + line + ": " + code[line]);
			newToken.beginLine = line;
			newToken.endLine = line;
			tokenList.add(newToken);
		}

		newToken = new Token();
		newToken.image = "";
		newToken.kind = EOL;
		newToken.beginLine = line;
		newToken.endLine = line;
		tokenList.add(newToken);

		Token[] tokenArray = tokenList.toArray(new Token[0]);
		String tokenDebug = "";
		for(int i = 0; i < tokenArray.length; i++)
		{
			tokenDebug+=tokenArray[i].image+","+tokenArray[i].kind+"|";
		}
		for(int i = tokenArray.length-2; i >= 0; i--)
		{
			tokenArray[i].next = tokenArray[i+1];
		}
		currentToken = tokenArray[0];

		if(debug)IO.out(tokenDebug);
		line++;
	}

	private String[] getMatches(String line) throws ParseException
	{
		//Check for bad tokens
		String[] interTokens = line.split(regex);
		for(int i = 0; i < interTokens.length; i++)
		{
			if(interTokens[i].matches("[^\\s]")) throw new ParseException("Invalid token on line " + line + ": " + interTokens[i]);
		}

		//Populate matches
		List<String> allMatches = new ArrayList<String>();
		Matcher m = Pattern.compile(regex).matcher(line);
		while (m.find()) 
		{
			allMatches.add(m.group());
		}

		return allMatches.toArray(new String[0]);
	}

	private void appendToken(String image, int line)
	{
		Token newToken = new Token();
		newToken.image = image;
		newToken.kind = getTokenKind(image);
		newToken.beginLine = line;
		newToken.endLine = line;

		appendToken(newToken);
	}

	private void appendToken(Token t)
	{
		Token currentToken = this.currentToken;
		while(currentToken.next != null) currentToken = currentToken.next;
		currentToken.next = t;
	}

	private int getTokenKind(String image)
	{
		if(image.matches("^"+_pointer+"$"))
		{
			return POINTER;
		}
		else if(image.matches("^"+_labelRef+"$"))
		{
			return LABEL;
		}
		else if(image.matches("^"+_string+"$"))
		{
			return STRING;
		}
		else if(image.matches("^"+_number+"$"))
		{
			return NUMBER;
		}
		else if(image.matches("^"+_memset+"$"))
		{
			return MSET;
		}
		else if(image.matches("^"+_mem+"$"))
		{
			return MEM;
		}
		else if(image.matches("^"+_address+"$"))
		{
			return ADDR;
		}
		else if(image.matches("^"+_teqs+"$"))
		{
			return TEQS;
		}
		else if(image.matches("^"+_teqn+"$"))
		{
			return TEQN;
		}
		else if(image.matches("^"+_tnes+"$"))
		{
			return TNES;
		}
		else if(image.matches("^"+_tnen+"$"))
		{
			return TNEN;
		}
		else if(image.matches("^"+_tgts+"$"))
		{
			return TGTS;
		}
		else if(image.matches("^"+_tgtn+"$"))
		{
			return TGTN;
		}
		else if(image.matches("^"+_tges+"$"))
		{
			return TGES;
		}
		else if(image.matches("^"+_tgen+"$"))
		{
			return TGEN;
		}
		else if(image.matches("^"+_tlts+"$"))
		{
			return TLTS;
		}
		else if(image.matches("^"+_tltn+"$"))
		{
			return TLTN;
		}
		else if(image.matches("^"+_tles+"$"))
		{
			return TLES;
		}
		else if(image.matches("^"+_tlen+"$"))
		{
			return TLEN;
		}
		else if(image.matches("^"+_tnum+"$"))
		{
			return TNUM;
		}
		else if(image.matches("^"+_jump+"$"))
		{
			return JUMP;
		}
		else if(image.matches("^"+_jsr+"$"))
		{
			return JSR;
		}
		else if(image.matches("^"+_ret+"$"))
		{
			return RET;
		}
		else if(image.matches("^"+_acc+"$"))
		{
			return ACC;
		}
		else if(image.matches("^"+_data+"$"))
		{
			return DATA;
		}
		else if(image.matches("^"+_datb+"$"))
		{
			return DATB;
		}
		else if(image.matches("^"+_datc+"$"))
		{
			return DATC;
		}
		else if(image.matches("^"+_datd+"$"))
		{
			return DATD;
		}
		else if(image.matches("^"+_push+"$"))
		{
			return PUSH;
		}
		else if(image.matches("^"+_pop+"$"))
		{
			return POP;
		}
		else if(image.matches("^"+_top+"$"))
		{
			return TOP;
		}
		else if(image.matches("^"+_out+"$"))
		{
			return OUT;
		}
		else if(image.matches("^"+_in+"$"))
		{
			return IN;
		}
		else if(image.matches("^"+_move+"$"))
		{
			return MOVE;
		}
		else if(image.matches("^"+_mult+"$"))
		{
			return MUL;
		}
		else if(image.matches("^"+_div+"$"))
		{
			return DIV;
		}
		else if(image.matches("^"+_sub+"$"))
		{
			return SUB;
		}
		else if(image.matches("^"+_add+"$"))
		{
			return ADD;
		}
		else if(image.matches("^"+_cat+"$"))
		{
			return CAT;
		}
		else if(image.matches("^"+_rand+"$"))
		{
			return RAND;
		}
		else if(image.matches("^"+_char+"$"))
		{
			return CHAR;
		}
		else if(image.matches("^"+_length+"$"))
		{
			return LGTH;
		}
		else if(image.matches("^"+_exit+"$"))
		{
			return EXIT;
		}
		else return -1;
	}
}