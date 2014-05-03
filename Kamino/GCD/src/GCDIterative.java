public class GCDIterative {

    public GCDIterative(int x, int y) {
        System.out.println(GCD(x, y));
    }

    public int GCD(int x, int y) {
        int remainder;
        while (y != 0) {
            remainder = x % y;
            x = y;
            y = remainder;
        }
        return x;
    }

    public static void main(String[] args) {
        int x = Integer.valueOf(args[0]);
        int y = Integer.valueOf(args[1]);
        if (x >= y && y >= 0) {
            new GCDIterative(x, y);
        } else {
            System.err.println("GCDIterative: input: integer x, integer y such that x >= y and y >= 0");
        }
    }

}
