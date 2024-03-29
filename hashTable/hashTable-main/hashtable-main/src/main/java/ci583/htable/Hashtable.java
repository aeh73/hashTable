package ci583.htable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * A HashTable with no deletions allowed. Duplicates overwrite the existing value. Values are of
 * type V and keys are strings -- one extension is to adapt this class to use other types as keys.
 *
 * The underlying data is stored in the array `arr', and the actual values stored are pairs of
 * (key, value). This is so that we can detect collisions in the hash function and look for the next
 * location when necessary.
 */
public class Hashtable<V> {

	private static final int DOUBLE_HASH_MAX = 8; //used in the doubleHash method.
	private Object[] arr; //an array of Pair objects, where each pair contains the key and value stored in the hashtable.
	private int max; //the size of arr. This should be a prime number.
	private int itemCount; //the number of items stored in arr.
	private final double maxLoad = 0.6; //the maximum load factor.

	public enum PROBE_TYPE {
		LINEAR_PROBE, QUADRATIC_PROBE, DOUBLE_HASH
	}
	private final PROBE_TYPE probeType; //the type of probe to use when dealing with collisions

	/**
	 * Create a new Hashtable with a given initial capacity and using a given probe type.
	 * @param initialCapacity	The desired size of the Hashtable.
	 * @param pt				The probe type to be used.
	 */
	public Hashtable(int initialCapacity, PROBE_TYPE pt) {
		max = nextPrime(initialCapacity);			//Checks if initial capacity isPrime using the nextPrime method and becomes
		 											//the smallest prime number larger than or equal to the requested initial capacity
		arr = new Object[max];		     			//Creates a new Hashtable with the new capacity
		this.probeType = pt; 						//Sets the user defined probetype
	}
	
	/**
	 * Create a new Hashtable with a given initial capacity and using the default probe type.
	 * @param initialCapacity	The desired size of the Hashtable.
	 */
	public Hashtable(int initialCapacity) {
		//Improved code - removed redundant checks
		max = nextPrime(initialCapacity);			//Checks if initial capacity isPrime using the nextPrime method and becomes
										    		//the smallest prime number larger than or equal to the requested initial capacity
		arr = new Object[max];		     			//Creates a new Hashtable with the new capacity
		probeType = PROBE_TYPE.LINEAR_PROBE;		//And with a default probetype of 'LINEAR_PROBE'
	}

	/**
	 * Store the value against the given key. If the key is null or an empty string, throw an
	 * IllegalArgumentException. If the loadFactor exceeds maxLoad, call the resize
	 * method to resize the array. the If key already exists then its value should be overwritten.
	 * Create a new Pair item containing the key and value, then use the findEmpty method to find an unoccupied 
	 * position in the array to store the pair. Call findEmpty with the hashed value of the key as the starting
	 * position for the search, stepNum of zero and the original key. Increment the item count if and only if a new
	 * item was stored.
	 * @param key		The key to store.
	 * @param value 	The value to store against the key.
	 */
	public void put(String key, V value) {
		if (key == null) {
			throw new IllegalArgumentException("The key must not be null..");
		} else if (getLoadFactor()>=maxLoad) {
			resize();									 
		}
		
		int index = findEmptyOrSameKey(hash(key), key, 0);//use the hash method to find the hash value of the key
		if (hasKey(key)==true) {						  //i.e. returns the index of the pair 
			((Pair)arr[index]).value = value;  			  //if the location of the key/value pair already contains a pair
			return;										  //the value will be overwritten - this is done by casting a pair on  
		} else {										  //the arr[index] and retreiving its value
			arr[index]=new Pair(key, value);			  //else if it hasn't been stored before then store it and increment itemCount 
			itemCount++;									              
		}
	}

	/**
	 * Get the value associated with key, or return an empty Optional if the key does not exist. Use the find method to search the
	 * array, starting at the hashed value of the key, stepNum of zero and the original key. If the key is found return
	 * the value associated it wrapped up in an Optional using the Optional.of method, e.g. Optional.of(pair.value). If the key is not
	 * found return Optional.empty(). 
	 *
	 * @param key	The key of the object we are looking for.
	 * @return		An Optional containing the value we are asked to find, which is empty if the key was not present.
	 */
	public Optional<V> get(String key) {	
		if (find(hash(key), key, 0)==null) {		   	 //if the result of calling find with the hashed key,key and stepnum 
			return Optional.empty(); 				     //returns null return an optional.empty()
		} else {										 //else return the result of the find call
			return find(hash(key), key, 0);	
		}
	}

	/**
	 * Return true if the Hashtable contains this key, false otherwise.
	 * @param key	The key of the object we are looking for.
	 * @return		True if the hashtable contains the key.
	 */
	public boolean hasKey(String key) {
		return get(key).isPresent();					 //returns boolean if key.isPresent
	}

	/**
	 * Return all the keys in this Hashtable as a collection.
	 * @return	The collection of keys.
	 */
	public Collection<String> getKeys() {
		ArrayList<String> keys = new ArrayList<>();		//Creates arraylist to store keys
		for(int i = 0; i < max; i++) {					//loops through the array until it reaches max
			if(arr[i]!=null) {							// if arr[i] still has pair objects then create a new pair object
				Hashtable<V>.Pair pair = (Hashtable<V>.Pair)arr[i];// that references the arr[i] pair
				String key = pair.key;					//assign the key of the pair to a single variable key
				keys.add(key); 							//adds the key to the collection
			}
		}
		return keys;
	}

	/**
	 * Return the load factor, which is the ratio of itemCount to max.
	 * @return	The load factor
	 */
	public double getLoadFactor() {
		return (double)itemCount/max;					//items stored divided by the size of the array with 1 of the variables
	}													//casted as a double to return a decimal result if needed					 
	
	/**
	 * Return the maximum capacity of the Hashtable.
	 * @return	The maximum capacity.
	 */
	public int getCapacity() {
		return max;
	}
	
	/**
	 * Find an Optional containing the value stored for this key, starting the search at position startPos in the array.
	 * If the item at position startPos is null, the Hashtable does not contain the value, so return Optional.empty().
	 * If the key stored in the pair at position startPos matches the key we're looking for, return an Optional
	 * containing the associated value. If the key stored in the pair at position startPos does not match the key
	 * we're looking for, this is a hash collision so use the getNextLocation method with an incremented value of
	 * stepNum to find the next location to search (the way that this is calculated will differ depending on the
	 * probe type being used). Then use the value of the next location in a recursive call to find.
	 * @param startPos	The array index to check.
	 * @param key		The key of the Pair object we're looking for.
	 * @param stepNum	The number of times this method has been called in the current search.
	 * @return			The value of the Pair object with the right key.
	 */	
	private Optional<V> find(int startPos, String key, int stepNum) {	
		if (arr[startPos]==null) {												   // if startpos is null return the optional.empty()
			return Optional.empty();
		} else if(((Pair)arr[startPos]).key.equals(key)){						   //cast pair on arr[startpos] to retrive its key and compare
			return Optional.of(((Pair)arr[startPos]).value);					   //if its equal return optional.of
		} else { 
			return find(getNextLocation(startPos, key, stepNum + 1), key, stepNum);//recursive find call using getnextlocation method as startpos parameter
		}		
	}

	/**
	 * Find the first location where a value associated with key can be stored. Suitable locations are either
	 * unoccupied or contain a Pair object with the same key. The search begins at position startPos.
	 * If startPos is unoccupied or contains a Pair object with the same key, return startPos. Otherwise use
	 * the getNextLocation method with an incremented value of stepNum to find the appropriate next position to check
	 * (which will differ depending on the probe type being used) and use this in a recursive call to findEmpty.
	 * @param startPos	The array index to check.
	 * @param key		The key to store.
	 * @param stepNum	The number of times this method has been called in the current search for a location.
	 * @return			The location at which a Pair object with the key `key' can be stored.
	 */
	private int findEmptyOrSameKey(int startPos, String key, int stepNum) {
		if (arr[startPos]==null || ((Pair)arr[startPos]).key.equals(key)) {		   //cast pair on arr[startpos] to retrive its key and compare
			return startPos;
		} else {																   //recursive find call using getnextlocation method as startpos parameter
			return findEmptyOrSameKey(getNextLocation(startPos, key, stepNum + 1), key, stepNum + 1);
		}	
	}

	/**
	 * Finds the next position in the Hashtable array starting at position startPos. If the linear
	 * probe is being used, we just increment startPos. If the double hash probe type is being used, 
	 * add the double hashed value of the key to startPos. If the quadratic probe is being used, add
	 * the square of the step number to startPos.
	 * @param startPos	The starting position within the array.
	 * @param key		The kay of the value to find or store.
	 * @param stepNum	The number of times this method has been called in the current search for a location.
	 * @return			The next location
	 */
	private int getNextLocation(int startPos, String key, int stepNum) {
		int step = startPos;
		switch (probeType) {
		case LINEAR_PROBE:
			step++;
			break;
		case DOUBLE_HASH:
			step += doubleHash(key);
			break;
		case QUADRATIC_PROBE:
			step += stepNum * stepNum;
			break;
		default:
			break;
		}
		return step % max;
	}

	/**
	 * A secondary hash function which returns a small value (less than or equal to DBL_HASH_K)
	 * to probe the next location if the double hash probe type is being used.
	 * @param key	The string to hash
	 * @return		The hash value
	 */
	private int doubleHash(String key) {
		return (hash(key) % DOUBLE_HASH_MAX) + 1;
	}

	/**
	 * Return an int value calculated by hashing the key. See the lecture slides for information
	 * on creating hash functions. The return value should be a positive number less than max,
	 * the maximum capacity of the array
	 * @param key	The string to hash
	 * @return		The hash value
	 */
	private int hash(String key) {
		//Attempt at djb2 translating c to java - http://www.cse.yorku.ca/~oz/hash.html
		long hash = 5381;
		for (int i = 0; i < key.length(); i++) {
			hash = key.charAt(i) + ((hash << 5) - hash);
		}
		return (int) (hash%max);
	}

	/**
	 * Return true if n is prime
	 * @param n		The number to test
	 * @return		True if n is prime, false otherwise.
	 * A prime is a natural number greater than 1 that has no positive divisors other than 1 and itself.
	 * https://stackoverflow.com/questions/9625663/calculating-and-printing-the-nth-prime-number
	 */
	private boolean isPrime(int n) {
	/*Improved code, runs faster more corner cases to remove the need to test every number
	 *  up until the chosen number*/
		if (n<2) {									  //Corner cases
			return false;							  
		} else if(n==2||n==3) {
			return true;
		} else if(n%2==0 || n%3==0) {				  //eliminates the need to test even numbers
			return false;  
		}
		double sqr = Math.sqrt(n)+1;				  //Taking the square root of n and adding 1					  		
	/*There's no reason to loop the whole way up to n. If you've not found a multiple of n by time you've reached its square root, it's not prime.*/
		for (int i = 6; i<=sqr; i+=6) {				  //reduces the amount of checks needed
			if(n%(i-1) == 0 || n%(i+1) == 0) {		  //aswell by incrementing by 6 and starting at 6.
				return false;	
			}								  
		}
		return true;								  //else return true it is a prime number		
	}

	/**
	 * Get the smallest prime number which is larger than or equal to n
	 * @param n		The number for which to find the next prime.
	 * @return		The smallest prime number larger than or equal to n
	 */
	private int nextPrime(int n) {
		for(int i = n; true; i++) {					//For loop starts at 'n' or the previous prime number and increments up until 
			if(isPrime(i)) {						//condition is met - if 'i' is prime then simply return i
				return i;
			}
		}			
	}

	/**
	 * Resize the hashtable, to be used when the load factor exceeds maxLoad. The new size of
	 * the underlying array should be the smallest prime number which is at least twice the size
	 * of the old array.
	 */
	private void resize() {
		max = nextPrime(max*2);					//set max to a new value which is a prime number at least twice as large as the previous value
		itemCount = 0;					   		//reset itemCount to zero
		Object[] oldTable = arr;				//make a copy of the array
        arr = new Object[max];					//set the array to be a new empty array with size `max`
        for(int i = 0; i<oldTable.length; i++)  //loop through the old array calling put for every position that is not null
        	if(oldTable[i]!=null) {				//Re-adding keys and values aslong as the bucket is not empty from the oldTable
        		Pair pair = (Pair)oldTable[i];  //casts each key/value combination from the oldTable as a pair and assigns them to
        		put(pair.key, pair.value);		//a new pair object followed by calling put on the pair to the new table
        	}
	}

	
	/**
	 * Instances of Pair are stored in the underlying array. We can't just store
	 * the value because we need to check the original key in the case of collisions.
	 *
	 */
	private class Pair {
		private String key;
		private V value;

		Pair(String key, V value) {
			this.key = key;
			this.value = value;
		}
	}

}
