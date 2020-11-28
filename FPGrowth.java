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
        root: a OneItemset that represents the root of the FP tree.
    */

    private int minsup, num_transactions;
    private String transaction_file;
    private List<OneItemset> candidates;
    private OneItemset root;
    
    
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
        fpg.count1Itemsets();  
        
        //testing
        System.out.println("Sorted: ");
        
        //  Sort candidates in descending order of count.
        Collections.sort(fpg.candidates, Collections.reverseOrder());
        
        //printing
        for(int i = 0; i < fpg.candidates.size(); i++){
            System.out.println(fpg.candidates.get(i).toString());
        }
        
        //  Build the FP tree.
        fpg.constructFPTree();
        
        //  Sort the candidates in ascending order of count
        Collections.sort(fpg.candidates);
        
        //printing
        for(int i = 0; i < fpg.candidates.size(); i++){
            System.out.println(fpg.candidates.get(i).toString());
        }
        
        //  Finding conditional pattern bases for each item.
        List<List<OneItemset>> conditional_pattern_base;
        List<OneItemset> conditional_fp_tree;
        for (OneItemset item: fpg.candidates) {
            System.out.println("conditional stuff for "+item);
            conditional_pattern_base = fpg.conditionalPatternBase(item);
            conditional_fp_tree = fpg.conditionalFPTree(conditional_pattern_base);
            
            System.out.print("cft: ");
            for (OneItemset i: conditional_fp_tree) {
                System.out.print(i + "  ");
            }
            System.out.println();
        }
    }
    
    
    /*  Constructor  */
    public FPGrowth(String tf, int ms) {
        transaction_file = tf;
        minsup = ms;
        candidates = new ArrayList<>();
        root = new OneItemset(null);
    } 
    
    
    private void count1Itemsets() throws FileNotFoundException{
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
        
        //  Eliminating infrequent candidates.
        int min_count = (int) Math.ceil((double) minsup * 0.01 * num_transactions);
        System.out.println("min count "+min_count);
        int i = 0;
        while (i < candidates.size()) {
            if (candidates.get(i).count.intValue() < min_count)
                candidates.remove(i);
            else i++;
        }
        
        
        //  printing
        //for(int i = 0; i < candidates.size(); i++){
            //System.out.println(candidates.get(i).toString());
        //}
    }
    
    
    private void constructFPTree() throws FileNotFoundException {
        /*
        Constructs the global FP tree.
        */
        
        File file = new File(transaction_file);
        Scanner scanner = new Scanner(file);
        
        scanner.next();  // Skip the total number of transactions
        
        int num_items;
        OneItemset current_item;
        List<OneItemset> transaction;
        
        while (scanner.hasNext()) {
            
            /*****  Reading the transactions items from the database  *****/
            
            scanner.next();  // skip the transaction id
            num_items = scanner.nextInt();
            
            transaction = new ArrayList<>();
            
            //  Add the items and their counts according to the candidate table
            for (int i = 0; i < num_items; i++) {
                current_item = new OneItemset(scanner.nextInt());
                
                //  Only add them to the transaction if they are in the candidate table.
                if (candidates.contains(current_item))
                    transaction.add(candidates.get(candidates.indexOf(current_item)));
            }
            
            //  Sort them in reverse order of their counts
            Collections.sort(transaction, Collections.reverseOrder());
            
            //printing
            //for (OneItemset i: transaction) System.out.print(i + "  ");
            //System.out.println();
            
            
            OneItemset current_node, previous_node, chaining_pointer;
            previous_node = root;
                
            //  Looping through the transaction to add the items to the FP tree
            //  one by one.    
            for (int i = 0; i < transaction.size(); i++) {
                
                //  Make a new FPNode and new OneItemset with count set to 1 (default).
                current_node = new OneItemset(transaction.get(i).value);
                
                //  If the previous node has a child with the same value, 
                //  simply just increment the value of the existing node.
                if (previous_node.children.contains(current_node)) {
                    current_node = previous_node.children.get(previous_node.children.indexOf(current_node));
                    current_node.increment();
                    System.out.println("incremented node "+ current_node);
                }
                
                //  Otherwise add the new node.
                else {
                    System.out.println("adding new node "+ current_node+ " to " + previous_node);
                    previous_node.children.add(current_node);
                    current_node.parent = previous_node;
                    //System.out.println(current_node.item() + " belongs to " + previous_node.item());
                    
                    //  Attach a chaining pointer from the candidate list for the
                    //  projected FP tree.
                    
                    if (candidates.get(candidates.indexOf(current_node)).next_node == null) {
                        candidates.get(candidates.indexOf(current_node)).next_node = current_node;
                    }
                    
                    else {
                        chaining_pointer = candidates.get(candidates.indexOf(current_node)).next_node;
                        
                        //  Find the end of the chain.
                        while (chaining_pointer.next_node != null) {
                            chaining_pointer = chaining_pointer.next_node;
                        }
                    
                        //  Add the new node to the chain.
                        chaining_pointer.next_node = current_node;
                    }
                }
                
                previous_node = current_node;
            }
        }
        
        scanner.close();
    }
    
    
    private List<List<OneItemset>> conditionalPatternBase(OneItemset item) {
        /*
        Returns the conditional pattern base for a given itemset.
        */
        
        OneItemset current_node, current_path = item.next_node;
        List<List<OneItemset>> cpb = new ArrayList<>();
        LinkedList<OneItemset> pattern;
        Integer support;
        
        //  Iterate across the chaining pointers to find each path in the tree.
        while (current_path != null) {
            //System.out.println("chaining " + current_path);
            current_node = current_path.parent;
            support = current_path.count;  //  Support of the path.
            pattern = new LinkedList<>();
            
            //  Traversing up the tree from each leaf to the root.
            //  The nodes are traversed in reverse order, so we append
            //  each one to the start of the list
            while (!current_node.equals(root)) {
                pattern.addFirst(new OneItemset(current_node.value, support));
                current_node = current_node.parent;
            }
            
            //  Add the path to the list and move to the next leaf.
            if (pattern.size() > 0) {
                cpb.add(pattern);
                
                //printing
                System.out.print("pattern: ");
                for (OneItemset i: pattern) System.out.print(i + "  ");
                System.out.println();
            }
            current_path = current_path.next_node;
        }
        
        return cpb;
    }
    
    
    private List<OneItemset> conditionalFPTree(List<List<OneItemset>> cpb) {
        /*
        Builds conditional FP tree from a given conditional pattern base.
        
        Does this by finding the nodes that occur in all paths in the
        conditional pattern base.
        */
        
        List<OneItemset> cft = new ArrayList<>();
        List<OneItemset> first_path;
        OneItemset item;
        Integer count = 0;
        boolean in_all = true;
        
        if (!cpb.isEmpty()) {
            //  We are just comparing the first path to the others.
            //  Anything not contained in the first path will not be
            //  in the conditional tree.
            first_path = cpb.get(0);
            
            //  Looping through the first path.
            for (int i = 0; i < first_path.size(); i++) {
                item = first_path.get(i);
                count = item.count;
                System.out.println("looking at " + item);
                
                //  Looping through the remaining paths in the conditional pattern base.
                for (int j = 1; j < cpb.size(); j++) {
                    System.out.println("j="+j+", n="+cpb.size());
                    
                    //  If item is not in another path, do not add to conditional tree.
                    if (!cpb.get(j).contains(item)) {
                        System.out.println("not here");
                        in_all = false;
                    }
                    //  Else add the support of the item in the other paths to the total support.
                    else {
                        count = count + cpb.get(j).get(cpb.get(j).indexOf(item)).count;
                    }
                }
                
                //  If the item occurs in all paths, add to the conditional tree.
                if (in_all) {
                    cft.add(new OneItemset(item.value, count));
                }
            }
        }
        
        return cft;
    }
    
    
    class OneItemset implements Comparable<OneItemset> {
        /*
        A node class for the FP tree containing one 1-itemset, its count,
        and some necessary pointers.
        
        Attributes:
            value: an Integer representing a 1-itemset in a transaction.
            count: an Integer representing the support of that itemset.
            parent: the node in the FP Tree that is the immediate parent of this node.
            children: the list child nodes of this node.
            next_node: a chaining pointer to the next FP node of the same value
                (for mining the FP tree).
        */
        
        private Integer value, count;
        private OneItemset parent;
        private List<OneItemset> children;
        private OneItemset next_node;
        
        
        public OneItemset(Integer val) {
            value = val;
            count = 1;
            parent = null;
            children = new LinkedList<>();
            next_node = null;
        }
        
        public OneItemset(Integer val, Integer cnt) {
            value = val;
            count = cnt;
            parent = null;
            children = new LinkedList<>();
            next_node = null;
        }
        
        public void increment() { count = count + 1; }
        
        
        @Override
        public int compareTo(OneItemset o) {
            /*
            Compares only the counts of each item so that they can be sorted by their counts.
            */
            if (count.compareTo(o.count) == 0)
                return value.compareTo(o.value);
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
            /*
            For easier testing/debugging.
            */
            return this.value + ":" + this.count; 
        }
    }
}