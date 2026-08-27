import { contextBridge, ipcRenderer } from 'electron';

/**
 * The only bridge between the renderer and Node. Everything else the coach app does goes
 * through the HTTP API, so this surface should stay small.
 */
const courtProof = {
  /** Returns the absolute path of the chosen file, or null when the dialog is cancelled. */
  pickVideoFile: (): Promise<string | null> => ipcRenderer.invoke('dialog:pickVideoFile'),

  /** OS-encrypted session storage, handled entirely in the main process. */
  session: {
    read: (): Promise<string | null> => ipcRenderer.invoke('session:read'),
    write: (payload: string): Promise<void> => ipcRenderer.invoke('session:write', payload),
    clear: (): Promise<void> => ipcRenderer.invoke('session:clear'),
  },
} as const;

contextBridge.exposeInMainWorld('courtProof', courtProof);

export type CourtProofBridge = typeof courtProof;
