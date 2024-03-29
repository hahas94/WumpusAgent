package wumpus;

import partialObservability.ManualAI;
import partialObservability.MyAI;
import partialObservability.RandomAI;

import fullObservability.*;

import java.util.Random;
import java.io.IOException;
import java.io.File;
import java.util.Scanner;

public class World
{
	// ===============================================================
	// =						Declarations
	// ===============================================================	
	
	// Tile Structure
	public static class Tile
	{
		boolean pit    = false;
		boolean wumpus = false;
		boolean gold   = false;
		boolean breeze = false;
		boolean stench = false;

		// own defined functions
		public void setPit(){this.pit = true;}
		public void setWumpus(){this.wumpus = true;}
		public void unSetPit(){this.pit = false;}

	    public boolean getPit() {return this.pit;}
	    public boolean getWumpus() {return this.wumpus;}
	    public boolean getGold() {return this.gold;}
	    public boolean getBreeze() {return this.breeze;}
	    public boolean getStench() {return this.stench;}
	}
	
	// Operation Variables
	private boolean 	debug;			// If true, displays board info after every move
	private boolean		manualAI;		// If true, alters the behavior of debug for flow purposes
	private boolean     search;			// if true, we are in total observable case, passes 'board' to the agent!
	
	// Agent Variables
	private Agent 	agent;			// The agent
	private int 	score;			// The agent's score
	private boolean	goldLooted;		// True if gold was successfully looted
	private boolean	hasArrow;		// True if the agent can shoot
	private boolean	bump;			// Bump percept flag
	private boolean	scream;			// Scream percept flag
	private int		agentDir;		// The direction the agent is facing: 0 - right, 1 - down, 2 - left, 3 - up
	private int		agentX;			// The column where the agent is located ( x-coord = col-coord )
	private int		agentY;			// The row where the agent is located ( y-coord = row-coord )

	private Agent.Action	lastAction;	// The last action the agent made
	
	// Board Variables
	private int			colDimension;	// The number of columns the game board has
	private int			rowDimension;	// The number of rows the game board has
	private Tile[][]	board;			// The game board
	
	// Random Variable
	private Random rand;
	
	// ===============================================================
	// =						Constructors
	// ===============================================================	
	
	public World ( ) throws Exception
	{
		this ( false, false, false, false, null );
	}
	
	public World ( boolean _debug, boolean _randomAI, boolean _manualAI, boolean _search ) throws Exception
	{
		this ( _debug, _randomAI, _manualAI, _search, null );
	}
	
	public World ( boolean _debug, boolean _randomAI, boolean _manualAI, boolean _search, File worldFile ) throws Exception
	{
		// Operation Flags
		debug        = _debug;
		manualAI     = _manualAI;
		search       = _search;
		
		// Agent Initialization
		goldLooted   = false;
		hasArrow     = true;
		bump         = false;
		scream       = false;
		score        = 0;
		agentDir     = 0;
		agentX       = 0;
		agentY       = 0;
		lastAction   = Agent.Action.CLIMB;
		
		// Board Initialization
		if ( worldFile != null )
		{
			Scanner scan = new Scanner ( worldFile );
		
			if ( !scan.hasNextInt() )
				throw new Exception();
			
			colDimension = scan.nextInt();
			
			if ( !scan.hasNextInt() )
				throw new Exception();
			
			rowDimension = scan.nextInt();
			
			board = new Tile[colDimension][rowDimension];
		
			for ( int r = 0; r < rowDimension; ++r )
				for ( int c = 0; c < colDimension; ++c )
					board[c][r] = new Tile();

			addFeatures ( scan );
		}
		else
		{
			rand = new Random ( );
			colDimension = 7;
			rowDimension = 7;
			board = new Tile[colDimension][rowDimension];
		
			for ( int r = 0; r < rowDimension; ++r )
				for ( int c = 0; c < colDimension; ++c )
					board[c][r] = new Tile();

			addFeatures ( );
		}

		if ( _search ) {
			if (_manualAI)
				agent = new ManualAI();
			else
				agent = new SearchAI(board);
		}
		else {
			if (_randomAI)
				agent = new RandomAI();
			else if (_manualAI)
				agent = new ManualAI();
			else
				agent = new MyAI();
		}
	}
	
	// ===============================================================
	// =					Engine Function
	// ===============================================================	
	
	public int run ( )
	{
		while ( score >= -1000 )
		{
			if ( debug || manualAI )
			{
				printWorldInfo();
				
				if ( !manualAI )
				{
					// Pause the game, only if manualAI isn't on
					// because manualAI pauses for us
					System.out.println("Press ENTER to continue...");
					try
					{
						System.in.read();
					}
					catch (IOException ioe)
					{
					}
				}
			}
			// Get the move
			lastAction = agent.getAction
			(
				board[agentX][agentY].stench,
				board[agentX][agentY].breeze,
				board[agentX][agentY].gold,
				bump,
				scream
			);

			// Make the move
			--score;
			bump   = false;
			scream = false;
			
			switch ( lastAction )
			{
				case TURN_LEFT:
					if (--agentDir < 0) agentDir = 3;
					break;
					
				case TURN_RIGHT:
					if (++agentDir > 3) agentDir = 0;
					break;
					
				case FORWARD:
					if ( agentDir == 0 && agentX+1 < colDimension )
						++agentX;
					else if ( agentDir == 1 && agentY-1 >= 0 )
						--agentY;
					else if ( agentDir == 2 && agentX-1 >= 0 )
						--agentX;
					else if ( agentDir == 3 && agentY+1 < rowDimension )
						++agentY;
					else
						bump = true;
					
					if ( board[agentX][agentY].pit || board[agentX][agentY].wumpus )
					{
						score -= 1000;
						if (debug) printWorldInfo();
						return score;
					}
					break;
				
				case SHOOT:
					if ( hasArrow )
					{
						hasArrow = false;
						score -= 10;
						if ( agentDir == 0 )
						{
							for ( int x = agentX; x < colDimension; ++x )
								if ( board[x][agentY].wumpus )
								{
									board[x][agentY].wumpus = false;
									board[x][agentY].stench = true;
									scream = true;
								}
						}
						else if ( agentDir == 1 )
						{
							for ( int y = agentY; y >= 0; --y )
								if ( board[agentX][y].wumpus )
								{
									board[agentX][y].wumpus = false;
									board[agentX][y].stench = true;
									scream = true;
								}
						}
						else if ( agentDir == 2 )
						{
							for ( int x = agentX; x >= 0; --x )
								if ( board[x][agentY].wumpus )
								{
									board[x][agentY].wumpus = false;
									board[x][agentY].stench = true;
									scream = true;
								}
						}
						else if ( agentDir == 3 )
						{
							for ( int y = agentY; y < rowDimension; ++y )
								if ( board[agentX][y].wumpus )
								{
									board[agentX][y].wumpus = false;
									board[agentX][y].stench = true;
									scream = true;
								}
						}
					}
					break;
					
				case GRAB:
					if ( board[agentX][agentY].gold )
					{
						board[agentX][agentY].gold = false;
						goldLooted = true;
					}
					break;
					
				case CLIMB:
					if ( agentX == 0 && agentY == 0 )
					{
						if ( goldLooted )
							score += 1000;
						if (debug) printWorldInfo();
						return score;
					}
					break;
			}
		}
		return score;
	}
	// ===============================================================
	// =				World Generation Functions
	// ===============================================================
	
	private void addFeatures ( )
	{		
		// Generate pits
		for ( int r = 0; r < rowDimension; ++r )
			for ( int c = 0; c < colDimension; ++c )
				if ( (c != 0 || r != 0) && randomInt(10) < 2 )
					addPit ( c, r );
		
		// Generate wumpus
		int wc = randomInt(colDimension);
		int wr = randomInt(rowDimension);
		
		while ( wc == 0 && wr == 0 )
		{
			wc = randomInt(colDimension);
			wr = randomInt(rowDimension);
		}
		
		addWumpus ( wc, wr );
		
		// Generate gold
		int gc = randomInt(colDimension);
		int gr = randomInt(rowDimension);
			
		while ( gc == 0 && gr == 0 )
		{
			gc = randomInt(colDimension);
			gr = randomInt(rowDimension);
		}
		
		addGold ( gc, gr );
	}
	
	private void addFeatures ( Scanner scan ) throws Exception
	{
		int c, r;
		
		// Add the Wumpus
		if ( !scan.hasNextInt() )
			throw new Exception();
		
		c = scan.nextInt();
		
		if ( !scan.hasNextInt() )
			throw new Exception();
		
		r = scan.nextInt();
		
		addWumpus ( c, r );
		
		// Add the Gold
		
		if ( !scan.hasNextInt() )
			throw new Exception();
		
		c = scan.nextInt();
		
		if ( !scan.hasNextInt() )
			throw new Exception();
		
		r = scan.nextInt();
		
		addGold ( c, r );
		
		// Add the Pits
		
		if ( !scan.hasNextInt() )
			throw new Exception();
		
		int numOfPits = scan.nextInt();
		
		while ( numOfPits > 0 && scan.hasNextLine() )
		{
			--numOfPits;
		
			if ( !scan.hasNextInt() )
				throw new Exception();
			
			c = scan.nextInt();
		
			if ( !scan.hasNextInt() )
				throw new Exception();
			
			r = scan.nextInt();
			
			addPit ( c, r );
		}

		scan.close();
	}
	
	private void addPit ( int c, int r )
	{
		if ( isInBounds(c, r) )
		{
			board[c][r].pit = true;
			addBreeze ( c+1, r );
			addBreeze ( c-1, r );
			addBreeze ( c, r+1 );
			addBreeze ( c, r-1 );
		}
	}
	
	private void addWumpus ( int c, int r )
	{
		if ( isInBounds(c, r) )
		{
			board[c][r].wumpus = true;
			addStench ( c+1, r );
			addStench ( c-1, r );
			addStench ( c, r+1 );
			addStench ( c, r-1 );
		}
	}
	
	private void addGold ( int c, int r )
	{
		if ( isInBounds(c, r) )
			board[c][r].gold = true;
	}
	
	private void addStench ( int c, int r )
	{
		if ( isInBounds(c, r) )
			board[c][r].stench = true;
	}
	
	private void addBreeze ( int c, int r )
	{
		if ( isInBounds(c, r) )
			board[c][r].breeze = true;
	}
	
	private boolean isInBounds ( int c, int r )
	{
		return ( c < colDimension && r < rowDimension && c >= 0  && r >= 0);
	}
	
	// ===============================================================
	// =				World Printing Functions
	// ===============================================================
	
	private void printWorldInfo ( )
	{
		printBoardInfo();
		printAgentInfo();
	}
	
	private void printBoardInfo ( )
	{
		for ( int r = rowDimension-1; r >= 0; --r )
		{
			for ( int c = 0; c < colDimension; ++c )
				printTileInfo ( c, r );
			System.out.println("");
			System.out.println("");
		}
	}

	private void printTileInfo ( int c, int r )
	{
		StringBuilder tileString = new StringBuilder();
		
		if (board[c][r].pit)    tileString.append("P");
		if (board[c][r].wumpus) tileString.append("W");
		if (board[c][r].gold)   tileString.append("$");
		if (board[c][r].breeze) tileString.append("~");
		if (board[c][r].stench) tileString.append("s");
		
		if ( agentX == c && agentY == r )
			tileString.append("@");
		
		tileString.append(".");
		
		System.out.printf("%8s", tileString.toString());
	}
	
	private void printAgentInfo ( )
	{
		System.out.println("Score: "   + score);
		System.out.println("AgentX: "  + agentX);
		System.out.println("AgentY: "  + agentY);
		printDirectionInfo();
		printActionInfo();
		printPerceptInfo();
	}
	
	private void printDirectionInfo ( )
	{
		switch (agentDir)
		{
			case 0:
				System.out.println("AgentDir: Right");
				break;
				
			case 1:
				System.out.println("AgentDir: Down");
				break;
				
			case 2:
				System.out.println("AgentDir: Left");
				break;
				
			case 3:
				System.out.println("AgentDir: Up");
				break;
				
			default:
				System.out.println("AgentDir: Invalid");
		}
	}
	
	private void printActionInfo ( )
	{
		switch (lastAction)
		{
			case TURN_LEFT:
				System.out.println("Last Action: Turned Left");
				break;
				
			case TURN_RIGHT:
				System.out.println("Last Action: Turned Right");
				break;
				
			case FORWARD:
				System.out.println("Last Action: Moved Forward");
				break;
				
			case SHOOT:
				System.out.println("Last Action: Shot the arrow");
				break;
				
			case GRAB:
				System.out.println("Last Action: Grabbed");
				break;
				
			case CLIMB:
				System.out.println("Last Action: Climbed");
				break;
				
			default:
				System.out.println("Last Action: Invalid");
		}
	}
	
	private void printPerceptInfo ( )
	{
		StringBuilder perceptString = new StringBuilder("Percepts: ");
		
		if (board[agentX][agentY].stench) perceptString.append("Stench, ");
		if (board[agentX][agentY].breeze) perceptString.append("Breeze, ");
		if (board[agentX][agentY].gold)   perceptString.append("Glitter, ");
		if (bump)                         perceptString.append("Bump, ");
		if (scream)                       perceptString.append("Scream");
		
		if ( perceptString.charAt(perceptString.length()-1) == ' '
				&& perceptString.charAt(perceptString.length()-2) == ',' )
		{
			perceptString.deleteCharAt(perceptString.length()-1);
			perceptString.deleteCharAt(perceptString.length()-1);
		}
		
		System.out.println(perceptString.toString());
	}
	
	// ===============================================================
	// =					Helper Functions
	// ===============================================================
	
	private int randomInt ( int limit )
	{
		return rand.nextInt(limit);
	}
}
