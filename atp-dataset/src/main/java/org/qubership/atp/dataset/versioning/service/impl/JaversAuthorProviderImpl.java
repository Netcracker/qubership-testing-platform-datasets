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

package org.qubership.atp.dataset.versioning.service.impl;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.dataset.versioning.service.JaversAuthorProvider;

public class JaversAuthorProviderImpl implements JaversAuthorProvider {

    private static final String UNAUTHENTICATED = "unauthenticated";

    private final Provider<UserInfo> userInfoProvider;

    public JaversAuthorProviderImpl(Provider<UserInfo> userInfoProvider) {
        this.userInfoProvider = userInfoProvider;
    }

    @Override
    public String provide() {
        String name = UNAUTHENTICATED;
        UserInfo userInfo = userInfoProvider.get();
        if (!isNull(userInfo)) {
            String fullName = userInfo.getFullName();
            name = StringUtils.isNotEmpty(fullName) ? fullName : userInfo.getUsername();
            name = name == null ? "" : name;
        }
        return name;
    }

    @Override
    public String getUsername() {
        UserInfo userInfo = userInfoProvider.get();
        return nonNull(userInfo) ? userInfo.getUsername() : UNAUTHENTICATED;
    }
}
