package com.vizualize.peaksandvaleys;

import java.util.ArrayList;
import java.util.HashMap;

public class PeaksAndValeys {

	public static double findLocalMin(double [] arrA){
		return findLocalMin(arrA, 0, arrA.length);
	}
	
	public static double findLocalMin(double [] arrA, int start, int end){

		//find the mid
		int mid = start + (end-start)/2;
		int size = arrA.length;

		//check the local minima (element is smaller than its left and right neighbors)
		//first check if left and right neighbor exists
		if((mid==0 || arrA[mid-1]>arrA[mid]) &&
				(mid==size-1 || arrA[mid+1]>arrA[mid]))
			return arrA[mid];
		//check if left neighbor is less than mid element, if yes go left
		else if(mid>0 && arrA[mid]>arrA[mid-1]){
			return findLocalMin(arrA, start, mid);
		}else { //if(mid<size-1 && arrA[mid]>arrA[mid+1])
			return findLocalMin(arrA, mid+1, end);
		}
	}
	
	public static double findLocalMax(double [] arrA){
		return findLocalMax(arrA, 0, arrA.length);
	}
	
	public static double findLocalMax(double [] arrA, int start, int end){

		//find the mid
		int mid = start + (end-start)/2;
		int size = arrA.length;

		//check the local minima (element is smaller than its left and right neighbors)
		//first check if left and right neighbor exists
		if((mid==0 || arrA[mid-1]<arrA[mid]) &&
				(mid==size-1 || arrA[mid+1]<arrA[mid]))
			return arrA[mid];
		//check if left neighbor is less than mid element, if yes go left
		else if(mid>0 && arrA[mid]<arrA[mid-1]){
			return findLocalMax(arrA, start, mid);
		}else { //if(mid<size-1 && arrA[mid]<arrA[mid+1])
			return findLocalMax(arrA, mid+1, end);
		}
	}
	
	public static HashMap<Integer, String> localMinima(double [] arrA) {
		int size = arrA.length;
		int sampleSize = 7;		
		HashMap<Integer, String> resultHash = new HashMap<>();
		
		for (int i = 0;i<size - sampleSize;i++) {
			double first = arrA[i];
			double second = arrA[i+1];
			double third = arrA[i+2];
			double fourth = arrA[i+2];
			double fifth = arrA[i+3];
			double sixth = arrA[i+4];
			double seventh = arrA[i+5];
			double eight = arrA[i+6];
			double nineth = arrA[i+6];
			if (first > fifth && second > fifth && third > fifth && fourth > fifth &&
					fifth < sixth && fifth < seventh && fifth < eight && fifth < nineth) {
				resultHash.put(i+3, "bottom");
			}
			
			if (first < fifth && second < fifth && third < fifth && fourth < fifth &&
					fifth > sixth && fifth > seventh && fifth > eight && fifth > nineth) {
				resultHash.put(i+3, "top");
			}			
		}
		return resultHash;
	}
}
