package com.aseantec.smartfitness.knowledge.port;

import java.util.List;
import java.util.Map;

public interface KnowledgePort {
    List<Map<String, Object>> retrieve(RetrieveQuery query);
}
