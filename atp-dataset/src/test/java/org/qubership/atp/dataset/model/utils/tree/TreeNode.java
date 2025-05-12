/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.dataset.model.utils.tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Iterators;

public class TreeNode<T> implements Iterable<TreeNode<T>> {

    T data;
    TreeNode<T> parent;
    List<TreeNode<T>> children;

    public TreeNode(T data) {
        this.data = data;
        this.children = new LinkedList<>();
    }

    public TreeNode<T> addChild(T child) {
        TreeNode<T> childNode = new TreeNode<T>(child);
        childNode.parent = this;
        this.children.add(childNode);
        return childNode;
    }

    @Override
    public Iterator<TreeNode<T>> iterator() {
        return children.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TreeNode<?> treeNode = (TreeNode<?>) o;
        return Objects.equals(data, treeNode.data) &&
                Objects.equals(parent, treeNode.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, parent);
    }

    public void printTree(Consumer<String> lines) {
        AtomicInteger offset = new AtomicInteger(0);
        new AllRefsIterator<TreeNode<T>>(Iterators.singletonIterator(this), false) {
            @Nullable
            @Override
            protected Iterator<? extends TreeNode<T>> getChildren(@Nonnull TreeNode<T> parent) {
                return parent.children.iterator();
            }

            @Override
            protected void forwardToNewParent(TreeNode<T> parent) {
                offset.incrementAndGet();
            }

            @Override
            protected void backToPreviousParent() {
                offset.decrementAndGet();
            }
        }.forEachRemaining(tTreeNode -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < offset.get(); i++) {
                sb.append("     ");
            }
            sb.append(Objects.toString(tTreeNode.data));
            lines.accept(sb.toString());
        });
    }

    public List<TreeNode<T>> getPathInclusive() {
        List<TreeNode<T>> path = getPath();
        path.add(this);
        return path;
    }

    public List<TreeNode<T>> getPath() {
        TreeNode<T> parent = this.parent;
        List<TreeNode<T>> result = new ArrayList<>();
        while (parent != null) {
            result.add(0, parent);
            parent = parent.parent;
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (parent != null) {
            result.append(parent.toString()).append(">");
        }
        result.append(Objects.toString(data.toString()));
        return result.toString();
    }
}
