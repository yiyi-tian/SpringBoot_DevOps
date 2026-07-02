const STORAGE_KEY = 'devops_device_id';

/** @returns {string} Stable client device identifier for multi-device login tracking */
export function getOrCreateDeviceId() {
  let id = localStorage.getItem(STORAGE_KEY);
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem(STORAGE_KEY, id);
  }
  return id;
}
