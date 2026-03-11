import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Plane } from 'lucide-react';
import { Badge } from '../components/ui/Badge';
import CompanyLogo from '../components/common/CompanyLogo';
import { useAppStore } from '../store/useAppStore';
import { fetchCompanyListing, type CompanyListingItemDto } from '../lib/api';
import PageLoader from '../components/common/PageLoader';
import ErrorState from '../components/common/ErrorState';

export default function CompaniesPage() {
    const { selectedCountry } = useAppStore();
    const [visaSponsorshipOnly, setVisaSponsorshipOnly] = useState(false);
    const [companies, setCompanies] = useState<CompanyListingItemDto[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(false);

    useEffect(() => {
        setIsLoading(true);
        setError(false);
        fetchCompanyListing(visaSponsorshipOnly, selectedCountry)
            .then(data => {
                setCompanies(data);
            })
            .catch(err => {
                console.error('Failed to fetch companies:', err);
                setError(true);
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, [selectedCountry, visaSponsorshipOnly]);

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-bold">Companies</h1>
                <label className="flex items-center gap-2 text-sm font-medium text-secondary cursor-pointer">
                    <input
                        type="checkbox"
                        checked={visaSponsorshipOnly}
                        onChange={(e) => setVisaSponsorshipOnly(e.target.checked)}
                        className="rounded border-border text-accent focus:ring-accent"
                    />
                    Visa sponsors only
                </label>
            </div>
            
            {isLoading ? (
                <PageLoader />
            ) : error ? (
                <ErrorState message="Failed to load companies." onRetry={() => {
                    setIsLoading(true);
                    setError(false);
                    fetchCompanyListing(visaSponsorshipOnly, selectedCountry)
                        .then(setCompanies)
                        .catch(() => setError(true))
                        .finally(() => setIsLoading(false));
                }} />
            ) : companies.length === 0 ? (
                <p className="text-secondary text-center py-8">No companies found.</p>
            ) : (
                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                    {companies.map((company) => (
                        <Link 
                            key={company.id} 
                            to={`/company/${company.id}`}
                            className="block p-4 border border-border rounded-lg bg-card hover:border-accent transition-colors group"
                        >
                            <div className="flex items-start gap-4">
                                <CompanyLogo 
                                    logoUrl={company.logo} 
                                    companyName={company.name}
                                    className="w-12 h-12 rounded-md bg-white shrink-0" 
                                    imageClassName="p-1"
                                />
                                <div className="flex-1 min-w-0">
                                    <h2 className="font-semibold text-primary truncate group-hover:text-accent transition-colors">
                                        {company.name}
                                    </h2>
                                    <div className="text-sm text-secondary mt-1">
                                        {company.activeRoles} active role{company.activeRoles === 1 ? '' : 's'}
                                    </div>
                                    {company.visaSponsorship?.offered && (
                                        <div className="mt-2">
                                            <Badge variant="blue" className="inline-flex items-center gap-1">
                                                <Plane className="w-3 h-3" />
                                                Visa Sponsor
                                            </Badge>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </Link>
                    ))}
                </div>
            )}
        </div>
    );
}
