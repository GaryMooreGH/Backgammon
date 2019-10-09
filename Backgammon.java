import java.util.concurrent.TimeUnit;

public class Backgammon {
    // This is the main class for the Backgammon game. It orchestrates the running of the game.

    public static final int NUM_PLAYERS = 2;

    private final Players players = new Players();
    private final Board board = new Board(players);
    private final UI ui = new UI(board,players);

    private void getPlayerNames() {
        for (Player player : players) {
            ui.promptPlayerName();
            String name = ui.getString();
            ui.displayString("> " + name);
            player.setName(name);
            ui.displayPlayerColor(player);
        }
    }
    
    //Asks user for a score limit and ensures it is an Integer
    private int getScoreLimit() {
    	ui.promptScoreLimit();
    	int scorelimit = ui.getScoreLimit();
    	ui.displayString("You are playing up to a score of: " + scorelimit);
    	return scorelimit;
    }

    private void rollToStart() {
        do {
            for (Player player : players) {
                player.getDice().rollDie();
                ui.displayRoll(player);
            }
            if (players.isEqualDice()) {
                ui.displayDiceEqual();
            }
        } while (players.isEqualDice());
        players.setCurrentAccordingToDieRoll();
        ui.displayDiceWinner(players.getCurrent());
        ui.display();
    }

    //Take turns now returns an Integer (to ensure game will always quit on 'quit')
    private int takeTurns() throws InterruptedException {
        Command command = new Command();
        boolean firstMove = true;
        int score = 1;
        int doubleCube = 1;
        do {
            Player currentPlayer = players.getCurrent();
            Dice currentDice;
            
            if (firstMove) {
                currentDice = new Dice(players.get(0).getDice().getDie(),players.get(1).getDice().getDie());
                firstMove = false;
            } else {
                currentPlayer.getDice().rollDice();
                ui.displayRoll(currentPlayer);
                currentDice = currentPlayer.getDice();
            }
            Plays possiblePlays;
            possiblePlays = board.getPossiblePlays(currentPlayer,currentDice);
            
            //Gives player option to double the match score before rolling
            ui.displayString("Enter 'double' to double games score or any other command to continue");
            String eqDouble = ui.getString();
            
            
            if(eqDouble.equals("double")) {
            	//Both players can double at beginning but only the player that recieved the double can after this
            	if(players.getCurrent().hasDouble()) {
	            	players.getCurrent().useDouble();
	            	players.advanceCurrentPlayer();
	            	players.getCurrent().receiveDouble();
	            	do {
	            		ui.promptDoubleOption(players.getCurrent());
	            		command = ui.getCommand();
	            	}while(!command.isNo() && !command.isYes());
	            		
	            	//If other player accepts double, doubles current game score
	            	if(command.isYes()) {
	            		
	            		score *= 2;
	            		doubleCube *= 2;
	            		board.setDoubleValue(doubleCube);
	            		ui.display();
	            	
	            	}
	            
	            	players.advanceCurrentPlayer();
            	}
            	else {
            		ui.displayString("You do not currently have the doubling cube");
            	}
            	
            }
            
            //Exits game if player refuses the double cube and this player concedes the game
            if(!command.isNo() && !command.isQuit()) {
	            if (possiblePlays.number()==0) {
	                ui.displayNoMove(currentPlayer);
	            } else if (possiblePlays.number()==1) {
	                ui.displayForcedMove(currentPlayer);
	                board.move(currentPlayer, possiblePlays.get(0));
	            } else {
	                ui.displayPlays(currentPlayer, possiblePlays);
	                ui.promptCommand(currentPlayer);
	                command = ui.getCommand(possiblePlays);
	                ui.displayString("> " + command);
	                if (command.isMove()) {
	                    board.move(currentPlayer, command.getPlay());
	                } else if (command.isCheat()) {
	                    board.cheat();
	                }
	                
	            }
	            ui.display();
	            TimeUnit.SECONDS.sleep(2);
	            players.advanceCurrentPlayer();
	            ui.display();
            }
            
            
        } while (!command.isQuit() && !board.isGameOver() && !command.isNo());
        
        //Ensures game will completely quit if entered
        if(command.isQuit()) {
        	return 0;
        }
        
        //If game comes to a natural end returns score
        else if(board.isGameOver()) {
        	ui.displayGameWinner(board.getWinner());
        	players.setWinner(board.getWinner());
        	board.getWinner().addWin(score);
        	
        	TimeUnit.SECONDS.sleep(2);
        	return score;
        }
        
        //If player refuses doubling cube and game ends
        else if(command.isNo()){
        	ui.displayGameWinner(players.getCurrent());
        	players.setWinner(players.getCurrent());
        	players.getCurrent().addWin(score);
        	TimeUnit.SECONDS.sleep(2);
        	return score;
        }
        
        else {
        	return 0;
        }
    }

    private void play() throws InterruptedException {
//        board.setUI(ui);
        ui.display();
        ui.displayStartOfGame();
        boolean end = false;
        
        //Current match number
        int matchLength = 1;
        
        //Sets score for first game
	    int limit = getScoreLimit();
	    
	    //So game will quit if 'quit' is entered in getScoreLimit
	    if(limit != 0) {
		    getPlayerNames();
		    
		    //Plays until one of the players reaches the score limit (or quits)
		    while(players.get(0).getScore() < limit && players.get(1).getScore() < limit) {
		    	
			   rollToStart();
		       int win = takeTurns();
		       
		       //Breaks if 'quit' is entered during the game and program quits
			   if (win == 0){
				   end = true;
			        break;
			     }
			   else {
			        
			      //Any key to continue!
					   ui.displayString("Please enter any key to continue!");
					   String contG = ui.getString();
			     }
			   matchLength++;
			  board.nextGame();
			   board.reset();
		    }
		       
		    if(!end) {
	
		    	//Shows winner of match and score they won by
			    ui.displayMatchWinner(players.getWinner());
			    ui.displayScore(players.getCurrent(), players.getOther());
			    
			        
			    Command command = new Command();
			    //Asks players would they like to play another match
			    ui.promptNextMatch();
			    command = ui.getCommand();
			    
			    while(!command.isNo() && !command.isQuit()) {
			    	
			    	board.resetGame();
			    	
			    	//Asks players for the new score limit and new player names
			    	//Also resets the new players score back to 0
			    	limit = getScoreLimit();
				    getPlayerNames();
				    
				    //Plays until one of the players reaches the score limit (or quits)
				    while(players.get(0).getScore() != limit && players.get(1).getScore() != limit) {
				    	
					   rollToStart();
				       int win = takeTurns();
				       
				       //Breaks if 'quit' is entered during the game and program quits
					   if (win == 0){
						   end = true;
					        break;
					     }
					   else {
						 //Any key to continue!
						   ui.displayString("Please enter any key to continue!");
						   String contG = ui.getString();
					     }
					   
					   // WHERE TO RESET THE BOARD
					   board.reset();
				    }
				    if(!end) {
					    ui.displayMatchWinner(players.getWinner());
					    ui.displayScore(players.getCurrent(), players.getOther());
					     
					    ui.promptNextMatch();
					    command = ui.getCommand();
				    }
			    }
		    }
    	}
	        
    }

    public static void main(String[] args) throws InterruptedException {
        Backgammon game = new Backgammon();
        game.play();
        System.exit(0);
    }
}
