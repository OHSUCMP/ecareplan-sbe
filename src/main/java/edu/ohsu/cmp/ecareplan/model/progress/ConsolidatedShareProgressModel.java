package edu.ohsu.cmp.ecareplan.model.progress;

import edu.ohsu.cmp.ecareplan.model.ProgressStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConsolidatedShareProgressModel implements IProgress {
    private static final Logger logger = LoggerFactory.getLogger(ConsolidatedShareProgressModel.class);

    private final List<ShareProgressModel> sdsProgressModelList;

    public ConsolidatedShareProgressModel(List<ShareProgressModel> shareProgressModelList) {
        if (shareProgressModelList == null || shareProgressModelList.isEmpty()) {
            throw new IllegalArgumentException("shareProgressModelList cannot be null or empty");
        }

        if (shareProgressModelList.stream().map(ShareProgressModel::getEndpointName).collect(Collectors.toSet()).size() > 1) {
            throw new IllegalArgumentException("shareProgressModelList cannot contain multiple distinct endpoint names");
        }

        this.sdsProgressModelList = shareProgressModelList;
    }

    @Override
    public String getEndpointName() {
        return sdsProgressModelList.getFirst().getEndpointName();
    }

    @Override
    public String getLabel() {
        return "SDS";
    }

    @Override
    public ProgressStatus getStatus() {
        Set<ProgressStatus> distinctStatusSet = sdsProgressModelList.stream().map(ShareProgressModel::getStatus).collect(Collectors.toSet());
        if (distinctStatusSet.stream().allMatch(status -> status.equals(ProgressStatus.COMPLETED))) {
            return ProgressStatus.COMPLETED;
        } else if (distinctStatusSet.stream().anyMatch(status -> status.equals(ProgressStatus.RUNNING))) {
            return ProgressStatus.RUNNING;
        } else {
            return ProgressStatus.WAITING_TO_START;
        }
    }

    @Override
    public String getMessage() {
        if (getStatus() == ProgressStatus.COMPLETED) {
            return getErrors().isEmpty() ?
                    "Share from " + getEndpointName() + " has completed successfully." :
                    "Share from " + getEndpointName() + " has completed with errors.  See error list for details.";

        } else if (getStatus() == ProgressStatus.RUNNING) {
            return "Share from " + getEndpointName() + " is running";

        } else {
            return "Share from " + getEndpointName() + " is waiting to start";
        }
    }

    @Override
    public Integer getCurrent() {
        return sdsProgressModelList.stream().map(ShareProgressModel::getCurrent).reduce(0, Integer::sum);
    }

    @Override
    public Integer getTotal() {
        return sdsProgressModelList.stream().map(ShareProgressModel::getTotal).reduce(0, Integer::sum);
    }

    @Override
    public Integer getPercentComplete() {
        return Math.round((float) getCurrent() / getTotal() * 100);
    }

    @Override
    public List<String> getErrors() {
        List<String> list = new ArrayList<>();
        for (ShareProgressModel spm : sdsProgressModelList) {
            list.addAll(spm.getErrors());
        }
        return list;
    }

    @Override
    public Date getLastUpdated() {
        return null;
    }
}
