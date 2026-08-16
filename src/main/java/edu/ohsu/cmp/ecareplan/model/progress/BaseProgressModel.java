package edu.ohsu.cmp.ecareplan.model.progress;

import java.util.Date;

public abstract class BaseProgressModel implements IProgress {
    protected Date lastUpdated;

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

    @Override
    public Date getLastUpdated() {
        return lastUpdated;
    }
}
