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

package org.qubership.atp.dataset.model.utils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Named;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.ParameterOverlap;
import org.qubership.atp.dataset.model.impl.file.FileData;

import com.google.common.base.MoreObjects;

/**
 * Represents a state of a parameter, which may be: Unreachable or Reachable[Overlap,Default,Uninitialized].
 */
public abstract class OverlapItem {
    protected final DataSet sourceDs;

    private OverlapItem(@Nonnull DataSet sourceDs) {
        this.sourceDs = sourceDs;
    }

    @Nonnull
    public abstract Optional<Parameter> getParameter();

    /**
     * See {@link Reachable} and {@link Unreachable}.
     */
    public abstract boolean isReachable();

    /**
     * Casts item to reachable.
     *
     * @throws IllegalStateException if {@link #isReachable()} returns false;
     */
    @Nonnull
    public abstract Reachable asReachable();

    /**
     * Returns initial data has been used for search.
     */
    @Nonnull
    public DataSet getSourceDs() {
        return sourceDs;
    }

    /**
     * Returns initial data attribute path ids has been used for search.
     */
    @Nullable
    public abstract Collection<UUID> getSearchByAttrPathIds();

    /**
     * Returns initial target attribute id has been used for search.
     */
    @Nonnull
    public abstract UUID getSearchByTargetAttrId();

    /**
     * Represents a parameter which not set because some parent reference not specified.
     */
    protected static class Unreachable extends OverlapItem {

        private final Collection<UUID> attributePathIds;
        private final UUID targetAttributeId;
        private final DataSet notFoundIn;
        private final UUID notFoundAttr;

        protected Unreachable(@Nonnull DataSet sourceDs, @Nullable Collection<UUID> attributePathIds,
                              @Nonnull UUID targetAttributeId,
                              @Nonnull DataSet notFoundIn,
                              @Nonnull UUID notFoundAttr) {
            super(sourceDs);
            this.attributePathIds = attributePathIds;
            this.targetAttributeId = targetAttributeId;
            this.notFoundIn = notFoundIn;
            this.notFoundAttr = notFoundAttr;
        }

        @Nonnull
        @Override
        public Optional<Parameter> getParameter() {
            return Optional.empty();
        }

        @Override
        public boolean isReachable() {
            return false;
        }

        @Nonnull
        @Override
        public Reachable asReachable() {
            throw new IllegalStateException("Attribute is not reachable: " + this);
        }

        @Nullable
        @Override
        public Collection<UUID> getSearchByAttrPathIds() {
            return attributePathIds;
        }

        @Nonnull
        @Override
        public UUID getSearchByTargetAttrId() {
            return targetAttributeId;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("sourceDs", sourceDs)
                    .add("attributePathIds", attributePathIds)
                    .add("targetAttributeId", targetAttributeId)
                    .add("notFoundIn", notFoundIn)
                    .add("notFoundAttr", notFoundAttr)
                    .toString();
        }
    }

    protected static class Overlap extends Reachable {

        private final ParameterOverlap overlap;

        protected Overlap(@Nonnull DataSet sourceDs,
                          @Nonnull List<Attribute> foundByAttrPath,
                          @Nonnull DataSet targetDs, @Nonnull ParameterOverlap overlap) {
            super(sourceDs, foundByAttrPath, overlap.getAttribute(), targetDs);
            this.overlap = overlap;
        }

        @Nonnull
        @Override
        public Optional<Parameter> getParameter() {
            return Optional.of(overlap);
        }

        @Nonnull
        @Override
        public ParameterOverlap asParameterOverlap() {
            return overlap;
        }

        @Override
        public boolean isParameterOverlap() {
            return true;
        }

        @Override
        public boolean isOverlap() {
            //it is an overlap in context of source ds only if it is defined there.
            return getSourceDs().equals(getTargetDs());
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("sourceDs", sourceDs)
                    .add("foundByAttrPath", foundByAttrPath)
                    .add("targetDs", targetDs)
                    .add("overlap", overlap)
                    .toString();
        }
    }

    protected static class DefaultUninitialized extends Default {

        protected DefaultUninitialized(@Nonnull DataSet sourceDs,
                                       @Nonnull List<Attribute> foundByAttrPath, @Nonnull Attribute foundTargetAttr,
                                       @Nonnull DataSet targetDs) {
            super(sourceDs, foundByAttrPath, foundTargetAttr, targetDs);
        }

        @Nonnull
        @Override
        public Optional<Parameter> getParameter() {
            return Optional.empty();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("sourceDs", sourceDs)
                    .add("foundByAttrPath", foundByAttrPath)
                    .add("foundTargetAttr", foundTargetAttr)
                    .add("targetDs", targetDs)
                    .toString();
        }
    }

    public static class DefaultInitialized extends Default {

        private final Parameter parameter;

        public DefaultInitialized(@Nonnull DataSet sourceDs,
                                  @Nonnull List<Attribute> foundByAttrPath,
                                  @Nonnull DataSet targetDs, @Nonnull Parameter parameter) {
            super(sourceDs, foundByAttrPath, parameter.getAttribute(), targetDs);
            this.parameter = parameter;
        }

        @Nonnull
        @Override
        public Optional<Parameter> getParameter() {
            return Optional.of(parameter);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("sourceDs", sourceDs)
                    .add("foundByAttrPath", foundByAttrPath)
                    .add("targetDs", targetDs)
                    .add("parameter", parameter)
                    .toString();
        }
    }

    /**
     * Represents a parameter which may be set - ds reference path is filled up.
     */
    public abstract static class Reachable extends OverlapItem {

        protected final DataSet targetDs;
        protected final List<Attribute> foundByAttrPath;
        protected final Attribute foundTargetAttr;

        private Reachable(@Nonnull DataSet sourceDs,
                          @Nonnull List<Attribute> foundByAttrPath, @Nonnull Attribute foundTargetAttr,
                          @Nonnull DataSet targetDs) {
            super(sourceDs);
            this.targetDs = targetDs;
            this.foundByAttrPath = foundByAttrPath;
            this.foundTargetAttr = foundTargetAttr;
        }


        @Nonnull
        public DataSet getTargetDs() {
            return targetDs;
        }

        /**
         * Overlap may not be null.
         *
         * @throws IllegalStateException if it is a target parameter and not an overlap.
         */
        @Nonnull
        public abstract ParameterOverlap asParameterOverlap();

        public abstract boolean isParameterOverlap();

        public boolean isLeaf() {
            return !isParameterOverlap();
        }

        @Nonnull
        public Attribute getAttribute() {
            return foundTargetAttr;
        }

        @Override
        public boolean isReachable() {
            return true;
        }

        @Nonnull
        @Override
        public Reachable asReachable() {
            return this;
        }

        @Nonnull
        public List<Attribute> getFoundByAttrPath() {
            return foundByAttrPath;
        }

        /**
         * Overlap UUID path.
         * */
        public List<UUID> getUuidPath() {
            List<UUID> result = new LinkedList<>();
            if (foundByAttrPath != null) {
                for (Attribute attribute : foundByAttrPath) {
                    result.add(attribute.getId());
                }
            }
            return result;
        }

        @Nullable
        @Override
        public Collection<UUID> getSearchByAttrPathIds() {
            return foundByAttrPath.stream()
                    .map(Identified::getId)
                    .collect(Collectors.toList());
        }

        @Nonnull
        @Override
        public UUID getSearchByTargetAttrId() {
            return foundTargetAttr.getId();
        }

        public Optional<String> getValue() {
            return getParameter().flatMap(param -> {
                switch (getAttribute().getType()) {
                    case TEXT:
                    case ENCRYPTED:
                    case CHANGE:
                        return Optional.ofNullable(param.getText());
                    case LIST:
                        return Optional.ofNullable(param.getListValue()).map(Named::getName);
                    case DSL:
                        return Optional.ofNullable(param.getDataSetReference()).map(Named::getName);
                    case FILE:
                        return Optional.ofNullable(param.getFileData()).map(FileData::getFileName);
                    default:
                        throw new UnsupportedOperationException(
                                String.format("Could not extract text value from parameter %s with type %s",
                                        this,
                                        getAttribute().getType().getName()));
                }
            });
        }

        public Optional<?> getValueRef() {
            return getParameter().flatMap(param -> {
                switch (getAttribute().getType()) {
                    case LIST:
                        return Optional.ofNullable(param.getListValue()).map(Identified::getId);
                    case DSL:
                        return Optional.ofNullable(param.getDataSetReference()).map(Identified::getId);
                    case FILE:
                        return Optional.ofNullable(param.getFileData()).map(FileData::getUrl);
                    default:
                        return Optional.empty();
                }
            });
        }

        public abstract boolean isOverlap();
    }

    private abstract static class Default extends Reachable {

        private Default(@Nonnull DataSet sourceDs,
                        @Nonnull List<Attribute> foundByAttrPath, @Nonnull Attribute foundTargetAttr,
                        @Nonnull DataSet targetDs) {
            super(sourceDs, foundByAttrPath, foundTargetAttr, targetDs);
        }

        @Nonnull
        @Override
        public ParameterOverlap asParameterOverlap() {
            throw new IllegalStateException("It is not an overlap: " + this);
        }

        @Override
        public boolean isParameterOverlap() {
            return false;
        }

        @Override
        public boolean isOverlap() {
            return false;
        }
    }
}
