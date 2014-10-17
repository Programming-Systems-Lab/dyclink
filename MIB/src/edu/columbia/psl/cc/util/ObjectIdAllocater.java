package edu.columbia.psl.cc.util;

import java.util.concurrent.atomic.AtomicInteger;

public class ObjectIdAllocater {

	private static AtomicInteger indexer = new AtomicInteger();
	
	public static synchronized int getIndex() {
		return indexer.incrementAndGet();
	}
	
	public static void main(String[] args) {
		int i = getIndex();
	}
}
