/*
Author: Jenny Li
Student no: 230130751
CPSC 473
Assignment 2
*/


import java.util.*;
import java.io.*;

public class FPGrowth{

    private int minsup, num_transactions;
    private Scanner scanner;
    private String transaction_file;
    private List<Integer> candidates;
    private List<Integer> counts;
    private FPNode root;
    
    public FPGrowth(String transaction_file, int minsup) throws FileNotFoundException {
        this.transaction_file = transaction_file;
        this.minsup = minsup;
        File file = new File(transaction_file);
        this.scanner = new Scanner(file);
        count_1_itemsets();  //initialize candidates and counts
        this.root = new FPNode(null, 0);
    } 
    
    public static void main(String[] args) throws FileNotFoundException {
        //default values for transaction file and minimum support threshold
        String transaction_file = "data.txt";
        int minsup = 50;
        
        if(args.length > 0) {
            transaction_file = args[0];
        }
        if(args.length > 1) {
            minsup = Integer.parseInt(args[1]);
        }
        
        FPGrowth fpg = new FPGrowth(transaction_file, minsup);
    }
    
    /*
        Finds the occurences of each 1-itemset and prunes the infrequent ones.
    */
    private void count_1_itemsets() {
        this.candidates = new ArrayList<>();  //list of individual items
        this.counts = new ArrayList<>();  //counts for each item from candidates[i].
        int num_items, current_item, index, current_count;
        
        this.num_transactions = this.scanner.nextInt();
        System.out.println("num transactions "+this.num_transactions);
        
        //determine counts for each item
        while(this.scanner.hasNext()) {
            this.scanner.next(); //skip tID
            num_items = this.scanner.nextInt();
            
            System.out.print("      ");
            
            for(int i = 0; i < num_items; i++) {
                current_item = this.scanner.nextInt();
                System.out.print(current_item + " ");
                
                //increment the count if item has already been seen before.
                if(candidates.contains(current_item)) {
                    index = this.candidates.indexOf(current_item);
                    current_count = this.counts.get(index);
                    this.counts.set(index, current_count + 1);
                }
                
                //otherwise add to list of items and add a count of 1.
                else {
                    this.candidates.add(current_item);
                    this.counts.add(1);
                }
            }
            System.out.println();
        }
        
        //eliminate infrequent candidates
        int min_count = (int) Math.ceil(this.num_transactions * this.minsup * 0.01);
        System.out.println("min count " + min_count);
        
        for(int i = 0; i < counts.size(); i++) {
            if(counts.get(i) < min_count) {
                counts.remove(i);
                candidates.remove(i);
            }
        }
        
        //printing
        for(int i = 0; i < candidates.size(); i++){
            System.out.print(candidates.get(i).toString() + " ");
            System.out.println(counts.get(i).toString());
        }
    }
    
    
    /*
        Constructs the FP tree.
    */
    private void construct_FP_tree() {
        
    }
    
    
    
    class FPNode {
        
        private Integer item;
        private int count;
        private List<FPNode> children;
        
        public FPNode(Integer item, int count) {
            this.item = item;
            this.count = count;
            this.children = new LinkedList<>();
        }
        
        public Integer item() {return this.item;}
        public int count() {return this.count;}
        public List<FPNode> children() {return this.children;}
    }
}