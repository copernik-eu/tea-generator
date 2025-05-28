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

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.copernik.tea.model.PaginationDetails;
import eu.copernik.tea.model.Product;
import java.util.List;
import org.jspecify.annotations.NonNull;

public class ProductPaginationDetails extends PaginationDetails {
    public static final String JSON_PROPERTY_RESULTS = "results";

    @NonNull
    private List<Product> results = List.of();

    @JsonProperty(JSON_PROPERTY_RESULTS)
    public @NonNull List<Product> getResults() {
        return results;
    }

    public ProductPaginationDetails results(@NonNull List<Product> results) {
        setResults(results);
        return this;
    }

    public void setResults(@NonNull List<Product> results) {
        this.results = results;
    }
}
