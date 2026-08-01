const PROFILE_PHOTO_PREFIX = 'securebank.profile-photo.';

const getStorageKey = (userId: string) => `${PROFILE_PHOTO_PREFIX}${userId}`;

export const getProfilePhoto = (userId?: string | null) => {
  if (!userId || typeof window === 'undefined') {
    return null;
  }

  return window.localStorage.getItem(getStorageKey(userId));
};

export const saveProfilePhoto = (userId: string, photoDataUrl: string) => {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.setItem(getStorageKey(userId), photoDataUrl);
};
