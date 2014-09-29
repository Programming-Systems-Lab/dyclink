package edu.columbia.psl.cc;

import java.util.Arrays;

import edu.columbia.psl.cc.annot.analyzeClass;
import edu.columbia.psl.cc.annot.extractTemplate;
import edu.columbia.psl.cc.annot.testTemplate;

@analyzeClass
public class Sort {
	
	@testTemplate
	public void mySort(int[] ar) {
		for (int i = 1; i < ar.length; i++) {
			int j = i;
			while (j > 0 && ar[j-1] > ar[j]) {
				int tmp = ar[j];
				ar[j] = ar[j-1];
				ar[j-1] = tmp;
				j--;
			}
		}
	}

	@extractTemplate
	void bubbleSort(int ar[]) {
	   for (int i = (ar.length - 1); i >= 0; i--) {
	      for (int j = 1; j <= i; j++) {
	         if (ar[j-1] > ar[j]) {
	              int temp = ar[j-1];
	              ar[j-1] = ar[j];
	              ar[j] = temp;
	         } 
	      } 
	    } 
	}
	
	@extractTemplate
	void selectionSort(int[] ar) {
		for (int i = 0; i < ar.length-1; i++) {
		      int min = i;
		      for (int j = i+1; j < ar.length; j++)
		            if (ar[j] < ar[min]) min = j;
		      int temp = ar[i];
		      ar[i] = ar[min];
		      ar[min] = temp;
		} 
	}
	
	@extractTemplate
	void insertionSort(int[] ar) {
	   for (int i=1; i < ar.length; i++) {
	      int index = ar[i]; int j = i;
	      while (j > 0 && ar[j-1] > index)
	      {
	           ar[j] = ar[j-1];
	           j--;
	      }
	      ar[j] = index;
	   } 
	}
	
	public void showArray(int[] ar) {
		System.out.println(Arrays.toString(ar));
	}
	
	public static void main(String[] args) {
		Sort s = new Sort();
		int[] ar = {5, 7, 1, 4, 3, 6};
		//s.selectionSort(ar);
		s.mySort(ar);
		s.showArray(ar);
	}
}
