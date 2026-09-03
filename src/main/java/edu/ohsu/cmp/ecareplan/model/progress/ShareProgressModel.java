package edu.ohsu.cmp.ecareplan.model.progress;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.model.ProgressStatus;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

public class ShareProgressModel extends BaseProgressModel implements IProgress {
    private final DataSet<?> dataSet;
    private final Endpoint endpoint;
    private ProgressStatus status;
    private Integer current;
    private final Integer total;
    private final List<String> errors;
    private transient Future<Void> future = null;

    public ShareProgressModel(DataSet<?> dataSet, Endpoint endpoint, ProgressStatus status, Integer current, Integer total) {
        super(endpoint.getName());

        this.dataSet = dataSet;
        this.endpoint = endpoint;
        this.status = status;
        this.current = current;
        this.total = total;
        this.errors = new ArrayList<>();
        this.lastUpdated = new Date();
    }

    public DataSet<?> getDataSet() {
        return dataSet;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public String getLabel() {
        return "SDS";
    }

    @Override
    public ProgressStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        if (status == ProgressStatus.COMPLETED) {
            return errors.isEmpty() ?
                    "Share of " + dataSet.getDisplay() + " from " + endpoint.getName() + " has completed successfully." :
                    "Share of " + dataSet.getDisplay() + " from " + endpoint.getName() + " has completed with errors.  See error list for details.";

        } else if (status == ProgressStatus.RUNNING) {
            return "Share of " + dataSet.getDisplay() + " from " + endpoint.getName() + " is running.";

        } else {
            return "Share of " + dataSet.getDisplay() + " from " + endpoint.getName() + " is waiting to start.";
        }
    }

    public void setStatus(ProgressStatus status) {
        this.status = status;
        lastUpdated = new Date();
    }

    @Override
    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
        lastUpdated = new Date();
    }

    @Override
    public Integer getTotal() {
        return total;
    }

    @Override
    public Integer getPercentComplete() {
        if (total == 0) {
            return 100;
        } else {
            return total == 100 ?
                    current :
                    Math.round(current * 100 / (float) total);
        }
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        errors.add(error);
        lastUpdated = new Date();
    }

    public Future<Void> getFuture() {
        return future;
    }

    public void setFuture(Future<Void> future) {
        this.future = future;
    }
}
