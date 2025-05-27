package eu.copernik.tea;

import com.github.packageurl.PackageURL;
import eu.copernik.tea.model.Collection;
import eu.copernik.tea.model.Component;
import eu.copernik.tea.model.Product;
import eu.copernik.tea.model.Release;
import java.io.IOException;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface TeaRepositoryManager {

    @Nullable
    Product getProductById(UUID id) throws IOException;

    void saveProduct(Product product) throws IOException;

    Component getOrCreateComponentByPurl(PackageURL purl) throws IOException;

    void saveComponent(Component component) throws IOException;

    Release getOrCreateReleaseByVersion(Component component, String version) throws IOException;

    void saveRelease(Component component, Release release) throws IOException;

    @Nullable
    Collection getCollectionByReleaseAndVersion(Release release, int version) throws IOException;

    @Nullable
    Collection getLatestCollectionByRelease(Release release) throws IOException;

    void saveCollection(Collection collection) throws IOException;
}
