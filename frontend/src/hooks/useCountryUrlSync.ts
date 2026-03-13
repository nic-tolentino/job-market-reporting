import { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAppStore } from '../store/useAppStore';

/**
 * A hook that synchronizes the selected country in the application store
 * with the 'country' query parameter in the URL.
 * 
 * - If the URL has a country, it updates the store.
 * - If the store changes (e.g., via Navbar), it updates the URL.
 * - If no country is in the URL, it sets it from the store.
 */
export function useCountryUrlSync() {
    const [searchParams, setSearchParams] = useSearchParams();
    const { selectedCountry, setSelectedCountry } = useAppStore();

    // URL → Store: only run when URL changes
    useEffect(() => {
        const urlCountry = searchParams.get('country');
        if (urlCountry && urlCountry !== selectedCountry) {
            setSelectedCountry(urlCountry);
        } else if (!urlCountry && selectedCountry) {
            // Ensure country is always in the URL
            const newParams = new URLSearchParams(searchParams);
            newParams.set('country', selectedCountry);
            setSearchParams(newParams, { replace: true });
        }
    }, [searchParams]); // removed selectedCountry from dependencies

    // Store → URL: update URL when store changes (e.g., from Navbar dropdown)
    useEffect(() => {
        const urlCountry = searchParams.get('country');
        if (selectedCountry && selectedCountry !== urlCountry) {
            const newParams = new URLSearchParams(searchParams);
            newParams.set('country', selectedCountry);
            setSearchParams(newParams, { replace: true });
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedCountry, setSearchParams]); 
}
