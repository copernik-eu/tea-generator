package eu.copernik.tea.internal;

import eu.copernik.tea.TeaRepository;
import eu.copernik.tea.TeaRepositoryManagerFactory;
import java.util.Objects;
import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class DefaultTeaRepositoryManagerFactory implements TeaRepositoryManagerFactory {

    @Override
    public DefaultTeaRepositoryManager newInstance(TeaRepository repository) {
        Objects.requireNonNull(repository, "Repository cannot be null");
        return new DefaultTeaRepositoryManager(repository.getBasePath());
    }
}
