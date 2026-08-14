package edu.ohsu.cmp.ecareplan.model.progress;

import edu.ohsu.cmp.ecareplan.model.ProgressStatus;

import java.util.List;

public interface IProgress {
    String getLabel();
    ProgressStatus getStatus();
    String getMessage();
    Integer getCurrent();
    Integer getTotal();
    Integer getPercentComplete();
    List<String> getErrors();
}
