import { useCallback, useEffect, useRef, useState } from "react";

import {
  DocumentDownloadError,
  DownloadCancelledError,
  prepareDocumentDownload,
} from "../services/document-download-service";
import type { DocumentoResponse } from "../types/api";
import type {
  DownloadOutcome,
  DownloadProgressSnapshot,
  DownloadState,
} from "../types/download";

const INITIAL_STATE: DownloadState = {
  status: "idle",
  documentId: null,
  documentName: null,
  bytesWritten: 0,
  totalBytes: 0,
  progress: null,
  message: null,
};

export function useDocumentDownload() {
  const [state, setState] = useState<DownloadState>(INITIAL_STATE);
  const mountedRef = useRef(true);
  const inFlightRef = useRef(false);
  const cancellationRequestedRef = useRef(false);
  const lastProgressRef = useRef(-1);
  const taskRef = useRef<Awaited<
    ReturnType<typeof prepareDocumentDownload>
  > | null>(null);

  const updateState = useCallback((nextState: DownloadState) => {
    if (mountedRef.current) setState(nextState);
  }, []);

  const updateCancelledState = useCallback(
    (document: DocumentoResponse) => {
      updateState({
        status: "cancelled",
        documentId: document.id,
        documentName: document.nome,
        bytesWritten: 0,
        totalBytes: document.tamanhoBytes,
        progress: null,
        message: "Download cancelado.",
      });
    },
    [updateState],
  );

  const start = useCallback(
    async (document: DocumentoResponse): Promise<DownloadOutcome> => {
      if (inFlightRef.current) return { status: "ignored" };

      inFlightRef.current = true;
      cancellationRequestedRef.current = false;
      lastProgressRef.current = -1;
      updateState({
        status: "preparing",
        documentId: document.id,
        documentName: document.nome,
        bytesWritten: 0,
        totalBytes: document.tamanhoBytes,
        progress: 0,
        message: "Preparando download…",
      });

      const onProgress = ({
        bytesWritten,
        totalBytes,
      }: DownloadProgressSnapshot) => {
        const progress =
          totalBytes > 0
            ? Math.min(1, Math.max(0, bytesWritten / totalBytes))
            : null;
        const roundedProgress = progress == null ? -1 : Math.round(progress * 100);
        if (roundedProgress === lastProgressRef.current) return;
        lastProgressRef.current = roundedProgress;

        updateState({
          status: "downloading",
          documentId: document.id,
          documentName: document.nome,
          bytesWritten,
          totalBytes,
          progress,
          message:
            progress == null
              ? "Baixando documento…"
              : `Baixando… ${roundedProgress}%`,
        });
      };

      try {
        const task = await prepareDocumentDownload(document, onProgress);
        taskRef.current = task;

        if (cancellationRequestedRef.current) {
          updateCancelledState(document);
          return { status: "cancelled" };
        }

        updateState({
          status: "downloading",
          documentId: document.id,
          documentName: document.nome,
          bytesWritten: 0,
          totalBytes: document.tamanhoBytes,
          progress: 0,
          message: "Baixando… 0%",
        });

        const uri = await task.start();
        if (cancellationRequestedRef.current) {
          updateCancelledState(document);
          return { status: "cancelled" };
        }

        updateState({
          status: "sharing",
          documentId: document.id,
          documentName: document.nome,
          bytesWritten: document.tamanhoBytes,
          totalBytes: document.tamanhoBytes,
          progress: 1,
          message: "Download concluído. Abrindo opções…",
        });
        await task.share(uri);

        updateState({
          status: "success",
          documentId: document.id,
          documentName: document.nome,
          bytesWritten: document.tamanhoBytes,
          totalBytes: document.tamanhoBytes,
          progress: 1,
          message: "Download concluído.",
        });
        return { status: "completed" };
      } catch (error) {
        if (
          cancellationRequestedRef.current ||
          error instanceof DownloadCancelledError
        ) {
          updateCancelledState(document);
          return { status: "cancelled" };
        }

        const normalizedError =
          error instanceof DocumentDownloadError
            ? error
            : new DocumentDownloadError(
                "Não foi possível preparar o download. Tente novamente.",
              );
        updateState({
          status: "error",
          documentId: document.id,
          documentName: document.nome,
          bytesWritten: 0,
          totalBytes: document.tamanhoBytes,
          progress: null,
          message: normalizedError.message,
        });
        return { status: "failed", error: normalizedError };
      } finally {
        await taskRef.current?.cleanup();
        taskRef.current = null;
        inFlightRef.current = false;
        cancellationRequestedRef.current = false;
      }
    },
    [updateCancelledState, updateState],
  );

  const cancel = useCallback(async () => {
    if (!inFlightRef.current) return;
    cancellationRequestedRef.current = true;
    setState((current) => ({
      ...current,
      status: "cancelling",
      message: "Cancelando download…",
    }));
    await taskRef.current?.cancel();
  }, []);

  const reset = useCallback(() => {
    if (!inFlightRef.current && mountedRef.current) setState(INITIAL_STATE);
  }, []);

  useEffect(() => {
    return () => {
      mountedRef.current = false;
      cancellationRequestedRef.current = true;
      void taskRef.current?.cancel();
    };
  }, []);

  const isBusy =
    state.status === "preparing" ||
    state.status === "downloading" ||
    state.status === "cancelling" ||
    state.status === "sharing";

  return { cancel, isBusy, reset, start, state };
}
