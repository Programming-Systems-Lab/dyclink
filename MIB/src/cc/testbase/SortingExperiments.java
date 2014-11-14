package cc.testbase;

import java.util.Arrays;
import java.util.Random;

public class SortingExperiments {
	
	public int[] selectionSort(int[] numbers)
	{
	    for (int i = 0; i < numbers.length - 1; i++) {
	        int index = i;
	        for (int j = i + 1; j < numbers.length; j++)
	            if (numbers[j] < numbers[index]) //Finds smallest number
	                index = j;
	 
	        int smallerNumber = numbers[index];  //Swap
	        numbers[index] = numbers[i];
	        numbers[i] = smallerNumber;
	 
	    }
	    return numbers;
	}
		
	public void insertionSortWhile(int[] numbers) {
	    for (int i = 0; i < numbers.length; i++) {
	        int copyNumber = numbers[i];
	        int j = i;
	        while (j > 0 && copyNumber < numbers[j-1]) {
	            numbers[j] = numbers[j-1];
	            j--;
	        }
	        numbers[j] = copyNumber;
	    }
	}
	
	public void insertionSortFor(int[] numbers) {
	    for (int i = 0; i < numbers.length; i++) {
	        int copyNumber = numbers[i];
	        int j = i - 1;
	        for (; j >= 0 && numbers[j] > copyNumber; j--) {
	        	numbers[j + 1] = numbers[j];
	        }
	        numbers[j + 1] = copyNumber;
	    }
	}
	
	public int[] bubbleSort(int[] data){
		int lenD = data.length;
		int tmp = 0;
		for(int i = 0;i<lenD;i++){
			for(int j = (lenD-1);j>=(i+1);j--){
				if(data[j]<data[j-1]){
					tmp = data[j];
					data[j]=data[j-1];
					data[j-1]=tmp;
				}
			}
		}
		return data;
	}
	
	public int[] quickSort(int[] data){
		int lenD = data.length;
		int pivot = 0;
		int ind = lenD/2;
		int i,j = 0,k = 0;
		if(lenD<2){
			return data;
		} else{
			int[] L = new int[lenD];
			int[] R = new int[lenD];
			int[] sorted = new int[lenD];
			pivot = data[ind];
			for(i=0;i<lenD;i++){
				if(i!=ind){
					if(data[i]<pivot){
						L[j] = data[i];
						j++;
					} else{
						R[k] = data[i];
						k++;
					}
				}
			}
			int[] sortedL = new int[j];
			int[] sortedR = new int[k];
			System.arraycopy(L, 0, sortedL, 0, j);
			System.arraycopy(R, 0, sortedR, 0, k);
			sortedL = quickSort(sortedL);
			sortedR = quickSort(sortedR);
			System.arraycopy(sortedL, 0, sorted, 0, j);
			sorted[j] = pivot;
			System.arraycopy(sortedR, 0, sorted, j+1, k);
			return sorted;
		}
	}
	
	public int[] mergeSort(int[] data){
		int lenD = data.length;
		if(lenD<=1){
			return data;
		} else{
			int[] sorted = new int[lenD];
			int middle = lenD/2;
			int rem = lenD-middle;
			int[] L = new int[middle];
			int[] R = new int[rem];
			System.arraycopy(data, 0, L, 0, middle);
			System.arraycopy(data, middle, R, 0, rem);
			L = this.mergeSort(L);
			R = this.mergeSort(R);
			sorted = merge(L, R);
			return sorted;
		}
	}
		 
	public int[] merge(int[] L, int[] R){
		int lenL = L.length;
		int lenR = R.length;
		int[] merged = new int[lenL+lenR];
		int i = 0;
		int j = 0;
		while(i<lenL||j<lenR){
			if(i<lenL & j<lenR){
				if(L[i]<=R[j]){
					merged[i+j] = L[i];
					i++;
				} else{
					merged[i+j] = R[j];
					j++;
				}
			} else if(i<lenL){
				merged[i+j] = L[i];
				i++;
			} else if(j<lenR){
				merged[i+j] = R[j];
				j++;
			}
		}
		return merged;
	}
	
	public int[] shellSort(int[] data){
		int lenD = data.length;
		int inc = lenD/2;
		while(inc>0){
			for(int i=inc;i<lenD;i++){
				int tmp = data[i];
				int j = i;
				while(j>=inc && data[j-inc]>tmp){
					data[j] = data[j-inc];
					j = j-inc;
				}
				data[j] = tmp;
			}
			inc = (inc /2);
		}
		return data;
	}
	
	public void bucketSort(int[] a, int maxVal) {		
		int [] bucket=new int[maxVal+1];
		
		for (int i=0; i<bucket.length; i++) {
			bucket[i]=0;
		}
	 
		for (int i=0; i<a.length; i++) {
			bucket[a[i]]++;
		}
	 
		int outPos=0;
		for (int i=0; i<bucket.length; i++) {
			for (int j=0; j<bucket[i]; j++) {
				a[outPos++]=i;
			}
		}
	}
	
	public void countingSort(int[] a, int low, int high) {		
	    int[] counts = new int[high - low + 1]; // this will hold all possible values, from low to high
	    for (int x : a)
	        counts[x - low]++; // - low so the lowest possible value is always 0
	 
	    int current = 0;
	    for (int i = 0; i < counts.length; i++)
	    {
	        Arrays.fill(a, current, current + counts[i], i + low); // fills counts[i] elements of value i + low in current
	        current += counts[i]; // leap forward by counts[i] steps
	    }
	}
	
	public void pigeonhole_sort(int[] a) {
	    // size of range of values in the list (ie, number of pigeonholes we need)
	    int min = a[0], max = a[0];
	    for (int x : a) {
	        min = Math.min(x, min);
	        max = Math.max(x, max);
	    }
	    final int size = max - min + 1;
	 
	    // our array of pigeonholes
	    int[] holes = new int[size];  
	 
	    // Populate the pigeonholes.
	    for (int x : a)
	        holes[x - min]++;
	 
	    // Put the elements back into the array in order.
	    int i = 0;
	    for (int count = 0; count < size; count++)
	        while (holes[count]-- > 0)
	            a[i++] = count + min;
	}
	
	public void gnomeSort( int[] theArray ) { 
		for ( int index = 1; index < theArray.length; ) { 
			if ( theArray[index - 1] <= theArray[index] ) { 
				++index; 
			} else { 
				int tempVal = theArray[index]; 
				theArray[index] = theArray[index - 1]; 
				theArray[index - 1] = tempVal; 
				--index; 
				if ( index == 0 ) { 
					index = 1; 
				}           
			} 
		} 
	}
	
	public static int[] copyData(int[] data) {
		int[] ret = new int[data.length];
		System.arraycopy(data, 0, ret, 0, data.length);
		return ret;
	}
	
	public static int high(int[] data) {
		int high = Integer.MIN_VALUE;
		
		for (int i = 0; i < data.length; i++) {
			if (data[i] > high) {
				high = data[i];
			}
		}
		
		return high;
	}
	
	public static int low(int[] data) {
		int low = Integer.MAX_VALUE;
		
		for (int i = 0; i < data.length; i++) {
			if (data[i] < low) {
				low = data[i];
			}
		}
		
		return low;
	}
	
	public static void main(String[] args) {
		/*int size = 4;
		Random randomGenerator = new Random();
		int[] inputData = new int[size];
		for (int i = 0; i < size; i++) {
			inputData[i] = randomGenerator.nextInt(100);
		}*/
		int[] inputData = {4, 83, 32, 15};
		
		SortingExperiments se = new SortingExperiments();
		
		int[] selectionInput = copyData(inputData);
		int[] selectionResult = se.selectionSort(selectionInput);
		System.out.println("Selection sort: " + Arrays.toString(selectionResult));
		
		int[] insertInput = copyData(inputData);
		se.insertionSortWhile(insertInput);
		System.out.println("Insertion sort: " + Arrays.toString(insertInput));
		
		int[] insertInput2 = copyData(inputData);
		se.insertionSortFor(insertInput2);
		System.out.println("Insertion sort2: " + Arrays.toString(insertInput2));
		
		int[] bubbleInput = copyData(inputData);
		int[] bubbleResult = se.bubbleSort(bubbleInput);
		System.out.println("Bubble sort: " + Arrays.toString(bubbleResult));
		
		int[] quickInput = copyData(inputData);
		int[] quickResult = se.quickSort(quickInput);
		System.out.println("Quick sort: " + Arrays.toString(quickResult));
		
		int[] mergeInput = copyData(inputData);
		int[] mergeResult = se.mergeSort(mergeInput);
		System.out.println("Merge sort: " + Arrays.toString(mergeResult));
		
		int[] shellInput = copyData(inputData);
		int[] shellResult = se.mergeSort(shellInput);
		System.out.println("Shell sort: " + Arrays.toString(shellResult));
		
		int[] bucketInput = copyData(inputData);
		int bucketHigh = high(inputData);
		se.bucketSort(bucketInput, bucketHigh);
		System.out.println("Bucket sort: " + Arrays.toString(bucketInput));
		
		int[] countingInput = copyData(inputData);
		int high = high(inputData);
		int low = low(inputData);
		se.countingSort(countingInput, low, high);
		System.out.println("Counting sort: " + Arrays.toString(countingInput));
		
		int[] pigeonholeInput = copyData(inputData);
		se.pigeonhole_sort(pigeonholeInput);
		System.out.println("Pigeonhole sort: " + Arrays.toString(pigeonholeInput));
		
		int[] gnomeInput = copyData(inputData);
		se.gnomeSort(gnomeInput);
		System.out.println("Gnome sort: " + Arrays.toString(gnomeInput));
	}
}
