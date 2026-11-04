/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.artifact.buildinfo;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompareMojoRepositoryTest {
    private static final RemoteRepository CENTRAL =
            new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();

    @Test
    void selectsUnmirroredRepositoryById() throws Exception {
        assertSame(CENTRAL, assertDoesNotThrow(() -> select("central", Collections.singletonList(CENTRAL))));
    }

    @Test
    void selectsEffectiveMirrorByOriginalRepositoryId() throws Exception {
        RemoteRepository mirror = mirror(CENTRAL);

        // Return the effective mirror, retaining its URL, policies, authentication and proxy.
        assertSame(mirror, assertDoesNotThrow(() -> select("central", Collections.singletonList(mirror))));
    }

    @Test
    void selectsMirrorRepresentingSeveralOriginalRepositories() throws Exception {
        RemoteRepository releases = new RemoteRepository.Builder(
                        "company-releases", "default", "https://releases.example.invalid/")
                .build();
        RemoteRepository mirror = mirror(releases, CENTRAL);

        assertSame(mirror, assertDoesNotThrow(() -> select("central", Collections.singletonList(mirror))));
    }

    @Test
    void selectsMirrorByItsOwnConfiguredId() throws Exception {
        RemoteRepository mirror = mirror(CENTRAL);

        assertSame(mirror, assertDoesNotThrow(() -> select("mirror-central", Collections.singletonList(mirror))));
    }

    @Test
    void effectiveRepositoryIdTakesPrecedenceOverAMirroredOriginalId() {
        RemoteRepository mirror = mirror(CENTRAL);

        assertSame(CENTRAL, assertDoesNotThrow(() -> select("central", Arrays.asList(mirror, CENTRAL))));
    }

    @Test
    void doesNotTreatAnUnrelatedMirrorAsCentral() {
        RemoteRepository releases = new RemoteRepository.Builder(
                        "company-releases", "default", "https://releases.example.invalid/")
                .build();

        assertThrows(
                MojoExecutionException.class, () -> select("central", Collections.singletonList(mirror(releases))));
    }

    @Test
    void stillRejectsUnknownRepositoryIds() {
        assertThrows(MojoExecutionException.class, () -> select("unknown", Collections.singletonList(CENTRAL)));
    }

    private static RemoteRepository mirror(RemoteRepository... originals) {
        return new RemoteRepository.Builder("mirror-central", "default", "https://mirror.example.invalid/")
                .setMirroredRepositories(Arrays.asList(originals))
                .setAuthentication(
                        new AuthenticationBuilder().addUsername("test-user").build())
                .setProxy(new Proxy("http", "proxy.example.invalid", 8080))
                .setReleasePolicy(new RepositoryPolicy(
                        true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
                .build();
    }

    private static RemoteRepository select(String referenceRepo, List<RemoteRepository> repositories) throws Exception {
        CompareMojo mojo = new CompareMojo(null, null, null, null, null, null);
        setField(mojo, "referenceRepo", referenceRepo);
        setField(mojo, "remoteRepos", repositories);
        Method method = CompareMojo.class.getDeclaredMethod("createReferenceRepo");
        method.setAccessible(true);
        try {
            return (RemoteRepository) method.invoke(mojo);
        } catch (InvocationTargetException exception) {
            throw (Exception) exception.getCause();
        }
    }

    private static void setField(CompareMojo mojo, String name, Object value) throws ReflectiveOperationException {
        Field field = CompareMojo.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(mojo, value);
    }
}
