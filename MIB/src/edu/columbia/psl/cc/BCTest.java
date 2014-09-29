package edu.columbia.psl.cc;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Hex;

public class BCTest {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		byte[] cipherKey = new byte[256/8];
        for (int i = 0; i < cipherKey.length; i++) {
            cipherKey[i] = (byte)i;
        }
		//KeyParameter param = new KeyParameter(Hex.decode("80000000000000000000000000000000"));
		KeyParameter param = new KeyParameter(cipherKey);
		/*BlockCipherVectorTest tester = new BlockCipherVectorTest(0, new AESEngineManual(),
                param, "00000000000000000000000000000000", "0EDD33D3C621E546455BD8BA1418BEC8");*/
		BlockCipherVectorTest tester = new BlockCipherVectorTest(0, new AESEngineManual(),
				param, "00112233445566778899AABBCCDDEEFF", "69C4E0D86A7B0430D8CDB78070B4C55A");
		tester.performTest();
	}

}
