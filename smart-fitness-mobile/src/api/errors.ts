import { colors } from '@/src/theme';

export const ERROR_COPY: Record<number, string> = {
  1001: '参数校验失败',
  1002: '数据格式不匹配',
  2001: '未登录或登录已失效',
  2002: '登录已过期',
  2003: '登录失败次数过多，请稍后再试',
  2004: '没有权限',
  3001: '请先完成建档',
  3002: '已有进行中的训练课',
  3003: '这条建议不能再确认',
  3004: '当前课次状态不允许该操作',
  3005: '今日系统额度已用尽',
  3006: '请求过于频繁',
  3007: '已有进行中的教练回合',
  3008: '护栏拒绝了训练处方，改为休息',
  4001: '资源不存在',
  5001: '系统错误',
  5002: '教练服务暂时不可用',
};

export class ApiError extends Error {
  readonly code: number;
  readonly httpStatus: number;

  constructor(code: number, message: string, httpStatus: number) {
    super(message || ERROR_COPY[code] || '请求失败');
    this.name = 'ApiError';
    this.code = code;
    this.httpStatus = httpStatus;
  }

  get displayMessage(): string {
    return ERROR_COPY[this.code] ?? this.message;
  }
}

export function errorTone(code: number): string {
  if (code === 3005 || code === 3007 || code === 3002) {
    return colors.warning;
  }
  return colors.danger;
}

export const ErrorCopy = ERROR_COPY;
export const ApiErr = ApiError;
export const toneForError = errorTone;
