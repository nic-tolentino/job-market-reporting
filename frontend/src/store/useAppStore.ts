import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AppState {
    selectedCountry: string;
    setSelectedCountry: (country: string) => void;
}

export const useAppStore = create<AppState>()(
    persist(
        (set) => ({
            selectedCountry: 'NZ',
            setSelectedCountry: (country: string) => set({ selectedCountry: country }),
        }),
        {
            name: 'dev-assembly-storage',
        }
    )
);
