import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "GP Diagnostik | Klinisk Beslutningsstøtte",
  description:
    "AI-assisteret diagnostisk støtteværktøj til praktiserende læger baseret på PLO og DSAM retningslinjer",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="da">
      <body style={{ fontFamily: "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif" }}>
        {children}
      </body>
    </html>
  );
}
