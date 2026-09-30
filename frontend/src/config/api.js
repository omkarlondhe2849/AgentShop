// Central API base URL configuration.
// In development, Vite's proxy handles /api → localhost:8080.
// In production, VITE_API_URL points to the deployed backend.
const API_URL = import.meta.env.VITE_API_URL || '';

export default API_URL;
