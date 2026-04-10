import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

const useAuthStore = create(
  persist(
    (set) => ({
      token: null,
      nickname: null,
      username: null,
      isLoggedIn: false,
      _hasHydrated: false,
      setHasHydrated: (state) => set({ _hasHydrated: state }),
      login: (token, nickname, username) => set({ token, nickname, username, isLoggedIn: true }),
      logout: () => set({ token: null, nickname: null, username: null, isLoggedIn: false }),
    }),
    {
      name: 'auth-storage', 
      storage: createJSONStorage(() => localStorage),
      onRehydrateStorage: () => (state) => {
        state.setHasHydrated(true);
      }
    }
  )
);

export default useAuthStore;
