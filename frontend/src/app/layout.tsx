import type { Metadata } from "next";
import "./globals.css";
import { AuthProvider } from "src/app/contexts/AuthContext";
import { BuildRefresh } from "src/components/shared/BuildRefresh";
import { ErrorBoundary } from "src/components/shared/ErrorBoundary";
import { GlobalErrorHandler } from "src/components/shared/GlobalErrorHandler";

export const metadata: Metadata = {
  title: "Books Claro",
  description: "Portal Books Claro — Claro / Logística",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="pt-BR" className="h-full antialiased">
      <body className="flex min-h-full flex-col">
        <AuthProvider>
          <BuildRefresh />
          <GlobalErrorHandler />
          <ErrorBoundary>{children}</ErrorBoundary>
        </AuthProvider>
      </body>
    </html>
  );
}
