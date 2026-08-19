import * as FileSystem from "expo-file-system/legacy";
import * as Sharing from "expo-sharing";

import { readSession } from "../storage/session";
import type { DocumentoResponse } from "../types/api";
import type { DownloadProgressSnapshot } from "../types/download";
import { API_BASE_URL, requireApiUrl } from "./api";

const DOWNLOAD_DIRECTORY_NAME = "book-dinamico-downloads";
const STORAGE_MARGIN_BYTES = 20 * 1024 * 1024;

type PreparedDocumentDownload = {
  start: () => Promise<string>;
  cancel: () => Promise<void>;
  share: (uri: string) => Promise<void>;
  cleanup: () => Promise<void>;
};

export async function prepareDocumentDownload(
  document: DocumentoResponse,
  onProgress: (progress: DownloadProgressSnapshot) => void,
): Promise<PreparedDocumentDownload> {
  requireApiUrl();

  const session = await readSession();
  if (!session) throw new SessionExpiredError();
  if (!FileSystem.cacheDirectory) {
    throw new DocumentDownloadError("Armazenamento temporário indisponível.");
  }
  if (!(await Sharing.isAvailableAsync())) {
    throw new DocumentDownloadError(
      "Não há um aplicativo disponível para abrir ou salvar o documento.",
    );
  }

  await assertAvailableStorage(document.tamanhoBytes);

  const directory = `${FileSystem.cacheDirectory}${DOWNLOAD_DIRECTORY_NAME}/`;
  await FileSystem.makeDirectoryAsync(directory, { intermediates: true });

  const filename = `documento_${document.id}_${sanitizeFilename(document.nome)}.${document.extensao.toLowerCase()}`;
  const target = `${directory}${filename}`;
  await removeTemporaryFile(target);

  const download = FileSystem.createDownloadResumable(
    `${API_BASE_URL}/api/documentos/${document.id}/download`,
    target,
    {
      headers: {
        Authorization: `Bearer ${session.token}`,
        "X-Client-Type": "mobile",
      },
    },
    ({ totalBytesExpectedToWrite, totalBytesWritten }) => {
      onProgress({
        bytesWritten: totalBytesWritten,
        totalBytes:
          totalBytesExpectedToWrite > 0
            ? totalBytesExpectedToWrite
            : document.tamanhoBytes,
      });
    },
  );

  return {
    start: async () => {
      try {
        const result = await download.downloadAsync();
        if (!result) throw new DownloadCancelledError();
        if (result.status === 401) throw new SessionExpiredError();
        if (result.status < 200 || result.status >= 300) {
          throw new DocumentDownloadError(
            `O servidor recusou o download (código ${result.status}).`,
          );
        }

        await assertDownloadedFile(result.uri, document.tamanhoBytes);
        return result.uri;
      } catch (error) {
        throw normalizeDownloadError(error);
      }
    },
    cancel: async () => {
      try {
        await download.cancelAsync();
      } catch {
        // A tarefa pode já ter atingido um estado terminal.
      }
    },
    share: async (uri) => {
      try {
        await Sharing.shareAsync(uri, {
          dialogTitle: `Abrir ou compartilhar ${document.nome}`,
          mimeType: mimeType(document.extensao),
        });
      } catch {
        throw new DocumentDownloadError(
          "O download foi concluído, mas não foi possível abrir as opções do dispositivo.",
        );
      }
    },
    cleanup: () => removeTemporaryFile(target),
  };
}

export class DocumentDownloadError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "DocumentDownloadError";
  }
}

export class SessionExpiredError extends DocumentDownloadError {
  constructor() {
    super("Sessão expirada. Entre novamente.");
    this.name = "SessionExpiredError";
  }
}

export class DownloadCancelledError extends DocumentDownloadError {
  constructor() {
    super("Download cancelado.");
    this.name = "DownloadCancelledError";
  }
}

async function assertAvailableStorage(fileSize: number) {
  try {
    const freeBytes = await FileSystem.getFreeDiskStorageAsync();
    if (freeBytes < fileSize + STORAGE_MARGIN_BYTES) {
      throw new DocumentDownloadError(
        "Espaço insuficiente no dispositivo para baixar este documento.",
      );
    }
  } catch (error) {
    if (error instanceof DocumentDownloadError) throw error;
    // A consulta de espaço pode não estar disponível em todos os dispositivos.
  }
}

async function assertDownloadedFile(uri: string, expectedSize: number) {
  const info = await FileSystem.getInfoAsync(uri);
  if (!info.exists) {
    throw new DocumentDownloadError("O arquivo baixado não foi encontrado.");
  }

  const actualSize = "size" in info ? info.size : undefined;
  if (
    expectedSize > 0 &&
    typeof actualSize === "number" &&
    actualSize !== expectedSize
  ) {
    throw new DocumentDownloadError(
      "O download ficou incompleto. Verifique sua conexão e tente novamente.",
    );
  }
}

async function removeTemporaryFile(uri: string) {
  try {
    await FileSystem.deleteAsync(uri, { idempotent: true });
  } catch {
    // Arquivos no cache também são elegíveis à limpeza pelo sistema operacional.
  }
}

function normalizeDownloadError(error: unknown) {
  if (error instanceof DocumentDownloadError) return error;

  const message = error instanceof Error ? error.message : "";
  if (/space|ENOSPC|no space/i.test(message)) {
    return new DocumentDownloadError(
      "Espaço insuficiente no dispositivo para concluir o download.",
    );
  }
  if (/network|socket|connect|timeout|unable to download/i.test(message)) {
    return new DocumentDownloadError(
      "A conexão foi interrompida durante o download. Tente novamente.",
    );
  }
  return new DocumentDownloadError(
    "Não foi possível concluir o download. Tente novamente.",
  );
}

function sanitizeFilename(name: string) {
  return name.trim().replace(/[^a-zA-Z0-9._-]+/g, "_") || "documento";
}

function mimeType(extension: DocumentoResponse["extensao"]) {
  const mimeTypes: Record<DocumentoResponse["extensao"], string> = {
    XLSM: "application/vnd.ms-excel.sheet.macroEnabled.12",
    XLSX: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    XLSB: "application/vnd.ms-excel.sheet.binary.macroEnabled.12",
    XLTX: "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
    XLTM: "application/vnd.ms-excel.template.macroEnabled.12",
    PPTX: "application/vnd.openxmlformats-officedocument.presentationml.presentation",
  };
  return mimeTypes[extension];
}
