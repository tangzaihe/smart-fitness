import { parseApiJson, recordOf } from '@/src/api/json';

export interface SseEvent {
  event: string;
  data: unknown;
}

export async function parseSseStream(
  reader: ReadableStreamDefaultReader<Uint8Array>,
  onEvent: (event: SseEvent) => void,
): Promise<void> {
  const decoder = new TextDecoder();
  let buffer = '';
  let eventName = 'message';
  let dataLines: string[] = [];

  const dispatch = () => {
    if (dataLines.length === 0) {
      eventName = 'message';
      return;
    }
    const raw = dataLines.join('\n');
    let data: unknown = raw;
    try {
      data = parseApiJson(raw);
    } catch {
      data = raw;
    }
    onEvent({ event: eventName, data });
    eventName = 'message';
    dataLines = [];
  };

  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }
    buffer += decoder.decode(value, { stream: true });
    buffer = buffer.replace(/\r\n/g, '\n');
    let newline = buffer.indexOf('\n');
    while (newline >= 0) {
      const line = buffer.slice(0, newline);
      buffer = buffer.slice(newline + 1);
      if (line.startsWith(':')) {
        newline = buffer.indexOf('\n');
        continue;
      }
      if (line === '') {
        dispatch();
        newline = buffer.indexOf('\n');
        continue;
      }
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim();
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).replace(/^ /, ''));
      }
      newline = buffer.indexOf('\n');
    }
  }
  if (dataLines.length > 0) {
    dispatch();
  }
}

export function eventRecord(data: unknown) {
  return recordOf(data);
}

export const parseSse = parseSseStream;
export const asEventRecord = eventRecord;
