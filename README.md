Hello! This is my implementation of several playing algorithms for the game Othello. 
- (0) allows human play in place of an algorithm,
- (1) is the random move algorithm which selects a legal move at random and plays it, 
- (2) is a single-move lookahead algorithm that selects the legal move that flips the most tiles in favor of the player, 
- (3) is also a single-move lookahead algorithm that identifies the maximum weighted square of all legal moves and plays it, 
- (4) is a reinforcement learning algorithm that uses tabulation of the board states to apply a reward or discount to board states that resulted in a win or a loss, respectively, and 
- (5) is a minimax algorithm with alpha-beta pruning (set to a depth limit of 4) that creates a minimax tree to find the move that results in the maximum number of player-colored tiles after a given number of turns.
Use variables 
- "blacktype" and "whitetype" to define what algorithms are to be used, 
- "printing" for printing of board states after each turn, 
- "numgames" to change the total number of games played, 
- "testmode" to run every play algorithm against the random algorithm, and 
- "abdepth" to change the depth of the minimax tree.
