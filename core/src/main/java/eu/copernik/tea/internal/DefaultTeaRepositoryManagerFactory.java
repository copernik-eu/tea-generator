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
package eu.copernik.tea.internal;

import eu.copernik.tea.TeaRepository;
import eu.copernik.tea.spi.TeaRepositoryManagerFactory;
import java.util.Objects;
import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class DefaultTeaRepositoryManagerFactory implements TeaRepositoryManagerFactory {

    public static final TeaRepositoryManagerFactory INSTANCE = new DefaultTeaRepositoryManagerFactory();

    @Override
    public DefaultTeaRepositoryManager newInstance(TeaRepository repository) {
        Objects.requireNonNull(repository, "Repository cannot be null");
        return new DefaultTeaRepositoryManager(repository.getBasePath());
    }
}
