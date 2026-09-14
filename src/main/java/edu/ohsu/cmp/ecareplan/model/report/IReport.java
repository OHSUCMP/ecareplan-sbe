package edu.ohsu.cmp.ecareplan.model.report;

import edu.ohsu.cmp.ecareplan.entity.User;

public interface IReport {
    String getName();
    User getUser();
    String getSubject();
    String getContent();
}
