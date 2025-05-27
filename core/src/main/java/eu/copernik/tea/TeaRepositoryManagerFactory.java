/*
 * Copyright © 2025 Piotr P. Karwasz
 *
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

import eu.copernik.tea.internal.DefaultTeaRepositoryManagerFactory;

public interface TeaRepositoryManagerFactory {

    /**
     * Returns a default instance of TeaRepositoryManagerFactory.
     *
     * @return a default instance of TeaRepositoryManagerFactory
     */
    static TeaRepositoryManagerFactory getInstance() {
        return DefaultTeaRepositoryManagerFactory.INSTANCE;
    }

    /**
     * Creates a new instance of TeaRepositoryManager.
     *
     * @return a new TeaRepositoryManager instance
     */
    TeaRepositoryManager newInstance(TeaRepository repository) throws IllegalArgumentException;
}
