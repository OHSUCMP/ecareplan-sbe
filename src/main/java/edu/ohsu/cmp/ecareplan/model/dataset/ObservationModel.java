package edu.ohsu.cmp.ecareplan.model.dataset;

import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;

import java.util.List;

public abstract class ObservationModel extends BaseDataSetModel<Observation> {
    public ObservationModel(Observation resource) {
        super(resource);
    }

    protected Quantity getQuantityFromComponent(Coding coding, List<Observation.ObservationComponentComponent> components) {
        for (Observation.ObservationComponentComponent component : components) {
            if (FhirUtil.hasCoding(component.getCode(), coding)) {
                if (component.hasValueQuantity()) {
                    return new Quantity(
                            component.getValueQuantity().getValue().toString(),
                            component.getValueQuantity().getUnit()
                    );
                }
            }
        }
        return null;
    }

    protected static final class Quantity {
        public final String value;
        public final String unit;

        public Quantity(String value, String unit) {
            this.value = value;
            this.unit = unit;
        }

        public String getValue() {
            return value;
        }

        public String getUnit() {
            return unit;
        }
    }
}
