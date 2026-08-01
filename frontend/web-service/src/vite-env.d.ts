/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Overrides the user-service base URL. Defaults to the gateway-proxied `/api/v1/users`. */
  readonly VITE_USER_API_BASE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
