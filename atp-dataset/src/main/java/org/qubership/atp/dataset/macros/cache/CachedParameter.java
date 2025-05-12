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

package org.qubership.atp.dataset.macros.cache;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.model.Parameter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CachedParameter {
    private Parameter parameter;
    private UUID topDataSetId;
    private List<UUID> parameterPath;

    @Override
    public int hashCode() {
        if (parameterPath.isEmpty()) {
            return parameter.hashCode();
        } else {
            int hashCode = 1;
            for (Object obj : parameterPath) {
                hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
            }
            hashCode = 31 * hashCode + parameter.hashCode();
            hashCode = 31 * hashCode + topDataSetId.hashCode();
            return hashCode;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CachedParameter) {
            CachedParameter other = (CachedParameter) obj;
            boolean sameParameter = other.getParameter().equals(parameter);
            boolean samePath = other.getParameterPath().equals(parameterPath);
            boolean sameDataSet = other.getTopDataSetId().equals(topDataSetId);
            return sameParameter && samePath && sameDataSet;
        } else if (obj instanceof Parameter) {
            return obj.equals(parameter);
        }
        return false;
    }
}
