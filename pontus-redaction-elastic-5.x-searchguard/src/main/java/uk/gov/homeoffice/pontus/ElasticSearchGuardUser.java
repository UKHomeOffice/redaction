/*
 * Copyright 2015 floragunn UG (haftungsbeschränkt)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.gov.homeoffice.pontus;

import com.floragunn.searchguard.user.User;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ElasticSearchGuardUser extends User {

//    public static final ElasticSearchGuardUser ANONYMOUS = new ElasticSearchGuardUser("sg_anonymous", Lists.newArrayList("sg_anonymous_backendrole"));
//    public static final ElasticSearchGuardUser SG_INTERNAL = new ElasticSearchGuardUser("_sg_internal");
    private static final long serialVersionUID = -5500938501822658596L;
    private final String name;
    private final Set<String> roles = new HashSet<String>();

    public ElasticSearchGuardUser(final String name, final Collection<String> toAdd) {
        super(name,toAdd);

        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must not be null or empty");
        }

        this.name = name;

        if (toAdd != null) {
            this.addRoles(toAdd);
        }

    }

    public ElasticSearchGuardUser(final String name) {
        this(name, null);
    }

    public String getName() {
        return name;
    }

    public Set<String> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public void addRole(final String role) {
        roles.add(role);
    }

    public void addRoles(final Collection<String> toAdd) {
        roles.addAll(toAdd);
    }

    public boolean isUserInRole(final String role) {
        return roles.contains(role);
    }

    @Override
    public String toString() {
        return "ElasticSearchGuardUser [name=" + name + ", roles=" + roles + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ElasticSearchGuardUser other = (ElasticSearchGuardUser) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    public void copyRolesFrom(final ElasticSearchGuardUser elasticSearchGuardUser) {
        this.addRoles(elasticSearchGuardUser.getRoles());
    }
}