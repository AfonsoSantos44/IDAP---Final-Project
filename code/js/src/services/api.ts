const API_BASE_URL = "/api";

class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

export async function fetchApi<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  await delay(1000);
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    credentials: 'include', 
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  });

  if (!response.ok) {
    const error = await response
      .json()
      .catch(() => ({ title: "Unknown error" }));
    const errorMessage = response.statusText;
    throw new ApiError(response.status, errorMessage);
  }

  if (response.status === 204 || response.headers.get("content-length") === "0" ) {
    return undefined as T;
  }

  return response.json();
}

/**
 * Upload helper for multipart/form-data requests (e.g. image uploads).
 * Deliberately does NOT set Content-Type so the browser adds the multipart boundary.
 */
export async function uploadApi<T>(
  endpoint: string,
  formData: FormData,
  method: string = "PUT"
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method,
    credentials: "include",
    body: formData,
  });

  if (!response.ok) {
    await response.json().catch(() => ({ title: "Unknown error" }));
    throw new ApiError(response.status, response.statusText);
  }

  if (response.status === 204 || response.headers.get("content-length") === "0") {
    return undefined as T;
  }

  return response.json();
}

export { ApiError, API_BASE_URL };

/*********************************************
 * Auxiliary Functions emulating IO latency
 */
function delay(delayInMs: number) {
  return new Promise((resolve) => {
    setTimeout(() => resolve(undefined), delayInMs);
  });
}