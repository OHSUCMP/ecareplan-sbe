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
        logger.info("refreshing definitions for RxClass {}", rxClass);
        long start = System.currentTimeMillis();

        RefreshHelper helper = new RefreshHelper(repository, rxClass);
        helper.execute();

        logger.info("done refreshing definitions for RxClass {} - created: {}, updated: {}, deleted: {} - took {} ms",
                rxClass, helper.getCreated(), helper.getUpdated(), helper.getDeleted(),
                System.currentTimeMillis() - start);
    }


    private static final class RefreshHelper {
        private final RxClassMemberRepository repository;
        private final String rxClass;

        private Map<String, RxClassMember> map;
        private Set<String> alreadyProcessed;
        private int created = 0;
        private int updated = 0;
        private int deleted = 0;

        public RefreshHelper(RxClassMemberRepository repository, String rxClass) {
            this.repository = repository;
            this.rxClass = rxClass;
        }

        public void execute() {
            map = new LinkedHashMap<>();
            for (RxClassMember member : repository.findByRxClass(rxClass)) {
                map.put(member.getRxCui(), member);
            }
            alreadyProcessed = new HashSet<>();
            created = 0;
            updated = 0;
            deleted = 0;

            try {
                for (Concept classMember : getClassMemberList("ATCPROD")) {
                    processConcept(classMember);
                }

                for (Concept classMember : getClassMemberList("ATC")) {
                    processConcept(classMember);

                    Set<String> validTermTypes = Set.of("SCD", "SBD", "GPCK", "BPCK");
                    for (Concept relatedMember : getAllRelatedList(classMember.getRxcui())) {
                        if (validTermTypes.contains(relatedMember.getTty())) {
                            processConcept(relatedMember);
                        }
                    }
                }

            } catch (Exception e) {
                logger.error("error refreshing rxclass members for {}", rxClass, e);
            }

            if ( ! map.isEmpty() ) {
                List<String> toDelete = map.values().stream().map(RxClassMember::getRxCui).toList();
                deleted = toDelete.size();
                logger.debug("Deleting {} RxClassMembers for rxClass {} with rxcui in: [{}]", toDelete.size(), rxClass, StringUtils.join(toDelete, ","));
                repository.deleteAll(map.values());
            }
        }

        public int getCreated() {
            return created;
        }

        public int getUpdated() {
            return updated;
        }

        public int getDeleted() {
            return deleted;
        }

        private void processConcept(Concept classMember) {
            if (alreadyProcessed.contains(classMember.getRxcui())) {
                logger.debug("Already processed RxClassMember with rxcui {} for rxClass {} - name: {}, tty: {} - skipping -", classMember.getRxcui(), rxClass, classMember.getName(), classMember.getTty());

            } else {
                try {
                    if (map.containsKey(classMember.getRxcui())) {
                        RxClassMember current = map.remove(classMember.getRxcui());
                        if ( ! current.getName().equals(classMember.getName()) || ! current.getTty().equals(classMember.getTty()) ) {
                            current.setName(classMember.getName());
                            current.setTty(classMember.getTty());
                            current.setUpdated(new Date());
                            logger.debug("Updating RxClassMember with rxcui {} for rxClass {} - name: {}, tty: {}", classMember.getRxcui(), rxClass, classMember.getName(), classMember.getTty());
                            repository.save(current);
                            updated ++;

                        } else {
                            logger.debug("RxClassMember with rxcui {} for rxClass: {} - name: {}, tty: {} already up to date", classMember.getRxcui(), rxClass, classMember.getName(), classMember.getTty());
                        }

                    } else {
                        RxClassMember rxClassMember = new RxClassMember(rxClass, classMember.getRxcui(), classMember.getName(), classMember.getTty());
                        rxClassMember.setCreated(new Date());
                        rxClassMember.setUpdated(new Date());
                        logger.debug("Creating RxClassMember with rxcui {} for rxClass: {} - name: {}, tty: {}", classMember.getRxcui(), rxClass, classMember.getName(), classMember.getTty());
                        repository.save(rxClassMember);
                        created ++;
                    }

                } catch (Exception e) {
                    logger.error("Error processing RxClassMember with rxcui {} for rxClass: {} - name: {}, tty: {}", classMember.getRxcui(), rxClass, classMember.getName(), classMember.getTty(), e);

                } finally {
                    alreadyProcessed.add(classMember.getRxcui());
                }
            }
        }

        private List<Concept> getClassMemberList(String relaSource) throws IOException {
            JsonNode root = getJsonRootNode("https://rxnav.nlm.nih.gov/REST/rxclass/classMembers.json?classId=" + rxClass + "&relaSource=" + relaSource);
            List<Concept> list = new ArrayList<>();
            JsonNode drugMemberGroup = root.path("drugMemberGroup");
            JsonNode drugMember = drugMemberGroup.path("drugMember");
            if (drugMember.isArray()) {
                for (JsonNode member : drugMember) {
                    JsonNode minConcept = member.path("minConcept");
                    String rxcui = minConcept.path("rxcui").asText();
                    String name = minConcept.path("name").asText();
                    String tty = minConcept.path("tty").asText();
                    list.add(new Concept(rxcui, name, tty));
                }
            }
            return list;
        }

        private List<Concept> getAllRelatedList(String rxCui) throws IOException {
            JsonNode root = getJsonRootNode("https://rxnav.nlm.nih.gov/REST/rxcui/" + rxCui + "/allrelated.json");
            List<Concept> list = new ArrayList<>();
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
                            list.add(new Concept(rxcui, name, tty));
                        }
                    }
                }
            }
            return list;
        }

        private JsonNode getJsonRootNode(String url) throws IOException {
            HttpResponse response = new HttpRequest().get(url);
            if (response.getResponseCode() >= 200 && response.getResponseCode() <= 300) {
                logger.debug("got {} response for {} : {}", response.getResponseCode(), url, response.getResponseBody());
                return new ObjectMapper().readTree(response.getResponseBody());
            } else {
                throw new MyHttpException(response.getResponseCode(), response.getResponseBody());
            }
        }
    }

    private static final class Concept {
        private final String rxcui;
        private final String name;
        private final String tty;

        public Concept(String rxcui, String name, String tty) {
            this.rxcui = rxcui;
            this.name = name;
            this.tty = tty;
        }

        public String getRxcui() {
            return rxcui;
        }

        public String getName() {
            return name;
        }

        public String getTty() {
            return tty;
        }
    }
}
