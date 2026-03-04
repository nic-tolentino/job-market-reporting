import { Users, Calendar, Terminal, MapPin } from 'lucide-react';
import { H2 } from '../ui/Typography';
import { ResourceCard, WideResourceCard } from './ResourceCard';
import { COMMUNITY_RESOURCES } from '../../constants/techResources';
import { SpotlightSection } from './SpotlightSection';
import { SubmitResourceModal } from '../common/SubmitResourceModal';
import { useState } from 'react';

interface CommunityTabProps {
    techId: string;
    techName: string;
}

export const CommunityTab = ({ techId, techName }: CommunityTabProps) => {
    const [isSubmitModalOpen, setIsSubmitModalOpen] = useState(false);
    const resources = COMMUNITY_RESOURCES[techId.toLowerCase()];

    if (!resources) {
        return (
            <div className="py-20 text-center animate-in fade-in duration-500">
                <div className="mx-auto w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
                    <Users className="h-8 w-8 text-gray-400" />
                </div>
                <H2>Community Hub Coming Soon</H2>
                <p className="text-gray-500 mt-2">We're gathering local meetups and community info for {techName}.</p>
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
                    className="bg-white shadow-sm"
                />
                <ResourceCard
                    id="local-communities"
                    icon={MapPin}
                    title="Local Communities"
                    items={resources.communities}
                    className="bg-white shadow-sm"
                />
            </div>

            {/* Featured Wide Section: Projects */}
            <WideResourceCard
                id="local-projects"
                icon={Terminal}
                title="NZ Open Source Projects"
                items={resources.localProjects}
                subtitle="Local Contributions"
                className="bg-white border-blue-50/50 shadow-sm"
            />

            {/* Middle Section: Community Spotlight Gallery */}
            <SpotlightSection
                id="local-experts"
                title="Community Spotlight"
                items={resources.localExperts}
            />

            {/* Bottom: Regional Connectivity */}
            <div className="bg-gradient-to-r from-blue-50/50 to-indigo-50/50 rounded-3xl p-8 border border-white flex flex-col md:flex-row items-center justify-between gap-6 shadow-sm">
                <div className="max-w-md text-center md:text-left">
                    <h3 className="text-xl font-bold text-slate-800">Missed a local group?</h3>
                    <p className="text-slate-500 mt-1 text-sm">We're constantly expanding our NZ tech directory. If you know a community in Wellington, Christchurch or Dunedin, let us know!</p>
                </div>
                <button
                    onClick={() => setIsSubmitModalOpen(true)}
                    className="bg-white text-blue-600 px-6 py-3 rounded-2xl font-bold shadow-[0_4px_14px_rgba(0,0,0,0.05)] hover:shadow-[0_4px_20px_rgba(59,130,246,0.15)] hover:scale-[1.02] transition-all border border-blue-50 whitespace-nowrap active:scale-95"
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
