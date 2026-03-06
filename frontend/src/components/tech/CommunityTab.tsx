import { Users, Calendar, Terminal, MapPin } from 'lucide-react';
import { H2 } from '../ui/Typography';
import { ResourceCard, WideResourceCard } from './ResourceCard';
import { COMMUNITY_RESOURCES } from '../../constants/techResources';
import { SpotlightSection } from './SpotlightSection';
import { SubmitResourceModal } from '../common/SubmitResourceModal';
import { useState, useMemo } from 'react';
import { useAppStore } from '../../store/useAppStore';

interface CommunityTabProps {
    techId: string;
    techName: string;
}

export const CommunityTab = ({ techId, techName }: CommunityTabProps) => {
    const [isSubmitModalOpen, setIsSubmitModalOpen] = useState(false);
    const { selectedCountry } = useAppStore();
    const rawResources = COMMUNITY_RESOURCES[techId.toLowerCase()];

    const resources = useMemo(() => {
        if (!rawResources) return null;
        const filterFn = (items: any[]) => items.filter(item => 
            !item.countries || 
            item.countries.includes(selectedCountry) || 
            item.countries.includes('Global')
        );

        return {
            communities: filterFn(rawResources.communities),
            upcomingEvents: filterFn(rawResources.upcomingEvents),
            localProjects: filterFn(rawResources.localProjects),
            localExperts: filterFn(rawResources.localExperts),
        };
    }, [rawResources, selectedCountry]);

    if (!resources) {
        return (
            <div className="py-20 text-center animate-in fade-in duration-500">
                <div className="mx-auto w-16 h-16 bg-inset rounded-full flex items-center justify-center mb-4">
                    <Users className="h-8 w-8 text-muted" />
                </div>
                <H2>Community Hub Coming Soon</H2>
                <p className="text-muted mt-2">We're gathering local meetups and community info for {techName}.</p>
            </div>
        );
    }

    return (
        <div className="space-y-12 animate-in fade-in slide-in-from-bottom-4 duration-700">


            {/* Primary Grid: Events & Communities side by side */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                <ResourceCard
                    id="local-events"
                    icon={Calendar}
                    title="Upcoming Tech Events"
                    items={resources.upcomingEvents}
                    className="shadow-theme-sm"
                />
                <ResourceCard
                    id="local-communities"
                    icon={MapPin}
                    title="Local Communities"
                    items={resources.communities}
                    className="shadow-theme-sm"
                />
            </div>

            {/* Featured Wide Section: Projects */}
            <WideResourceCard
                id="local-projects"
                icon={Terminal}
                title={`${selectedCountry} Open Source Projects`}
                items={resources.localProjects}
                subtitle="Local Contributions"
                className="border-accent-subtle shadow-theme-sm"
            />

            {/* Middle Section: Community Spotlight Gallery */}
            <SpotlightSection
                id="local-experts"
                title="Community Spotlight"
                items={resources.localExperts}
            />

            {/* Bottom: Regional Connectivity */}
            <div className="banner-cta rounded-3xl p-8 border flex flex-col md:flex-row items-center justify-between gap-6 shadow-theme-sm">
                <div className="max-w-md text-center md:text-left">
                    <h3 className="text-xl font-bold text-primary">Missed a local group?</h3>
                    <p className="text-muted mt-1 text-sm">We're constantly expanding our {selectedCountry} tech directory. If you know a community you'd like to see here, let us know!</p>
                </div>
                <button
                    onClick={() => setIsSubmitModalOpen(true)}
                    className="bg-card text-accent px-6 py-3 rounded-2xl font-bold shadow-[0_4px_14px_rgba(0,0,0,0.05)] hover:shadow-[0_4px_20px_rgba(59,130,246,0.15)] hover:scale-[1.02] transition-all border border-accent-subtle whitespace-nowrap active:scale-95"
                >
                    Suggest a Resource
                </button>
            </div>

            <SubmitResourceModal
                isOpen={isSubmitModalOpen}
                onClose={() => setIsSubmitModalOpen(false)}
                techName={techName}
                contextType="Community"
            />
        </div>
    );
};
