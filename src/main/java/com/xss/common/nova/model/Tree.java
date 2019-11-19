package com.xss.common.nova.model;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
public class Tree<T> {
    private int depth;
    private int level = 1;
    private T ele;
    private Tree parent;
    private List<Tree<T>> subTrees = new ArrayList<>();

    public Tree(T ele) {
        this.ele = ele;
    }

    public void addChild(Tree<T> tree) {
        subTrees.add(tree);
        tree.setLevel(level + 1);
        tree.setParent(this);
        Tree root = this.getRoot();
        this.depth = root.calDepth() + 1;

        root.breadthFirstTraversal().forEach(node -> ((Tree) node).setDepth(this.depth));
    }

    public List<Tree<T>> breadthFirstTraversal() {
        Map<Integer, List<Tree<T>>> nodes = new TreeMap();
        this.groupTree(nodes);

        List<Tree<T>> result = new ArrayList<>();
        nodes.values().forEach(item -> result.addAll(item));
        return result;
    }

    private void groupTree(Map<Integer, List<Tree<T>>> nodes) {
        if (nodes.containsKey(this.level)) {
            nodes.get(this.level).add(this);
        } else {
            nodes.put(this.level, Lists.newArrayList(this));
        }

        if (subTrees.size() != 0) {
            this.subTrees.forEach(node -> node.groupTree(nodes));
        }
    }

    private int calDepth() {
        if (this.getSubTrees().size() == 0) {
            return 0;
        } else if (this.getSubTrees().size() == 1) {
            return 1 + this.subTrees.get(0).calDepth();
        } else {
            return 1 + this.subTrees.stream().map(node -> node.calDepth())
                    .max((depth1, depth2) -> depth1 - depth2).orElse(0);
        }
    }

    private Tree getRoot() {
        if (level == 1) {
            return this;
        } else {
            return this.getParent().getRoot();
        }
    }

    @Override
    public String toString() {
        return "depth:" + this.depth + ", level:" + this.level + ", ele:" + ele;
    }
}
