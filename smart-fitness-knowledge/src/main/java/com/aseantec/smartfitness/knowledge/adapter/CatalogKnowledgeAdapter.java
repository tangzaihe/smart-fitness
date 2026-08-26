package com.aseantec.smartfitness.knowledge.adapter;

import com.aseantec.smartfitness.knowledge.port.KnowledgePort;
import com.aseantec.smartfitness.knowledge.port.RetrieveQuery;
import com.aseantec.smartfitness.training.entity.ExerciseCatalog;
import com.aseantec.smartfitness.training.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CatalogKnowledgeAdapter implements KnowledgePort {
    private static final Set<String> FREE_WEIGHT = Set.of("BARBELL", "DUMBBELL", "KETTLEBELL");
    private final CatalogService catalogService;

    @Override
    public List<Map<String, Object>> retrieve(RetrieveQuery query) {
        List<String> equipment = query.getEquipment() == null ? List.of() : query.getEquipment();
        List<String> liked = query.getLiked() == null ? List.of() : query.getLiked();
        List<String> disliked = query.getDisliked() == null ? List.of() : query.getDisliked();
        List<String> never = query.getNever() == null ? List.of() : query.getNever();
        Set<String> exclude = query.getExcludePatterns() == null ? Set.of() : query.getExcludePatterns();
        int limit = query.getLimit() <= 0 ? 8 : Math.min(query.getLimit(), 8);
        List<Map<String, Object>> scored = new ArrayList<>();
        for (ExerciseCatalog item : catalogService.listActive()) {
            if (never.contains(item.getCode()) || never.contains(item.getName())) {
                continue;
            }
            if (item.getPattern() != null && exclude.contains(item.getPattern())) {
                continue;
            }
            if (query.getPattern() != null && !query.getPattern().isBlank()
                    && !query.getPattern().equals(item.getPattern())) {
                continue;
            }
            if (query.getMuscleGroup() != null && !query.getMuscleGroup().isBlank()
                    && !query.getMuscleGroup().equals(item.getMuscleGroup())) {
                continue;
            }
            double eq = equipmentMatch(item.getEquipment(), equipment);
            double pat = query.getPattern() == null || query.getPattern().isBlank()
                    || query.getPattern().equals(item.getPattern()) ? 1.0 : 0.0;
            double mus = query.getMuscleGroup() == null || query.getMuscleGroup().isBlank()
                    || query.getMuscleGroup().equals(item.getMuscleGroup()) ? 1.0 : 0.0;
            double score = 0.50 * eq + 0.25 * pat + 0.15 * mus;
            if (isLiked(item, liked)) {
                score += 0.10;
            }
            if (isDisliked(item, disliked)) {
                score -= 0.15;
            }
            score = Math.max(0, score);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", item.getCode());
            row.put("name", item.getName());
            row.put("pattern", item.getPattern());
            row.put("muscleGroup", item.getMuscleGroup());
            row.put("equipment", item.getEquipment());
            row.put("swapGroup", item.getSwapGroup());
            row.put("score", Math.round(score * 1000.0) / 1000.0);
            scored.add(row);
        }
        scored.sort(Comparator.comparingDouble(m -> -((Double) m.get("score"))));
        return scored.size() > limit ? scored.subList(0, limit) : scored;
    }

    private double equipmentMatch(String itemEq, List<String> userEq) {
        if ("BODYWEIGHT".equals(itemEq)) {
            return 1.0;
        }
        return userEq.contains(itemEq) ? 1.0 : 0.0;
    }

    private boolean isLiked(ExerciseCatalog item, List<String> liked) {
        if (liked.contains(item.getCode()) || liked.contains(item.getName())) {
            return true;
        }
        return liked.contains("free_weight") && FREE_WEIGHT.contains(item.getEquipment());
    }

    private boolean isDisliked(ExerciseCatalog item, List<String> disliked) {
        return disliked.contains(item.getCode()) || disliked.contains(item.getName());
    }
}
