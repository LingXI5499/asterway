import { api } from "./http";

export function query(values = {}) {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== "" && value !== undefined && value !== null) params.set(key, value);
  }
  return params.size ? `?${params}` : "";
}

export const workspaceApi = {
  get: (path, params, signal) => api(`/agent/${path}${query(params)}`, { signal }),
  post: (path, body) => api(`/agent/${path}`, { method: "POST", body }),
  patch: (path, body) => api(`/agent/${path}`, { method: "PATCH", body }),
  remove: (path, version) => api(`/agent/${path}${query({ version })}`, { method: "DELETE" }),
  upload: (file, { documentId, version, title } = {}) => api(
    `/agent/knowledge${documentId ? `/${documentId}/content` : ""}${query({ fileName: file.name, version, title })}`,
    { method: documentId ? "PUT" : "POST", rawBody: file,
      headers: { "Content-Type": "application/octet-stream", "X-File-Type": file.type || "application/octet-stream" } },
  ),
};

export const timeText = (value) => value ? new Date(value).toLocaleString() : "—";
