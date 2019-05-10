package symspell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// Usage: single word in GUI :  Display spelling suggestions
//        Enter without input:  Terminate the program

public class SymSpellHash
{
    private static int editDistanceMax=2;
    private static int verbose = 0;
    //0: top suggestion
    //1: all suggestions of smallest edit distance 
    //2: all suggestions <= editDistanceMax (slower, no early termination)

    private static class dictionaryItem
    {
        public List<Integer> suggestions = new ArrayList<Integer>();
        public int count = 0;
    }

    private static class suggestItem
    {
        public String term = "";
        public int distance = 0;
        public int count = 0;

        @Override
        public boolean equals(Object obj)
        {
            return term.equals(((suggestItem)obj).term);
        }
        
        @Override
        public int hashCode()
        {
            return term.hashCode();
        }       
    }

    //List of all dictionary words, based upon the IRD operation.
    private static HashMap<String, Object> dictionary = new HashMap<String, Object>(); //initialisierung

    //List of unique words.
    private static List<String> wordlist = new ArrayList<String>();

    
    private static Iterable<String> parseWords(String text)
    {
    	List<String> allMatches = new ArrayList<String>();
    	Matcher m = Pattern.compile("[\\w-[\\d_]]+").matcher(text.toLowerCase());
    	while (m.find()) {
    		allMatches.add(m.group());
    	}
    	return allMatches;
    }

    public static int maxlength = 0;//maximum dictionary term length

    //for every word there all deletes with an edit distance of 1..editDistanceMax created and added to the dictionary
    //every delete entry has a suggestions list, which points to the original term(s) it was created from
    //The dictionary may be dynamically updated (word frequency and new words) at any time by calling createDictionaryEntry
    public static boolean CreateDictionaryEntry(String key, String language)
    {
    	boolean result = false;
        dictionaryItem value=null;
        Object valueo;
        valueo = dictionary.get(language+key);
        if (valueo!=null)
        {
            //int or dictionaryItem? delete existed before word!
            if (valueo instanceof Integer) { 
            	int tmp = (int)valueo; 
            	value = new dictionaryItem(); 
            	value.suggestions.add(tmp); 
            	dictionary.put(language + key,value); 
        	}

            //already exists:
            //1. word appears several times
            //2. word1==deletes(word2) 
            else
            {
                value = (dictionaryItem)valueo;
            }

            //prevent overflow
            //Happened if term has occured lot many time example A, the types
            if (value.count < Integer.MAX_VALUE) value.count++;
        }
        else if (wordlist.size() < Integer.MAX_VALUE)
        {
            value = new dictionaryItem();
            value.count++;
            dictionary.put(language + key, value);

            if (key.length() > maxlength) maxlength = key.length();
        }
       
        if(value.count == 1)
        {
            //word2index
            wordlist.add(key);
            int keyint = (int)(wordlist.size() - 1);

            result = true;

            //create deletes
            for (String delete : Edits(key, 0, new HashSet<String>()))
            {
                Object value2;
                value2 = dictionary.get(language+delete);
                if (value2!=null)
                {
                    //already exists:
                    //1. word1==deletes(word2) 
                    //2. deletes(word1)==deletes(word2) 
                    //int or dictionaryItem? single delete existed before!
                    if (value2 instanceof Integer) 
                    {
                        //transformes int to dictionaryItem
                        int tmp = (int)value2; 
                        dictionaryItem di = new dictionaryItem(); 
                        di.suggestions.add(tmp); 
                        dictionary.put(language + delete,di);
                        if (!di.suggestions.contains(keyint)) AddLowestDistance(di, key, keyint, delete);
                    }
                    else if (!((dictionaryItem)value2).suggestions.contains(keyint)) AddLowestDistance((dictionaryItem) value2, key, keyint, delete);
                }
                else
                {
                    dictionary.put(language + delete, keyint);         
                }

            }
        }
        
        
        return result;
    }

    //create a frequency dictionary from a corpus
    public static void CreateDictionary(String corpus, String language)
    {
    	File f = new File(corpus);
        if(!(f.exists() && !f.isDirectory()))
        {
            System.out.println("File not found: " + corpus);
            return;
        }

        System.out.println("Creating dictionary ...");
        long startTime = System.currentTimeMillis();
        long wordCount = 0;
        
        BufferedReader br = null;
        try {
			br = new BufferedReader(new FileReader(corpus));
	        String line;
	        while ((line = br.readLine()) != null) 
	        {
	            for (String key : parseWords(line))
	            {
	               if (CreateDictionaryEntry(key, language)) wordCount++;
	            }
	        }
        }
        catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        long endTime = System.currentTimeMillis();
        System.out.println("Dictionary Created in time "+(endTime-startTime)+"ms ");
        //System.out.println("\rDictionary: " + wordCount + " words, " + dictionary.size() + " entries, edit distance=" + editDistanceMax + " in time " + (endTime-startTime)+"ms ");
    }

    //save some time and space
    private static void AddLowestDistance(dictionaryItem item, String suggestion, int suggestionint, String delete)
    {
        //remove all existing suggestions of higher distance, if verbose<2
    	//TODO check
        if ((verbose < 2) && (item.suggestions.size() > 0) && (wordlist.get(item.suggestions.get(0)).length()-delete.length() > suggestion.length() - delete.length())) item.suggestions.clear();
        //do not add suggestion of higher distance than existing, if verbose<2
        if ((verbose == 2) || (item.suggestions.size() == 0) || (wordlist.get(item.suggestions.get(0)).length()-delete.length() >= suggestion.length() - delete.length())) item.suggestions.add(suggestionint); 
    }

    //inexpensive and language independent: only deletes, no transposes + replaces + inserts
    private static HashSet<String> Edits(String word, int editDistance, HashSet<String> deletes)
    {
        editDistance++;
        if (word.length() > 1)
        {
            for (int i = 0; i < word.length(); i++)
            {
            	//delete ith character
                String delete =  word.substring(0,i)+word.substring(i+1);
                if (deletes.add(delete))
                {
                    //recursion, if maximum edit distance not yet reached
                    if (editDistance < editDistanceMax) Edits(delete, editDistance, deletes);
                }
            }
        }
        return deletes;
    }

    private static List<suggestItem> Lookup(String input, String language, int editDistanceMax)
    {
        //save some time
        if (input.length() - editDistanceMax > maxlength) 
        	return new ArrayList<suggestItem>();

        List<String> candidates = new ArrayList<String>();
        HashSet<String> hashset1 = new HashSet<String>();
 
        List<suggestItem> suggestions = new ArrayList<suggestItem>();
        HashSet<String> hashset2 = new HashSet<String>();

        Object valueo;

        //add original term
        candidates.add(input);

        while (candidates.size()>0)
        {
            String candidate = candidates.get(0);
            candidates.remove(0);
             
            //if canddate distance is already higher than suggestion distance, than there are no better suggestions to be expected

            nosort:{
            
            	if ((verbose < 2) && (suggestions.size() > 0) && (input.length()-candidate.length() > suggestions.get(0).distance)) 
            		break nosort;

	            //read candidate entry from dictionary
            	valueo = dictionary.get(language + candidate);
	            if (valueo != null)
	            {
	                dictionaryItem value= new dictionaryItem();
	                if (valueo instanceof Integer) 
	                	value.suggestions.add((int)valueo);
	                else value = (dictionaryItem)valueo;
	
	                //if count>0 then candidate entry is correct dictionary term, not only delete item
	                if ((value.count > 0) && hashset2.add(candidate))
	                {
	                    //add correct dictionary term term to suggestion list
	                    suggestItem si = new suggestItem();
	                    si.term = candidate;
	                    si.count = value.count;
	                    si.distance = input.length() - candidate.length();
	                    suggestions.add(si);
	                    //early termination
	                    if ((verbose < 2) && (input.length() - candidate.length() == 0)) 
	                    	break nosort;
	                }
	
	                //iterate through suggestions (to other correct dictionary items) of delete item and add them to suggestion list
	                Object value2;
	                for (int suggestionint : value.suggestions)
	                {
	             
	                	//TODO code here
	                	String suggestion = wordlist.get(suggestionint);
	                    if (hashset2.add(suggestion))
	                    {
	                        int distance = 0;
	                        if (suggestion != input)
	                        {
	                            if (suggestion.length() == candidate.length()) distance = input.length() - candidate.length();
	                            else if (input.length() == candidate.length()) distance = suggestion.length() - candidate.length();
	                            else
	                            {
	                                int ii = 0;
	                                int jj = 0;
	                                while ((ii < suggestion.length()) && (ii < input.length()) && (suggestion.charAt(ii) == input.charAt(ii))) ii++;
	                                while ((jj < suggestion.length() - ii) && (jj < input.length() - ii) && (suggestion.charAt(suggestion.length() - jj - 1) == input.charAt(input.length() - jj - 1))) jj++;
	                                if ((ii > 0) || (jj > 0)) { 
	                                	distance = DamerauLevenshteinDistance(suggestion.substring(ii, suggestion.length() - jj), input.substring(ii, input.length() - jj)); 
	                                } 
	                                else distance = DamerauLevenshteinDistance(suggestion, input);
	                            }
	                        }

	                        //remove all existing suggestions of higher distance, if verbose<2
	                        if ((verbose < 2) && (suggestions.size() > 0) && (suggestions.get(0).distance > distance)) suggestions.clear();
	                        //do not process higher distances than those already found, if verbose<2
	                        if ((verbose < 2) && (suggestions.size() > 0) && (distance > suggestions.get(0).distance)) continue;
	
	                        if (distance <= editDistanceMax)
	                        {
	                        	value2 = dictionary.get(language + suggestion);
	                        	if (value2!=null)
	                            {
	                                suggestItem si = new suggestItem();
	                                si.term = suggestion;
	                                si.count = ((dictionaryItem)value2).count;
	                                si.distance = distance;
	                                suggestions.add(si);
	                            }
	                        }
	                    }
	                }
	            }      
	           
	            if (input.length() - candidate.length() < editDistanceMax)
	            {
	                //save some time
	                //do not create edits with edit distance smaller than suggestions already found
	                if ((verbose < 2) && (suggestions.size() > 0) && (input.length() - candidate.length() >= suggestions.get(0).distance)) continue;
	
	                for (int i = 0; i < candidate.length(); i++)
	                {
	                    String delete = candidate.substring(0, i)+candidate.substring(i+1);
	                    if (hashset1.add(delete)) candidates.add(delete);
	                }
	            }
            }
        }
        
        //sort by ascending edit distance, then by descending word frequency
        if (verbose < 2) 
        	//suggestions.Sort((x, y) => -x.count.CompareTo(y.count));
        	Collections.sort(suggestions, new Comparator<suggestItem>()
                    {
                public int compare(suggestItem f1, suggestItem f2)
                {
                    return -(f1.count-f2.count);
                }        
            });
        else 
        	
        	Collections.sort(suggestions, new Comparator<suggestItem>()
                    {
                public int compare(suggestItem x, suggestItem y)
                {
                    return ((2*x.distance-y.distance)>0?1:0) - ((x.count - y.count)>0?1:0);
                }        
            });
        if ((verbose == 0)&&(suggestions.size()>1)) 
        	return suggestions.subList(0, 1); 
        else return suggestions;
    }

    public static String Correct(String input, String language)
    {
        String correctWord = input;
        List<suggestItem> suggestions = null;
        long startTime = System.currentTimeMillis();
        
        
        //check in dictionary for existence and frequency; sort by ascending edit distance, then by descending word frequency
        suggestions = Lookup(input, language, editDistanceMax);

        for (suggestItem suggestion: suggestions)
        {
            System.out.println( suggestion.term + " " + suggestion.distance + " " + suggestion.count);
            correctWord = suggestion.term;
        }
        System.out.println(suggestions.size() + " suggestions");
        System.out.println("Check Completed!!");
        long endTime = System.currentTimeMillis();
        System.out.println("in Time:  " + (endTime-startTime)+"ms ");
        
        
        return correctWord;
    }

    
    public static int DamerauLevenshteinDistance(String a, String b) {
    	  final int inf = a.length() + b.length() + 1;
		  int[][] H = new int[a.length() + 2][b.length() + 2];
		  for (int i = 0; i <= a.length(); i++) {
		   H[i + 1][1] = i;
		   H[i + 1][0] = inf;
		  }
		  for (int j = 0; j <= b.length(); j++) {
		   H[1][j + 1] = j;
		   H[0][j + 1] = inf;
		  }
		  HashMap<Character, Integer> DA = new HashMap<Character, Integer>();
		  for (int d = 0; d < a.length(); d++) 
		   if (!DA.containsKey(a.charAt(d)))
		    DA.put(a.charAt(d), 0);
		  
		   
		  for (int d = 0; d < b.length(); d++) 
		   if (!DA.containsKey(b.charAt(d)))
		    DA.put(b.charAt(d), 0);
		  
		  for (int i = 1; i <= a.length(); i++) {
		   int DB = 0;
		   for (int j = 1; j <= b.length(); j++) {
		    final int i1 = DA.get(b.charAt(j - 1));
		    final int j1 = DB;
		    int d = 1;
		    if (a.charAt(i - 1) == b.charAt(j - 1)) {
		     d = 0;
		     DB = j;
		    }
		    H[i + 1][j + 1] = min(
		      H[i][j] + d, 
		      H[i + 1][j] + 1,
		      H[i][j + 1] + 1, 
		      H[i1][j1] + ((i - i1 - 1)) 
		      + 1 + ((j - j1 - 1)));
		   }
		   DA.put(a.charAt(i - 1), i);
		  }
		  return H[a.length() + 1][b.length() + 1];
		 }
	 public static int min(int a, int b, int c, int d) {
		 return Math.min(a, Math.min(b, Math.min(c, d)));
	 }
}
