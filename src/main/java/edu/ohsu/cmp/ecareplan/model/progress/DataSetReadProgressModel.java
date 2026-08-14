package edu.ohsu.cmp.ecareplan.model.progress;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.model.ProgressStatus;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;

import java.util.ArrayList;
import java.util.List;

public class DataSetReadProgressModel extends BaseProgressModel implements IProgress {
    private final DataSet<?> dataSet;
    private final Endpoint endpoint;
    private final ProgressStatus status;
    private final List<String> errors;

    public DataSetReadProgressModel(DataSet<?> dataSet, Endpoint endpoint, ProgressStatus status, List<String> errors) {
        this.dataSet = dataSet;
        this.endpoint = endpoint;
        this.status = status;
        this.errors = errors != null ?
                new ArrayList<>(errors) :
                new ArrayList<>();
    }

    @Override
    public String getLabel() {
        return dataSet.getName();
    }

    @Override
    public ProgressStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        if (status == ProgressStatus.COMPLETED) {
            return errors.isEmpty() ?
                    "Read from " + endpoint.getName() + " has successfully completed." :
                    "Read from " + endpoint.getName() + " has completed, but with errors.  See error list for details.";

        } else if (status == ProgressStatus.RUNNING) {
            return "Read from " + endpoint.getName() + " is in progress.";

        } else {
            return "Read from " + endpoint.getName() + " is waiting to start.";
        }
    }

    @Override
    public Integer getCurrent() {
        return null;
    }

    @Override
    public Integer getTotal() {
        return null;
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }
}
