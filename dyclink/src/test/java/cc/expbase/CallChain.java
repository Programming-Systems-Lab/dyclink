package cc.expbase;

public class CallChain {
	
	public int data1;
	
	public double data2;
	
	public CallChain() {
		data1 = 18;
		data2 = 5.4987;
	}
	
	public double method1(CallChain cc) {
		cc.data1 = 15;
		double result = 0;
		for (int i = 0; i < 3; i++) {
			result += this.method2(cc, i);
		}
		return cc.data2 + result; 
	}
	
	public double method2(CallChain cc, int i) {
		if (i % 2 == 0) {
			return cc.data1 + this.method3(cc, 2);
		} else {
			cc.data2 = 3;
			return this.method3(cc, 1);
		}
	}
	
	public double method3(CallChain cc, int j) {
		if (j > 1) {
			cc.data2 = 3.29;
			return cc.data2 + 5;
		} else {
			return cc.data2 + 3;
		}
	}
	
	public static void main(String[] args) {
		CallChain cc = new CallChain();
		System.out.println(cc.method1(cc));
	}

}
