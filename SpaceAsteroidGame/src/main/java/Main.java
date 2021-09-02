import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Random;

public class Main {

    static ArrayList<GameObject> gameObjects = new ArrayList<>();
    static int points;
    static Player player;
    static Random random = new Random();
    static int level = 1;

    public static void main(String[] args) throws Exception {

        TerminalSize ts = new TerminalSize(90, 36);
        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
        terminalFactory.setInitialTerminalSize(ts);
        Terminal terminal = terminalFactory.createTerminal();

        terminal.setCursorVisible(false); //Gömmer pekaren
        TerminalSize terminalSize = terminal.getTerminalSize(); //Hämtar storleken på terminalen
        int columns = terminalSize.getColumns(); //sätter en variabel till maxvärdet av bredden (x-värdet)
        int rows = terminalSize.getRows(); //sätter en variabel till maxvärdet av höjden (y-värdet)

        int startY = rows / 2; //ska vi byta till int så blir det bättre koppling till columns & rows?
        int startX = columns / 10;
        int playerWidth = 6;
        int playerHeight = 3;

        player = new Player(startX, startY, startX, startY, playerWidth, playerHeight, 3);

        inputOutput(terminal, rows, columns);

    }

    private static void inputOutput(Terminal terminal, int rows, int columns) throws Exception {
        LocalTime lastTimeMode = LocalTime.now();
        LocalTime lastTimePoint = LocalTime.now();
        LocalTime pointsTime = LocalTime.now();
        LocalTime lastTimeLife = LocalTime.now();
        boolean continueReadingInput = true;
        int thousandCounter = 1;


        while (continueReadingInput) {

            KeyStroke keyStroke = null;

            int timeStep = 0;
            int timeStepForAsteroids = 0;
            int timeStepForPoints = 0;
            int timeStepForLives = 0;
            int levelManager = 10;


            do {
                Thread.sleep(levelManager); //might throw InterruptedException
                keyStroke = terminal.pollInput();
                if (timeStep > 5) {
                    timeStep = 0;
                    newPosition(); //metod för side scroller
                    moveObjects(terminal); //metod för objekthanteraren
                    movePlayer(terminal);
                    removeGameObjectWhenOutOfScreen(terminal);
                    if (points/1000 == thousandCounter){
                        System.out.println(points/1000);
                        System.out.println(thousandCounter);
                        thousandCounter++;
                        if (levelManager > 0) {
                            levelManager-=2;
                            level++;
                        }

                    }
                    pointsTime = pointsCheck(pointsTime); // Passiv inkomst

                    if (timeStepForAsteroids > 5) {
                        timeStepForAsteroids = 0;
                        createNewGameObjects(rows, columns,GameObjectType.ASTEROID);
                    }

                    if (timeStepForPoints > 20) {
                        timeStepForPoints= 0;
                        createNewGameObjects(rows, columns,GameObjectType.POINT);
                    }

                    if (timeStepForLives > 60) {
                        timeStepForLives = 0;
                        createNewGameObjects(rows, columns, GameObjectType.LIFE);
                    }

                    if (checkCrash(terminal)) { //metod för kollisionskontroll
                        gameOver(terminal, rows, columns); //metod för game over
                        terminal.flush();
                        Thread.sleep(1000*5);
                        terminal.close();
                        continueReadingInput = false;
                        break;
                    }
                    timeStepForAsteroids++;
                    timeStepForPoints++;
                    timeStepForLives++;

                }
                timeStep++;
                drawScoreBoard(terminal);
                terminal.flush();

            } while (keyStroke == null);

            if(keyStroke == null){
                break;
            }

            KeyType type = keyStroke.getKeyType();
            Character c = keyStroke.getCharacter();
            System.out.println(type + ", " + c);

            if (c == Character.valueOf('q') || c == Character.valueOf('Q')) {
                quitGame(terminal, rows, columns);
                continueReadingInput = false;
                terminal.close();
                System.out.println("quit");
            }

            callMovementManeuver(keyStroke, rows, columns); //Kolla sen när spelet körs om vi måste öka eller minska hastighet

            if (LocalTime.now().isAfter(lastTimeMode.plusNanos(800000000))) {
                createNewGameObjects(rows, columns, GameObjectType.ASTEROID);
                newPosition();
                lastTimeMode = LocalTime.now();
            }
            if(LocalTime.now().isAfter(lastTimePoint.plusSeconds(4))){
                createNewGameObjects(rows, columns, GameObjectType.POINT);
                lastTimePoint = LocalTime.now();
            }

            if (LocalTime.now().isAfter(lastTimeLife.plusSeconds(8))) {
                createNewGameObjects(rows, columns, GameObjectType.LIFE);
                lastTimeLife = LocalTime.now();
            }

            if (checkCrash(terminal)) {
                gameOver(terminal, rows, columns); //metod för game over
                terminal.flush();
                Thread.sleep(1000*5);
                terminal.close();
                break;

            } else {

                moveObjects(terminal);
                movePlayer(terminal);
                pointsTime = pointsCheck(pointsTime);
                removeGameObjectWhenOutOfScreen(terminal);
            }
            drawScoreBoard(terminal);
            terminal.flush();
        }
    }
    private static void gameOver(Terminal terminal,int rows,int columns) throws Exception {
        String gameOver = "GAME OVER GAME OVER GAME OVER GAME OVER GAME OVER GAME OVER";
        terminal.clearScreen();
        terminal.setCursorPosition(columns/2 - (gameOver.length()/2),rows/2);

        for (int i = 0; i < gameOver.length(); i++) {
            terminal.setForegroundColor(TextColor.ANSI.WHITE);
            terminal.putCharacter(gameOver.charAt(i));
        }

        String finalPoints = "POINTS: " + points;
        terminal.setCursorPosition(columns/2 - (finalPoints.length()/2), rows/4);
        for (char point : finalPoints.toCharArray()) {
            terminal.setForegroundColor(TextColor.ANSI.WHITE);
            terminal.putCharacter(point);
        }

        printShip(terminal, columns, rows);

    }

    private static void quitGame(Terminal terminal, int rows, int columns) throws Exception {
        String quiting = "QUITING GAME";
        terminal.clearScreen();
        terminal.setCursorPosition(columns/2 - (quiting.length()/2), rows/2);
        for (int q = 0; q < quiting.length(); q++) {
            terminal.setForegroundColor(TextColor.ANSI.WHITE);
            terminal.putCharacter(quiting.charAt(q));
        }
        printShip(terminal, columns, rows);
        terminal.flush();
        Thread.sleep(1000*3);
    }

    private static void printShip(Terminal terminal, int columns, int rows) throws Exception {
        int i1 = 0;
        for (char[] charArray : player.getShape()) {
            int i2 = 0;
            for (char c : charArray) {

                terminal.setCursorPosition(columns/2 + i2 - (player.sizeWidth/2), rows/3 + i1);
                terminal.setForegroundColor(TextColor.ANSI.WHITE);
                terminal.putCharacter(c);
                i2++;
            }
            i1++;

        }
    }

    private static boolean checkCrash(Terminal terminal) throws Exception {

        int playerX, playerY;
        int objectX, objectY;
        for (GameObject object : gameObjects) {

            for (int pX = 0; pX < player.getSizeWidth(); pX++) {

                for (int pY = 0; pY < player.getSizeHeight(); pY++) {

                    for (int oX = 0; oX < object.getSizeWidth(); oX++) {

                        for (int oY = 0; oY < object.getSizeHeight(); oY++) {

                            playerX = player.getX() + pX;
                            playerY = player.getY() + pY;
                            objectX = object.getX() + oX;
                            objectY = object.getY() + oY;

                            if (playerX == objectX && playerY == objectY) {
                                if (object instanceof Point) {
                                    points += 288; // refactor?
                                    removeGameObjectFromScreen(terminal, object);
                                    gameObjects.remove(object);
                                    return false;
                                } else if (object instanceof Asteroid) {
                                    if(player.getHealth() <= 1){
                                        return true;
                                    } else {
                                        player.setHealth(player.getHealth()-1);
                                        gameObjects.remove(object);
                                        return false;
                                    }

                                } else if (object instanceof Life) {
                                    player.setHealth(player.getHealth()+ 1);
                                    removeGameObjectFromScreen(terminal, object);
                                    gameObjects.remove(object);
                                    return false;
                                }
                            }

                        }
                    }

                }

            }


        }
        return false;
    }

    private static boolean checkPlayer(int rows) {
        if (player.y < 0 || player.y > rows) {
            return false;
        }
        return true;
    }

    private static boolean checkGameObject(GameObject gameObject) {
        if (gameObject.x < 1) {
            return false;
        }
        return true;
    }

    private static boolean putPlayerBackUP(int rows, int stepSize) {
        if  ((player.y+stepSize) > rows - player.sizeHeight){
            return true;
        }
        return false;
    }

    private static boolean putPlayerBackDOWN(int rows, int stepSize) {
        if ((player.y - stepSize) < 0) {
            return true;
        }
        return false;
    }

    private static boolean putPlayerBackRIGHT(int columns, int stepSize) {
        if ((player.x + stepSize) < 5) {
            return true;
        }
        return false;
    }

    private static boolean putPlayerBackLEFT(int columns, int stepSize) {
        if ((player.x + stepSize) > 15) {
            return true;
        }
        return false;
    }

    private static void callMovementManeuver(KeyStroke keyStroke, int rows, int columns) {
        int stepSize = 1;
        switch (keyStroke.getKeyType()) {
            case ArrowDown -> {
                if (!putPlayerBackUP(rows, stepSize)) {
                    player.oldY = player.y;
                    player.setY(player.y + stepSize); //se till så att det finns en set-metod i player (som plusar på y med värdet som skickas in)
                }
            }
            case ArrowUp -> {
                if (!putPlayerBackDOWN(rows, stepSize)) {
                    player.oldY = player.y;
                    player.setY(player.y - stepSize);
                }
            }
            case ArrowLeft -> {
                if (!putPlayerBackRIGHT(columns, stepSize)) {
                    player.oldX = player.x;
                    player.setX(player.x - stepSize);
                }
            }
            case ArrowRight -> {
                if (!putPlayerBackLEFT(columns, stepSize)) {
                    player.oldX = player.x;
                    player.setX(player.x + stepSize);
                }
            }
            default -> { //kanske inte behövs?
                return;
            }
        }
    }

    private static void drawScoreBoard(Terminal terminal) throws Exception {
        String scoreboard = "Points: " + points;
        String health = "Health: " + player.getHealth();
        String currentLevel = "Level: " + level;
        for (int i = 0; i < scoreboard.length(); i++) {
            terminal.setCursorPosition(i + 5, 1);
            terminal.setForegroundColor(TextColor.ANSI.WHITE);
            terminal.putCharacter(scoreboard.charAt(i));
        }
        for (int k = 0; k < health.length(); k++) {
            terminal.setCursorPosition(k + 5, 2);
            terminal.setForegroundColor(TextColor.ANSI.WHITE);
            terminal.putCharacter(health.charAt(k));
        }

        for (int l = 0; l < currentLevel.length(); l++) {
            terminal.setCursorPosition(l + 70, 1);
            terminal.setForegroundColor(TextColor.ANSI.WHITE);
            terminal.putCharacter(currentLevel.charAt(l));
        }
    }

    private static void moveObjects(Terminal terminal) throws Exception {

        for (GameObject object : gameObjects) {
            terminal.setCursorPosition(object.oldX, object.oldY);
            terminal.putCharacter(' ');
            terminal.setCursorPosition(object.x, object.y);
            if (object instanceof Asteroid) {
                Asteroid a = (Asteroid) object;
                terminal.setForegroundColor(TextColor.ANSI.WHITE);
                terminal.putCharacter(a.getShape());
            }
            if (object instanceof Point) {
                Point sp = (Point) object;
                terminal.setForegroundColor(TextColor.ANSI.MAGENTA);
                terminal.putCharacter(sp.getShape());
            }
            if (object instanceof Life) {
                Life l = (Life) object;
                terminal.setForegroundColor(TextColor.ANSI.RED);
                terminal.putCharacter(l.getShape());
            }
        }
    }

    private static void movePlayer(Terminal terminal2) throws Exception {
        erasePlayer(terminal2);
        int i1 = 0;

        for (char[] charArray : player.getShape()) {
            int i2 = 0;
            for (char c : charArray) {

                terminal2.setCursorPosition(player.x + i2, player.y + i1);
                terminal2.setForegroundColor(TextColor.ANSI.CYAN);
                terminal2.putCharacter(c);
                i2++;
            }
            i1++;

        }

    }

    private static void erasePlayer(Terminal terminal) throws Exception {

        int i1 = 0;
        for (char[] charArray : player.getShape()) {
            int i2 = 0;
            for (char c : charArray) {
                terminal.setCursorPosition(player.oldX + i2, player.oldY + i1);
                terminal.putCharacter(' ');

                i2++;
            }
            i1++;

        }
        terminal.flush();
    }

    private static void removeGameObjectWhenOutOfScreen(Terminal terminal) throws IOException {

        for (GameObject gameObject : gameObjects) {
            if (!checkGameObject(gameObject)) {
                terminal.setCursorPosition(gameObject.oldX, gameObject.oldY);
                terminal.putCharacter(' ');
                terminal.setCursorPosition(gameObject.x, gameObject.y);
                terminal.putCharacter(' ');
                gameObjects.remove(gameObject);
                return;
            }
        }
    }

    private static void createNewGameObjects(int rows, int columns, GameObjectType gameObjectType) {
        int randomPosition = random.nextInt(rows - 6) + 5;
        while (checkGameObjectsPositions(randomPosition,columns)) {
            randomPosition = random.nextInt(rows - 6) + 5;
        }
        if (gameObjectType == GameObjectType.ASTEROID) {
            gameObjects.add(
                    new Asteroid(columns, randomPosition, columns, randomPosition, 1, 1,
                            '\u25CF'));
        } else if (gameObjectType == GameObjectType.POINT) {
            gameObjects.add(
                    new Point(columns, randomPosition, columns, randomPosition, 1, 1,
                            '\u20BF'));
        } else if (gameObjectType == GameObjectType.LIFE) {
            gameObjects.add(new Life(columns, randomPosition, columns, randomPosition, 1, 1));
        }
    }

    private static boolean checkGameObjectsPositions(int randomPosition, int columns) {

        for (GameObject gameObject : gameObjects) {
            if (gameObject.y == randomPosition && gameObject.x == columns) {
                return true;
            }
        }

        return false;
    }

    private static void newPosition() {
        //Loopar igenom listan av astroider och sätter ett nytt x värde
        // System.out.println("newPositions");
        for (GameObject asteroid : gameObjects) {
            asteroid.oldX = asteroid.x;
            asteroid.x-=1;
        }
    }

    private static LocalTime pointsCheck (LocalTime pointsTime) {
        if (LocalTime.now().isAfter(pointsTime.plusSeconds(2))) {
            points++;
            return LocalTime.now();
        } return pointsTime;
    }

    private static void removeGameObjectFromScreen(Terminal terminal, GameObject gameObject) throws Exception {
        terminal.setCursorPosition(gameObject.oldX, gameObject.oldY);
        terminal.putCharacter(' ');
        terminal.setCursorPosition(gameObject.x, gameObject.y);
        terminal.putCharacter(' ');
    }

}


