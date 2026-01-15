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

package org.qubership.atp.dataset.service.jpa.model;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.dataset.model.utils.Utils;

import lombok.Getter;

/**
 * Path step for reference macros. Contains ID or name, or both. The reason is,
 * that reference macro can have reference to some parameter with list of UUIDs OR list of String?
 * or some mixed collection.
 * To check reference match we need to check ID, if it's null - check name.
 * */
public class PathStep {
    @Getter
    private String name;
    @Getter
    private UUID id;

    /**
     * Sets field value depending on variable type.
     * */
    public PathStep(String identifier) {
        if (Utils.isUuid(identifier)) {
            id = UUID.fromString(identifier);
        } else {
            this.name = identifier;
        }
    }

    public PathStep(UUID id) {
        this.id = id;
    }

    /**
     * True if any field matched.
     * */
    public boolean matches(String name, UUID id) {
        if (id != null && this.id != null) {
            return id.equals(this.id);
        } else {
            if (StringUtils.isNotEmpty(name)) {
                return name.equals(this.name);
            }
        }
        return false;
    }

    /**
     * True if any field matched.
     * */
    public boolean matches(PathStep other) {
        if (this.id != null) {
            return other.getId().equals(this.id);
        } else {
            return other.getName().equals(this.name);
        }
    }


    @Override
    public String toString() {
        return "Search Step: " + (name != null ? name : "") + (id != null ? id : "");
    }
}
