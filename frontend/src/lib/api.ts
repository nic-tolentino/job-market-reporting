import { mockCompanyLeaderboard } from './mockData';
import type { CompanyLeaderboard } from './mockData';

/**
 * Finds a company by its ID slug.
 * In a real app, this would be an async fetch from the Spring Boot API.
 */
export const getCompanyById = (id: string | undefined): CompanyLeaderboard => {
    if (!id) return { id: '', name: 'Unknown Company', logo: '?', activeRoles: 0 };

    // Normalize ID for lookup
    const searchId = id.toLowerCase();
    const found = mockCompanyLeaderboard.find(c => c.id === searchId);

    if (found) return found;

    // Fallback logic for when we haven't integrated the real API yet
    return {
        id: id,
        name: id.split('-').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' '),
        logo: id.charAt(0).toUpperCase(),
        activeRoles: 0
    };
};

/**
 * Normalizes tech ID for display
 */
export const formatTechName = (techId: string | undefined): string => {
    if (!techId) return 'Technology';
    return techId.charAt(0).toUpperCase() + techId.slice(1);
};
