import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        medical: {
          blue: "#1a56db",
          "blue-dark": "#1e429f",
          "blue-light": "#ebf5ff",
          green: "#0e9f6e",
          "green-light": "#f3faf7",
          red: "#e02424",
          "red-light": "#fdf2f2",
          yellow: "#c27803",
          "yellow-light": "#fdfdea",
          gray: "#6b7280",
          "gray-light": "#f9fafb",
        },
      },
      typography: {
        DEFAULT: {
          css: {
            maxWidth: "none",
          },
        },
      },
    },
  },
  plugins: [],
};
export default config;
