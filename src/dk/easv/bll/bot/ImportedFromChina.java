package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;
import dk.easv.bll.move.Move;

import java.util.*;

public class ImportedFromChina implements IBot{
    private static final String BOTNAME = "ImportedFromChina";
    private static final int MOVE_TIME = 1000;
    private Random random = new Random();
    private int opponentId;

    @Override
    public String getBotName() {
        return BOTNAME;
    }

    public ImportedFromChina()
    {
        random = new Random();
    }

    @Override
    public IMove doMove(IGameState state) {
        Node DNode = new Node(state);
        long endTime = System.currentTimeMillis() + MOVE_TIME;
        opponentId = (state.getMoveNumber()+1)%2;

        while (System.currentTimeMillis() < endTime) {
            Node promisingNode = selectPromisingNode(DNode);
            if (!isGameOver(promisingNode.getState())) {
                expandNode(promisingNode);
            }
            Node nodeToExplore = promisingNode;
            if (!nodeToExplore.getChildren().isEmpty()) {
                nodeToExplore = promisingNode.getRandomChild();
            }
            int rolloutResult = performRollout(nodeToExplore);
            backPropagation(nodeToExplore, rolloutResult);
        }

        Node bestNode = DNode.getChildWithMaxScore();
        return getMove(DNode, bestNode);
    }

    private Node selectPromisingNode(Node node) {
        while (!node.getChildren().isEmpty()) {
            node = getBestNode(node);
        }
        return node;
    }


    private Node getBestNode(Node node) {
        int totalParentVisits = node.getVisits();
        Node bestNode = null;
        double maxUCB1 = Double.MIN_VALUE;
        for (Node child : node.getChildren()) {
            double ucb1Value = calculate(totalParentVisits, child.getVisits(), child.getScore());
            if (ucb1Value > maxUCB1) {
                maxUCB1 = ucb1Value;
                bestNode = child;
            }
        }
        return bestNode != null ? bestNode : node;
    }


    private double calculate(int totalVisits, int nodeVisits, double nodeScore) {
        if (nodeVisits == 0) {
            return Double.MAX_VALUE;
        }
        double exploitation = nodeScore / (double) nodeVisits;
        double exploration = Math.sqrt(Math.log(totalVisits) / (double) nodeVisits);
        return exploitation + 2 * exploration;
    }


    private void expandNode(Node parentNode) {
        List<IMove> availableMoves = parentNode.getState().getField().getAvailableMoves();
        for(IMove move : availableMoves) {
            IMove randomMove = availableMoves.get(random.nextInt(availableMoves.size()));
            Node childNode = new Node(parentNode.getState());
            childNode.setParent(parentNode);
            parentNode.getChildren().add(childNode);
            performMove(childNode.getState(), move.getX(), move.getY());
        }
    }

    private int performRollout(Node nodeToExplore) {
        Node tempNode = new Node(nodeToExplore);
        IGameState tempState = tempNode.getState();
        while(!isGameOver(tempState)) {
            randomPlay(tempState);
        }
        if(isWin(tempState) &&  (tempState.getMoveNumber()+1)%2 == opponentId) {
            return 0;
        }
        else if(isWin(tempState) && (tempState.getMoveNumber()+1)%2 == (opponentId+1)%2) {
            return 50;
        }
        else {
            return 15;
        }
    }

    private void backPropagation(Node node, int value) {
        while(node != null) {
            node.incrementVisit();
            node.addScore(value);
            node = node.getParent();
        }
    }

    private void randomPlay(IGameState state) {
        List<IMove> availableMoves = state.getField().getAvailableMoves();
        IMove randomMove = availableMoves.get(random.nextInt(availableMoves.size()));
        performMove(state, randomMove.getX(), randomMove.getY());
    }

    private class Node {

        private Node parent;
        private IGameState state;
        private int score;
        private int visits;
        private List<Node> children;

        public Node(Node node) {
            this.state = cloneGameState(node.state);
            this.children = new ArrayList();
            if(node.getParent() != null) {
                this.parent = node.getParent();
            }
            List<Node> childArray = node.getChildren();
            for(Node child : childArray) {
                this.children.add(new Node(child));
            }
            this.score = node.getScore();
            this.visits = node.getVisits();
        }

        public Node(IGameState state) {
            this.state = cloneGameState(state);
            String[][] board = new String[9][9];
            String[][] macroboard = new String[3][3];
            for(int i = 0; i < board.length; i++) {
                for(int j = 0; j < board[i].length; j++) {
                    board[i][j] = state.getField().getBoard()[i][j];
                }
            }
            for(int i = 0; i < macroboard.length; i++) {
                for(int j = 0; j < macroboard[i].length; j++) {
                    macroboard[i][j] = state.getField().getMacroboard()[i][j];
                }
            }

            this.state.getField().setBoard(board);
            this.state.getField().setMacroboard(macroboard);
            this.state.setMoveNumber(state.getMoveNumber());
            this.state.setRoundNumber(state.getRoundNumber());
            this.children = new ArrayList();
            this.score = 0;
            this.visits = 0;
        }

        private IGameState cloneGameState(IGameState state) {
            IGameState clonedState = new GameState();
            clonedState.getField().setBoard(cloneArray(state.getField().getBoard()));
            clonedState.getField().setMacroboard(cloneArray(state.getField().getMacroboard()));
            clonedState.setMoveNumber(state.getMoveNumber());
            clonedState.setRoundNumber(state.getRoundNumber());
            return clonedState;
        }

        private String[][] cloneArray(String[][] original) {
            String[][] copy = new String[original.length][original[0].length];
            for (int i = 0; i < original.length; i++) {
                copy[i] = original[i].clone();
            }
            return copy;
        }

        public List<Node> getChildren() {
            return children;
        }

        public Node getChildWithMaxScore() {
            return Collections.max(this.children, Comparator.comparing(c -> {
                return c.getScore();
            }));
        }

        public Node getRandomChild() {
            return children.get(random.nextInt(children.size()));
        }

        public Node getParent() {
            return parent;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public IGameState getState() {
            return state;
        }

        public void addScore(int score) {
            this.score += score;
        }

        public int getScore() {
            return score;
        }

        public void incrementVisit() {
            visits++;
        }

        public int getVisits() {
            return visits;
        }

    }

    private static final String PLAYER_ONE = "0";
    private static final String PLAYER_TWO = "1";
    private static final String EMPTY_FIELD = IField.EMPTY_FIELD;
    private static final String AVAILABLE_FIELD = IField.AVAILABLE_FIELD;


    private IMove getMove(Node parentNode, Node childNode) {
        String[][] parentBoard = parentNode.getState().getField().getBoard();
        String[][] childBoard = childNode.getState().getField().getBoard();
        for(int i = 0; i < parentBoard.length; i++) {
            for(int j = 0; j < parentBoard[i].length; j++) {
                if(!parentBoard[i][j].equals(childBoard[i][j])) {
                    return new Move(i,j);
                }
            }
        }
        return null;
    }

    private void performMove(IGameState state, int moveX, int moveY) {
        String[][] board = state.getField().getBoard();
        board[moveX][moveY] = state.getMoveNumber()%2 + "";String currentPlayerSymbol = String.valueOf(state.getMoveNumber() % 2);
        board[moveX][moveY] = currentPlayerSymbol;
        state.getField().setBoard(board);
        updateMacroboard(state, moveX, moveY);
        state.setMoveNumber(state.getMoveNumber()+1);
    }

    private void updateMacroboard(IGameState state, int moveX, int moveY) {
        updateMicroboardState(state, moveX, moveY);
        updateMicroboardsAvailability(state, moveX, moveY);
    }

    private void updateMicroboardState(IGameState state, int moveX, int moveY) {
        String[][] macroboard = state.getField().getMacroboard();
        int MICROBOARD_SIZE = 3;
        int startingXPosition = (moveX / MICROBOARD_SIZE) * MICROBOARD_SIZE;
        int startingYPosition = (moveY / MICROBOARD_SIZE) * MICROBOARD_SIZE;
        if(isWinOnMicroboard(state, startingXPosition, startingYPosition)) {
            macroboard[moveX/3][moveY/3] = state.getMoveNumber()%2+"";
        }
        else if(isDrawOnMicroboard(state, startingXPosition, startingYPosition)) {
            macroboard[moveX/3][moveY/3] = "-";
        }
        state.getField().setMacroboard(macroboard);
    }

    private void updateMicroboardsAvailability(IGameState state, int moveX, int moveY) {
        int MICROBOARD_SIZE = 3;
        int activeMicroboardX = moveX % MICROBOARD_SIZE;
        int activeMicroboardY = moveY % MICROBOARD_SIZE;
        String[][] macroboard = state.getField().getMacroboard();
        String microboardStatus = macroboard[activeMicroboardX][activeMicroboardY];
        if (isMicroboardAvailableOrEmpty(microboardStatus)) {
            setAvailableMicroboard(state, activeMicroboardX, activeMicroboardY);
        }
        else {
            setAllMicroboardsAvailable(state);
        }
    }

    private boolean isMicroboardAvailableOrEmpty(String status) {
        return status.equals(IField.AVAILABLE_FIELD) || status.equals(IField.EMPTY_FIELD);
    }

    private void setAvailableMicroboard(IGameState state, int activeMicroboardX, int activeMicroboardY) {
        String[][] macroboard = state.getField().getMacroboard();
        macroboard[activeMicroboardX][activeMicroboardY] = IField.AVAILABLE_FIELD;
        for(int x = 0; x < macroboard.length; x++) {
            for(int y = 0; y < macroboard[x].length; y++) {
                if(x == activeMicroboardX && y == activeMicroboardY) {
                    macroboard[x][y] = IField.AVAILABLE_FIELD;
                }
                else if(macroboard[x][y].equals(IField.AVAILABLE_FIELD)) {
                    macroboard[x][y] = IField.EMPTY_FIELD;
                }
            }
        }
        state.getField().setMacroboard(macroboard);
    }

    private void setAllMicroboardsAvailable(IGameState state) {
        String[][] macroboard = state.getField().getMacroboard();
        for(int x = 0; x < 3; x++) {
            for(int y = 0; y < 3; y++) {
                if(macroboard[x][y].equals(IField.EMPTY_FIELD)) {
                    macroboard[x][y] = IField.AVAILABLE_FIELD;
                }
            }
        }
    }

    private boolean isWinOnMicroboard(IGameState state, int startingX, int startingY) {
        String[][] board = state.getField().getBoard();
        return isWinOnBoard(board, startingX, startingY);
    }

    private boolean isDrawOnMicroboard(IGameState state, int startingX, int startingY) {
        boolean isDraw = true;
        String[][] board = state.getField().getBoard();
        for(int x = startingX; x < startingX+3; x++)
        {
            for(int y = startingY; y < startingY+3; y++)
            {
                if(board[x][y].equals(IField.EMPTY_FIELD))
                {
                    isDraw = false;
                }
            }
        }
        return isDraw;
    }

    private boolean isGameOver(IGameState state) {
        String[][] macroboard = state.getField().getMacroboard();
        return isWinOnBoard(macroboard, 0, 0) || isDraw(state);
    }

    private boolean isWin(IGameState state) {
        String[][] macroboard = state.getField().getMacroboard();
        return isWinOnBoard(macroboard, 0, 0);
    }

    private boolean isDraw(IGameState state) {
        String[][] macroboard = state.getField().getMacroboard();
        for(int x = 0; x < macroboard.length; x++) {
            for(int y = 0; y < macroboard[x].length; y++) {
                if(macroboard[x][y].equals(IField.EMPTY_FIELD) || macroboard[x][y].equals(IField.AVAILABLE_FIELD)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isWinOnBoard(String[][] board, int startingX, int startingY) {
        for (int x = startingX; x < startingX + 3; x++) {
            if (isHorizontalWin(board, x, startingY) || isVerticalWin(board, startingX, x)) {
                return true;
            }
        }
        return isDiagonalWin(board, startingX, startingY);
    }

    private boolean isHorizontalWin(String[][] board, int startingX, int startingY) {
        return checkLineForWin(board[startingX][startingY], board[startingX][startingY + 1], board[startingX][startingY + 2]);
    }

    private boolean isVerticalWin(String[][] board, int startingX, int startingY) {
        return checkLineForWin(board[startingX][startingY], board[startingX + 1][startingY], board[startingX + 2][startingY]);
    }

    private boolean isDiagonalWin(String[][] board, int row, int col) {
        boolean forwardDiagonal = checkLineForWin(board[row][col], board[row+1][col+1], board[row+2][col+2]);
        boolean backwardDiagonal = checkLineForWin(board[row][col+2], board[row+1][col+1], board[row+2][col]);
        return forwardDiagonal || backwardDiagonal;
    }


    private boolean checkLineForWin(String val1, String val2, String val3) {
        return val1.equals(val2) && val2.equals(val3) && (val1.equals(PLAYER_ONE) || val1.equals(PLAYER_TWO));
    }

}
