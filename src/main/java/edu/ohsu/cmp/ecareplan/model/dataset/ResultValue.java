package edu.ohsu.cmp.ecareplan.model.dataset;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public class ResultValue {
    private final List<Component> components;

    public ResultValue(String conceptName, BigDecimal value) {
        this.components = List.of(new Component(conceptName, value));
    }

    public ResultValue(Collection<Component> components) {
        this.components = List.copyOf(components);
    }

    public List<Component> getComponents() {
        return components;
    }

    public boolean isComparable() {
        return components.size() == 1; // can't compare composite values, that just doesn't work
    }

    public BigDecimal getValueForCompare() {
        return isComparable() ?
                components.getFirst().value :
                null;
    }

    public record Component(String conceptName, BigDecimal value) {
    }
}
