import java.util.*;

class ABNode {
    Integer move;
    int color;
    ABNode bestChild;
    int score;
    public ABNode(Integer move, int color, ABNode bestChild, int score) {
        this.move = move;
        this.color = color;
        this.bestChild = bestChild;
        this.score = score;
    }
}

class RLNode {
    int value;
    HashMap<Integer, RLNode> children;
    ArrayList<RLMove> moves;
    public RLNode(int value, HashMap<Integer, RLNode> children, ArrayList<RLMove> moves) {
        this.value = value;
        this.children = children;
        this.moves = moves;
    }
}

class RLMove {
    int move;
    int value;
    int count;
    public RLMove(int move, int value, int count) {
        this.move = move;
        this.value = value;
        this.count = count;
    }
}

public class Othello {
    static final int BLACK = -1;
    static final int EMPTY = 0;
    static final int WHITE = 1;

    static int totalBlackWins = 0;
    static int totalWhiteWins = 0;
    static int totalDraws = 0;
    static int totalBlackScore = 0;
    static int totalWhiteScore = 0;

    static ArrayList<Integer> blackValidMoves;
    static ArrayList<Integer> whiteValidMoves;

    static int currentColor;
    static int piecesPlayed;
    static int[] pieces = new int[3];
    static int[][] gameBoard = new int[8][8];

    static int[][] weightedBoard = null;

    static RLNode whiteRLNode = new RLNode(0, new HashMap<>(), null);
    static RLNode blackRLNode = new RLNode(0, new HashMap<>(), null);

    static int abType = 0;


    //mode of playing, 0 for human player, 1 for random, 2 for max tiles, 3 for weighted squares, 4 for rla, 5 for alpha-beta
    static int blackType = 1;
    static int whiteType = 1;

    //boolean to print the game board after every turn
    static boolean printing = false;

    //number of games to be played by each algorithm
    static int numGames = 10;

    //depth of minimax searching
    static int abDepth = 4;


    public static void main(String[] args) {
        //Running main will produce the win/loss/draw rates, as well as the average score and time taken to play, of each algorithm against the random algorithm
        int[] wins = new int[6];
        int[] losses = new int[6];
        int[] draws = new int[6];
        int[] score = new int[6];
        long[] times = new long[6];
        for (int i = 1; i < 6; i++) {
            whiteType = i;
            totalWhiteWins = 0;
            totalBlackWins = 0;
            totalDraws = 0;
            totalBlackScore = 0;
            totalWhiteScore = 0;
            long startTime = System.nanoTime();
            initializeGame();
            times[i] = System.nanoTime() - startTime;
            wins[i] = totalWhiteWins;
            losses[i] = totalBlackWins;
            draws[i] = totalDraws;
            score[i] = totalWhiteScore;
        }
        System.out.println("RESULTS:");
        int i = 1;
        System.out.println("Random:           Wins : " + wins[i] + ", Losses : " + losses[i] + ", Draws : " + draws[i] + ", Average Score : " + ((double)score[i]/numGames) + ", Time : " + ((double)times[i]/1000000000) + " seconds");
        i++;
        System.out.println("Max-Move:         Wins : " + wins[i] + ", Losses : " + losses[i] + ", Draws : " + draws[i] + ", Average Score : " + ((double)score[i]/numGames) + ", Time : " + ((double)times[i]/1000000000) + " seconds");
        i++;
        System.out.println("Weighted-Squares: Wins : " + wins[i] + ", Losses : " + losses[i] + ", Draws : " + draws[i] + ", Average Score : " + ((double)score[i]/numGames) + ", Time : " + ((double)times[i]/1000000000) + " seconds");
        i++;
        System.out.println("RLA:              Wins : " + wins[i] + ", Losses : " + losses[i] + ", Draws : " + draws[i] + ", Average Score : " + ((double)score[i]/numGames) + ", Time : " + (((double)times[i])/1000000000) + " seconds");
        i++;
        System.out.println("Alpha-Beta:       Wins : " + wins[i] + ", Losses : " + losses[i] + ", Draws : " + draws[i] + ", Average Score : " + ((double)score[i]/numGames) + ", Time : " + ((double)times[i]/1000000000) + " seconds");
        printWeightedBoard();

    }

    private static void initializeGame() {
        int blackTotal = 0;
        int whiteTotal = 0;
        int blackWins = 0;
        int whiteWins = 0;
        int draws = 0;
        for (int i = 0; i < numGames; i++) {
            if (i % 1000 == 0) {
                System.out.println("BLACK WINS: " + blackWins + ", AVERAGE SCORE : " + ((double)blackTotal/i));
                System.out.println("WHITE WINS: " + whiteWins + ", AVERAGE SCORE : " + ((double)whiteTotal/i));
                System.out.println("DRAWS: " + draws);
            }
            int result = playGame();
            if (result == BLACK) {
                blackWins++;
                totalBlackWins++;
            } else if (result == WHITE) {
                whiteWins++;
                totalWhiteWins++;
            } else {
                totalDraws++;
                draws++;
            }
            blackTotal+=pieces[BLACK+1];
            whiteTotal+=pieces[WHITE+1];
            System.out.println("GAME #"+i+", RESULT: " + result);
        }
        totalBlackScore = blackTotal;
        totalWhiteScore = whiteTotal;
        System.out.println("BLACK WINS: " + blackWins + ", AVERAGE SCORE : " + ((double)blackTotal/numGames));
        System.out.println("WHITE WINS: " + whiteWins + ", AVERAGE SCORE : " + ((double)whiteTotal/numGames));
        System.out.println("DRAWS: " + draws);
    }


    private static int playGame() {
        setBoard(gameBoard);
        boolean noMove = false;
        String print = "";

        ArrayList<Integer> whiteMovesPlayed = new ArrayList<>();
        ArrayList<Integer> blackMovesPlayed = new ArrayList<>();
        ArrayList<int[][]> whiteBoards = new ArrayList<>();
        ArrayList<int[][]> blackBoards = new ArrayList<>();

        while (piecesPlayed < 64) {
            blackValidMoves = allValidMoves(BLACK, gameBoard);
            whiteValidMoves = allValidMoves(WHITE, gameBoard);
            ArrayList<Integer> moves = currentColor == BLACK ? blackValidMoves : whiteValidMoves;
            printBoard(gameBoard, moves, printing);

            print = "";
            String color = currentColor == BLACK ? "BLACK" : "WHITE";
            print = print + "TURN : " + color + "\n";

            int[][] boardID = copyBoard(gameBoard);
            int move = -1;
            if (currentColor == WHITE) {
                move = switch (whiteType) {
                    case 0 -> humanMove(currentColor, gameBoard, moves);
                    case 1 -> randomMove(currentColor, gameBoard, moves);
                    case 2 -> maxMove(currentColor, gameBoard, moves);
                    case 3 -> weightedMove(currentColor, gameBoard, moves);
                    case 4 -> rlMove(currentColor, gameBoard, moves, whiteRLNode);
                    case 5 -> abMove(currentColor, gameBoard, moves);
                    default -> move;
                };
                if (move == -1) {
                    print = print + "NO POSSIBLE MOVE FOR WHITE\n";
                    if (noMove) {
                        print = print + "NO POSSIBLE MOVES FOR EITHER PLAYER\n";
                        piecesPlayed = 64;
                    } else {
                        noMove = true;
                    }
                } else {
                    whiteMovesPlayed.add(move);
                    whiteBoards.add(boardID);
                    piecesPlayed++;
                    print = print + "WHITE MOVE: " + toI(toEnd(move)) + ", " + toJ(toEnd(move)) + "\n";
                    noMove = false;
                }
            } else if (currentColor == BLACK) {
                move = switch (blackType) {
                    case 0 -> humanMove(currentColor, gameBoard, moves);
                    case 1 -> randomMove(currentColor, gameBoard, moves);
                    case 2 -> maxMove(currentColor, gameBoard, moves);
                    case 3 -> weightedMove(currentColor, gameBoard, moves);
                    case 4 -> rlMove(currentColor, gameBoard, moves, blackRLNode);
                    case 5 -> abMove(currentColor, gameBoard, moves);
                    default -> move;
                };
                if (move == -1) {
                    print = print + "NO POSSIBLE MOVE FOR BLACK\n";
                    if (noMove) {
                        print = print + "NO POSSIBLE MOVES FOR EITHER PLAYER\n";
                        piecesPlayed = 64;
                    } else {
                        noMove = true;
                    }
                } else {
                    blackBoards.add(boardID);
                    blackMovesPlayed.add(move);
                    piecesPlayed++;
                    print = print + "BLACK MOVE: " + toI(toEnd(move)) + ", " + toJ(toEnd(move)) + "\n";
                    noMove = false;

                }
            }
            if (printing) {
                System.out.print(print);
            }
            currentColor = -currentColor;
        }

        printBoard(gameBoard, null, printing);
        int result;
        if (pieces[BLACK+1] > pieces[WHITE+1]) {
            print = print + "BLACK WINS WITH " + pieces[BLACK+1] + " PIECES!\n";
            result = BLACK;
        } else if (pieces[BLACK+1] < pieces[WHITE+1]) {
            print = print + "WHITE WINS WITH " + pieces[WHITE+1] + " PIECES!\n";
            result = WHITE;
        } else {
            print = print + "DRAW!\n";
            result = EMPTY;
        }
        if (printing) {
            System.out.print(print);
        }
        if (whiteType == 4) {
            updateValues(whiteRLNode, whiteMovesPlayed, whiteBoards, result, WHITE);
        }
        if (blackType == 4) {
            updateValues(blackRLNode, blackMovesPlayed, blackBoards, result, BLACK);
        }
        return result;
    }

    private static int abMove(int color, int[][] board, ArrayList<Integer> moves) {
        ABNode node = createABTree(null, null, color, true, board, abDepth);
        if (node != null) {
            if (node.bestChild != null) {
                int choice = node.bestChild.move;
                verifyMove(toEnd(choice), color, board, moves);
                return choice;
            }
        } else {
            System.out.println("BAD MOVE");
        }
        return -1;
    }

    private static ABNode createABTree(Integer nodeMove, Integer bound, int color, boolean max, int[][] board, int depth) {
        int currentColor = max ? color : -color;
        ArrayList<Integer> moves = allValidMoves(currentColor, board);
        ABNode bestChild = null;
        Integer childBound = null;
        if (depth == 0 || moves.isEmpty()) {
            if (abType == 0) {
                int score = countScore(board, currentColor);
                return new ABNode(nodeMove, currentColor, null, score);
            } else {
                fillWeightedBoard();
                if (nodeMove != null) {
                    int score = weightedBoard[toI(toEnd(nodeMove))][toJ(toEnd(nodeMove))];
                    return new ABNode(nodeMove, currentColor, null, score);
                } else {
                    return new ABNode(nodeMove, currentColor, null, -122);
                }
            }
        } else {
            for (Integer move : moves) {
                int[][] copy = copyBoard(board);
                verifyMove(toEnd(move), currentColor, copy, moves);
                ABNode child = createABTree(move, childBound, color, !max, copy, depth - 1);
                if (child != null) {
                    if (bound != null) {
                        if (max) {
                            if (child.score > bound) {
                                return null;
                            }
                        } else {
                            if (child.score < bound) {
                                return null;
                            }
                        }
                    }
                    if (bestChild == null) {
                        bestChild = child;
                        childBound = child.score;
                    } else if (max && child.score > bestChild.score) {
                        bestChild = child;
                        childBound = child.score;
                    } else if (!max && child.score < bestChild.score) {
                        bestChild = child;
                        childBound = child.score;
                    }
                }
            }
            if (bestChild != null) {
                return new ABNode(nodeMove, currentColor, bestChild, bestChild.score);
            } else {
                return null;
            }
        }
    }

    private static int rlMove(int color, int[][] board, ArrayList<Integer> moves, RLNode root) {
        int choice = -1;
        if (!moves.isEmpty()) {
            RLNode node = getState(boardToID(board), root);
            if (node != null) {
                int highestValue = Integer.MIN_VALUE;
                for (int i  = 0; i < node.moves.size(); i++) {
                    if (node.moves.get(i).value > highestValue) {
                        highestValue = node.moves.get(i).value;
                        choice = node.moves.get(i).move;
                    }
                }
            } else {
                choice = moves.get((int) (Math.random() * moves.size()));
            }

            verifyMove(toEnd(choice), color, board, moves);
        }
        return choice;
    }

    private static int weightedMove(int color, int[][] board, ArrayList<Integer> moves) {
        fillWeightedBoard();
        int choice = -1;
        int choiceWeight = -121;
        if (!moves.isEmpty()) {
            for (Integer move : moves) {
                int squareWeight = weightedBoard[toI(toEnd(move))][toJ(toEnd(move))];
                if (squareWeight > choiceWeight) {
                    choice = move;
                    choiceWeight = squareWeight;
                }
            }
            verifyMove(toEnd(choice), color, board, moves);
        }
        return choice;
    }

    private static int maxMove(int color, int[][] board, ArrayList<Integer> moves) {
        HashMap<Integer, Integer> moveMagnitudes = new HashMap<>();
        int choice = -1;
        int choiceMagnitude = -1;
        if (!moves.isEmpty()) {
            for (Integer move : moves) {
                int moveEnd = toEnd(move);
                int moveMagnitude = findMagnitude(move);
                if (moveMagnitudes.containsKey(moveEnd)) {
                    moveMagnitude = moveMagnitude + moveMagnitudes.get(moveEnd);
                }
                moveMagnitudes.put(moveEnd, moveMagnitude);
                if (moveMagnitude > choiceMagnitude) {
                    choiceMagnitude = moveMagnitude;
                    choice = move;
                }

            }
            verifyMove(toEnd(choice), color, board, moves);
        }
        return choice;
    }

    private static int randomMove(int color, int[][] board, ArrayList<Integer> moves) {
        int choice = -1;
        if (!moves.isEmpty()) {
            choice = moves.get((int) (Math.random() * moves.size()));
            verifyMove(toEnd(choice), color, board, moves);
        }
        return choice;
    }

    private static int humanMove(int color, int[][] board, ArrayList<Integer> moves) {
        Scanner readIn = new Scanner(System.in);
        boolean valid = false;
        int choice = -1;
        if (!moves.isEmpty()) {
            while (!valid) {
                System.out.println("Please enter a valid move in \"y x\" format (top-left is (0,0)): ");
                String nextline = readIn.nextLine();
                System.out.println(nextline);
                choice = parseResponse(nextline);
                System.out.println(choice);
                valid = verifyMove(choice, color, board, moves);
                System.out.println(valid);
            }
        }
        return choice;
    }

    private static boolean verifyMove(int coordinate, int color, int[][] board, ArrayList<Integer> moves) {
        if (coordinate == -1) {
            return false;
        }
        boolean result = false;
        for (Integer move: moves) {
            if (toEnd(move) == coordinate) {
                result = true;
                makeMove(move, color, board);
            }
        }
        return result;
    }

    private static void makeMove(int line, int color, int[][] board) {
        int startI = toI(toStart(line));
        int startJ = toJ(toStart(line));
        int endI = toI(toEnd(line));
        int endJ = toJ(toEnd(line));

        int deltaI, deltaJ;
        if (startI > endI) {
            deltaI = -1;
        } else if (startI == endI) {
            deltaI = 0;
        } else {
            deltaI = 1;
        }

        if (startJ > endJ) {
            deltaJ = -1;
        } else if (startJ == endJ) {
            deltaJ = 0;
        } else {
            deltaJ = 1;
        }


        while (startI != (endI + deltaI) || startJ != (endJ + deltaJ)) {
            board[startI][startJ] = color;
            startI += deltaI;
            startJ += deltaJ;
        }
    }

    private static ArrayList<Integer> allValidMoves(int color, int[][] board) {
        ArrayList<Integer> validMoves = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] == color) {
                    for (int k = -1; k < 2; k++) {
                        for (int l = -1; l < 2; l++) {
                            int move = findMove(toCoordinate(i, j), k, l, color, board);
                            if (move != -1) {
                                validMoves.add(move);
                            }
                        }
                    }
                }
            }
        }
        return validMoves;
    }

    private static int findMove(int coordinate, int iDelta, int jDelta, int color, int[][] board) {
        int i = toI(coordinate) + iDelta;
        int j = toJ(coordinate) + jDelta;
        boolean opp = false;
        while (i >= 0 && i < 8 && j >= 0 && j < 8) {
            if (board[i][j] == color) {
                return -1;
            }
            if (board[i][j] == EMPTY) {
                if (opp) {
                    return toLine(coordinate, toCoordinate(i, j));
                } else {
                    return -1;
                }
            }
            if (board[i][j] == -color) {
                opp = true;
            }
            i += iDelta;
            j += jDelta;
        }
        return -1;
    }

    private static boolean containsEnd(int coordinate, ArrayList<Integer> moves) {
        for (Integer move: moves) {
            if (coordinate == toEnd(move)) {
                return true;
            }
        }
        return false;
    }

    private static int findMagnitude(int line) {
        if (toI(toStart(line)) != toI(toEnd(line))) {
            return Math.abs(toI(toStart(line)) - toI(toEnd(line)));
        } else {
            return Math.abs(toJ(toStart(line)) - toJ(toEnd(line)));
        }
    }

    private static int toLine(int start, int end) {
        return start * 100 + end;
    }

    private static int toStart(int line) {
        return line / 100;
    }

    private static int toEnd(int line) {
        return line % 100;
    }

    private static int toCoordinate(int i, int j) {
        return i * 10 + j;
    }

    private static int toI(int coordinate) {
        return coordinate / 10;
    }

    private static int toJ(int coordinate) {
        return coordinate % 10;
    }

    private static void setBoard(int[][] board) {
        currentColor = BLACK;
        pieces[BLACK + 1] = 2;
        pieces[WHITE + 1] = 2;
        piecesPlayed = 4;
        for (int i = 0; i < 8; i++) {
            Arrays.fill(board[i], 0);
        }
        board[3][3] = WHITE;
        board[4][4] = WHITE;
        board[4][3] = BLACK;
        board[3][4] = BLACK;

//        board[4][1] = WHITE;
//        board[4][2] = WHITE;
//        board[4][3] = WHITE;
//        board[4][4] = WHITE;
//        board[4][5] = WHITE;
//        board[6][1] = WHITE;
//
//        board[3][2] = BLACK;
//        board[3][3] = BLACK;
//        board[3][4] = BLACK;
//        board[5][1] = BLACK;
//        board[5][4] = BLACK;
//        board[6][0] = BLACK;
    }

    private static void updateValues(RLNode root, ArrayList<Integer> movesPlayed, ArrayList<int[][]> boards, int result, int color) {
        int charge = result == color ? 1 : -1;
        for (int i = 0; i < boards.size(); i++) {
            int[] boardID = boardToID(boards.get(i));
            RLNode node = getState(boardID, root);
            if (node != null) {
                ArrayList<RLMove> moves = node.moves;
                for (RLMove move : moves) {
                    if (move.move == movesPlayed.get(i)) {
                        move.count++;
                        move.value = move.value + (charge * 100 / move.count);
                        break;
                    }
                }
            } else {
                ArrayList<RLMove> moves = new ArrayList<>();
                ArrayList<Integer> possibleMoves = allValidMoves(color, boards.get(i));
                for (Integer possibleMove : possibleMoves) {
                    if (possibleMove.equals(movesPlayed.get(i))) {
                        moves.add(new RLMove(possibleMove, 500 + (charge * 100) / 2, 2));
                    } else {
                        moves.add(new RLMove(possibleMove, 500, 1));
                    }
                }
                putToTree(boardID, root, moves);
            }
        }
    }

    private static RLNode getState(int[] boardID, RLNode root) {
        if (boardID.length == 0) {
            return root;
        } else {
            if (root.children.containsKey(boardID[0])) {
                return getState(Arrays.copyOfRange(boardID, 1, boardID.length), root.children.get(boardID[0]));
            } else {
                return null;
            }
        }
    }

    private static void putToTree(int[] boardID, RLNode root, ArrayList<RLMove> moves) {
        if (boardID.length == 0) {
            root.moves = moves;
        } else {
            if (root.children.containsKey(boardID[0])) {
                RLNode child = root.children.get(boardID[0]);
                putToTree(Arrays.copyOfRange(boardID, 1, boardID.length), child, moves);
            } else {
                RLNode child = new RLNode(boardID[0], new HashMap<>(), null);
                root.children.put(boardID[0], child);
                putToTree(Arrays.copyOfRange(boardID, 1, boardID.length), child, moves);
            }
        }
    }

    private static int treeSize(RLNode root, int depth) {
        if (root != null) {
            if (root.moves != null) {
                return 1;
            }
            int size = 0;
            for (Integer key : root.children.keySet()) {
                size += treeSize(root.children.get(key), depth + 1);
            }
            return size;
        }
        return 0;
    }

    private static void fillWeightedBoard() {
        if (weightedBoard == null) {
            weightedBoard = new int[8][8];
            weightedBoard[0][0] = 120;
            weightedBoard[0][1] = -20;
            weightedBoard[0][2] = 20;
            weightedBoard[0][3] = 5;
            weightedBoard[0][4] = 5;
            weightedBoard[0][5] = 20;
            weightedBoard[0][6] = -20;
            weightedBoard[0][7] = 120;

            weightedBoard[1][0] = -20;
            weightedBoard[1][1] = -40;
            weightedBoard[1][2] = -5;
            weightedBoard[1][3] = -5;
            weightedBoard[1][4] = -5;
            weightedBoard[1][5] = -5;
            weightedBoard[1][6] = -40;
            weightedBoard[1][7] = -20;

            weightedBoard[2][0] = 20;
            weightedBoard[2][1] = -5;
            weightedBoard[2][2] = 15;
            weightedBoard[2][3] = 3;
            weightedBoard[2][4] = 3;
            weightedBoard[2][5] = 15;
            weightedBoard[2][6] = -5;
            weightedBoard[2][7] = 20;

            weightedBoard[3][0] = 5;
            weightedBoard[3][1] = -5;
            weightedBoard[3][2] = 3;
            weightedBoard[3][3] = 3;
            weightedBoard[3][4] = 3;
            weightedBoard[3][5] = 3;
            weightedBoard[3][6] = -5;
            weightedBoard[3][7] = 5;

            weightedBoard[4][0] = 5;
            weightedBoard[4][1] = -5;
            weightedBoard[4][2] = 3;
            weightedBoard[4][3] = 3;
            weightedBoard[4][4] = 3;
            weightedBoard[4][5] = 3;
            weightedBoard[4][6] = -5;
            weightedBoard[4][7] = 5;

            weightedBoard[5][0] = 20;
            weightedBoard[5][1] = -5;
            weightedBoard[5][2] = 15;
            weightedBoard[5][3] = 3;
            weightedBoard[5][4] = 3;
            weightedBoard[5][5] = 15;
            weightedBoard[5][6] = -5;
            weightedBoard[5][7] = 20;

            weightedBoard[6][0] = -20;
            weightedBoard[6][1] = -40;
            weightedBoard[6][2] = -5;
            weightedBoard[6][3] = -5;
            weightedBoard[6][4] = -5;
            weightedBoard[6][5] = -5;
            weightedBoard[6][6] = -40;
            weightedBoard[6][7] = -20;

            weightedBoard[7][0] = 120;
            weightedBoard[7][1] = -20;
            weightedBoard[7][2] = 20;
            weightedBoard[7][3] = 5;
            weightedBoard[7][4] = 5;
            weightedBoard[7][5] = 20;
            weightedBoard[7][6] = -20;
            weightedBoard[7][7] = 120;
        }
    }

    private static void printBoard(int[][] board, ArrayList<Integer> moves, boolean printingBool) {
        String print = "";
        pieces[BLACK+1] = 0;
        pieces[WHITE+1] = 0;
        print = print + "   --0---1---2---3---4---5---6---7--\n";
        print = print + "|  --------------------------------- \n";
        for (int i = 0; i < 8; i++) {
            print = print + (i + "  ");
            for (int j = 0; j < 8; j++) {
                print = print + "| ";
                if (board[i][j] == 0) {
                    if (moves != null && containsEnd(toCoordinate(i, j), moves)) {
                        print = print + "X ";
                    } else {
                        print = print + "O ";
                    }
                } else {
                    switch (board[i][j]) {
                        case BLACK:
                            pieces[BLACK+1]++;
                            print = print + "B ";
                            break;
                        case WHITE:
                            pieces[WHITE+1]++;
                            print = print + "W ";
                            break;
                    }
                }
            }
            print = print + "|\n";
            print = print + "|  --------------------------------- \n";
        }
        print = print + ("BLACK: " + pieces[BLACK+1] + ", WHITE: " + pieces[WHITE+1]);

        if (printingBool) {
            System.out.println(print);
        }
    }

    private static void printWeightedBoard() {
        fillWeightedBoard();
        String print = "";
        pieces[BLACK+1] = 0;
        pieces[WHITE+1] = 0;
        print = print + "   ---0----1----2----3----4----5----6----7--\n";
        print = print + "|  ----------------------------------------- \n";
        for (int i = 0; i < 8; i++) {
            print = print + (i + "  ");
            for (int j = 0; j < 8; j++) {
                print = print + "| ";
                print = print + String.format("%1$3s", weightedBoard[i][j]);

            }
            print = print + "|\n";
            print = print + "|  ----------------------------------------- \n";
        }

        System.out.println(print);

    }

    private static int parseResponse(String input) {
        int coordI = 0;
        int coordJ = 0;
        int count = 0;
        for (int i = 0; i < input.length(); i++) {
            if (Character.isDigit(input.charAt(i))) {
                if (count == 0) {
                    coordI = Integer.parseInt(input.substring(i, i+1));
                    count++;
                } else if (count == 1) {
                    coordJ = Integer.parseInt(input.substring(i, i+1));
                    return toCoordinate(coordI, coordJ);
                }
            }
        }
        return -1;
    }

    private static int[][] copyBoard(int[][] board) {
        int[][] newBoard = new int[board.length][];
        for (int i = 0; i < board.length; i++) {
            newBoard[i] = new int[board[i].length];
            for (int j = 0; j < board[i].length; j++) {
                newBoard[i][j] = board[i][j];
            }
        }
        return newBoard;
    }

    private static int[] boardToID(int[][] board) {
        int[] id = new int[8];
        for (int i = 0; i < 8; i++) {
            int total = 0;
            int mult = 1;
            for (int j = 0; j < 8; j++) {
                total += board[i][j] * mult;
                mult*=10;
            }
            id[i] = total;
        }
        return id;
    }

    private static void printID(int[] id) {
        int[][] board = new int[8][8];
        for (int i = 0; i < 8; i++) {
            int total = id[i];
            for (int j = 0; j < 8; j++) {
                if (total%10 == 1 || total%10 == -9) {
                    board[i][j] = WHITE;
                    total -= WHITE;
                } else if (total%10 == -1 || total%10 == 9) {
                    board[i][j] = BLACK;
                    total -= BLACK;
                } else if (total%10 == 0) {
                    board[i][j] = EMPTY;
                }
                total /= 10;
            }
        }
        printBoard(board, null, true);
    }

    private static int countScore(int[][] board, int color) {
        int score = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] == color) {
                    score++;
                }
            }
        }
        return score;
    }
    private static void printTree(RLNode root) {
        System.out.println(root.value);
        for (RLNode child : root.children.values()) {
            printTree(child);
        }
    }
}