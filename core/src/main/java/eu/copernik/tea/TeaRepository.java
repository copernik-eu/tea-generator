/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.copernik.tea;

import java.nio.file.Path;

/**
 * This class represents a resource to store and manage TEA objects such as products, components, releases, and collections.
 */
public final class TeaRepository {

    private final Path basePath;

    /**
     * Constructs a new TeaRepository with the specified base path.
     *
     * @param basePath the base path where TEA objects will be stored
     */
    public TeaRepository(Path basePath) {
        this.basePath = basePath;
    }

    /**
     * Returns the base path of this TeaRepository.
     *
     * @return the base path where TEA objects are stored
     */
    public Path getBasePath() {
        return basePath;
    }
}
