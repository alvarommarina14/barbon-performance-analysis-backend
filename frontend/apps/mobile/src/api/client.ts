import { createApiClient } from '@court-proof/api-client';
import Constants from 'expo-constants';
import { Platform } from 'react-native';
import { secureTokenStore } from './tokenStore';

/**
 * A physical Android device cannot reach the host's localhost, so in development the base
 * URL is derived from the Metro host the app was loaded from.
 */
function developmentBaseUrl(): string {
  const hostUri = Constants.expoConfig?.hostUri ?? Constants.expoGoConfig?.debuggerHost;
  const host = hostUri?.split(':')[0];
  if (host) {
    return `http://${host}:8080`;
  }
  return Platform.OS === 'android' ? 'http://10.0.2.2:8080' : 'http://localhost:8080';
}

export const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL ?? developmentBaseUrl();

let sessionExpiredHandler: () => void = () => {};

/** Set by the auth provider so an expired session bounces the user back to the login screen. */
export function onSessionExpired(handler: () => void) {
  sessionExpiredHandler = handler;
}

export const api = createApiClient({
  baseUrl: API_BASE_URL,
  tokenStore: secureTokenStore,
  onSessionExpired: () => sessionExpiredHandler(),
});
