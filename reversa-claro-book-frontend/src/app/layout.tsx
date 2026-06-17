import type { Metadata } from "next";
import "./globals.css";
import { Providers } from "./contexts/providers";

export const metadata: Metadata = {
  title: "Book Dinâmico Claro",
  description: "Portal de Book Dinâmico — Claro / Logística",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR" className="h-full antialiased">
      <body className="min-h-full flex flex-col">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
