import { openSse } from '@/src/api/client';
import { parseAdviceEvent, toolLabel } from '@/src/coach/advice';
import { eventRecord, parseSseStream } from '@/src/coach/sse';
import type { AdvicePayload, CoachTrigger, CoachUiStatus } from '@/src/types';
import { useCallback, useRef, useState } from 'react';

interface CoachRunState {
  status: CoachUiStatus;
  runId: string | null;
  /** LLM 正在流式输出处方 JSON；仅用于 UI 状态，不展示原始 token。 */
  isGenerating: boolean;
  tools: string[];
  adviceId: string | null;
  kind: string | null;
  payload: AdvicePayload | null;
  errorMessage: string | null;
  errorCode: number | null;
}

const initial: CoachRunState = {
  status: 'idle',
  runId: null,
  isGenerating: false,
  tools: [],
  adviceId: null,
  kind: null,
  payload: null,
  errorMessage: null,
  errorCode: null,
};

export function useCoachRun() {
  const [state, setState] = useState<CoachRunState>(initial);
  const started = useRef(false);

  const start = useCallback(async (trigger: CoachTrigger = 'OBSERVE') => {
    started.current = true;
    setState({
      ...initial,
      status: 'running',
    });
    try {
      const response = await openSse(`/v1/coach/runs?trigger=${encodeURIComponent(trigger)}`);
      const body = response.body;
      if (!body) {
        throw new Error('SSE body missing');
      }
      await parseSseStream(body.getReader(), ({ event, data }) => {
        const row = eventRecord(data);
        setState((prev) => {
          if (event === 'run.created') {
            return { ...prev, runId: String(row.runId ?? prev.runId ?? ''), status: 'running' };
          }
          if (event === 'token') {
            return { ...prev, isGenerating: true, status: 'running' };
          }
          if (event === 'tool.start' || event === 'tool.result') {
            const label = toolLabel(data);
            const tools = prev.tools.includes(label) ? prev.tools : [...prev.tools, label];
            return { ...prev, tools, status: 'tooling' };
          }
          if (event === 'advice') {
            const advice = parseAdviceEvent(data);
            return {
              ...prev,
              status: 'awaiting_confirm',
              isGenerating: false,
              adviceId: advice.adviceId,
              kind: advice.kind,
              payload: advice.payload,
            };
          }
          if (event === 'error') {
            return {
              ...prev,
              status: prev.payload ? 'awaiting_confirm' : 'error',
              errorMessage: String(row.message ?? '教练回合失败'),
              errorCode: typeof row.code === 'number' ? row.code : null,
            };
          }
          if (event === 'done') {
            return {
              ...prev,
              status: prev.payload ? 'awaiting_confirm' : prev.status === 'error' ? 'error' : 'idle',
            };
          }
          return prev;
        });
      });
      setState((prev) => ({
        ...prev,
        status: prev.payload ? 'awaiting_confirm' : prev.status === 'error' ? 'error' : 'idle',
      }));
    } catch (error) {
      const message = error instanceof Error ? error.message : '教练回合失败';
      const code = error && typeof error === 'object' && 'code' in error ? Number(error.code) : null;
      setState((prev) => ({
        ...prev,
        status: prev.payload ? 'awaiting_confirm' : 'error',
        errorMessage: message,
        errorCode: code,
      }));
    }
  }, []);

  return { ...state, start, started };
}

export const useCoachSession = useCoachRun;
