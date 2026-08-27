/**
 * Token persistence differs per client — `expo-secure-store` on mobile, Electron's
 * `safeStorage` on desktop — so the client takes a store rather than owning one.
 */
export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

export interface TokenStore {
  read(): Promise<TokenPair | null>;
  write(tokens: TokenPair): Promise<void>;
  clear(): Promise<void>;
}

/** Non-persistent store; useful in tests and in the Electron main process. */
export function createMemoryTokenStore(): TokenStore {
  let tokens: TokenPair | null = null;
  return {
    async read() {
      return tokens;
    },
    async write(next) {
      tokens = next;
    },
    async clear() {
      tokens = null;
    },
  };
}
