public class FactorialIterative {

    public FactorialIterative(int n) {
        System.out.println(factorial(n));
    }

    public int factorial(int n) {
        int running_total = 1;
        while (n != 0) {
            running_total *= n;
            n--;
        }
        return running_total;
    }

    public static void main(String[] args) {
        int n = Integer.valueOf(args[0]);
        if (n >= 0) {
            new FactorialIterative(n);
        } else {
            System.err.println("FactorialIterative: input: integer n such that n >= 0");
        }
    }
}
