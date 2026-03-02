import { Users, Calendar, Terminal } from 'lucide-react';
import { H2 } from '../ui/Typography';
import { ResourceCard } from './ResourceCard';
import { COMMUNITY_RESOURCES } from '../../constants/techResources';

interface CommunityTabProps {
    techId: string;
    techName: string;
}

export const CommunityTab = ({ techId, techName }: CommunityTabProps) => {
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
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 animate-in fade-in duration-500">
            <ResourceCard icon={Users} title="Local Meetups" items={resources.meetups} />
            <ResourceCard icon={Calendar} title="Upcoming Events" items={resources.events} />
            <ResourceCard icon={Terminal} title="Local Open Source" items={resources.localProjects} />
            <ResourceCard icon={Users} title="Tech Celebrities" items={resources.celebrities} />
        </div>
    );
};
