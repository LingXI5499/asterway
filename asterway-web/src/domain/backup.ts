import { validateData } from './schema';
import { validatePrototype } from './prototypeSchema';
import type { AppData } from './types';
import type { PrototypeData } from './prototype';

export function validateBackup(input: unknown): {
  growth: AppData;
  prototype: PrototypeData | null;
} {
  const growth = validateData(input);
  const prototype =
    input && typeof input === 'object' && 'prototype' in input
      ? validatePrototype(input.prototype)
      : null;
  return { growth, prototype };
}
