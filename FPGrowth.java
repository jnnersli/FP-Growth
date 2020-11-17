/*
Author: Jenny Li
Student no: 230130751
CPSC 473
Assignment 2
*/


import java.util.*;
import java.io.*;

public class FPGrowth{
    /*
    Attributes:
        minsup: an int representing the minimum support threshold in percentage form.
        num_transactions: the total number of transactions in the transaction file.
        transaction_file: the name of the file it's reading from.
        candidates: a List of OneItemsets that represent the candidate table.
        root: an FPNode that represents the root of the FP tree.
    */

    private int minsup, num_transactions;
    private String transaction_file;
    private List<OneItemset> candidates;
    private FPNode root;
    
    
    /*  Main method  */
    public static void main(String[] args) throws FileNotFoundException {
        //  default values for transaction file and minimum support threshold
        String transaction_file = "data.txt";
        int minsup = 50;
        
        if(args.length > 0) {
            transaction_file = args[0];
        }
        if(args.length > 1) {
            minsup = Integer.parseInt(args[1]);
        }
        
        FPGrowth fpg = new FPGrowth(transaction_file, minsup);
        
        //  Initialize candidates and counts
        fpg.count_1_itemsets();  
        
        //testing
        System.out.println("Sorted: ");
        
        //  Sort the 1-itemsets in descending order of count.
        Collections.sort(fpg.candidates, Collections.reverseOrder());
        
        //printing
        for(int i = 0; i < fpg.candidates.size(); i++){
            System.out.println(fpg.candidates.get(i).toString());
        }
        
        fpg.construct_FP_tree();
    }
    
    
    /*  Constructor  */
    public FPGrowth(String tf, int ms) {
        transaction_file = tf;
        minsup = ms;
        candidates = new ArrayList<>();
        root = new FPNode(null);
    } 
    
    
    private void count_1_itemsets() throws FileNotFoundException{
        /*
        Finds the occurences of each 1-itemset.
        */
        
        File file = new File(transaction_file);
        Scanner scanner = new Scanner(file);
    
        int num_items, index, current_count;
        OneItemset current_item;
        
        num_transactions = Integer.parseInt(scanner.next());
        System.out.println("num transactions "+this.num_transactions);
        
        //  determine counts for each item
        while(scanner.hasNext()) {
            scanner.next(); //skip tID
            num_items = scanner.nextInt();
            
            for(int i = 0; i < num_items; i++) {
                current_item = new OneItemset(Integer.parseInt(scanner.next()));
                
                //  increment the count if item has already been seen before.
                if(candidates.contains(current_item)) {
                    candidates.get(candidates.indexOf(current_item)).increment();
                }
                
                //  otherwise add to list of items and add a count of 1.
                else {
                    candidates.add(current_item);
                }
            }
        }
        
        scanner.close();
        
        //  printing
        for(int i = 0; i < candidates.size(); i++){
            System.out.println(candidates.get(i).toString());
        }
    }
    
    
    private void construct_FP_tree() throws FileNotFoundException {
        /*
        Constructs the global FP tree.
        */
        
        File file = new File(transaction_file);
        Scanner scanner = new Scanner(file);
        
        scanner.next();  // Skip the total number of transactions
        
        int num_items;
        OneItemset current_item;
        List<OneItemset> transaction;
        
        //  Reading transactions one by one.
        while (scanner.hasNext()) {
            
            scanner.next();  // skip the transaction id
            num_items = scanner.nextInt();
            
            transaction = new ArrayList<>();
            
            //  Add the items and their counts according to the candidate table
            for (int i = 0; i < num_items; i++) {
                current_item = new OneItemset(scanner.nextInt());
                if (candidates.indexOf(current_item) == -1) {
                    candidates.add(current_item);
                    transaction.add(current_item);
                }
                else
                    transaction.add(candidates.get(candidates.indexOf(current_item)));
            }
            
            //  Sort them in reverse order of their counts
            Collections.sort(transaction, Collections.reverseOrder());
            
            //printing
            for (OneItemset i: transaction) System.out.print(i + "  ");
            System.out.println();
            
            
            FPNode current_node, previous_node, chaining_pointer;
            previous_node = root;
                
            //  Looping through the transaction to add the items to the FP tree
            //  one by one.    
            for (int i = 0; i < transaction.size(); i++) {
                
                //  Make a new FPNode and new OneItemset with count set to 1 (default).
                current_node = new FPNode(new OneItemset(transaction.get(i).value()));
                
                //  If the previous node is a leaf node, simply add onto it.
                if (previous_node.children().isEmpty()) {
                    System.out.println("adding new node "+ current_node.item() + " to " + previous_node.item());
                    previous_node.children.add(current_node);
                    current_node.setParent(previous_node);
                    System.out.println(current_node.item() + " belongs to " + previous_node.item());
                    
                    //  Attach a chaining pointer from the candidate list for the
                    //  projected FP tree.
                    
                    if (candidates.get(candidates.indexOf(current_node.item())).nextNode() == null) {
                        candidates.get(candidates.indexOf(current_node.item())).setNextNode(current_node);
                    }
                    
                    else {
                        chaining_pointer = candidates.get(candidates.indexOf(current_node.item())).nextNode();
                        
                        //  Find the end of the chain.
                        while (chaining_pointer.nextNode() != null) {
                            chaining_pointer = chaining_pointer.nextNode();
                        }
                    
                        //  Add the new node to the chain.
                        chaining_pointer.setNextNode(current_node);
                    }
                }
                
                //  If the previous node has a child with the same value, 
                //  simply just increment the value of the existing node.
                else if (previous_node.children().contains(current_node)) {
                    current_node = previous_node.children().get(previous_node.children().indexOf(current_node));
                    current_node.item().increment();
                    System.out.println("incremented node "+ current_node.item());
                }
                
                //  Otherwise add the new node.
                //  This is the same as the first condition but just to avoid null pointer issues
                //  this was easier.
                else {
                    System.out.println("adding new node "+ current_node.item() + " to " + previous_node.item());
                    previous_node.children.add(current_node);
                    current_node.setParent(previous_node);
                }
                
                previous_node = current_node;
            }
        }
        
        scanner.close();
    }
    
    
    class OneItemset implements Comparable<OneItemset> {
        /*
        Container class for each 1-itemset and its count.
        Makes sorting and comparing easier.
        
        Attributes:
            value: an Integer representing an item in a transaction.
            count: the support of that value.
            nextNode: a pointer to the next FP node of that value (for the candidate table only).
        */
        
        private Integer value;
        private Integer count;
        private FPNode nextNode;
        
        public OneItemset(Integer val) {
            value = val;
            count = 1;
            nextNode = null;
        }
        
        public Integer value() { return value; }
        public Integer count() { return count; }
        public FPNode nextNode() { return nextNode; }
        
        public void increment() { count = count + 1; }
        public void setNextNode(FPNode node) { nextNode = node; }
        
        @Override
        public int compareTo(OneItemset o) {
            /*
            Compares only the counts of each item so that they can be sorted by their counts.
            */
            return count.compareTo(o.count);
        }
        
        @Override
        public boolean equals(Object o) {
            /*
            Overwritten so that that when I call candidates.contains(), it matches against
            the item value, not the whole object.
            */
            if (o == this)
                return true;
            if (o == null || o.getClass() != this.getClass())
                return false;
            if (o.getClass() == this.getClass() && ((OneItemset) o).value == this.value)
                return true;
            return false;
        }
        
        @Override
        public String toString() {
            return this.value + ": " + this.count; 
        }
    }
    
    
    class FPNode {
        /*
        A node in the FP tree.
        
        Attributes:
            item: a OneItemset containing the item value and count.
            parent: the FPNode that is the immediate parent of this node.
            children: a List of FPNodes that are its child nodes.
            nextNode: the next node in the FP tree with the same item value.
        */
        
        private OneItemset item;
        private FPNode parent;
        private List<FPNode> children;
        private FPNode nextNode;
        
        public FPNode(OneItemset i) {
            item = i;
            children = new LinkedList<>();
            nextNode = null;
        }
        
        public OneItemset item() { return item; }
        public FPNode parent() { return parent; }
        public List<FPNode> children() { return children; }
        public FPNode nextNode() { return nextNode; }
        
        public void setParent(FPNode node) { parent = node; }
        public void setNextNode(FPNode node) { nextNode = node; }
        
        @Override
        public boolean equals(Object o) {
            /*
            Overwritten so that that when I compare two FPNodes, it only looks at the item.
            */
            if (o == this)
                return true;
            if (o == null || o.getClass() != this.getClass())
                return false;
            if (o.getClass() == this.getClass() && ((FPNode) o).item.equals(this.item))
                return true;
            return false;
        }
    }
}