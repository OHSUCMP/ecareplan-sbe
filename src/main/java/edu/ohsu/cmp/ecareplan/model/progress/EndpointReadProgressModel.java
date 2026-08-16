package edu.ohsu.cmp.ecareplan.model.progress;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.model.ProgressStatus;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;

import java.util.*;

public class EndpointReadProgressModel extends BaseProgressModel implements IProgress {
    private final Endpoint endpoint;
    private final Map<DataSet<?>, ProgressStatus> dataSetStatusMap;
    private final Map<DataSet<?>, List<String>> dataSetErrorsMap;

    public EndpointReadProgressModel(Endpoint endpoint) {
        this.endpoint = endpoint;
        dataSetStatusMap = new LinkedHashMap<>();
        for (DataSet<?> dataSet : DataSet.ALL_DATASETS_BY_PRIORITY) {
            dataSetStatusMap.put(dataSet, ProgressStatus.WAITING_TO_START);
        }
        dataSetErrorsMap = new LinkedHashMap<>();
        lastUpdated = new Date();
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public String getLabel() {
        return endpoint.getName();
    }

    @Override
    public ProgressStatus getStatus() {
        if (dataSetStatusMap.values().stream().allMatch(s -> s == ProgressStatus.COMPLETED))
            return ProgressStatus.COMPLETED;
        else if (dataSetStatusMap.values().stream().anyMatch(s -> s == ProgressStatus.RUNNING))
            return ProgressStatus.RUNNING;
        else
            return ProgressStatus.WAITING_TO_START;
    }

    public ProgressStatus getStatus(DataSet<?> dataSet) {
        return dataSetStatusMap.get(dataSet);
    }

    public void setStatus(DataSet<?> dataSet, ProgressStatus status) {
        dataSetStatusMap.put(dataSet, status);
        lastUpdated = new Date();
    }

    @Override
    public String getMessage() {
        if (dataSetStatusMap.values().stream().allMatch(s -> s == ProgressStatus.COMPLETED)) {
            return dataSetErrorsMap.isEmpty() ?
                    "All datasets have been successfully read." :
                    "All datasets have been read, but with errors.  See error list for details.";

        } else if (dataSetStatusMap.values().stream().anyMatch(s -> s == ProgressStatus.RUNNING)) {
            DataSet<?> dataSet = dataSetStatusMap.entrySet().stream()
                    .filter(e -> e.getValue() == ProgressStatus.RUNNING)
                    .findFirst().get().getKey();
            return "Reading dataset: " + dataSet.getName();

        } else {
            return "Waiting to start";
        }
    }

    @Override
    public Integer getCurrent() {
        return dataSetStatusMap.values().stream().filter(s -> s == ProgressStatus.COMPLETED).toList().size();
    }

    @Override
    public Integer getTotal() {
        return dataSetStatusMap.size();
    }

    @Override
    public List<String> getErrors() {
        return dataSetErrorsMap.values().stream().flatMap(List::stream).toList();
    }

    public List<String> getErrors(DataSet<?> dataSet) {
        return dataSetErrorsMap.containsKey(dataSet) ?
                dataSetErrorsMap.get(dataSet) :
                List.of();
    }

    public void addError(DataSet<?> dataSet, String error) {
        if ( ! dataSetErrorsMap.containsKey(dataSet)) {
            dataSetErrorsMap.put(dataSet, new ArrayList<>());
        }
        dataSetErrorsMap.get(dataSet).add(error);
        lastUpdated = new Date();
    }

    public DataSetReadProgressModel getDataSetReadProgressModel(DataSet<?> dataSet) {
        return new DataSetReadProgressModel(dataSet, endpoint, getStatus(dataSet), getErrors(dataSet), lastUpdated);
    }
}
