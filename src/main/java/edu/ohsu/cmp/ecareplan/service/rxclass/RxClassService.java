package edu.ohsu.cmp.ecareplan.service.rxclass;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ohsu.cmp.ecareplan.entity.rxclass.RxClassMember;
import edu.ohsu.cmp.ecareplan.exception.MyHttpException;
import edu.ohsu.cmp.ecareplan.http.HttpRequest;
import edu.ohsu.cmp.ecareplan.http.HttpResponse;
import edu.ohsu.cmp.ecareplan.repository.rxclass.RxClassMemberRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class RxClassService {
    private static final Logger logger = LoggerFactory.getLogger(RxClassService.class);

    @Autowired
    private RxClassMemberRepository repository;

    public Set<String> getRxClassSet(Collection<String> rxCuis) {
        Set<String> set = new LinkedHashSet<>();
        for (String rxCui : rxCuis) {
            for (RxClassMember member : repository.findByRxCui(rxCui)) {
                set.add(member.getRxClass());
            }
        }
        return set;
    }

    public Set<String> getRxClassSet(String rxCui) {
        Set<String> set = new LinkedHashSet<>();
        for (RxClassMember member : repository.findByRxCui(rxCui)) {
            set.add(member.getRxClass());
        }
        return set;
    }

    public void refresh(String rxClass) {
        Map<String, RxClassMember> map = new LinkedHashMap<>();
        for (RxClassMember member : repository.findByRxClass(rxClass)) {
            map.put(member.getRxCui(), member);
        }

        Set<String> alreadyProcessed = new HashSet<>();

        try {
            for (String classMemberRxCUI : getClassMemberRxCUIList(rxClass)) {
                try {
                    JsonNode root;

                    HttpResponse response = new HttpRequest().get("https://rxnav.nlm.nih.gov/REST/rxcui/" + classMemberRxCUI + "/allrelated.json");
                    if (response.getResponseCode() >= 200 && response.getResponseCode() <= 300) {
                        logger.debug("got {} response for {} : {}", response.getResponseCode(), rxClass, response.getResponseBody());
                        root = new ObjectMapper().readTree(response.getResponseBody());
                    } else {
                        throw new MyHttpException(response.getResponseCode(), response.getResponseBody());
                    }

                    JsonNode allRelatedGroup = root.path("allRelatedGroup");
                    JsonNode conceptGroupList = allRelatedGroup.path("conceptGroup");
                    if (conceptGroupList.isArray()) {
                        for (JsonNode conceptGroup : conceptGroupList) {
                            JsonNode conceptProperties = conceptGroup.path("conceptProperties");
                            if (conceptProperties.isArray()) {
                                for (JsonNode member : conceptProperties) {
                                    String rxcui = member.path("rxcui").asText();
                                    String name = member.path("name").asText();
                                    String tty = member.path("tty").asText();

                                    if (alreadyProcessed.contains(rxcui)) {
                                        logger.debug("Already processed RxClassMember with rxcui {} for rxClass {} - name: {}, tty: {} - skipping -", rxcui, rxClass, name, tty);
                                        continue;
                                    }

                                    if (map.containsKey(rxcui)) {
                                        RxClassMember current = map.remove(rxcui);
                                        if ( ! current.getName().equals(name) || ! current.getTty().equals(tty) ) {
                                            current.setName(name);
                                            current.setTty(tty);
                                            current.setUpdated(new Date());
                                            logger.debug("Updating RxClassMember with rxcui {} for rxClass {} -  name: {}, tty: {}", rxcui, rxClass, name, tty);
                                            repository.save(current);

                                        } else {
                                            logger.debug("RxClassMember with rxcui {} for rxClass: {} - name: {}, tty: {} already up to date", rxcui, rxClass, name, tty);
                                        }

                                    } else {
                                        RxClassMember rxClassMember = new RxClassMember(rxClass, rxcui, name, tty);
                                        rxClassMember.setCreated(new Date());
                                        rxClassMember.setUpdated(new Date());
                                        logger.debug("Creating RxClassMember with rxcui {} for rxClass: {} - name: {}, tty: {}", rxcui, rxClass, name, tty);
                                        repository.save(rxClassMember);
                                    }

                                    alreadyProcessed.add(rxcui);
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    logger.error("error getting all related for {}", classMemberRxCUI, e);
                }
            }

        } catch (Exception e) {
            logger.error("error refreshing rxclass members for {}", rxClass, e);
        }

        if ( ! map.isEmpty() ) {
            List<String> toDelete = map.values().stream().map(RxClassMember::getRxCui).toList();
            logger.debug("Deleting {} RxClassMembers for RxClass {} : [{}]", toDelete.size(), rxClass, StringUtils.join(toDelete, ","));
            repository.deleteAll(map.values());
        }
    }

    private List<String> getClassMemberRxCUIList(String rxClass) throws IOException {
        JsonNode root;

        HttpResponse response = new HttpRequest().get("https://rxnav.nlm.nih.gov/REST/rxclass/classMembers.json?classId=" + rxClass + "&relaSource=ATC&trans=0");
        if (response.getResponseCode() >= 200 && response.getResponseCode() <= 300) {
            logger.debug("got {} response for {} : {}", response.getResponseCode(), rxClass, response.getResponseBody());
            root = new ObjectMapper().readTree(response.getResponseBody());
        } else {
            throw new MyHttpException(response.getResponseCode(), response.getResponseBody());
        }

        List<String> list = new ArrayList<>();
        JsonNode drugMemberGroup = root.path("drugMemberGroup");
        JsonNode drugMember = drugMemberGroup.path("drugMember");
        if (drugMember.isArray()) {
            for (JsonNode member : drugMember) {
                JsonNode minConcept = member.path("minConcept");
                String rxcui = minConcept.path("rxcui").asText();
                list.add(rxcui);
            }
        }
        return list;
    }
}
