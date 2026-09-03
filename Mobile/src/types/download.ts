export type DownloadStatus =
  | "idle"
  | "preparing"
  | "downloading"
  | "cancelling"
  | "sharing"
  | "success"
  | "cancelled"
  | "error";

export type DownloadState = {
  status: DownloadStatus;
  documentId: number | null;
  documentName: string | null;
  bytesWritten: number;
  totalBytes: number;
  progress: number | null;
  message: string | null;
};

export type DownloadOutcome =
  | { status: "completed" }
  | { status: "cancelled" }
  | { status: "ignored" }
  | { status: "failed"; error: Error };

export type DownloadProgressSnapshot = {
  bytesWritten: number;
  totalBytes: number;
};
