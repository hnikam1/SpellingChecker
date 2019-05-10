package symspell;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import net.openhft.koloboke.collect.map.IntObjMap;
import net.openhft.koloboke.collect.map.hash.HashIntObjMaps;
import org.eclipse.jdt.annotation.Nullable;
import symspell.Metric;


 public class BkTree<T> {

    private final Metric<? super T> metric;

    private final IntObjMap<BkTree<T>> subtrees = HashIntObjMaps.newMutableMap();

    private final T root;


    public BkTree(Metric<? super T> metric, T root) {
        this.metric = metric;
        this.root = root;
    }

    public boolean add(T value) {
        BkTree<T> tree;
        BkTree<T> subtree = this;
        int distance;
        do {
            tree = subtree;
            if (value.equals(tree.root)) {
                return false;
            }
            distance = metric.distance(tree.root, value);
            subtree = tree.subtrees.get(distance);

        } while (subtree != null);
        tree.subtrees.put(distance, new BkTree<>(metric, value));
        return true;
    }

    public Queue<Match<T>> getAllWithinRadius(T given, int maxDistance) {
        List<Match<T>> matches = new ArrayList<>();
        Deque<BkTree<T>> front = new ArrayDeque<>();
        front.add(this);

        while (!front.isEmpty()) {
            BkTree<T> top = front.removeLast();


            int distance = metric.distance(top.root, given);
            if (distance <= maxDistance) {
                matches.add(new Match<>(top.root, distance));
            }
            if (!top.subtrees.isEmpty()) {
                for (int i = Math.max(0, distance - maxDistance); i <= distance + maxDistance; ++i) {
                    @Nullable BkTree<T> subtree = top.subtrees.get(i);
                    if (subtree != null) {
                        front.addLast(subtree);
                    }
                }
            }
        }
        return new PriorityQueue<>(matches);
    }
}


