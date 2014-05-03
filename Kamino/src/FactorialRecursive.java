public class FactorialRecursive {

    public FactorialRecursive(int n) {
        System.out.println(factorial(n));
    }

    public int factorial(int n) {
        if (n == 0)
            return 1;
        return n * factorial(n - 1);
    }

    public static void main(String[] args) {
        int n = Integer.valueOf(args[0]);
        if (n >= 0) {
            new FactorialRecursive(n);
        } else {
            System.err.println("FactorialRecursive: input: integer n such that n >= 0");
        }
    }
}
