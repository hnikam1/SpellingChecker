package symspell;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import symspell.Metric;


public class SymSpell<T> {
    public enum LookupType { FIRST_CLOSEST(0), ALL_CLOSEST(1), ALL_WITHIN_RANGE(2);

    private final int verbosity;
        LookupType(int v) {
            verbosity = v;
        }
    }

    private final int editDistanceMax;
    private final Map<String, Object> dictionary = new HashMap<>();
    private final List<String> wordList = new ArrayList<>();
    private final Metric<? super String> metric;
    private final int verbose;
    private int maxLength = 0;

    public SymSpell(int maxEditDistance, Metric<? super String> metric, LookupType type) {
        editDistanceMax = maxEditDistance;
        this.metric = metric;
        verbose = type.verbosity;
    }

    public boolean put(String key, T item) {
        boolean result = false;
        DictionaryItem value = null;
        Object valueo = dictionary.get(key);
        if (valueo != null) {
            if (valueo.getClass() != DictionaryItem.class) {
                int tmp = (Integer) valueo;
                value = new DictionaryItem<>(item);
                value.suggestions.add(tmp);
                dictionary.put(key, value);
            } else {
                value = (DictionaryItem) valueo;
            }

            if (value.count < Integer.MAX_VALUE)
                ++value.count;
        } else if (wordList.size() < Integer.MAX_VALUE) {
            value = new DictionaryItem<>(item);
            ++value.count;
            dictionary.put(key, value);
            maxLength = Math.max(key.length(), maxLength);
        }


        if (value.count == 1) {
            wordList.add(key);
            int keyint = wordList.size() - 1;
            result = true;

            for (String delete : edits(key, 0, new HashSet<>())) {
                Object value2 = dictionary.get(delete);
                if (value2 != null) {
                    if (value2.getClass() != DictionaryItem.class) {
                        int tmp = (Integer) value2;
                        DictionaryItem di = new DictionaryItem<>(item);
                        di.suggestions.add(tmp);
                        dictionary.put(delete, di);
                        if (!di.suggestions.contains(keyint)) {
                            addLowestDistance(di, key, keyint, delete, verbose);
                        }
                    } else {
                        DictionaryItem<T> v2 = (DictionaryItem<T>) value2;
                        if (!v2.suggestions.contains(keyint)) {
                            addLowestDistance(v2, key, keyint, delete, verbose);
                        }
                    }
                } else {
                    dictionary.put(delete, keyint);
                }
            }
        }
        return result;
    }

    private void addLowestDistance(DictionaryItem item, String suggestion, int suggestionint, String delete, int verbose) {
        if ((verbose < 2) && (item.suggestions.size() > 0) && (wordList.get(item.suggestions.get(0)).length() - delete.length() > suggestion.length() - delete.length()))
            item.suggestions.clear();
        //do not add suggestion of higher distance than existing, if verbose<2
        if ((verbose == 2) || (item.suggestions.size() == 0) || (wordList.get(item.suggestions.get(0)).length() - delete.length() >= suggestion.length() - delete.length()))
            item.suggestions.add(suggestionint);
    }

    private Set<String> edits(String word, int currentDistance, Set<String> deletes) {
        if (word.length() > 1) {
            ++currentDistance;
            for (int i = 0; i < word.length(); ++i) {
                String delete = new StringBuilder(word).deleteCharAt(i).toString();
                if (deletes.add(delete)) {
                    if (currentDistance <= editDistanceMax)
                        edits(delete, currentDistance, deletes);
                }
            }
        }
        return deletes;
    }



    public List<SuggestItem<T>> lookup(String input, int editDistanceMax) {
        if (input.length() - editDistanceMax > maxLength) {
            return Collections.emptyList();
        }
        List<String> candidates = new ArrayList<>();
        Set<String> hashset1 = new HashSet<>();
        List<SuggestItem<T>> suggestions = new ArrayList<>();
        Set<String> hashset2 = new HashSet<>();

        Object valueo;
        candidates.add(input);
        while (!candidates.isEmpty()) {
            String candidate = candidates.remove(0);

            if ((verbose < 2) && !suggestions.isEmpty() && (input.length() - candidate.length() > suggestions.get(0).getDistance())) {
                sort(suggestions);
            }

            valueo = dictionary.get(candidate);
            if (valueo != null) {
                DictionaryItem<T> value = new DictionaryItem<>(null);
                if (valueo.getClass() != DictionaryItem.class) {
                    value.suggestions.add((Integer) valueo);
                } else {
                    value = (DictionaryItem<T>) valueo;
                }

                if (value.count > 0 && hashset2.add(candidate)) {
                    SuggestItem<T> si = new SuggestItem<>(candidate, value.value, input.length() - candidate.length());
                    si.count = value.count;
                    suggestions.add(si);
                    if ((verbose < 2) && input.length() - candidate.length() == 0) {
                        sort(suggestions);
                    }
                }

                Object value2;
                TIntIterator itr = value.suggestions.iterator();
                while (itr.hasNext()) {
                    int suggestionInt = itr.next();
                    String suggestion = wordList.get(suggestionInt);
                    if (hashset2.add(suggestion)) {
                        int distance = 0;
                        if (!suggestion.equals(input)) {
                            if (suggestion.length() == candidate.length()) {
                                distance = input.length() - candidate.length();
                            } else if (input.length() == candidate.length()) {
                                distance = suggestion.length() - candidate.length();
                            } else {
                                int ii = 0;
                                int jj = 0;
                                while (ii < suggestion.length() && ii < input.length() && suggestion.charAt(ii) == input.charAt(ii)) {
                                    ++ii;
                                }
                                while (jj < suggestion.length() - ii && jj < input.length() - ii && suggestion.charAt(suggestion.length() - jj - 1) == input.charAt(input.length() - jj - 1)) {
                                    ++jj;
                                }
                                if ((ii > 0) || (jj > 0)) {
                                    distance = metric.distance(suggestion.substring(ii, suggestion.length() - jj), input.substring(ii, input.length() - jj));
                                } else {
                                    distance = metric.distance(suggestion, input);
                                }
                            }
                        }

                        if ((verbose < 2) && (suggestions.size() > 0) && (suggestions.get(0).getDistance() > distance))
                            suggestions.clear();
                        //do not process higher distances than those already found, if verbose<2
                        if ((verbose < 2) && (suggestions.size() > 0) && (distance > suggestions.get(0).getDistance()))
                            continue;

                        if (distance <= editDistanceMax) {
                            value2 = dictionary.get(suggestion);
                            if (value2 != null) {
                                DictionaryItem<T> dict = (DictionaryItem<T>) value2;
                                SuggestItem<T> si = new SuggestItem<>(suggestion, dict.value, distance);
                                si.count = ((DictionaryItem) value2).count;
                                suggestions.add(si);
                            }
                        }
                    }
                }  // end iterator
            } // end if


            if (input.length() - candidate.length() < editDistanceMax) {
                //save some time
                //do not create edits with edit distance smaller than suggestions already found
                if ((verbose < 2) && (suggestions.size() > 0) && (input.length() - candidate.length() >= suggestions.get(0).getDistance()))
                    continue;

                for (int i = 0; i < candidate.length(); ++i) {
                    String delete = new StringBuilder(candidate).deleteCharAt(i).toString();
                    if (hashset1.add(delete)) {
                        candidates.add(delete);
                    }
                }
            }


        } // end while

        if ((verbose == 0) && (suggestions.size() > 1))
            return suggestions.subList(0, 1);
        else
            return suggestions;

    }

    public void sort(List<SuggestItem<T>> suggestions) {
        if (verbose < 2)
            suggestions.sort((x, y) -> -Integer.compare(x.count, y.count));
        else
            suggestions.sort((x, y) -> Integer.compare(2 * x.getDistance(), y.getDistance()) - Integer.compare(x.count, y.count));
    }


    private static class DictionaryItem<T> {
        public TIntList suggestions = new TIntArrayList();
        public int count;
        public T value;

        public DictionaryItem(T value) {
            this.value = value;
        }
    }

    public static class SuggestItem<T> extends Match<T> {
        public String key = "";
        public int count;

        public SuggestItem(String key, T value, int dist) {
            super(value, dist);
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (o == this) return true;
            if (!(o instanceof SuggestItem<?>)) return false;
            SuggestItem<?> other = (SuggestItem<?>) o;
            return key.equals(other.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public String toString() {
            return String.format("SuggestItem{value=%s, dist=%d, key=%s, count=%d}", super.getValue(), super.getDistance(), key, count);
        }
    }
}


