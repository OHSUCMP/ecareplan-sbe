package edu.ohsu.cmp.ecareplan.service.view;

import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.dataset.PatientModel;
import edu.ohsu.cmp.ecareplan.model.view.SummaryModel;
import edu.ohsu.cmp.ecareplan.service.BaseService;
import edu.ohsu.cmp.ecareplan.service.EndpointService;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SummaryService extends BaseService {

    @Autowired
    private EndpointService endpointService;

    public List<SummaryModel> getSummaryModels(String sessionId) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        Map<String, UserEndpoint> map = new HashMap<>();
        for (UserEndpoint ue : endpointService.getAllUserEndpoints(workspace.getUser())) {
            map.put(ue.getEndpoint().getName(), ue);
        }

        List<SummaryModel> list = new ArrayList<>();
        for (PatientModel pm : workspace.getAllDataSetModels(DataSet.PATIENT)) {
            if (pm.isSourcedFromSDS()) {
                UserEndpoint ue = map.get(pm.getSourceEndpointName());
                list.add(new SummaryModel(pm, ue.getLastSyncCompleted()));
            } else {
                list.add(new SummaryModel(pm, null));
            }
        }

        return list;
    }
}
