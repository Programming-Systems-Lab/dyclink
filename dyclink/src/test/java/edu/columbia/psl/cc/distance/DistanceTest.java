package edu.columbia.psl.cc.distance;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.columbia.psl.cc.analysis.JaroWinklerDistance;

public class DistanceTest {
	
	private static final double TOLERANCE = Math.pow(10, -5);
	
	@Test
	public void testSimpleDistance() {
		int[] v1 = {14, 9, 7, 14, 9};
		int[] v2 = {14, 7, 3, 9, 14};
		
		JaroWinklerDistance cal = new JaroWinklerDistance(0.8, 5);
		double dist = cal.proximity(v1, v2);
		System.out.println("Dist: " + dist);
	}
	
	@Test
	public void testEqualDistance() {
		int[] v1 = {1, 2, 3, 4, 5};
		int[] v2 = {1, 2, 3, 4, 5};
		
		JaroWinklerDistance cal = new JaroWinklerDistance(0.8, 5);
		double dist = cal.proximity(v1, v2);
		assertEquals(dist, 1.0, TOLERANCE);
	}
	
	@Test
	public void testZeroDistance() {
		int[] v1 = {1, 2, 3, 4, 5};
		int[] v2 = {6, 7, 8, 9, 10};
		
		JaroWinklerDistance cal = new JaroWinklerDistance(0.8, 5);
		double dist = cal.proximity(v1, v2);
		assertEquals(dist, 0.0, TOLERANCE);
	}

}
