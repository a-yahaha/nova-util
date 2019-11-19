package com.xss.common.nova.model;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TreeTest {
    @Test
    public void test() {
        Tree<String> root = new Tree<>("root");
        Tree<String> a1 = new Tree<>("a1");
        Tree<String> a2 = new Tree<>("a2");
        Tree<String> a3 = new Tree<>("a3");
        root.addChild(a1);
        root.addChild(a2);
        root.addChild(a3);
        Tree<String> a21 = new Tree<>("a21");
        Tree<String> a22 = new Tree<>("a22");
        a2.addChild(a21);
        a2.addChild(a22);
        Tree<String> a31 = new Tree<>("a31");
        a3.addChild(a31);
        Tree<String> a311 = new Tree<>("a311");
        a31.addChild(a311);

        //校验根节点所有属性
        assertEquals(4, root.getDepth());
        assertEquals("root", root.getEle());
        assertEquals(1, root.getLevel());
        assertNull(root.getParent());
        assertEquals(3, root.getSubTrees().size());

        //校验根节点广度优先元素
        List<Tree<String>> trees = root.breadthFirstTraversal();
        assertEquals(8, trees.size());
        Tree nodeA1 = trees.get(1);
        assertEquals(4, nodeA1.getDepth());
        assertEquals("a1", nodeA1.getEle());
        assertEquals(2, nodeA1.getLevel());
        assertNotNull(nodeA1.getParent());
        assertEquals(0, nodeA1.getSubTrees().size());
        Tree nodeA2 = trees.get(2);
        assertEquals(4, nodeA2.getDepth());
        assertEquals("a2", nodeA2.getEle());
        assertEquals(2, nodeA2.getLevel());
        assertNotNull(nodeA2.getParent());
        assertEquals(2, nodeA2.getSubTrees().size());
        Tree nodeA3 = trees.get(3);
        assertEquals(4, nodeA3.getDepth());
        assertEquals("a3", nodeA3.getEle());
        assertEquals(2, nodeA3.getLevel());
        assertNotNull(nodeA3.getParent());
        assertEquals(1, nodeA3.getSubTrees().size());
        Tree nodeA21 = trees.get(4);
        assertEquals(4, nodeA21.getDepth());
        assertEquals("a21", nodeA21.getEle());
        assertEquals(3, nodeA21.getLevel());
        assertNotNull(nodeA21.getParent());
        assertEquals(0, nodeA21.getSubTrees().size());
    }
}
