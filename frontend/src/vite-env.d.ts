/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Optional absolute backend base URL (e.g. https://api.example.com/api).
   *  When unset, the app calls the same-origin "/api" path (Netlify/nginx/Vite proxy). */
  readonly VITE_API_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
