import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';

const memory = new Map<string, string>();

function webStore(): Storage | null {
  try {
    return globalThis.localStorage ?? null;
  } catch {
    return null;
  }
}

export async function storageGet(key: string): Promise<string | null> {
  if (Platform.OS === 'web') {
    return webStore()?.getItem(key) ?? memory.get(key) ?? null;
  }
  try {
    return await SecureStore.getItemAsync(key);
  } catch {
    return memory.get(key) ?? null;
  }
}

export async function storageSet(key: string, value: string): Promise<void> {
  memory.set(key, value);
  if (Platform.OS === 'web') {
    webStore()?.setItem(key, value);
    return;
  }
  await SecureStore.setItemAsync(key, value);
}

export async function storageDelete(key: string): Promise<void> {
  memory.delete(key);
  if (Platform.OS === 'web') {
    webStore()?.removeItem(key);
    return;
  }
  await SecureStore.deleteItemAsync(key);
}

export const storageRemove = storageDelete;
export const storageDel = storageDelete;
