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
        minsup: a double representing the minimum support threshold in decimal form.
        minimum_count: an int representing the minimum count of each frequent itemset.
        num_transactions: the total number of transactions in the transaction file.
        transaction_file: the name of the file it's reading from.
        candidates: a List of OneItemsets that represent the candidate table.
        root: a OneItemset that represents the root of the FP tree.
        frequent_patterns: A list of lists of OneItemsets that represents the frequent
            patterns found by the FP growth algorithm for a given dataset.
    */

    private double minsup;
    private int minimum_count, num_transactions;
    private String transaction_file;
    private List<OneItemset> candidates;
    private OneItemset root;
    private List<List<OneItemset>> frequent_patterns;
    
    
    /*  Main method  */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        
        //  default values for transaction file and minimum support threshold
        String transaction_file = "data.txt";
        double minsup = 0.5;
        
        //  file path
        if(args.length > 0) {
            transaction_file = args[0];
        }
        //  minimum support threshold
        if(args.length > 1) {
            minsup = Double.parseDouble(args[1]);
        }
        
        FPGrowth fpg = new FPGrowth(transaction_file, minsup);
        
        //  Initialize candidates and counts.
        fpg.count1Itemsets();  
        
        //  Sort candidates in descending order of count.
        Collections.sort(fpg.candidates, Collections.reverseOrder());
        
        //  Build the FP tree.
        fpg.constructFPTree();
        
        //  Sort the candidates in ascending order of count
        Collections.sort(fpg.candidates);
        
        //  Mining the FP tree.
        List<List<OneItemset>> conditional_pattern_base, projected_tree;
        for (OneItemset item: fpg.candidates) {
            //  For each single itemset, find the conditional pattern base.
            conditional_pattern_base = fpg.conditionalPatternBase(item);
            //for (List<OneItemset> pattern: conditional_pattern_base) {
                //System.out.println(pattern);
            //}
            
            //  Then build its projected FP tree.
            projected_tree = fpg.projectedTree(conditional_pattern_base);
            
            //  Then find the frequent patterns from the projected tree.
            fpg.frequent_patterns.addAll(fpg.frequentPatterns(projected_tree, item));
        }
            
        //  Output
        int total_patterns = fpg.candidates.size() + fpg.frequent_patterns.size();
        System.out.println("|FPs| = " + total_patterns);
        
        File output = new File("output.txt");
        PrintWriter writer = new PrintWriter(output);
        writer.println("|FPs| = " + total_patterns);
        
        //  Print the single itemsets.
        for (OneItemset itemset: fpg.candidates) {
            writer.println(itemset);
        } 
        
        //  Then print the frequent patterns.
        List<OneItemset> pattern;
        
        for (int i = 0; i < fpg.frequent_patterns.size(); i++) {
            pattern = fpg.frequent_patterns.get(i);
            
            for (int j = pattern.size()-1 ; j >= 0 ; j--) {
                    
                if (j == 0)
                    writer.println(pattern.get(j).value + " : " + pattern.get(j).count);
                else
                    writer.print(pattern.get(j).value + ", ");
                    
                writer.flush();
            }
        }
        
        writer.close();
    }
    
    
    /*  Constructor  */
    public FPGrowth(String tf, double ms) {
        transaction_file = tf;
        minsup = ms;
        candidates = new ArrayList<>();
        root = new OneItemset(null);
        frequent_patterns = new ArrayList<>();
    } 
    
    
    private void count1Itemsets() throws FileNotFoundException{
        /*
        Finds the occurences of each 1-itemset.
        */
        
        File file = new File(transaction_file);
        Scanner scanner = new Scanner(file);
    
        int num_items, index, current_count;
        OneItemset current_item;
        
        //  Initialize the rest of the object attributes based on the text file.
        num_transactions = Integer.parseInt(scanner.next());
        minimum_count = (int) Math.ceil(minsup * num_transactions);
        
        //  Determine counts for each item
        while(scanner.hasNext()) {
            scanner.next(); //skip tID
            num_items = scanner.nextInt();
            
            for(int i = 0; i < num_items; i++) {
                current_item = new OneItemset(Integer.parseInt(scanner.next()));
                
                //  Increment the count if item has already been seen before.
                if(candidates.contains(current_item)) {
                    candidates.get(candidates.indexOf(current_item)).increment();
                }
                
                //  Otherwise add to list of items and add a count of 1.
                else {
                    candidates.add(current_item);
                }
            }
        }
        
        scanner.close();
        
        //  Eliminating infrequent candidates.
        
        int i = 0;
        while (i < candidates.size()) {
            if (candidates.get(i).count.intValue() < minimum_count)
                candidates.remove(i);
            else i++;
        }
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
            
            
            
            /*****  Adding them to the global FP tree  *****/
            
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
                }
                
                //  Otherwise add the new node.
                else {
                    previous_node.children.add(current_node);
                    current_node.parent = previous_node;
                    
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
            }
            
            current_path = current_path.next_node;
        }
        
        return cpb;
    }
    
    
    private List<List<OneItemset>> projectedTree(List<List<OneItemset>> cpb) {
        /*
        Builds conditional FP tree from a given conditional pattern base.
        
        Does this by finding the nodes in each conditional pattern base
        that pass the minimum support threshold.
        */
        
        List<List<OneItemset>> projected_tree = new ArrayList<>();
        List<OneItemset> itemset, comparing_itemset, pattern;
        int min_count, count, j;
        boolean contains;
        
        //  Looping through the conditional pattern base list
        //  (the list of paths).
        for (int i = 0; i < cpb.size(); i++) {
            comparing_itemset = cpb.get(i);
            itemset = new ArrayList<>();
            j = 0;
            
            //  We are adding the items in each path to the itemset one by one.
            while (itemset.size() < comparing_itemset.size()) {
                pattern = new ArrayList<>();
                itemset.add(comparing_itemset.get(j));
                count = 0;
                min_count = num_transactions;
                
                //  Looping through the remainder of the list to search for our itemset.
                for (int k = i; k < cpb.size(); k++) {
                    contains = true;
                    
                    //  Deep comparing the itemset items.
                    for (OneItemset item: itemset) {
                        if (!cpb.get(k).contains(item)) {
                            contains = false;
                        }
                        else {
                            //  We take the minimum of the counts for each item
                            //  in an itemset.
                            if (cpb.get(k).get(cpb.get(k).indexOf(item)).count < min_count)
                                min_count = cpb.get(k).get(cpb.get(k).indexOf(item)).count;
                        }
                    }
                    
                    //  If the itemset appears in a given path, increment the count;
                    if (contains) {
                        count += min_count;
                    }
                }
                
                //  If an itemset appears at least as many times as the minimum support,
                //  we add it to the conditional fp tree.
                if (count >= minimum_count) {
                    for (OneItemset item: itemset) {
                        pattern.add(new OneItemset(item.value, count));
                    }
                    if (!projected_tree.contains(pattern)) {
                        projected_tree.add(pattern);
                    }
                }
                
                j++;
            }
        }
        
        return projected_tree;
    }
    
    
    private List<List<OneItemset>> frequentPatterns(List<List<OneItemset>> projected_tree, OneItemset item) {
        /*
        Generates frequent patterns from an item and its conditional FP tree.
        Just appends the item to the frequent pattern lists in its projected tree.
        */
        int count;
        for (int i = 0; i < projected_tree.size(); i++) {
            count = projected_tree.get(i).get(0).count;
            projected_tree.get(i).add(new OneItemset(item.value, count));
        }
        
        return projected_tree;
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
            return this.value + " : " + this.count; 
        }
    }
}