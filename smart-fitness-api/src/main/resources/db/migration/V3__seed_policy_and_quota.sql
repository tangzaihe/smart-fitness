-- Default policy persona (draft/active for P0 runtime) + global SYSTEM quota.

INSERT INTO coach_policy (id, name, created_at)
VALUES (1, 'coach.default', now());

INSERT INTO coach_policy_version (
    id, policy_id, version, system_prompt, tool_flags, guardrails, status, gray_percent, created_by, created_at
) VALUES (
    1,
    1,
    1,
    $prompt$You are Smart Fitness in-app coach. Never diagnose. Never invent exercise codes.
Always call retrieve_exercise_options before proposing a session.
Every session slot must include pick plus alternatives from the same swap_group.
If medical constraints exist, only REST. If readiness is low, REST or DELOAD.
Output a single JSON object matching schema_version=1.$prompt$,
    '{"tools":["get_athlete","get_readiness","get_recent_load","retrieve_exercise_options"]}'::jsonb,
    '{"g1":true,"g2":true,"g3":true,"g4":true,"g5":true}'::jsonb,
    'active',
    0,
    NULL,
    now()
);

INSERT INTO llm_quota (id, athlete_id, period, token_limit, cost_limit_minor, rpm_limit, created_at)
VALUES (1, NULL, 'DAY', 200000, NULL, 20, now());

INSERT INTO llm_price_list (
    id, provider, model, input_per_1k_minor, output_per_1k_minor, cached_per_1k_minor, currency, effective_from
) VALUES
(1, 'fake', 'fake-coach', 0, 0, 0, 'CNY', DATE '2026-01-01'),
(2, 'openai', 'gpt-4o-mini', 15, 60, 8, 'CNY', DATE '2026-01-01');
