package net.moulberry.utils.probability;

import org.intellij.lang.annotations.Flow;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class WeightedRandomSet<E> implements Set<E> {

    // region [Node Class Definitions]

    private static abstract class Node<E> {
        float weight;
        int leaves = 0;
        @Nullable InternalNode<E> parent = null;

        public Node(float weight) {
            this.weight = weight;
        }

        public void setIterated() {
            if (this.parent == null) return;

            this.parent.updateIterationWeight(-this.iterationWeight());
            if (this.parent.left == this) {
                this.parent.setIteratedLeft();
            } else {
                this.parent.setIteratedRight();
            }
        }

        public float iterationWeight() {
            return weight;
        }

        public void resetIteration() {}
    }

    private static class LeafNode<E> extends Node<E> {
        final @Nullable E element;

        public LeafNode(@Nullable E element, float weight) {
            super(weight);
            this.element = element;
        }
    }

    private static class InternalNode<E> extends Node<E> {
        @NotNull Node<E> left;
        @NotNull Node<E> right;

        boolean iteratedLeft = false;
        boolean iteratedRight = false;
        float iterationWeight = -1;

        public InternalNode(float weight, @NotNull Node<E> left, @NotNull Node<E> right) {
            super(weight);
            this.left = left;
            this.right = right;
        }

        public void updateIterationWeight(float deltaWeight) {
            if (iterationWeight < 0) iterationWeight = this.weight;
            iterationWeight = iterationWeight + deltaWeight;
            assert iterationWeight >= 0;

            if (this.parent != null) {
                this.parent.updateIterationWeight(deltaWeight);
            }
        }

        public void setIteratedLeft() {
            this.iteratedLeft = true;
            if (this.iteratedRight) {
                this.setIterated();
            }
        }

        public void setIteratedRight() {
            this.iteratedRight = true;
            if (this.iteratedLeft) {
                this.setIterated();
            }
        }

        public void resetIteration() {
            iterationWeight = -1;
            iteratedLeft = false;
            iteratedRight = false;

            left.resetIteration();
            right.resetIteration();
        }

        @Override
        public float iterationWeight() {
            return iterationWeight >= 0 ? iterationWeight : weight;
        }
    }

    // endregion

    private final boolean supportSimultaneousIteration;
    private final HashMap<E, LeafNode<E>> map = new HashMap<>();
    private int iterationId = 0;
    private @Nullable Node<E> root = null;

    public WeightedRandomSet() {
        this(false);
    }

    public WeightedRandomSet(boolean supportSimultaneousIteration) {
        this.supportSimultaneousIteration = supportSimultaneousIteration;
    }

    @Contract(pure = true)
    public float totalWeight() {
        if (root == null) return 0;
        return root.weight;
    }

    public WeightedRandomSet<E> deepClone() {
        WeightedRandomSet<E> v = new WeightedRandomSet<>();
        for (Map.Entry<E, LeafNode<E>> entry : map.entrySet()) {
            v.add(entry.getKey(), entry.getValue().weight);
        }
        return v;
    }

    @Contract(pure = true)
    public @Flow(sourceIsContainer = true) @Nullable E sample() {
        if (root == null) return null;

        LeafNode<E> node = get(ThreadLocalRandom.current().nextFloat(root.weight));

        if (node == null) {
            return null;
        } else {
            return node.element;
        }
    }

    @Contract(mutates = "this")
    public @Flow(sourceIsContainer = true) @Nullable E pop() {
        if (root == null) return null;

        LeafNode<E> node = get(ThreadLocalRandom.current().nextFloat(root.weight));

        if (node == null) {
            return null;
        } else {
            removeNode(node);
            map.remove(node.element);
            return node.element;
        }
    }

    @Contract(mutates = "this")
    public boolean add(@Flow(targetIsContainer = true) @NotNull E e, float weight) {
        Objects.requireNonNull(e);

        assert weight > 0;
        if (map.containsKey(e)) return false; // Already have element

        LeafNode<E> newNode = new LeafNode<>(e, weight);

        if (root == null) {
            root = newNode;
        } else {
            Node<E> node = root;

            while (node instanceof InternalNode<E> internal) {
                if (internal.left.leaves <= internal.right.leaves) {
                    node = internal.left;
                } else {
                    node = internal.right;
                }
            }

            insert(node, newNode);
        }

        this.iterationId = 0;
        map.put(e, newNode);
        return true;
    }

    // region [Set Method Implementations]

    @Override
    @Contract(pure = true)
    public @Range(from = 0, to = Integer.MAX_VALUE) int size() {
        if (root == null) return 0;
        if (root instanceof LeafNode<E>) return 1;
        return root.leaves;
    }

    @Override
    public boolean isEmpty() {
        return root == null;
    }

    @Override
    @Contract(pure = true)
    public boolean contains(@NotNull Object element) {
        return map.containsKey(element);
    }

    @Override
    public Object[] toArray() {
        Object[] objects = new Object[this.size()];
        int index = 0;
        for (E e : this) {
            objects[index++] = e;
        }
        return objects;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < this.size()) {
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), this.size());
        }

        int index = 0;
        for (E e : this) {
            a[index++] = (T) e;
        }
        while (index < a.length) {
            a[index++] = null;
        }
        return a;
    }

    @Override
    public boolean add(E e) {
        return add(e, 1);
    }

    @Override
    public boolean remove(Object o) {
        LeafNode<E> node = map.get(o);
        if (node == null) return false;

        removeNode(node);

        map.remove(o);
        return true;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return map.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c) {
            modified |= this.add(e);
        }
        return modified;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        if (!(c instanceof Set<?>)) c = new HashSet<>(c); // Ensure c has O(1) contains

        for (Map.Entry<E, LeafNode<E>> entry : this.map.entrySet()) {
            if (!c.contains(entry.getKey())) {
                removeNode(entry.getValue());
            }
        }

        return this.map.keySet().retainAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        if (!(c instanceof Set<?>)) c = new HashSet<>(c); // Ensure c has O(1) contains

        for (Map.Entry<E, LeafNode<E>> entry : this.map.entrySet()) {
            if (c.contains(entry.getKey())) {
                removeNode(entry.getValue());
            }
        }

        return this.map.keySet().removeAll(c);
    }

    @Override
    public void clear() {
        this.map.clear();
        this.root = null;
        this.iterationId = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (!(o instanceof Set)) return false;
        Collection<?> c = (Collection<?>) o;

        if (c.size() != size()) return false;
        try {
            return containsAll(c);
        } catch (ClassCastException | NullPointerException unused) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return map.keySet().hashCode();
    }

    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }

    // endregion

    // region [Iterator]

    @NotNull
    @Override
    public Iterator<E> iterator() {
        Node<E> root = this.root;

        if (root instanceof LeafNode<E> leafRoot) {
            E e = leafRoot.element;
            return new Iterator<>() {
                private boolean hasNext = true;
                public boolean hasNext() {
                    return hasNext;
                }
                public E next() {
                    if (!hasNext) throw new NoSuchElementException();
                    hasNext = false;
                    return e;

                }
                @Override
                public void forEachRemaining(Consumer<? super E> action) {
                    Objects.requireNonNull(action);
                    if (hasNext) {
                        hasNext = false;
                        action.accept(e);
                    }
                }
            };
        } else if (root instanceof InternalNode<E>) {
            if (supportSimultaneousIteration) {
                // noinspection unchecked
                return this.deepClone().unsafeIterator();
            } else {
                return unsafeIterator();
            }
        }

        return Collections.emptyIterator();
    }


    private Iterator<E> unsafeIterator() {
        assert root != null;

        final InternalNode<E> internalRoot = (InternalNode<E>) this.root;
        final int oldIterationId = this.iterationId;

        return new Iterator<>() {
            private int thisIterationId = 0;

            @Override
            public boolean hasNext() {
                long currIterationId = WeightedRandomSet.this.iterationId;
                if (thisIterationId == 0 && currIterationId == oldIterationId) {
                    thisIterationId = ++WeightedRandomSet.this.iterationId;
                    root.resetIteration();
                } else if (currIterationId != thisIterationId) {
                    throw new IllegalStateException("This Iterator is no longer valid");
                }
                return !internalRoot.iteratedLeft || !internalRoot.iteratedRight;
            }

            @Override
            public E next() {
                if (!hasNext()) throw new NoSuchElementException();

                float value = ThreadLocalRandom.current().nextFloat(internalRoot.iterationWeight());

                Node<E> node = root;

                while (node instanceof InternalNode<E> internal) {
                    float leftWeight = internal.left.iterationWeight();
                    if (!internal.iteratedLeft && (internal.iteratedRight || value < leftWeight)) {
                        node = internal.left;
                    } else if (!internal.iteratedRight) {
                        value -= leftWeight;
                        node = internal.right;
                    } else {
                        throw new NoSuchElementException();
                    }
                }

                if (node instanceof LeafNode<E> leaf) {
                    leaf.setIterated();
                    return leaf.element;
                }

                throw new NoSuchElementException();
            }
        };
    }

    // endregion

    // region [Internal Implementation]

    private void removeNode(LeafNode<E> node) {
        this.iterationId = 0;
        if (node.parent == null || node == root) {
            root = null;
        } else {
            update(node.parent, -node.weight, -1);

            Node<E> sibling = node.parent.left == node ? node.parent.right : node.parent.left;

            if (node.parent.parent == null || node.parent == root) {
                root = sibling;
                sibling.parent = null;
            } else {
                InternalNode<E> parentParent = node.parent.parent;
                if (parentParent.left == node.parent) {
                    parentParent.left = sibling;
                } else {
                    parentParent.right = sibling;
                }
                sibling.parent = parentParent;
            }
        }
    }

    @Contract(pure = true)
    private @Flow(sourceIsContainer = true) @Nullable LeafNode<E> get(float value) {
        if (root == null) return null;

        Node<E> node = root;

        while (node instanceof InternalNode<E> internal) {
            if (value < internal.left.weight) {
                node = internal.left;
            } else {
                value -= internal.left.weight;
                node = internal.right;
            }
        }

        if (node instanceof LeafNode<E> leaf) {
            return leaf;
        }

        return null;
    }

    private void insert(@NotNull Node<E> leaf, @NotNull Node<E> newNode) {
        InternalNode<E> parent = new InternalNode<>(leaf.weight, leaf, newNode);
        parent.leaves = 1;
        parent.parent = leaf.parent;

        if (leaf.parent == null) {
            root = parent;
        } else if (leaf.parent.left == leaf) {
            leaf.parent.left = parent;
        } else {
            leaf.parent.right = parent;
        }

        leaf.parent = parent;
        newNode.parent = parent;

        update(parent, newNode.weight, 1);
    }

    private void update(Node<E> node, float newWeight, int newLeaves) {
        while (node != null) {
            node.weight += newWeight;
            node.leaves += newLeaves;
            node = node.parent;
        }
    }

    // endregion

}
