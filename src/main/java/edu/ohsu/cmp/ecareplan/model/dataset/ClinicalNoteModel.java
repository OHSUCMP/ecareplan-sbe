package edu.ohsu.cmp.ecareplan.model.dataset;

import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.DocumentReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClinicalNoteModel extends BaseDataSetModel<DocumentReference> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final List<Binary> sourceBinaryList;

    public ClinicalNoteModel(DocumentReference documentReference, List<Binary> binaryList) {
        super(documentReference);
        this.sourceBinaryList = binaryList;
    }

    @Override
    public DocumentReference toResourceForSDSExport() {
        // if binary resources are available, make a copy of the source resource and integrate binary data into it
        if (sourceBinaryList != null) {
            DocumentReference documentReference = sourceResource.copy();

            Map<String, Binary> binaryMap = new HashMap<>();
            for (Binary binary : sourceBinaryList) {
                binaryMap.put(FhirUtil.toRelativeReference(binary.getId()), binary);
            }

            // integrate Binary data into DocumentReference
            for (DocumentReference.DocumentReferenceContentComponent content : documentReference.getContent()) {
                if (content.hasAttachment()) {
                    Attachment attachment = content.getAttachment();
                    if (attachment.hasUrl() && binaryMap.containsKey(attachment.getUrl())) {
                        Binary binary = binaryMap.get(attachment.getUrl());
                        if (binary.hasContentType()) {
                            attachment.setContentType(binary.getContentType());
                        }
                        if (binary.hasData()) {
                            attachment.setData(binary.getData());
                        }
                        attachment.setUrl(null);
                    } else {
                        logger.warn("Binary not found for attachment: " + attachment.getUrl());
                    }
                }
            }

            return documentReference;
        }

        return sourceResource;
    }

    public List<Binary> getSourceBinaryList() {
        return sourceBinaryList;
    }
}
