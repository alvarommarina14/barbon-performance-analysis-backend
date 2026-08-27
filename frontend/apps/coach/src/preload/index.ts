import { contextBridge, ipcRenderer } from 'electron';

/**
 * The only bridge between the renderer and Node. Everything else the coach app does goes
 * through the HTTP API, so this surface should stay small.
 */
const courtProof = {
  /** Returns the absolute path of the chosen file, or null when the dialog is cancelled. */
  pickVideoFile: (): Promise<string | null> => ipcRenderer.invoke('dialog:pickVideoFile'),
} as const;

contextBridge.exposeInMainWorld('courtProof', courtProof);

export type CourtProofBridge = typeof courtProof;
