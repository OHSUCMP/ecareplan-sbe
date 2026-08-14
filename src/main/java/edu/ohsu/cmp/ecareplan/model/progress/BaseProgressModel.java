package edu.ohsu.cmp.ecareplan.model.progress;

public abstract class BaseProgressModel implements IProgress {
    @Override
    public Integer getPercentComplete() {
        if (getCurrent() == null || getTotal() == null) return null;

        if (getTotal() == 0) {
            return 100;
        } else {
            return getTotal() == 100 ?
                    getCurrent() :
                    Math.round(getCurrent() * 100 / (float) getTotal());
        }
    }
}
