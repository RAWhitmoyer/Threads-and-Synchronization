/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package threadsandsync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.lang.System.out;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.IntBuffer;
import java.util.concurrent.*;
import java.util.*;

/**
 *
 * @author Ricky_W
 */
public class ThreadsAndSync {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        System.out.println("---");
        System.out.println("---");
        System.out.println("---");
        //primes less than 100
        long[] primes = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97};

        
        //max size of buffer (used in instantiation and bounds checking later on)
        final int CAPACITY = 1000;
        //buffer for the random numbers
        ArrayList<Long> buffer = new ArrayList<>(CAPACITY);
        //buffer for the potential primes filter out of the original buffer
        ArrayList<Long> potPrimes = new ArrayList<>(CAPACITY);


        /*
        thread that prompts the user for input
        */
        class PromptThread extends Thread {

            private long num1;
            private long num2;
            private long num3;
            private Scanner scan = new Scanner(System.in);

            public PromptThread() {

            }

            //asks user for numbers
            //stores numbers in the buffer if there is room
            public void run() {
                
                synchronized (buffer) {
                    //System.out.println("prompt begin");
                    System.out.println("---------------------------");
                    System.out.println("---------------------------");
                    System.out.println("Enter your first number: \n");
                    num1 = scan.nextInt();
                    System.out.println("Enter your second number: \n");
                    num2 = scan.nextInt();
                    System.out.println("Enter your third number: \n");
                    num3 = scan.nextInt();
                    if(buffer.size() < (CAPACITY - 3)){
                        buffer.add(num1);
                        buffer.add(num2);
                        buffer.add(num3);
                        buffer.notify();
                    }
                    else{
                        try{
                            buffer.wait();
                        } catch(InterruptedException e){
                            
                        }
                    }
                        
                    
                    //System.out.println("buffer size: " + buffer.size());
                    //System.out.println("prompt finish");
                }

            }
        }

        /*
        thread to generate random numbers using java Random class
        */
        class GenRandNumberThread extends Thread {

            private long currRand;

            public GenRandNumberThread() {

            }

            //generates random numbers and stores them in the buffer if there is room
            public void run() {

                synchronized (buffer) {
                    //System.out.println("generating randoms");
                    Random rand = new Random();
                    for (int x = 0; x < 100; x++) {
                        currRand = rand.nextInt(1000000000);
                        if (buffer.size() < CAPACITY) {
                            //System.out.println("adding " + currRand + " to the buffer");
                            buffer.add(currRand);
                            //System.out.println("B.S. " + buffer.size());
                            buffer.notify(); //let others know you placed something in the buffer
                        }//if
                        else {
                            x--; //to ensure we get the correct number of random numbers
                            try {
                                buffer.notify();//let others know they can use the buffer
                                buffer.wait();//the buffer is full so wait for it to empty
                            }//try
                            catch (InterruptedException e) {

                            }//catch

                        }//else
                    }//for
                    //System.out.println("Generating randoms complete");
                    //System.out.println("Buffer size: " + buffer.size());
                }//synchronized

            }
        }

        /*
        retrieves random numbers from Random.org
        website limits amount of random numbers you can get
        */
        class RetrRandNumThread extends Thread {

            public RetrRandNumThread() {

            }

            //gets random numbers from the internet and stores them in the buffer if there is room
            public void run() {

                synchronized (buffer) {
                    //for (int x = 0; x < 10; x++) { //since you can only get so many from the source at once
                        try {
                            for (int y = 0; y < 10; y++) {
                                //System.out.println("Starting to retrieve random numbers");
                                URL url = new URL("https://www.random.org/integers/?num=10000&min=1&max=1000000000&col=5&base=10&format=plain&rnd=new");
                                //BufferedReader readNums = new BufferedReader(new InputStreamReader(url.openStream()));
                                Scanner scan = new Scanner(url.openStream());

                                while (scan.hasNext()) {
                                    long temp = Long.parseLong(scan.next());
                                    if (buffer.size() < CAPACITY) {
                                        //System.out.println("retr adding " + temp);
                                        buffer.add(temp);
                                        //System.out.println("B.S. " + buffer.size());
                                        buffer.notify();
                                    }//if
                                    else {
                                        try {
                                            buffer.notify();//lets other threads know it is full
                                            buffer.wait(); //waits for space to continue placing numbers in
                                        } catch (InterruptedException e) {

                                        }//catch
                                    }//else

                                }//while

                                
                                //System.out.println("done retrieving random numbers");

                                //System.out.println(buffer.size());
                            }//for
                        } catch (MalformedURLException e) {

                        } catch (IOException ioe) {

                        }//catch
                    //}//for
                }//synchronized
            }//run
        }

        /*
        thread that filters number from the buffer and moves them to the 
        potential prime buffer if they meet requirements 
        */
        class FilterThread extends Thread {

            long currElement;
            long currPrime;
            int notPrime;

            public FilterThread() {

            }

            //used to filter the potentially primes out of the buffer
            public void run() {

                while (true) {
                    synchronized (buffer) {
                        //System.out.println("filtering");
                        if (buffer.size() > 0) {
                            currElement = buffer.remove(buffer.size() - 1);
                            for(int x = 0; x < primes.length; x++){
                                currPrime = primes[x];
                                notPrime = 0;
                                if((currElement % currPrime) == 0){
                                    notPrime = -1;
                                    break; //breaks for loop bc it is divisible by a prime less than 100
                                }
                                
                            }//for
                            if(notPrime != -1){
                                //not prime was never set to -1 so it can be put into potential prime buffer
                                synchronized (potPrimes) {
                                    if(potPrimes.size() < CAPACITY){
                                        potPrimes.add(currElement);
                                        potPrimes.notify();
                                        //System.out.println("new potential prime: " + currElement);
                                        //System.out.println("Pot. Prime size: " + potPrimes.size());
                                    }
                                    else{
                                        try{
                                            potPrimes.notify(); //wakes up waiting thread
                                            potPrimes.wait(); //makes this thread wait
                                        } catch(InterruptedException e){
                                            
                                        }//catch
                                    }//else
                                }//synchronized
                            }//if
                            
                        }//if
                         else {
                            try{
                                buffer.notify();
                                buffer.wait();
                            } catch(InterruptedException e){
                                
                            }//catch
                            
                        }//else

                    }//sychronized
                    
                }//while
            }//run
        }//class

        /*
        thread that pulls potential primes out of the buffer to display them
        */
        class DisplayThread extends Thread {

            long currEl;

            public DisplayThread() {

            }

            //used to display the potentially prime numbers
            public void run() {
                
                while(true){
                    //System.out.println("display thread running");
                    synchronized(potPrimes){
                         if(potPrimes.size() > 0){
                             currEl = potPrimes.remove(potPrimes.size() - 1);
                             System.out.println("Potential Prime: " + currEl);
                        }//if
                         else{
                                try{
                                    potPrimes.notify();//wakes up other thread to keep filtering
                                    potPrimes.wait(); //makes this thread wait for more data
                                }catch(InterruptedException e){
                                    
                                }
                                
                             //catch
                         }//else
                    }//synchronized
                   
                }//while
                
            }//run
        }//class

        
        
        
        PromptThread prompt = new PromptThread();
        GenRandNumberThread rand = new GenRandNumberThread();
        FilterThread filter1 = new FilterThread();
        FilterThread filter2 = new FilterThread();
        DisplayThread display = new DisplayThread();
        RetrRandNumThread retrieve = new RetrRandNumThread();
        
        System.out.println("starting rand");
        rand.start();
        
        System.out.println("starting prompt");
        prompt.start();
        
        System.out.println("starting filter 1");
        filter1.start();
        
        System.out.println("starting filter 2");
        filter2.start();
        
        System.out.println("starting display");
        display.start();
             
        System.out.println("Starting retrieve");
        retrieve.start();


    }

    
}
