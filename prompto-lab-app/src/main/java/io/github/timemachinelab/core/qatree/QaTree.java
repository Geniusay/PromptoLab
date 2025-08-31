package io.github.timemachinelab.core.qatree;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class QaTree {

    @Getter
    private QaTreeNode root;

    @Getter
    private QaTreeNode latestNode;

    private Map<String, QaTreeNode> nodeMap = new HashMap<>();

    public QaTree(QaTreeNode root) {
        this.root = root;
        this.latestNode = root;
        nodeMap.put(root.getId(), root);
    }

    public void addNode(String parentId, QaTreeNode node) {
        QaTreeNode parent = nodeMap.get(parentId);
        if (parent == null) {
            return;
        }
        parent.append(node);
        nodeMap.put(node.getId(), node);
        latestNode = node;
    }

    public QaTreeNode getNodeById(String id) {
        return nodeMap.get(id);
    }
    
    /**
     * 移除指定节点及其所有子节点
     * @param nodeId 要移除的节点ID
     * @return 是否移除成功
     */
    public boolean removeNode(String nodeId) {
        QaTreeNode nodeToRemove = nodeMap.get(nodeId);
        if (nodeToRemove == null) {
            return false;
        }
        
        // 递归移除所有子节点
        removeNodeAndChildren(nodeToRemove);
        
        // 从父节点的children中移除该节点
        removeFromParent(nodeToRemove);
        
        return true;
    }
    
    /**
     * 递归移除节点及其所有子节点
     * @param node 要移除的节点
     */
    private void removeNodeAndChildren(QaTreeNode node) {
        if (node == null) {
            return;
        }
        
        // 递归移除所有子节点
        if (node.getChildren() != null) {
            for (QaTreeNode child : node.getChildren().values()) {
                removeNodeAndChildren(child);
            }
        }
        
        // 从nodeMap中移除当前节点
        nodeMap.remove(node.getId());
    }
    
    /**
     * 从父节点的children中移除指定节点
     * @param nodeToRemove 要移除的节点
     */
    private void removeFromParent(QaTreeNode nodeToRemove) {
        // 遍历所有节点找到父节点
        for (QaTreeNode node : nodeMap.values()) {
            if (node.getChildren() != null && node.getChildren().containsKey(nodeToRemove.getId())) {
                node.removeChild(nodeToRemove.getId());
                break;
            }
        }
    }

}
