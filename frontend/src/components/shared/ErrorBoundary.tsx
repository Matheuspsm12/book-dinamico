"use client";

import { Component, type ErrorInfo, type ReactNode } from "react";
import { logClientEvent } from "src/lib/client-log";

type Props = {
  children: ReactNode;
};

type State = {
  hasError: boolean;
};

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    logClientEvent(
      "app-error",
      {
        tipo: "react-error-boundary",
        message: error.message ?? "unknown",
        stack: info.componentStack ?? "",
      },
      "error",
    );
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex min-h-full flex-1 flex-col items-center justify-center gap-3 px-6 text-center">
          <p className="text-lg font-semibold text-zinc-800">
            Ocorreu um erro inesperado.
          </p>
          <p className="text-sm text-zinc-500">
            Recarregue a página. Se o problema persistir, contate o
            administrador.
          </p>
          <button
            onClick={() => window.location.reload()}
            className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-semibold text-white hover:bg-zinc-700"
          >
            Recarregar
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}