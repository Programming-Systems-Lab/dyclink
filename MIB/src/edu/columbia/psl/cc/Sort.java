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
	public void bubbleSort(int ar[]) {
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
	public void selectionSort(int[] ar) {
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
	public void insertionSort(int[] ar) {
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
	
	@extractTemplate
	private void quickSort(int[] array, int lowerIndex, int higherIndex) {
        
        int i = lowerIndex;
        int j = higherIndex;
        // calculate pivot number, I am taking pivot as middle index number
        int pivot = array[lowerIndex+(higherIndex-lowerIndex)/2];
        // Divide into two arrays
        while (i <= j) {
            /**
             * In each iteration, we will identify a number from left side which 
             * is greater then the pivot value, and also we will identify a number 
             * from right side which is less then the pivot value. Once the search 
             * is done, then we exchange both numbers.
             */
            while (array[i] < pivot) {
                i++;
            }
            while (array[j] > pivot) {
                j--;
            }
            if (i <= j) {
                //exchangeNumbers(i, j);
                int temp = array[i];
                array[i] = array[j];
                array[j] = temp;
                //move index to next position on both sides
                i++;
                j--;
            }
        }
        // call quickSort() method recursively
        if (lowerIndex < j)
            quickSort(array, lowerIndex, j);
        if (i < higherIndex)
            quickSort(array, i, higherIndex);
    }
	
	public void showArray(int[] ar) {
		System.out.println(Arrays.toString(ar));
	}
	
	public static void main(String[] args) {
		Sort s = new Sort();
		int[] ar = {5, 7, 1, 4, 3, 6, 17, 8};
		int[] copy1 = new int[ar.length];
		int[] copy2 = new int[ar.length];
		int[] copy3 = new int[ar.length];
		int[] copy4 = new int[ar.length];
		int[] copy5 = new int[ar.length];
		System.arraycopy(ar, 0, copy1, 0, ar.length);
		System.arraycopy(ar, 0, copy2, 0, ar.length);
		System.arraycopy(ar, 0, copy3, 0, ar.length);
		System.arraycopy(ar, 0, copy4, 0, ar.length);
		System.arraycopy(ar, 0, copy5, 0, ar.length);
		//s.selectionSort(ar);
		s.mySort(copy1);
		s.showArray(copy1);
		s.selectionSort(copy2);
		s.showArray(copy2);
		s.insertionSort(copy3);
		s.showArray(copy3);
		s.bubbleSort(copy4);
		s.showArray(copy4);
		s.quickSort(copy5, 0, copy5.length - 1);
		s.showArray(copy5);
	}
}
