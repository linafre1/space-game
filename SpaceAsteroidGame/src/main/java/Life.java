public class Life extends GameObject {

    private char shape = '\u2665';

    public Life(int x, int y, int oldX, int oldY, int sizeWidth, int sizeHeight) {
        super(x, y, oldX, oldY, sizeWidth, sizeHeight);
    }

    public char getShape() {
        return shape;
    }
}
