package com.aseantec.smartfitness.knowledge.port;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Set;

@Value
@Builder
public class RetrieveQuery {
    List<String> equipment;
    List<String> liked;
    List<String> disliked;
    List<String> never;
    Set<String> excludePatterns;
    String pattern;
    String muscleGroup;
    int limit;
}
