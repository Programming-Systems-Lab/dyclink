public class GCDRecursive {

    public GCDRecursive(int x, int y) {
        System.out.println(GCD(x, y));
    }

    public int GCD(int x, int y) {
        if (y == 0)
            return x;
        return GCD(y, x % y);
    }

    public static void main(String[] args) {
        int x = Integer.valueOf(args[0]);
        int y = Integer.valueOf(args[1]);
        if (x >= y && y >= 0) {
            new GCDRecursive(x, y);
        } else {
            System.err.println("GCDRecursive: input: integer x, integer y such that x >= y and y >= 0");
        }
    }
}
