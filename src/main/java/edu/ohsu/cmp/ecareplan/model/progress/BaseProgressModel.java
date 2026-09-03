package edu.ohsu.cmp.ecareplan.model.progress;

import edu.ohsu.cmp.ecareplan.model.ProgressStatus;

import java.util.Date;

public abstract class BaseProgressModel implements IProgress {
    private final String endpointName;
    protected Date lastUpdated;

    public BaseProgressModel(String endpointName) {
        this.endpointName = endpointName;
    }

    @Override
    public Integer getPercentComplete() {
        if (getCurrent() == null || getTotal() == null) {
            return getStatus() == ProgressStatus.COMPLETED ?
                    100 :
                    0;
        }

        if (getTotal() == 0) {
            return 100;
        } else {
            return getTotal() == 100 ?
                    getCurrent() :
                    Math.round(getCurrent() * 100 / (float) getTotal());
        }
    }

    @Override
    public String getEndpointName() {
        return endpointName;
    }

    @Override
    public Date getLastUpdated() {
        return lastUpdated;
    }
}
